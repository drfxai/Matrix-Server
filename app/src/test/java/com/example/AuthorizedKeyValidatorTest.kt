package com.example

import com.example.server.AuthorizedKeyValidator
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthorizedKeyValidatorTest {
    @Test fun recognizesValidOpenSshEd25519KeyAndRealFingerprint() {
        val sample = "ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAINjD1/4Z03xrQl1HfzcOnaLAhBehtbh36bYarrei+JF8"
        val key = AuthorizedKeyValidator.parse(sample)
        assertNotNull(key)
        val fingerprint = AuthorizedKeyValidator.fingerprint(key!!)
        assertTrue(fingerprint.startsWith("SHA256:"))
        assertEquals(fingerprint, AuthorizedKeyValidator.fingerprint(
            AuthorizedKeyValidator.parse(sample + " laptop-comment")!!
        ))
    }
    @Test fun rejectsMultiLinePayloadsAndUnknownText() {
        assertNull(AuthorizedKeyValidator.parse(""))
        assertNull(AuthorizedKeyValidator.parse("not-a-key"))
        assertNull(AuthorizedKeyValidator.parse("ssh-ed25519 invalid-base64"))
        assertNull(AuthorizedKeyValidator.parse("ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQ\nssh-rsa abc"))
        assertNull(AuthorizedKeyValidator.parse("ssh-ed25519 abc\u0000comment"))
    }
}
