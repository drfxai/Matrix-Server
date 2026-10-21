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
        // This endpoint is selected by the user. Never silently substitute a Wi-Fi address
        // when the client needs a private overlay (or vice versa).
        val endpoint = address.trim().removeSurrounding("[", "]")
        require(ExportEndpointPolicy.isValid(endpoint)) {
            "Enter a valid non-loopback LAN/overlay IPv4, IPv6 or DNS hostname reachable by the client"
        }
        require(hostKey.startsWith("ssh-rsa ") && hostKey.split(' ').size >= 2) {
            "SSH host key was not available for secure pinning"
        }
        return buildString {
            appendLine("proxies:")
            appendLine("  - name: ${JSONObject.quote("Matrix Server SSH")}")
            appendLine("    type: ssh")
            appendLine("    server: ${JSONObject.quote(endpoint)}")
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

/**
 * Validates an SSH dial target without performing DNS lookup or asserting remote reachability.
 * No schemes, credentials, paths, ports or control characters may enter the Mihomo server field.
 * IPv6 literals are accepted with or without UI brackets.
 */
internal object ExportEndpointPolicy {
    fun isValid(host: String): Boolean {
        val h = host.trim()
        if (h.isEmpty() || h.length > 253 || h.any { it.isWhitespace() || it.isISOControl() } ||
            h.contains('@') || h.contains('/') || h.contains('\\') || h.contains('%') ||
            h.contains('?') || h.contains('#') || h.contains('[') || h.contains(']')) return false
        if (h == "0.0.0.0" || h == "127.0.0.1" || h == "::" || h == "::1" ||
            h.equals("localhost", true) || h.endsWith(".localhost", true)) return false
        if (h.contains(':')) {
            // Numeric IPv6 only: do not resolve untrusted input or accept host:port syntax.
            if (h.count { it == ':' } < 2 || h.any { it !in "0123456789abcdefABCDEF:." }) return false
            return runCatching { java.net.InetAddress.getByName(h) is java.net.Inet6Address }.getOrDefault(false)
        }
        val parts = h.split('.')
        if (parts.size == 4 && parts.all { p -> p.isNotEmpty() && p.all(Char::isDigit) }) {
            return parts.all { it.toIntOrNull() in 0..255 } &&
                h != "255.255.255.255" && !h.startsWith("127.") && !h.startsWith("0.")
        }
        return parts.size >= 2 && parts.all { p ->
            p.length in 1..63 && p.first().isLetterOrDigit() && p.last().isLetterOrDigit() &&
                p.all { it.isLetterOrDigit() || it == '-' }
        }
    }
}
