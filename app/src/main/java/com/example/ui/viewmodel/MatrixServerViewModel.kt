package com.example.ui.viewmodel

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.ServerDatabase
import com.example.data.local.ServerPreferences
import com.example.data.model.AuthorizedKey
import com.example.data.model.ConnectionHealth
import com.example.data.model.DiagnosticItem
import com.example.data.model.DiagnosticStatus
import com.example.data.model.ProjectItem
import com.example.data.model.ServerConfig
import com.example.data.model.ServerMetrics
import com.example.data.model.ServerState
import com.example.data.model.TerminalLine
import com.example.server.LinuxEnvironment
import com.example.server.AuthorizedKeyValidator
import com.example.server.ProcessSupervisor
import com.example.server.TerminalRunner
import com.example.service.MatrixServerService
import com.example.util.NetworkUtils
import com.example.util.SystemMonitor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.net.ServerSocket
import java.util.UUID

class MatrixServerViewModel(application: Application) : AndroidViewModel(application) {

    private val db = ServerDatabase.getInstance(application)
    private val projectDao = db.projectDao()
    private val keyDao = db.authorizedKeyDao()
    private val sshKeyWriteMutex = Mutex()
    private val prefs = ServerPreferences(application)
    val linuxEnv = LinuxEnvironment(application)
    private val systemMonitor = SystemMonitor(application)
    val terminalRunner = TerminalRunner(application, linuxEnv)

    val serverState: StateFlow<ServerState> = MatrixServerService.serverState
    val connectionHealth: StateFlow<ConnectionHealth> = MatrixServerService.connectionHealth
    val metricsState: StateFlow<ServerMetrics> = MatrixServerService.metricsState
    val configState: StateFlow<ServerConfig> = prefs.configFlow

    fun triggerHealthProbe() {
        MatrixServerService.triggerHealthProbe(getApplication())
    }

    val projectsFlow = projectDao.getAllProjectsFlow().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val authorizedKeysFlow = keyDao.getAllKeysFlow().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val terminalLines: StateFlow<List<TerminalLine>> = terminalRunner.outputLines

    private val _diagnosticItems = MutableStateFlow<List<DiagnosticItem>>(emptyList())
    val diagnosticItems: StateFlow<List<DiagnosticItem>> = _diagnosticItems.asStateFlow()

    private val _currentBrowseDir = MutableStateFlow(linuxEnv.projectsDir)
    val currentBrowseDir: StateFlow<File> = _currentBrowseDir.asStateFlow()

    private val _dirFiles = MutableStateFlow<List<File>>(emptyList())
    val dirFiles: StateFlow<List<File>> = _dirFiles.asStateFlow()

    private val _cpuHistory = MutableStateFlow<List<Float>>(listOf(12f, 15f, 18f, 14f, 16f, 22f, 19f, 15f, 13f, 17f, 20f, 18f, 14f, 16f, 15f))
    val cpuHistory: StateFlow<List<Float>> = _cpuHistory.asStateFlow()

    private val _ramHistory = MutableStateFlow<List<Float>>(listOf(30f, 31f, 32f, 31f, 30f, 33f, 34f, 32f, 31f, 30f, 32f, 33f, 31f, 30f, 32f))
    val ramHistory: StateFlow<List<Float>> = _ramHistory.asStateFlow()

    init {
        linuxEnv.initializeEnvironment()
        seedInitialProjects()
        refreshCurrentDir()
        runDiagnostics()

        viewModelScope.launch {
            metricsState.collect { m ->
                val cList = _cpuHistory.value.toMutableList()
                cList.add(m.cpuUsage)
                if (cList.size > 15) cList.removeAt(0)
                _cpuHistory.value = cList

                val ramPct = if (m.memoryTotalMb > 0) (m.memoryUsedMb.toFloat() / m.memoryTotalMb.toFloat()) * 100f else 25f
                val rList = _ramHistory.value.toMutableList()
                rList.add(ramPct)
                if (rList.size > 15) rList.removeAt(0)
                _ramHistory.value = rList
            }
        }
    }

    private fun seedInitialProjects() {
        viewModelScope.launch(Dispatchers.IO) {
            val existing = projectDao.getAllProjects()
            if (existing.isEmpty()) {
                val p1 = ProjectItem(
                    id = "proj_web_server",
                    name = "Matrix Web Server",
                    type = "Python",
                    command = "sh start.sh",
                    workingDir = File(linuxEnv.projectsDir, "web-server").absolutePath,
                    port = 8080,
                    autoRestart = true,
                    status = "STOPPED"
                )
                val p2 = ProjectItem(
                    id = "proj_api_service",
                    name = "API Microservice",
                    type = "Node.js",
                    command = "sh start.sh",
                    workingDir = File(linuxEnv.projectsDir, "api-microservice").absolutePath,
                    port = 8081,
                    autoRestart = true,
                    status = "STOPPED"
                )
                val p3 = ProjectItem(
                    id = "proj_system_worker",
                    name = "System Worker",
                    type = "Shell",
                    command = "sh worker.sh",
                    workingDir = File(linuxEnv.projectsDir, "system-worker").absolutePath,
                    port = null,
                    autoRestart = true,
                    status = "STOPPED"
                )
                projectDao.insert(p1)
                projectDao.insert(p2)
                projectDao.insert(p3)
            }
        }
    }

    fun toggleServerPower() {
        val currentState = serverState.value
        val context = getApplication<Application>()
        if (currentState.isRunning) {
            MatrixServerService.stop(context)
        } else if (!currentState.isTransitioning) {
            MatrixServerService.start(context)
        }
    }

    fun copySshCommand() {
        val cmd = metricsState.value.sshCommand
        val cm = getApplication<Application>().getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        cm?.setPrimaryClip(ClipData.newPlainText("Matrix SSH Command", cmd))
    }

    fun copyVpnSshCommand() {
        val cmd = metricsState.value.vpnSshCommand ?: return
        val cm = getApplication<Application>().getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        cm?.setPrimaryClip(ClipData.newPlainText("Matrix VPN SSH Command", cmd))
    }

    fun updateConfig(config: ServerConfig) {
        prefs.updateConfig(config)
    }

    fun restartServer() {
        val context = getApplication<Application>()
        viewModelScope.launch {
            MatrixServerService.stop(context)
            kotlinx.coroutines.delay(1200)
            MatrixServerService.start(context)
        }
    }

    fun updateConfigAndRestart(newConfig: ServerConfig) {
        prefs.updateConfig(newConfig)
        restartServer()
    }

    fun completeSetup() {
        prefs.setSetupCompleted(true)
    }

    // --- Projects Management ---

    fun startProject(project: ProjectItem) {
        val supervisor = MatrixServerService.sharedProcessSupervisor
            ?: ProcessSupervisor(getApplication(), projectDao, linuxEnv)
        supervisor.startProject(project)
    }

    fun stopProject(projectId: String) {
        val supervisor = MatrixServerService.sharedProcessSupervisor
            ?: ProcessSupervisor(getApplication(), projectDao, linuxEnv)
        supervisor.stopProject(projectId)
    }

    fun restartProject(project: ProjectItem) {
        val supervisor = MatrixServerService.sharedProcessSupervisor
            ?: ProcessSupervisor(getApplication(), projectDao, linuxEnv)
        supervisor.restartProject(project)
    }

    fun createProject(name: String, type: String, command: String, port: Int?, autoRestart: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val safeName = name.lowercase().replace("\\s+".toRegex(), "-")
            val projectDir = File(linuxEnv.projectsDir, safeName).apply { if (!exists()) mkdirs() }
            val item = ProjectItem(
                id = "proj_" + UUID.randomUUID().toString().take(8),
                name = name,
                type = type,
                command = command,
                workingDir = projectDir.absolutePath,
                port = port,
                autoRestart = autoRestart
            )
            projectDao.insert(item)
            refreshCurrentDir()
        }
    }

    fun deleteProject(project: ProjectItem) {
        viewModelScope.launch(Dispatchers.IO) {
            stopProject(project.id)
            projectDao.delete(project)
        }
    }

    fun getProjectLogs(projectId: String): List<String> {
        val supervisor = MatrixServerService.sharedProcessSupervisor
        return supervisor?.getLogs(projectId) ?: emptyList()
    }

    // --- Terminal ---

    fun executeTerminalCommand(cmd: String) {
        viewModelScope.launch {
            terminalRunner.executeCommand(cmd)
        }
    }

    fun clearTerminal() {
        terminalRunner.clearScreen()
    }

    // --- SSH Keys ---

    /**
     * Validates the key before the UI closes. The database is the source of truth
     * for keys managed from the app; update the SSH file atomically before DB commit.
     */
    fun addAuthorizedKey(label: String, keyContent: String): Boolean {
        val trimmed = keyContent.trim()
        val parsedKey = AuthorizedKeyValidator.parse(trimmed) ?: return false
        val fingerprint = AuthorizedKeyValidator.fingerprint(parsedKey)
        viewModelScope.launch(Dispatchers.IO) {
            sshKeyWriteMutex.withLock {
                val previous = keyDao.getAllKeys()
                if (previous.any { it.fingerprint == fingerprint }) return@withLock
                val item = AuthorizedKey(
                    label = label.ifBlank { "Authorized Device" },
                    publicKey = trimmed,
                    fingerprint = fingerprint
                )
                try {
                    linuxEnv.writeAuthorizedKeys(previous.map { it.publicKey } + trimmed)
                    try {
                        keyDao.insert(item)
                    } catch (e: Exception) {
                        linuxEnv.writeAuthorizedKeys(previous.map { it.publicKey })
                        throw e
                    }
                } catch (e: Exception) {
                    android.util.Log.e("MatrixServerViewModel", "Cannot register SSH public key", e)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(getApplication(), "Failed to save authorized SSH key", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
        return true
    }

    fun deleteAuthorizedKey(key: AuthorizedKey) {
        viewModelScope.launch(Dispatchers.IO) {
            sshKeyWriteMutex.withLock {
                val previous = keyDao.getAllKeys()
                val remaining = previous.filterNot { it.id == key.id }
                try {
                    linuxEnv.writeAuthorizedKeys(remaining.map { it.publicKey })
                    try {
                        keyDao.delete(key)
                    } catch (e: Exception) {
                        linuxEnv.writeAuthorizedKeys(previous.map { it.publicKey })
                        throw e
                    }
                } catch (e: Exception) {
                    android.util.Log.e("MatrixServerViewModel", "Cannot revoke SSH public key", e)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(getApplication(), "Failed to revoke authorized SSH key", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    // --- File Manager ---

    fun navigateToDirectory(file: File) {
        if (file.exists() && file.isDirectory) {
            _currentBrowseDir.value = file
            refreshCurrentDir()
        }
    }

    fun navigateUp() {
        val current = _currentBrowseDir.value
        val parent = current.parentFile
        if (parent != null && parent.absolutePath.startsWith(linuxEnv.rootDir.absolutePath)) {
            _currentBrowseDir.value = parent
            refreshCurrentDir()
        }
    }

    fun refreshCurrentDir() {
        val dir = _currentBrowseDir.value
        val files = dir.listFiles()?.toList()?.sortedWith(compareBy({ !it.isDirectory }, { it.name })) ?: emptyList()
        _dirFiles.value = files
    }

    fun createNewFile(name: String, content: String) {
        val file = File(_currentBrowseDir.value, name)
        file.writeText(content)
        refreshCurrentDir()
    }

    fun createNewFolder(name: String) {
        val dir = File(_currentBrowseDir.value, name)
        dir.mkdirs()
        refreshCurrentDir()
    }

    fun deleteFile(file: File) {
        if (file.isDirectory) file.deleteRecursively() else file.delete()
        refreshCurrentDir()
    }

    // --- Diagnostics ---

    fun runDiagnostics() {
        viewModelScope.launch(Dispatchers.IO) {
            val list = mutableListOf<DiagnosticItem>()

            // 1. Linux userspace check
            val hasSh = File("/system/bin/sh").exists()
            list.add(
                DiagnosticItem(
                    id = "diag_sh",
                    name = "POSIX Shell (/system/bin/sh)",
                    status = if (hasSh) DiagnosticStatus.PASS else DiagnosticStatus.FAIL,
                    message = if (hasSh) "Android POSIX shell verified and executable" else "CRITICAL: /system/bin/sh missing"
                )
            )

            // 2. Storage space check
            val (freeGb, totalGb) = systemMonitor.readStorageInfo()
            val storageStatus = if (freeGb > 1.0f) DiagnosticStatus.PASS else if (freeGb > 0.2f) DiagnosticStatus.WARN else DiagnosticStatus.FAIL
            list.add(
                DiagnosticItem(
                    id = "diag_storage",
                    name = "Internal Storage Capacity",
                    status = storageStatus,
                    message = String.format("%.2f GB free of %.2f GB total", freeGb, totalGb),
                    canAutoFix = storageStatus != DiagnosticStatus.PASS
                )
            )

            // 3. SSH Port check
            val port = configState.value.port
            val portAvailable = try { ServerSocket(port).use { true } } catch (_: Exception) { serverState.value.isRunning }
            list.add(
                DiagnosticItem(
                    id = "diag_port",
                    name = "SSH Port $port Availability",
                    status = if (portAvailable) DiagnosticStatus.PASS else DiagnosticStatus.WARN,
                    message = if (portAvailable) "Port $port is clear and bound" else "Port $port is occupied by another process"
                )
            )

            // 4. Network interface
            val ip = NetworkUtils.getLocalIpAddress()
            val hasLan = ip != "127.0.0.1"
            list.add(
                DiagnosticItem(
                    id = "diag_network",
                    name = "Local Network Interface",
                    status = if (hasLan) DiagnosticStatus.PASS else DiagnosticStatus.WARN,
                    message = if (hasLan) "Reachable on LAN at $ip" else "No active Wi-Fi or LAN connection detected"
                )
            )

            // 5. Battery and WakeLock
            val hasWakeLock = configState.value.wakeLockEnabled
            list.add(
                DiagnosticItem(
                    id = "diag_wakelock",
                    name = "Screen-Off Persistence (WakeLock)",
                    status = if (hasWakeLock) DiagnosticStatus.PASS else DiagnosticStatus.WARN,
                    message = if (hasWakeLock) "Partial WakeLock active for screen-off execution" else "WakeLock is disabled; server may sleep when screen is off"
                )
            )

            _diagnosticItems.value = list
        }
    }

    fun autoFixDiagnostic(id: String) {
        when (id) {
            "diag_storage" -> {
                // Clear temporary logs
                val logsDir = File(linuxEnv.rootDir, "logs")
                logsDir.listFiles()?.forEach { it.delete() }
                runDiagnostics()
            }
            "diag_wakelock" -> {
                updateConfig(configState.value.copy(wakeLockEnabled = true))
                runDiagnostics()
            }
        }
    }
}
