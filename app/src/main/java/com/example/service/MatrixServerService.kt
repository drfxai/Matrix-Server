package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.local.ServerDatabase
import com.example.data.local.ServerPreferences
import com.example.data.model.ConnectionHealth
import com.example.data.model.ConnectionIssueType
import com.example.data.model.ServerConfig
import com.example.data.model.ServerMetrics
import com.example.data.model.ServerState
import com.example.data.model.ServerStatusType
import com.example.server.LinuxEnvironment
import com.example.server.ProcessSupervisor
import com.example.server.SshServerManager
import com.example.util.NetworkUtils
import com.example.util.SystemMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.net.InetSocketAddress
import java.net.Socket

class MatrixServerService : Service() {

    private val tag = "MatrixServerService"
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var metricsJob: Job? = null

    private lateinit var linuxEnvironment: LinuxEnvironment
    private lateinit var sshServerManager: SshServerManager
    private lateinit var processSupervisor: ProcessSupervisor
    private lateinit var systemMonitor: SystemMonitor
    private lateinit var serverPreferences: ServerPreferences
    private var wakeLock: PowerManager.WakeLock? = null
    private var serverStartTimestamp: Long = 0L

    private var connectivityManager: ConnectivityManager? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    override fun onCreate() {
        super.onCreate()
        val homeDir = File(filesDir, "home").apply { if (!exists()) mkdirs() }
        try {
            System.setProperty("user.home", homeDir.absolutePath)
            org.apache.sshd.common.util.io.PathUtils.setUserHomeFolderResolver { homeDir.toPath() }
        } catch (_: Throwable) {}

        linuxEnvironment = LinuxEnvironment(this)
        val db = ServerDatabase.getInstance(this)
        processSupervisor = ProcessSupervisor(this, db.projectDao(), linuxEnvironment)
        sshServerManager = SshServerManager(this, linuxEnvironment)
        systemMonitor = SystemMonitor(this)
        serverPreferences = ServerPreferences(this)

        instance = this
        sharedProcessSupervisor = processSupervisor
        sharedSshServerManager = sshServerManager
        sharedLinuxEnvironment = linuxEnvironment

        createNotificationChannel()
        registerNetworkCallback()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START
        when (action) {
            ACTION_START -> startMatrixServer()
            ACTION_STOP -> stopMatrixServer()
            ACTION_COPY_SSH -> copySshCommand()
            ACTION_PROBE_HEALTH -> forceHealthProbe()
        }
        return START_STICKY
    }

    private fun startMatrixServer() {
        if (_serverState.value.isRunning || _serverState.value.isTransitioning) return

        _serverState.value = ServerState.STARTING
        _connectionHealth.value = _connectionHealth.value.copy(
            status = ServerStatusType.STARTING,
            headline = "Starting SSH Daemon...",
            detailMessage = "Binding local port, loading host keys, and starting Linux runtime..."
        )
        startForeground(NOTIFICATION_ID, buildNotification("Starting Linux userspace and SSH daemon..."))

        serviceScope.launch {
            try {
                val config = serverPreferences.loadConfig()

                // Acquire partial wake lock if enabled
                if (config.wakeLockEnabled) {
                    acquireWakeLock()
                }

                // Initialize Linux environment and sample projects
                linuxEnvironment.initializeEnvironment()

                // Start SSH server
                val result = sshServerManager.startServer(config)
                if (result.isSuccess) {
                    serverStartTimestamp = System.currentTimeMillis()
                    _serverState.value = ServerState.ON
                    Log.i(tag, "MATRIX SERVER is now ONLINE")

                    // Perform immediate health probe and start periodic monitoring
                    performHealthProbe(config)
                    startMetricsMonitoring()

                    // Update persistent notification
                    val ip = NetworkUtils.getLocalIpAddress()
                    updateNotification("ONLINE | ssh ${config.username}@$ip -p ${config.port}")
                } else {
                    val exMsg = result.exceptionOrNull()?.message ?: "Unknown bind error"
                    val isPortConflict = exMsg.contains("already in use", ignoreCase = true)

                    // No daemon is bound: DEGRADED would incorrectly appear as a running server,
                    // prevent retry, and leave the foreground service/wakelock alive.
                    _serverState.value = ServerState.OFF
                    _connectionHealth.value = ConnectionHealth(
                        status = ServerStatusType.CONNECTION_ISSUE,
                        issueType = if (isPortConflict) ConnectionIssueType.PORT_IN_USE else ConnectionIssueType.DAEMON_ERROR,
                        headline = if (isPortConflict) "Port Collision (Port ${config.port})" else "Daemon Bind Failure",
                        detailMessage = if (isPortConflict) {
                            "Port ${config.port} is occupied by another app or service. Select a different port in Settings."
                        } else {
                            "Failed to bind SSH daemon: $exMsg"
                        },
                        port = config.port
                    )
                    Log.e(tag, "Failed to start SSH server: $exMsg")
                    releaseWakeLock()
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
            } catch (e: Exception) {
                Log.e(tag, "Fatal error starting server", e)
                _serverState.value = ServerState.OFF
                _connectionHealth.value = ConnectionHealth(
                    status = ServerStatusType.STOPPED,
                    issueType = ConnectionIssueType.DAEMON_ERROR,
                    headline = "Fatal Server Error",
                    detailMessage = e.message ?: "Failed to start service"
                )
                stopSelf()
            }
        }
    }

    private fun stopMatrixServer() {
        _serverState.value = ServerState.STOPPING
        _connectionHealth.value = _connectionHealth.value.copy(
            status = ServerStatusType.STOPPING,
            headline = "Halting SSH Server...",
            detailMessage = "Terminating client sessions and stopping background service."
        )
        updateNotification("Stopping services and SSH daemon...")

        serviceScope.launch {
            try {
                processSupervisor.stopAllProjects()
                sshServerManager.stopServer()
                releaseWakeLock()

                metricsJob?.cancel()
                val config = serverPreferences.loadConfig()
                _serverState.value = ServerState.OFF
                _connectionHealth.value = ConnectionHealth(
                    status = ServerStatusType.STOPPED,
                    issueType = ConnectionIssueType.NONE,
                    headline = "SSH Server Stopped",
                    detailMessage = "Daemon is idle. Tap POWER on console to start server.",
                    port = config.port
                )
                Log.i(tag, "MATRIX SERVER is now OFF")
            } finally {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
    }

    private fun copySshCommand() {
        val metrics = _metricsState.value
        val cmd = metrics.sshCommand
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.setPrimaryClip(ClipData.newPlainText("SSH Command", cmd))
        Toast.makeText(this, "Copied: $cmd", Toast.LENGTH_SHORT).show()
    }

    private fun forceHealthProbe() {
        serviceScope.launch {
            val config = serverPreferences.loadConfig()
            performHealthProbe(config)
        }
    }

    private fun probeLocalSocket(port: Int): Pair<Boolean, Long> {
        val startTime = System.currentTimeMillis()
        return try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress("127.0.0.1", port), 500)
                val elapsed = System.currentTimeMillis() - startTime
                Pair(true, elapsed)
            }
        } catch (_: Exception) {
            Pair(false, -1L)
        }
    }

    private fun performHealthProbe(config: ServerConfig) {
        val activeSessions = sshServerManager.activeSessions.value
        val (isSocketOk, latency) = probeLocalSocket(config.port)
        val lanIp = NetworkUtils.getLocalIpAddress()
        val vpnIp = NetworkUtils.getVpnIpAddress()
        val isWifi = NetworkUtils.isWifiConnected(this)
        val isVpn = vpnIp != null
        val hasNetwork = (lanIp != "127.0.0.1") || isVpn

        val (status, issueType, headline, detail) = when {
            !_serverState.value.isRunning -> {
                HealthStatus(
                    ServerStatusType.STOPPED,
                    ConnectionIssueType.NONE,
                    "SSH Server Stopped",
                    "Daemon is offline. Tap START to accept incoming connections."
                )
            }
            !isSocketOk -> {
                HealthStatus(
                    ServerStatusType.CONNECTION_ISSUE,
                    ConnectionIssueType.SOCKET_TIMEOUT,
                    "Socket Probe Unresponsive",
                    "Port ${config.port} refused or timed out on local TCP probe. SSH clients cannot establish a handshake."
                )
            }
            !hasNetwork -> {
                HealthStatus(
                    ServerStatusType.CONNECTION_ISSUE,
                    ConnectionIssueType.NO_NETWORK,
                    "No Wi-Fi or VPN Network Detected",
                    "Daemon is listening on localhost only. Connect to Wi-Fi or Tailscale so remote clients can connect."
                )
            }
            else -> {
                HealthStatus(
                    ServerStatusType.RUNNING,
                    ConnectionIssueType.NONE,
                    "SSH Listener Running (Local Check)",
                    "Local TCP probe passed (${latency}ms) on port ${config.port}. Remote SSH, forwarding and VPN traffic are NOT yet verified."
                )
            }
        }

        _connectionHealth.value = ConnectionHealth(
            status = status,
            issueType = issueType,
            headline = headline,
            detailMessage = detail,
            ipAddress = lanIp,
            port = config.port,
            vpnIpAddress = vpnIp,
            isWifiConnected = isWifi,
            isVpnActive = isVpn,
            isSocketResponsive = isSocketOk,
            socketProbeLatencyMs = latency,
            activeSessionsCount = activeSessions,
            lastCheckTimestamp = System.currentTimeMillis()
        )

        // Keep serverState in sync
        if (status == ServerStatusType.CONNECTION_ISSUE) {
            _serverState.value = ServerState.DEGRADED
        } else if (status == ServerStatusType.RUNNING) {
            _serverState.value = ServerState.ON
        }
    }

    private data class HealthStatus(
        val status: ServerStatusType,
        val issueType: ConnectionIssueType,
        val headline: String,
        val detail: String
    )

    private fun startMetricsMonitoring() {
        metricsJob?.cancel()
        metricsJob = serviceScope.launch {
            while (isActive && _serverState.value.isRunning) {
                val config = serverPreferences.loadConfig()
                val activeSessions = sshServerManager.activeSessions.value
                val runningProjects = processSupervisor.runningCount.value
                val uptimeSec = (System.currentTimeMillis() - serverStartTimestamp) / 1000L

                val metrics = systemMonitor.gatherMetrics(
                    activeSessions = activeSessions,
                    runningProjects = runningProjects,
                    sshPort = config.port,
                    uptimeSeconds = uptimeSec
                )
                _metricsState.value = metrics

                // Perform real-time health probe
                performHealthProbe(config)

                // Update notification text
                val health = _connectionHealth.value
                val notificationText = when (health.status) {
                    ServerStatusType.RUNNING -> "ONLINE | ssh ${config.username}@${metrics.lanIpAddress} -p ${metrics.sshPort}"
                    ServerStatusType.CONNECTION_ISSUE -> "ISSUE: ${health.headline} (Port ${config.port})"
                    else -> "Matrix Server"
                }
                updateNotification(notificationText)

                delay(if (config.lowPowerMode) 5000L else 2500L)
            }
        }
    }

    private fun acquireWakeLock() {
        if (wakeLock == null) {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MatrixServer:BackgroundWakeLock").apply {
                setReferenceCounted(false)
                acquire(12 * 60 * 60 * 1000L) // Safe limit 12h
            }
            Log.i(tag, "Partial WakeLock acquired for screen-off persistence")
        }
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
                Log.i(tag, "WakeLock released")
            }
        } catch (_: Exception) {}
        wakeLock = null
    }

    private fun registerNetworkCallback() {
        try {
            connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            networkCallback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    serviceScope.launch {
                        delay(800)
                        val config = serverPreferences.loadConfig()
                        if (_serverState.value.isRunning) {
                            performHealthProbe(config)
                        }
                    }
                }

                override fun onLost(network: Network) {
                    serviceScope.launch {
                        delay(400)
                        val config = serverPreferences.loadConfig()
                        if (_serverState.value.isRunning) {
                            performHealthProbe(config)
                        }
                    }
                }
            }
            connectivityManager?.registerDefaultNetworkCallback(networkCallback!!)
        } catch (e: Exception) {
            Log.w(tag, "Could not register network callback", e)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Matrix Server Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Ongoing persistent notification while Matrix Server is active"
                setShowBadge(false)
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(contentText: String): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, MatrixServerService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val copyIntent = Intent(this, MatrixServerService::class.java).apply {
            action = ACTION_COPY_SSH
        }
        val copyPendingIntent = PendingIntent.getService(
            this, 2, copyIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("MATRIX SERVER")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(openPendingIntent)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop Server", stopPendingIntent)
            .addAction(android.R.drawable.ic_menu_share, "Copy SSH", copyPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun updateNotification(text: String) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        nm?.notify(NOTIFICATION_ID, buildNotification(text))
    }

    override fun onDestroy() {
        super.onDestroy()
        releaseWakeLock()
        networkCallback?.let { connectivityManager?.unregisterNetworkCallback(it) }
        serviceScope.cancel()
        instance = null
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val CHANNEL_ID = "matrix_server_channel"
        const val NOTIFICATION_ID = 1337

        const val ACTION_START = "com.example.action.START_SERVER"
        const val ACTION_STOP = "com.example.action.STOP_SERVER"
        const val ACTION_COPY_SSH = "com.example.action.COPY_SSH"
        const val ACTION_PROBE_HEALTH = "com.example.action.PROBE_HEALTH"

        private val _serverState = MutableStateFlow(ServerState.OFF)
        val serverState: StateFlow<ServerState> = _serverState.asStateFlow()

        private val _connectionHealth = MutableStateFlow(ConnectionHealth())
        val connectionHealth: StateFlow<ConnectionHealth> = _connectionHealth.asStateFlow()

        private val _metricsState = MutableStateFlow(ServerMetrics())
        val metricsState: StateFlow<ServerMetrics> = _metricsState.asStateFlow()

        @Volatile
        var instance: MatrixServerService? = null
            private set

        @Volatile
        var sharedProcessSupervisor: ProcessSupervisor? = null
            private set

        @Volatile
        var sharedSshServerManager: SshServerManager? = null
            private set

        @Volatile
        var sharedLinuxEnvironment: LinuxEnvironment? = null
            private set

        fun start(context: Context) {
            val intent = Intent(context, MatrixServerService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, MatrixServerService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }

        fun triggerHealthProbe(context: Context) {
            val intent = Intent(context, MatrixServerService::class.java).apply {
                action = ACTION_PROBE_HEALTH
            }
            context.startService(intent)
        }
    }
}
