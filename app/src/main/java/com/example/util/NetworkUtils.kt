package com.example.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.data.model.ProjectItem
import java.net.Inet4Address
import java.net.NetworkInterface

data class NetworkInterfaceInfo(
    val name: String,
    val ipAddress: String,
    val type: String, // "WIFI", "VPN", "CELLULAR", "LAN"
    val isUp: Boolean
)

data class ActiveSocketInfo(
    val serviceName: String,
    val port: Int,
    val bindAddress: String,
    val status: String,
    val protocol: String
)

data class ActiveSessionInfo(
    val sessionId: String,
    val clientIp: String,
    val clientPort: Int,
    val username: String,
    val connectedAt: String,
    val protocolVersion: String,
    val channelType: String
)

object NetworkUtils {

    fun getLocalIpAddress(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return "127.0.0.1"
            var candidateIp: String? = null

            for (networkInterface in interfaces) {
                if (!networkInterface.isUp || networkInterface.isLoopback) continue
                val name = networkInterface.name.lowercase()

                // Check for WiFi / LAN interfaces first
                val isLan = name.startsWith("wlan") || name.startsWith("eth") || name.startsWith("rndis")

                val addresses = networkInterface.inetAddresses
                for (inetAddress in addresses) {
                    if (!inetAddress.isLoopbackAddress && inetAddress is Inet4Address) {
                        val host = inetAddress.hostAddress ?: continue
                        if (isLan) {
                            return host
                        } else if (candidateIp == null) {
                            candidateIp = host
                        }
                    }
                }
            }
            return candidateIp ?: "127.0.0.1"
        } catch (_: Exception) {
            return "127.0.0.1"
        }
    }

    fun getVpnIpAddress(): String? {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return null
            for (networkInterface in interfaces) {
                if (!networkInterface.isUp || networkInterface.isLoopback) continue
                val name = networkInterface.name.lowercase()

                // Look for Tailscale or Wireguard or tun interfaces
                if (name.contains("tailscale") || name.startsWith("tun") || name.startsWith("wg")) {
                    val addresses = networkInterface.inetAddresses
                    for (inetAddress in addresses) {
                        if (!inetAddress.isLoopbackAddress && inetAddress is Inet4Address) {
                            return inetAddress.hostAddress
                        }
                    }
                }
            }
        } catch (_: Exception) {}
        return null
    }

    fun getActiveInterfaces(): List<NetworkInterfaceInfo> {
        val result = mutableListOf<NetworkInterfaceInfo>()
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return emptyList()
            for (networkInterface in interfaces) {
                if (!networkInterface.isUp || networkInterface.isLoopback) continue
                val name = networkInterface.name.lowercase()

                val type = when {
                    name.startsWith("wlan") -> "WIFI"
                    name.contains("tailscale") || name.startsWith("tun") || name.startsWith("wg") -> "VPN MESH"
                    name.startsWith("rmnet") || name.startsWith("ccmni") || name.startsWith("p2p") -> "CELLULAR"
                    name.startsWith("eth") || name.startsWith("rndis") -> "LAN"
                    else -> "NETWORK"
                }

                val addresses = networkInterface.inetAddresses
                for (inetAddress in addresses) {
                    if (!inetAddress.isLoopbackAddress && inetAddress is Inet4Address) {
                        val host = inetAddress.hostAddress ?: continue
                        result.add(
                            NetworkInterfaceInfo(
                                name = networkInterface.name,
                                ipAddress = host,
                                type = type,
                                isUp = networkInterface.isUp
                            )
                        )
                    }
                }
            }
        } catch (_: Exception) {}
        if (result.isEmpty()) {
            result.add(NetworkInterfaceInfo("lo", "127.0.0.1", "LOOPBACK", true))
        }
        return result
    }

    fun getActiveSockets(sshPort: Int, isSshRunning: Boolean, projects: List<ProjectItem>): List<ActiveSocketInfo> {
        val sockets = mutableListOf<ActiveSocketInfo>()
        if (isSshRunning) {
            sockets.add(
                ActiveSocketInfo(
                    serviceName = "SSH Server Daemon",
                    port = sshPort,
                    bindAddress = "0.0.0.0",
                    status = "LISTENING",
                    protocol = "TCP"
                )
            )
        }
        projects.filter { it.status == "RUNNING" && it.port != null }.forEach { proj ->
            sockets.add(
                ActiveSocketInfo(
                    serviceName = proj.name,
                    port = proj.port!!,
                    bindAddress = "127.0.0.1",
                    status = "LISTENING",
                    protocol = "TCP"
                )
            )
        }
        return sockets
    }

    fun isNetworkConnected(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun isWifiConnected(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }
}
