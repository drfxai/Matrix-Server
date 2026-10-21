package com.example

import com.example.server.SshEndpointSelfCheck
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * An SSH banner returned by this phone is not the same thing as reachability
 * from the other device. The self-check never probes an unassigned IP.
 */
class SshEndpointSelfCheckTest {
    private val interfaces = setOf("192.168.1.24", "10.247.210.65")

    @Test fun privateOverlayIpOwnedByPhoneCanBeLocallyVerified() {
        var called = false
        val r = SshEndpointSelfCheck.inspect("10.247.210.65", 2222, interfaces) { host, port ->
            called = true
            assertTrue(host == "10.247.210.65" && port == 2222)
            true
        }
        assertTrue(called)
        assertTrue(r.localSshVerified)
        assertTrue(r.message.contains("client still needs a network route"))
    }

    @Test fun unassignedPrivateIpIsNotDialedAndNeverClaimsReachability() {
        var called = false
        val r = SshEndpointSelfCheck.inspect("10.247.210.66", 2222, interfaces) { _, _ ->
            called = true
            true
        }
        assertFalse(called)
        assertFalse(r.localSshVerified)
        assertTrue(r.message.contains("NOT assigned"))
        assertTrue(r.message.contains("Nothing was contacted"))
    }

    @Test fun hostnameRequiresExternalClientVerificationWithoutLocalDial() {
        var called = false
        val r = SshEndpointSelfCheck.inspect("matrix.example.com", 2222, interfaces) { _, _ ->
            called = true
            true
        }
        assertFalse(called)
        assertFalse(r.localSshVerified)
        assertTrue(r.message.contains("routing separately"))
    }

    @Test fun boundIpWithoutSshBannerIsNotReportedAsWorking() {
        val r = SshEndpointSelfCheck.inspect("192.168.1.24", 2222, interfaces) { _, _ -> false }
        assertFalse(r.localSshVerified)
        assertTrue(r.message.contains("no SSHv2 identification"))
    }

    @Test fun malformedHostOrPortNeverTriggersNetworkProbe() {
        var calls = 0
        listOf("ssh://192.168.1.24:2222", "127.0.0.1", "192.168.1.24:2222").forEach { host ->
            val r = SshEndpointSelfCheck.inspect(host, 2222, interfaces) { _, _ ->
                calls++
                true
            }
            assertFalse(r.localSshVerified)
        }
        assertFalse(SshEndpointSelfCheck.inspect("192.168.1.24", 65536, interfaces) { _, _ ->
            calls++
            true
        }.localSshVerified)
        assertTrue(calls == 0)
    }
}
