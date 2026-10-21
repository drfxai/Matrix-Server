package com.example.server

import com.example.data.model.ServerConfig
import org.json.JSONArray
import org.json.JSONObject

/**
 * Maximus VPN's MihomoParser accepts a Clash/Mihomo proxies document.
 * JSON is valid YAML syntax and safely escapes arbitrary credentials and host strings.
 * Never log, persist, or send this export to remote telemetry.
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
        val node = JSONObject()
            .put("name", "Matrix Server SSH")
            .put("type", "ssh")
            .put("server", address)
            .put("port", config.port)
            .put("username", config.username)
            .put("password", config.password)
            .put("host-key", JSONArray().put(hostKey))
            .put("host-key-algorithms", JSONArray().put("rsa"))
            .put("udp", false)
        return JSONObject().put("proxies", JSONArray().put(node)).toString(2)
    }
}
