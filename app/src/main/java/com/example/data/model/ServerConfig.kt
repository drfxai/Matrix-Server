package com.example.data.model

data class ServerConfig(
    val port: Int = 2222,
    val username: String = "matrix",
    val passwordAuthEnabled: Boolean = false,
    val password: String = "",
    val publicKeyAuthEnabled: Boolean = true,
    val wakeLockEnabled: Boolean = true,
    val autoStartOnBoot: Boolean = false,
    val lowPowerMode: Boolean = false,
    // Opt-in: permit authenticated SSH direct-tcpip outbound connections for Mihomo/Maximus VPN.
    val vpnTcpForwardingEnabled: Boolean = false,
    val setupCompleted: Boolean = false
)
