package com.example

import com.example.server.AuthorizedKeyValidator
import org.junit.Assert.assertNull
import org.junit.Test

class AuthorizedKeyValidatorTest {
    @Test fun rejectsMultiLinePayloadsAndUnknownText() {
        assertNull(AuthorizedKeyValidator.parse(""))
        assertNull(AuthorizedKeyValidator.parse("not-a-key"))
        assertNull(AuthorizedKeyValidator.parse("ssh-ed25519 invalid-base64"))
        assertNull(AuthorizedKeyValidator.parse("ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQ\nssh-rsa abc"))
        assertNull(AuthorizedKeyValidator.parse("ssh-ed25519 abc\u0000comment"))
    }
}
