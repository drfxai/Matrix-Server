package com.example.server

import java.net.InetSocketAddress
import java.net.Socket

/** A local SSH identification probe; it does NOT verify external reachability or login. */
internal data class LocalSshProbeResult(val responsive: Boolean, val latencyMs: Long)

/**
 * A plain socket connect is not proof an SSH daemon is serving the port:
 * a different process can bind it, or a daemon can stop responding after bind.
 * Check the RFC 4253 SSH identification line, without sending credentials.
 */
internal object LocalSshProtocolProbe {
    fun check(port: Int, timeoutMs: Int = 850): LocalSshProbeResult {
        if (port !in 1..65535 || timeoutMs !in 1..10_000) return LocalSshProbeResult(false, -1)
        val started = System.nanoTime()
        return try {
            Socket().use { socket ->
                socket.soTimeout = timeoutMs
                socket.connect(InetSocketAddress("127.0.0.1", port), timeoutMs)
                val line = StringBuilder()
                val input = socket.getInputStream()
                // SSH identification lines are limited to 255 bytes, including CRLF.
                // Do not let a non-SSH listener stall or allocate unbounded memory.
                for (i in 0 until 255) {
                    val byte = input.read()
                    if (byte == -1 || byte == 10) break
                    if (byte != 13) line.append(byte.toChar())
                }
                if (!line.startsWith("SSH-2.0-")) {
                    LocalSshProbeResult(false, -1)
                } else {
                    LocalSshProbeResult(true, (System.nanoTime() - started) / 1_000_000)
                }
            }
        } catch (_: java.io.IOException) {
            LocalSshProbeResult(false, -1)
        }
    }
}
