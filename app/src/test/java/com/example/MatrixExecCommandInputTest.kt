package com.example

import com.example.data.model.ServerConfig
import com.example.server.LinuxEnvironment
import com.example.server.MatrixExecCommand
import org.apache.sshd.server.ExitCallback
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.lang.reflect.Proxy
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * The Maximus Panels SSH client uses exec("sh -s") with a shell script on channel stdin.
 * This test runs the exact exec-command bridge using the CI host's POSIX shell.
 * It tests stdin/stdout/stderr and exit propagation; it is not a remote VPN test.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MatrixExecCommandInputTest {
    private data class Execution(val code: Int, val stdout: String, val stderr: String)

    private fun execute(command: String, stdin: String): Execution {
        val context = RuntimeEnvironment.getApplication()
        val env = LinuxEnvironment(context)
        check(env.homeDir.isDirectory || env.homeDir.mkdirs())
        val stdout = ByteArrayOutputStream()
        val stderr = ByteArrayOutputStream()
        var exitCode: Int? = null
        val callback = Proxy.newProxyInstance(
            ExitCallback::class.java.classLoader,
            arrayOf(ExitCallback::class.java)
        ) { _, method, args ->
            if (method.name == "onExit") exitCode = args?.getOrNull(0) as? Int
            null
        } as ExitCallback
        val cmd = MatrixExecCommand(
            env, ServerConfig(), command, shellExecutable = "/bin/sh"
        )
        cmd.setInputStream(ByteArrayInputStream(stdin.toByteArray(Charsets.UTF_8)))
        cmd.setOutputStream(stdout)
        cmd.setErrorStream(stderr)
        cmd.setExitCallback(callback)

        // Prevent a regression from wedging the whole CI test suite.
        val pool = Executors.newSingleThreadExecutor()
        try {
            pool.submit { cmd.run() }.get(12, TimeUnit.SECONDS)
        } finally {
            pool.shutdownNow()
        }
        return Execution(
            checkNotNull(exitCode) { "SSH exec callback was not invoked" },
            stdout.toString("UTF-8"),
            stderr.toString("UTF-8")
        )
    }

    @Test fun standardInputShellScriptExecutesToCompletion() {
        // Use an absolute shell path since Android's deliberately restricted PATH is
        // retained in the tested command's child environment.
        val result = execute("/bin/sh -s", "printf 'matrix-stdin-ok\\n'\n")
        assertEquals(0, result.code)
        assertEquals("matrix-stdin-ok", result.stdout.trim())
        assertTrue(result.stderr.isBlank())
    }

    @Test fun remoteScriptStderrAndFailureExitArePreserved() {
        val result = execute("/bin/sh -s", "printf 'result\\n'; printf 'error\\n' >&2; exit 7\n")
        assertEquals(7, result.code)
        assertEquals("result", result.stdout.trim())
        assertEquals("error", result.stderr.trim())
    }

    @Test fun emptyExecInputClosesStdinAndDoesNotHang() {
        val result = execute("/bin/sh -s", "")
        assertEquals(0, result.code)
    }
}
