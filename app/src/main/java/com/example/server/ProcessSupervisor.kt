package com.example.server

import android.content.Context
import android.util.Log
import com.example.data.local.ProjectDao
import com.example.data.model.ProjectItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

data class ManagedProcess(
    val project: ProjectItem,
    val process: Process,
    val pid: Int,
    val startTime: Long = System.currentTimeMillis()
)

class ProcessSupervisor(
    private val context: Context,
    private val projectDao: ProjectDao,
    private val linuxEnv: LinuxEnvironment
) {
    private val tag = "ProcessSupervisor"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val activeProcesses = ConcurrentHashMap<String, ManagedProcess>()
    private val projectLogs = ConcurrentHashMap<String, MutableList<String>>()

    private val _runningCount = MutableStateFlow(0)
    val runningCount: StateFlow<Int> = _runningCount.asStateFlow()

    private val logsDir: File get() = File(context.filesDir, "logs").apply { if (!exists()) mkdirs() }
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

    fun startProject(project: ProjectItem) {
        scope.launch {
            if (activeProcesses.containsKey(project.id)) {
                Log.w(tag, "Project ${project.name} is already running")
                return@launch
            }

            try {
                logMessage(project.id, "--- STARTING PROJECT: ${project.name} ---")
                logMessage(project.id, "Command: ${project.command}")
                logMessage(project.id, "Working Directory: ${project.workingDir}")

                val workDir = File(project.workingDir)
                if (!workDir.exists()) workDir.mkdirs()

                val pb = ProcessBuilder("/system/bin/sh", "-c", project.command)
                pb.directory(workDir)
                val env = pb.environment()
                env["HOME"] = linuxEnv.homeDir.absolutePath
                env["PATH"] = "${linuxEnv.binDir.absolutePath}:/sbin:/system/sbin:/system/bin:/system/xbin:/odm/bin:/vendor/bin"
                env["USER"] = "matrix"
                env["PORT"] = project.port?.toString() ?: "8080"
                env["MATRIX_SERVER"] = "1"

                val proc = pb.start()
                val pid = getProcessPid(proc)

                val managed = ManagedProcess(project, proc, pid)
                activeProcesses[project.id] = managed
                _runningCount.value = activeProcesses.size

                projectDao.updateStatus(project.id, "RUNNING", pid)
                logMessage(project.id, "Process started successfully with PID: $pid")

                // Stream stdout
                scope.launch {
                    val reader = BufferedReader(InputStreamReader(proc.inputStream))
                    var line: String?
                    try {
                        while (reader.readLine().also { line = it } != null) {
                            line?.let { logMessage(project.id, "[STDOUT] $it") }
                        }
                    } catch (_: Exception) {}
                }

                // Stream stderr
                scope.launch {
                    val reader = BufferedReader(InputStreamReader(proc.errorStream))
                    var line: String?
                    try {
                        while (reader.readLine().also { line = it } != null) {
                            line?.let { logMessage(project.id, "[STDERR] $it") }
                        }
                    } catch (_: Exception) {}
                }

                // Monitor lifecycle
                scope.launch {
                    val exitCode = proc.waitFor()
                    activeProcesses.remove(project.id)
                    _runningCount.value = activeProcesses.size
                    logMessage(project.id, "Process terminated with exit code: $exitCode")

                    if (exitCode != 0) {
                        projectDao.updateStatus(project.id, "FAILED", null)
                        handleAutoRestart(project)
                    } else {
                        projectDao.updateStatus(project.id, "STOPPED", null)
                    }
                }

            } catch (e: Exception) {
                Log.e(tag, "Failed to start project ${project.name}", e)
                logMessage(project.id, "ERROR: Failed to launch process: ${e.message}")
                projectDao.updateStatus(project.id, "FAILED", null)
            }
        }
    }

    fun stopProject(projectId: String) {
        scope.launch {
            val managed = activeProcesses[projectId]
            if (managed != null) {
                logMessage(projectId, "--- STOPPING PROJECT ---")
                try {
                    managed.process.destroy() // SIGTERM
                    delay(500)
                    if (managed.process.isAlive) {
                        managed.process.destroyForcibly() // SIGKILL
                    }
                } catch (e: Exception) {
                    Log.e(tag, "Error terminating project process $projectId", e)
                }
                activeProcesses.remove(projectId)
                _runningCount.value = activeProcesses.size
                projectDao.updateStatus(projectId, "STOPPED", null)
                logMessage(projectId, "Project stopped.")
            }
        }
    }

    fun restartProject(project: ProjectItem) {
        scope.launch {
            stopProject(project.id)
            delay(800)
            startProject(project)
        }
    }

    fun stopAllProjects() {
        val keys = activeProcesses.keys.toList()
        for (id in keys) {
            stopProject(id)
        }
    }

    private fun handleAutoRestart(project: ProjectItem) {
        if (!project.autoRestart) return
        val currentRestarts = project.restartCount
        if (currentRestarts < 5) {
            val backoffSeconds = (1L shl currentRestarts).coerceAtMost(30L)
            logMessage(project.id, "Auto-restart policy active. Retrying in ${backoffSeconds}s (attempt ${currentRestarts + 1}/5)...")
            scope.launch {
                delay(backoffSeconds * 1000L)
                val updated = project.copy(restartCount = currentRestarts + 1)
                projectDao.update(updated)
                startProject(updated)
            }
        } else {
            logMessage(project.id, "Max auto-restart attempts reached (5/5). Marked as FAILED.")
        }
    }

    fun getLogs(projectId: String): List<String> {
        return projectLogs[projectId]?.toList() ?: emptyList()
    }

    private fun logMessage(projectId: String, message: String) {
        val timestamp = dateFormat.format(Date())
        val formatted = "[$timestamp] $message"
        val list = projectLogs.getOrPut(projectId) { mutableListOf() }
        synchronized(list) {
            list.add(formatted)
            if (list.size > 200) list.removeAt(0)
        }
        // Also persist to log file
        try {
            val logFile = File(logsDir, "$projectId.log")
            logFile.appendText("$formatted\n")
        } catch (_: Exception) {}
    }

    private fun getProcessPid(process: Process): Int {
        return try {
            val field = process.javaClass.getDeclaredField("pid")
            field.isAccessible = true
            field.getInt(process)
        } catch (_: Exception) {
            (1000..9999).random()
        }
    }
}
