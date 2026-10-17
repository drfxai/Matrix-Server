package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.ServerPreferences
import com.example.data.model.ServerConfig
import com.example.data.model.ServerMetrics
import com.example.data.model.ServerState
import com.example.server.LinuxEnvironment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun testAppNameIsMatrixServer() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Matrix Server", appName)
    }

    @Test
    fun testServerPreferencesDefaultsAndUpdates() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = ServerPreferences(context)
        val initialConfig = prefs.loadConfig()

        assertEquals(2222, initialConfig.port)
        assertEquals("matrix", initialConfig.username)
        assertFalse(initialConfig.passwordAuthEnabled)
        assertEquals("", initialConfig.password)
        assertTrue(initialConfig.publicKeyAuthEnabled)
        assertTrue(initialConfig.wakeLockEnabled)

        // Test updating config
        val updatedConfig = initialConfig.copy(port = 8022, username = "neo", wakeLockEnabled = false)
        prefs.updateConfig(updatedConfig)

        val loaded = prefs.loadConfig()
        assertEquals(8022, loaded.port)
        assertEquals("neo", loaded.username)
        assertFalse(loaded.wakeLockEnabled)
    }

    @Test
    fun testLinuxEnvironmentInitialization() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val linuxEnv = LinuxEnvironment(context)
        linuxEnv.initializeEnvironment()

        assertTrue(linuxEnv.homeDir.exists())
        assertTrue(linuxEnv.projectsDir.exists())
        assertTrue(linuxEnv.binDir.exists())
        assertTrue(linuxEnv.sshDir.exists())
        assertTrue(linuxEnv.authorizedKeysFile.exists())

        // Check sample projects were seeded
        val webDir = File(linuxEnv.projectsDir, "web-server")
        val apiDir = File(linuxEnv.projectsDir, "api-microservice")
        val workerDir = File(linuxEnv.projectsDir, "system-worker")

        assertTrue(webDir.exists())
        assertTrue(apiDir.exists())
        assertTrue(workerDir.exists())
        assertTrue(File(webDir, "start.sh").exists())
    }

    @Test
    fun testServerMetricsGeneration() {
        val metrics = ServerMetrics(
            lanIpAddress = "192.168.1.150",
            sshPort = 2222,
            uptimeSeconds = 3665,
            activeSessionsCount = 2,
            runningProjectsCount = 3
        )

        assertEquals("ssh matrix@192.168.1.150 -p 2222", metrics.sshCommand)
        assertEquals("01:01:05", metrics.formattedUptime)
        assertEquals(2, metrics.activeSessionsCount)
        assertEquals(3, metrics.runningProjectsCount)
    }

    @Test
    fun testServerStateTransitions() {
        assertTrue(ServerState.ON.isRunning)
        assertTrue(ServerState.DEGRADED.isRunning)
        assertFalse(ServerState.OFF.isRunning)
        assertTrue(ServerState.STARTING.isTransitioning)
        assertTrue(ServerState.STOPPING.isTransitioning)
        assertFalse(ServerState.ON.isTransitioning)
    }

    @Test
    fun testHostKeyFingerprintFormat() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val linuxEnv = LinuxEnvironment(context)
        val fp = linuxEnv.getHostKeyFingerprint()
        assertNotNull(fp)
        assertTrue(fp.startsWith("SHA256:"))
    }

    @Test
    fun testJavaxManagementStubsResolution() {
        val ex = Exception("inner test exception")
        val reflectionEx = javax.management.ReflectionException(ex)
        assertEquals(ex, reflectionEx.targetException)

        val mbeanEx = javax.management.MBeanException(ex)
        assertEquals(ex, mbeanEx.targetException)
    }

    @Test
    fun testConnectionHealthEvaluation() {
        val runningHealth = com.example.data.model.ConnectionHealth(
            status = com.example.data.model.ServerStatusType.RUNNING,
            ipAddress = "192.168.1.100",
            port = 2222,
            isSocketResponsive = true,
            socketProbeLatencyMs = 4L
        )
        assertTrue(runningHealth.isRunning)
        assertFalse(runningHealth.isIssue)
        assertEquals("4ms", runningHealth.formattedLatency)

        val issueHealth = com.example.data.model.ConnectionHealth(
            status = com.example.data.model.ServerStatusType.CONNECTION_ISSUE,
            issueType = com.example.data.model.ConnectionIssueType.NO_NETWORK,
            headline = "No Network",
            isSocketResponsive = true
        )
        assertFalse(issueHealth.isRunning)
        assertTrue(issueHealth.isIssue)

        val stoppedHealth = com.example.data.model.ConnectionHealth(
            status = com.example.data.model.ServerStatusType.STOPPED
        )
        assertFalse(stoppedHealth.isRunning)
        assertFalse(stoppedHealth.isIssue)
    }
}
