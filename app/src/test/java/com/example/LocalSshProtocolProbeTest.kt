package com.example

import com.example.server.LocalSshProtocolProbe
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.InetAddress
import java.net.ServerSocket
import kotlin.concurrent.thread

/** Validates the local probe's behavior without requiring any real network service. */
class LocalSshProtocolProbeTest {
    private fun serve(banner: String, check: (Int) -> Unit) {
        ServerSocket(0, 1, InetAddress.getByName("127.0.0.1")).use { listener ->
            val acceptor = thread(name = "LocalSshBannerTest", isDaemon = true) {
                listener.accept().use { peer ->
                    peer.getOutputStream().write(banner.toByteArray(Charsets.US_ASCII))
                    peer.getOutputStream().flush()
                }
            }
            check(listener.localPort)
            acceptor.join(1500)
        }
    }

    @Test fun recognisesRealSshIdentification() {
        serve("SSH-2.0-APACHE-SSHD-TEST\r\n") { port ->
            val result = LocalSshProtocolProbe.check(port, 1000)
            assertTrue(result.responsive)
            assertTrue(result.latencyMs >= 0)
        }
    }

    @Test fun anUnrelatedTcpListenerIsNotMistakenForSsh() {
        serve("HTTP/1.1 200 OK\r\n") { port ->
            assertFalse(LocalSshProtocolProbe.check(port, 1000).responsive)
        }
    }

    @Test fun rejectedPortsHaveNoValidSshStatus() {
        assertFalse(LocalSshProtocolProbe.check(0).responsive)
        assertFalse(LocalSshProtocolProbe.check(65536).responsive)
        assertFalse(LocalSshProtocolProbe.check(2222, -1).responsive)
    }
}
