package com.example.server

import com.example.data.model.ServerConfig
import org.json.JSONObject

/**
 * Maximus VPN detects Mihomo configurations using "proxies:" and forwards
 * them to MihomoParser. Use explicit YAML, not a JSON root (JSON roots currently
 * get sent to the Xray JSON parser by Maximus's universal import path).
 * JSONObject.quote provides JSON/YAML-compatible double-quoted scalar escaping.
 */
object MaximusVpnSshProfile {
    fun generate(config: ServerConfig, address: String, hostKey: String): String {
        require(config.vpnTcpForwardingEnabled) { "Enable SSH TCP forwarding and restart the server first" }
        require(config.passwordAuthEnabled && SshAuthPolicy.validPassword(config.password)) {
            "Password authentication is required for the automatic Maximus VPN export"
        }
        require(config.port in 1..65535)
        require(address.isNotBlank() && address != "127.0.0.1" && address != "0.0.0.0") {
            "No usable LAN or private-overlay server address detected"
        }
        require(hostKey.startsWith("ssh-rsa ") && hostKey.split(' ').size >= 2) {
            "SSH host key was not available for secure pinning"
        }
        return buildString {
            appendLine("proxies:")
            appendLine("  - name: ${JSONObject.quote("Matrix Server SSH")}")
            appendLine("    type: ssh")
            appendLine("    server: ${JSONObject.quote(address)}")
            appendLine("    port: ${config.port}")
            appendLine("    username: ${JSONObject.quote(config.username)}")
            appendLine("    password: ${JSONObject.quote(config.password)}")
            appendLine("    host-key:")
            appendLine("      - ${JSONObject.quote(hostKey)}")
            appendLine("    host-key-algorithms:")
            appendLine("      - rsa")
            appendLine("    udp: false")
        }
    }
}
