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
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
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
        val yaml = MaximusVpnSshProfile.generate(
            modified, "192.168.1.32", "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQCtest"
        )
        assertTrue(yaml.startsWith("proxies:\n"))
        assertTrue(yaml.contains("    type: ssh"))
        assertTrue(yaml.contains("    server: ${JSONObject.quote("192.168.1.32")}"))
        assertTrue(yaml.contains("    port: 2222"))
        assertTrue(yaml.contains("    username: ${JSONObject.quote("matrix")}"))
        assertTrue(yaml.contains("    password: ${JSONObject.quote(modified.password)}"))
        assertTrue(yaml.contains("      - ${JSONObject.quote("ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQCtest")}"))
        assertTrue(yaml.contains("    host-key-algorithms:"))
        assertTrue(yaml.contains("      - rsa"))
        assertTrue(yaml.contains("    udp: false"))
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

    @Test
    fun privateOverlayAddressIsExportedExactlyAndNotReplacedByLanIp() {
        val yaml = MaximusVpnSshProfile.generate(config, "10.247.210.65",
            "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQCtest")
        assertTrue(yaml.contains("server: \"10.247.210.65\""))
        assertTrue(yaml.startsWith("proxies:"))
        assertTrue(yaml.contains("udp: false"))
    }

    @Test
    fun invalidOrMisleadingEndpointsAreRejectedBeforeSecretExport() {
        listOf("127.0.0.1", "localhost", "0.0.0.0", "ssh://10.247.210.65:2222",
            "10.247.210.65:2222", "10.247.210.999", "10.247.210.65\\nmalicious",
            "[::1]", "user@host.example", "http://host.example", "fd00::1%wlan0")
            .forEach { address ->
                assertRejected(config, address)
            }
        assertTrue(com.example.server.ExportEndpointPolicy.isValid("192.168.1.12"))
        assertTrue(com.example.server.ExportEndpointPolicy.isValid("10.247.210.65"))
        assertTrue(com.example.server.ExportEndpointPolicy.isValid("matrix.example.com"))
        assertTrue(com.example.server.ExportEndpointPolicy.isValid("fd00::1234"))
    }
}
