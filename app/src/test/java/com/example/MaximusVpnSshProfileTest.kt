package com.example

import com.example.data.model.ServerConfig
import com.example.server.MaximusVpnSshProfile
import com.example.server.PublicDestinationPolicy
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MaximusVpnSshProfileTest {

    private val config = ServerConfig(
        port = 2222,
        username = "matrix",
        passwordAuthEnabled = true,
        password = "CorrectStrongPass!42",
        vpnTcpForwardingEnabled = true
    )

    @Test
    fun exportedMihomoSshProfileIsPinnedAndEscapesCredentials() {
        val modified = config.copy(password = "strong_password\"\\test")
        val json = JSONObject(MaximusVpnSshProfile.generate(
            modified, "192.168.1.32", "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQCtest"
        ))
        val node = json.getJSONArray("proxies").getJSONObject(0)
        assertEquals("ssh", node.getString("type"))
        assertEquals("192.168.1.32", node.getString("server"))
        assertEquals(2222, node.getInt("port"))
        assertEquals(modified.password, node.getString("password"))
        assertEquals("matrix", node.getString("username"))
        assertEquals("ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQCtest",
            node.getJSONArray("host-key").getString(0))
        assertEquals("rsa", node.getJSONArray("host-key-algorithms").getString(0))
        assertFalse(node.getBoolean("udp"))
    }

    @Test
    fun forwardingAndCredentialsAreRequiredForExport() {
        assertRejected(config.copy(vpnTcpForwardingEnabled = false), "192.168.1.32")
        assertRejected(config.copy(passwordAuthEnabled = false), "192.168.1.32")
        assertRejected(config, "127.0.0.1")
    }

    private fun assertRejected(config: ServerConfig, address: String) {
        var rejected = false
        try {
            MaximusVpnSshProfile.generate(config, address, "ssh-rsa AAAA")
        } catch (_: IllegalArgumentException) {
            rejected = true
        }
        assertTrue(rejected)
    }

    @Test
    fun outboundPolicyDeniesLocalAndSpecialRanges() {
        listOf("127.0.0.1", "10.0.0.1", "192.168.1.1",
            "172.16.10.2", "100.64.0.1", "169.254.169.254",
            "0.0.0.0", "localhost", "example.local", "198.18.0.1",
            "203.0.113.1", "::1", "fd00::1", "fe80::1"
        ).forEach { assertFalse("Must deny $it", PublicDestinationPolicy.isAllowed(it)) }
    }

    @Test
    fun outboundPolicyAllowsPublicIpAddresses() {
        assertTrue(PublicDestinationPolicy.isAllowed("1.1.1.1"))
        assertTrue(PublicDestinationPolicy.isAllowed("8.8.8.8"))
        assertTrue(PublicDestinationPolicy.isAllowed("2606:4700:4700::1111"))
    }
}
