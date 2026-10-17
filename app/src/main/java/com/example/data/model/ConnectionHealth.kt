package com.example.data.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.MatrixBlue
import com.example.ui.theme.MatrixGreen
import com.example.ui.theme.MatrixRed
import com.example.ui.theme.MatrixYellow

enum class ServerStatusType(val label: String) {
    RUNNING("ONLINE"),
    STOPPED("STOPPED"),
    STARTING("STARTING"),
    STOPPING("STOPPING"),
    CONNECTION_ISSUE("CONNECTION ISSUE")
}

enum class ConnectionIssueType {
    NONE,
    NO_NETWORK,          // Not connected to Wi-Fi/LAN or VPN
    PORT_IN_USE,         // Port already in use by another service
    SOCKET_TIMEOUT,      // Port opened but TCP probe timed out
    DAEMON_ERROR         // SshServer crashed or failed to bind
}

data class ConnectionHealth(
    val status: ServerStatusType = ServerStatusType.STOPPED,
    val issueType: ConnectionIssueType = ConnectionIssueType.NONE,
    val headline: String = "Server Stopped",
    val detailMessage: String = "Daemon is idle. Tap start to begin accepting SSH connections.",
    val ipAddress: String = "127.0.0.1",
    val port: Int = 2222,
    val vpnIpAddress: String? = null,
    val isWifiConnected: Boolean = false,
    val isVpnActive: Boolean = false,
    val isSocketResponsive: Boolean = false,
    val socketProbeLatencyMs: Long = -1L,
    val activeSessionsCount: Int = 0,
    val lastCheckTimestamp: Long = System.currentTimeMillis()
) {
    val indicatorColor: Color
        get() = when (status) {
            ServerStatusType.RUNNING -> MatrixGreen
            ServerStatusType.CONNECTION_ISSUE -> MatrixYellow
            ServerStatusType.STARTING, ServerStatusType.STOPPING -> MatrixBlue
            ServerStatusType.STOPPED -> MatrixRed
        }

    val isIssue: Boolean
        get() = status == ServerStatusType.CONNECTION_ISSUE

    val isRunning: Boolean
        get() = status == ServerStatusType.RUNNING

    val formattedLatency: String
        get() = if (socketProbeLatencyMs >= 0) "${socketProbeLatencyMs}ms" else "Timeout"
}
