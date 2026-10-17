package com.example.server

import org.apache.sshd.common.config.keys.AuthorizedKeyEntry
import org.apache.sshd.common.config.keys.KeyUtils
import org.apache.sshd.common.config.keys.PublicKeyEntryResolver
import java.security.PublicKey

/** Accept exactly one OpenSSH public key, never multi-line or arbitrary directives. */
object AuthorizedKeyValidator {
    fun parse(input: String): PublicKey? {
        val line = input.trim()
        if (line.isEmpty() || line.length > 16384 ||
            line.contains('\n') || line.contains('\r') || line.contains('\u0000')) return null
        return runCatching {
            AuthorizedKeyEntry.parseAuthorizedKeyEntry(line)
                ?.resolvePublicKey(null, PublicKeyEntryResolver.IGNORING)
        }.getOrNull()
    }

    fun fingerprint(key: PublicKey): String = KeyUtils.getFingerPrint(key)
}
