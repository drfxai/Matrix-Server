package com.example.server

import com.example.data.model.ServerConfig

/** Centralized fail-closed checks used before opening a network listening socket. */
object SshAuthPolicy {
    fun validPassword(password: String): Boolean = password.length in 12..128

    fun validUsername(username: String): Boolean =
        username.matches(Regex("^[A-Za-z0-9._-]{1,32}$"))

    fun canStart(config: ServerConfig, hasAuthorizedKey: Boolean): Boolean {
        if (config.port !in 1024..65535 || !validUsername(config.username)) return false
        val passwordReady = config.passwordAuthEnabled && validPassword(config.password)
        val keysReady = config.publicKeyAuthEnabled && hasAuthorizedKey
        return passwordReady || keysReady
    }
}
