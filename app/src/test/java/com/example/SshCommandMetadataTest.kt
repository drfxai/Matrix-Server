package com.example

import com.example.data.model.ServerMetrics
import org.junit.Assert.assertEquals
import org.junit.Test

class SshCommandMetadataTest {
    @Test fun customSshUsernameIsReflectedInCopiedCommands() {
        val m = ServerMetrics(
            lanIpAddress = "192.168.1.20",
            vpnIpAddress = "10.247.210.65",
            sshPort = 2222,
            sshUsername = "my-ssh-user"
        )
        assertEquals("ssh my-ssh-user@192.168.1.20 -p 2222", m.sshCommand)
        assertEquals("ssh my-ssh-user@10.247.210.65 -p 2222", m.vpnSshCommand)
    }
}
