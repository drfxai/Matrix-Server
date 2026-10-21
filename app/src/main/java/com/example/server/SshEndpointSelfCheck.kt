package com.example.server

import com.example.util.NetworkUtils
import java.net.InetSocketAddress
import java.net.Socket

/**
 * Server-side IP self-check. This cannot prove that Maximus VPN can route from
 * a second phone to this listener. It only verifies an interface owned by this phone.
 *
 * In particular an address like 10.247.210.65 is meaningful only if the
 * intended overlay is already up on the server AND the other phone can route to it.
 */
internal object SshEndpointSelfCheck {
    data class Result(
        val message: String,
        val localSshVerified: Boolean
    )

    fun run(address: String, port: Int): Result {
        val addresses = NetworkUtils.getActiveInterfaces().map { it.ipAddress }.toSet()
        return inspect(address, port, addresses) { host, p -> probe(host, p) }
    }

    /** Pure decision boundary so unit tests cannot make any outbound connections. */
    internal fun inspect(
        address: String,
        port: Int,
        localIps: Set<String>,
        probeLocal: (String, Int) -> Boolean
    ): Result {
        val host = address.trim().removeSurrounding("[", "]")
        if (port !in 1..65535) return Result("Invalid SSH port. Configure port 1–65535.", false)
        if (!ExportEndpointPolicy.isValid(host)) {
            return Result("Invalid SSH endpoint. Enter an IP address or DNS hostname without scheme or port.", false)
        }
        if (host !in localIps) {
            return Result(
                "The address $host is NOT assigned to an active local interface on this phone. " +
                    "If it is a hostname or public/forwarded endpoint, verify its routing separately. " +
                    "For a private LAN/overlay address check that its interface is connected. " +
                    "Nothing was contacted by this test.",
                false
            )
        }
        val listening = probeLocal(host, port)
        return if (listening) Result(
            "SSH identification verified locally at $host:$port. " +
                "The client still needs a network route and must separately test authentication and TCP forwarding.",
            true
        ) else Result(
            "$host is assigned to this phone, but no SSHv2 identification was received at port $port. " +
                "Restart the SSH service and confirm the port in Settings.",
            false
        )
    }

    /** Never called with an arbitrary host: [inspect] requires a local assigned IP. */
    private fun probe(host: String, port: Int): Boolean = try {
        Socket().use { socket ->
            socket.soTimeout = 1100
            socket.connect(InetSocketAddress(host, port), 1100)
            val input = socket.getInputStream()
            val banner = StringBuilder()
            var terminated = false
            repeat(255) {
                val byte = input.read()
                if (terminated) return@repeat
                if (byte == 10) terminated = true
                else if (byte == -1) terminated = true
                else if (byte != 13) banner.append(byte.toChar())
            }
            banner.startsWith("SSH-2.0-") && terminated
        }
    } catch (_: java.io.IOException) {
        false
    }
}
