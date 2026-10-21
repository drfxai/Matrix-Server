package com.example

import com.example.data.model.ServerConfig
import com.example.server.LinuxEnvironment
import com.example.server.MaximusVpnSshProfile
import com.example.server.SshServerManager
import org.apache.sshd.client.SshClient
import org.apache.sshd.client.keyverifier.ServerKeyVerifier
import org.apache.sshd.common.config.keys.PublicKeyEntry
import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.net.ServerSocket

/**
 * Real loopback SSH handshake through the production MINA server manager.
 * No external interface, VPN, or SSH credentials are exposed.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MatrixSshLoopbackIntegrationTest {

    @Test fun realHostKeyPinnedSshPasswordLoginAndExport() {
        val context = RuntimeEnvironment.getApplication()
        val freePort = ServerSocket(0).use { it.localPort }
        val config = ServerConfig(
            port = freePort,
            username = "matrix",
            password = "StrongIntegrationPass!2026",
            passwordAuthEnabled = true,
            publicKeyAuthEnabled = false,
            vpnTcpForwardingEnabled = true
        )
        val manager = SshServerManager(context, LinuxEnvironment(context))
        val started = manager.startServer(config)
        assertTrue("SSH daemon startup: ${started.exceptionOrNull()?.message}", started.isSuccess)
        try {
            val pinned = requireNotNull(manager.currentHostPublicKey()) {
                "Could not export the actual listener host public key"
            }
            assertTrue(pinned.startsWith("ssh-rsa "))
            val yaml = MaximusVpnSshProfile.generate(config, "10.247.210.65", pinned)
            assertTrue(yaml.contains("    type: ssh"))
            assertTrue(yaml.contains("    server: \"10.247.210.65\""))
            assertTrue(yaml.contains("    host-key:"))

            val client = SshClient.setUpDefaultClient()
            client.serverKeyVerifier = ServerKeyVerifier { _, _, presented ->
                PublicKeyEntry.toString(presented) == pinned
            }
            client.start()
            try {
                client.connect(config.username, "127.0.0.1", freePort).verify(10_000).session.use { session ->
                    session.addPasswordIdentity(config.password)
                    session.auth().verify(10_000)
                    assertTrue("SSH password login should authenticate", session.isAuthenticated)
                }
            } finally {
                client.stop()
            }
            // After stopping, a fresh listener must start without retaining
            // stale running/session state or silently rotating the pinned key.
            manager.stopServer()
            assertFalse(manager.isServerRunning.value)
            assertTrue(manager.startServer(config).isSuccess)
            assertEquals(pinned, manager.currentHostPublicKey())
        } finally {
            manager.stopServer()
        }
    }
}
