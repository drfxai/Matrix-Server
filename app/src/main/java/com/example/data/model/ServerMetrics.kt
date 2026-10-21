package com.example.data.model

data class ServerMetrics(
    val cpuUsage: Float = 0f,
    val memoryUsedMb: Long = 0L,
    val memoryTotalMb: Long = 0L,
    val storageFreeGb: Float = 0f,
    val storageTotalGb: Float = 0f,
    val batteryPercent: Int = 100,
    val isCharging: Boolean = false,
    val thermalStatus: String = "Normal",
    val activeSessionsCount: Int = 0,
    val runningProjectsCount: Int = 0,
    val lanIpAddress: String = "127.0.0.1",
    val vpnIpAddress: String? = null,
    val sshPort: Int = 2222,
    val sshUsername: String = "matrix",
    val uptimeSeconds: Long = 0L,
    val lastHealthCheckTime: Long = System.currentTimeMillis()
) {
    val sshCommand: String
        get() = "ssh $sshUsername@$lanIpAddress -p $sshPort"

    val vpnSshCommand: String?
        get() = vpnIpAddress?.let { "ssh $sshUsername@$it -p $sshPort" }

    val formattedUptime: String
        get() {
            val hours = uptimeSeconds / 3600
            val minutes = (uptimeSeconds % 3600) / 60
            val seconds = uptimeSeconds % 60
            return String.format("%02d:%02d:%02d", hours, minutes, seconds)
        }
}
