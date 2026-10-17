package com.example

import com.example.data.model.ServerConfig
import com.example.server.SshAuthPolicy
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SshAuthPolicyTest {
    @Test fun refusesDefaultAndWeakPassword() {
        assertFalse(SshAuthPolicy.validPassword(""))
        assertFalse(SshAuthPolicy.validPassword("short"))
        assertTrue(SshAuthPolicy.validPassword("correct-horse-staple"))
    }

    @Test fun refusesUnauthenticatedServer() {
        assertFalse(SshAuthPolicy.canStart(ServerConfig(), false))
        assertFalse(SshAuthPolicy.canStart(ServerConfig(publicKeyAuthEnabled = true), false))
    }

    @Test fun acceptsConfiguredMethodOnly() {
        val keyOnly = ServerConfig(publicKeyAuthEnabled = true)
        assertTrue(SshAuthPolicy.canStart(keyOnly, true))
        val passwordOnly = ServerConfig(
            passwordAuthEnabled = true,
            password = "very-long-random-passphrase",
            publicKeyAuthEnabled = false
        )
        assertTrue(SshAuthPolicy.canStart(passwordOnly, false))
        assertFalse(SshAuthPolicy.canStart(passwordOnly.copy(port = 22), false))
        assertFalse(SshAuthPolicy.canStart(passwordOnly.copy(username = "bad user"), false))
    }
}
