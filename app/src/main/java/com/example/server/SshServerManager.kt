package com.example.server

import android.content.Context
import android.util.Log
import com.example.data.local.ServerDatabase
import com.example.data.model.ServerConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.apache.sshd.common.config.keys.AuthorizedKeyEntry
import org.apache.sshd.common.config.keys.PublicKeyEntry
import org.apache.sshd.common.config.keys.KeyUtils
import org.apache.sshd.common.config.keys.PublicKeyEntryResolver
import org.apache.sshd.common.session.Session
import org.apache.sshd.common.session.SessionListener
import org.apache.sshd.server.Environment
import org.apache.sshd.server.ExitCallback
import org.apache.sshd.server.SshServer
import org.apache.sshd.server.channel.ChannelSession
import org.apache.sshd.server.command.Command
import org.apache.sshd.server.auth.password.PasswordAuthenticator
import org.apache.sshd.server.auth.pubkey.PublickeyAuthenticator
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider
import org.apache.sshd.sftp.server.SftpSubsystemFactory
import org.apache.sshd.server.forward.ForwardingFilter
import org.apache.sshd.server.forward.RejectAllForwardingFilter
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.net.ServerSocket
import java.security.PublicKey
import java.security.MessageDigest
import java.nio.charset.StandardCharsets
import java.util.concurrent.atomic.AtomicInteger

class SshServerManager(
    private val context: Context,
    private val linuxEnvironment: LinuxEnvironment
) {
    private val tag = "SshServerManager"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var sshServer: SshServer? = null

    init {
        val home = linuxEnvironment.homeDir
        if (!home.exists()) home.mkdirs()
        try {
            System.setProperty("user.home", home.absolutePath)
            org.apache.sshd.common.util.io.PathUtils.setUserHomeFolderResolver { home.toPath() }
        } catch (_: Throwable) {}
    }

    private val _activeSessions = MutableStateFlow(0)
    val activeSessions: StateFlow<Int> = _activeSessions.asStateFlow()

    private val _isServerRunning = MutableStateFlow(false)
    val isServerRunning: StateFlow<Boolean> = _isServerRunning.asStateFlow()

    private val sessionCounter = AtomicInteger(0)

    fun startServer(config: ServerConfig): Result<Unit> {
        return try {
            if (_isServerRunning.value) {
                return Result.success(Unit)
            }

            if (!SshAuthPolicy.canStart(config, config.publicKeyAuthEnabled && hasAuthorizedKeys())) {
                return Result.failure(IllegalStateException(
                    "Configure a strong password or at least one valid authorized SSH public key"
                ))
            }

            // Verify port availability
            if (!isPortAvailable(config.port)) {
                return Result.failure(IllegalStateException("Port ${config.port} is already in use by another service"))
            }

            linuxEnvironment.initializeEnvironment()

            val home = linuxEnvironment.homeDir
            if (!home.exists()) home.mkdirs()
            try {
                System.setProperty("user.home", home.absolutePath)
                org.apache.sshd.common.util.io.PathUtils.setUserHomeFolderResolver { home.toPath() }
            } catch (_: Throwable) {}

            val server = SshServer.setUpDefaultServer()
            server.port = config.port
            // Bind to all interfaces for intentional LAN access; never permit anonymous logins.
            server.host = "0.0.0.0"
            // SSH port forwarding is OFF by default in MINA. Without direct-tcpip,
            // Mihomo SSH profiles can authenticate but cannot transport TCP traffic.
            // Enable only explicitly: no remote listeners, agent or X11 forwarding.
            server.forwardingFilter = if (config.vpnTcpForwardingEnabled) {
                ForwardingFilter.asForwardingFilter(null, null, PublicTcpForwardingFilter)
            } else {
                RejectAllForwardingFilter.INSTANCE
            }

            // Setup Host Key Provider
            val keyProvider = SimpleGeneratorHostKeyProvider(linuxEnvironment.hostKeyFile.toPath())
            keyProvider.algorithm = "RSA"
            server.keyPairProvider = keyProvider

            // Setup Password Authentication
            server.passwordAuthenticator = PasswordAuthenticator { username, password, _ ->
                config.passwordAuthEnabled && username == config.username &&
                    SshAuthPolicy.validPassword(config.password) &&
                    MessageDigest.isEqual(
                        password.toByteArray(StandardCharsets.UTF_8),
                        config.password.toByteArray(StandardCharsets.UTF_8)
                    )
            }

            // Setup Public Key Authentication
            server.publickeyAuthenticator = PublickeyAuthenticator { username, key, _ ->
                if (config.publicKeyAuthEnabled && username == config.username) {
                    val isAuthorized = verifyPublicKey(key)
                    isAuthorized
                } else {
                    false
                }
            }

            // Interactive Shell Factory connecting to /system/bin/sh
            server.shellFactory = org.apache.sshd.server.shell.ShellFactory { channel ->
                MatrixShellCommand(linuxEnvironment, config)
            }

            // Direct Command Execution (e.g. ssh user@host "ls -la")
            server.commandFactory = org.apache.sshd.server.command.CommandFactory { channel, command ->
                MatrixExecCommand(linuxEnvironment, config, command)
            }

            // SFTP Subsystem for remote file transfer
            val sftpFactory = SftpSubsystemFactory()
            server.subsystemFactories = listOf(sftpFactory)

            // Session Listener
            server.addSessionListener(object : SessionListener {
                override fun sessionCreated(session: Session) {
                    _activeSessions.value = sessionCounter.incrementAndGet()
                }

                override fun sessionClosed(session: Session) {
                    _activeSessions.value = sessionCounter.updateAndGet { current ->
                        if (current > 0) current - 1 else 0
                    }
                }
            })

            server.start()
            sshServer = server
            _isServerRunning.value = true
            Log.i(tag, "MATRIX SSH Server started successfully on port ${config.port}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Failed to start SSH server", e)
            _isServerRunning.value = false
            Result.failure(e)
        }
    }

    /** Exactly the same public host key used by the live SSH listener, for Mihomo pinning. */
    fun currentHostPublicKey(): String? = runCatching {
        val provider = sshServer?.keyPairProvider ?: return@runCatching null
        val key = provider.loadKeys(null).firstOrNull()?.public ?: return@runCatching null
        PublicKeyEntry.toString(key)
    }.getOrNull()

    @Synchronized
    fun stopServer() {
        try {
            sshServer?.stop(true)
            Log.i(tag, "MATRIX SSH Server stopped")
        } catch (e: Exception) {
            Log.e(tag, "Error stopping SSH server", e)
        } finally {
            // Never retain stale ONLINE state if shutdown itself throws.
            sshServer = null
            _isServerRunning.value = false
            _activeSessions.value = 0
            sessionCounter.set(0)
        }
    }

    private fun hasAuthorizedKeys(): Boolean = authorizedKeys().isNotEmpty()

    private fun verifyPublicKey(key: PublicKey): Boolean =
        authorizedKeys().any { trusted -> KeyUtils.compareKeys(trusted, key) }

    private fun authorizedKeys(): List<PublicKey> {
        val file = linuxEnvironment.authorizedKeysFile
        if (!file.isFile || file.length() == 0L) return emptyList()
        return try {
            AuthorizedKeyEntry.readAuthorizedKeys(file.toPath()).mapNotNull {
                it.resolvePublicKey(null, PublicKeyEntryResolver.IGNORING)
            }
        } catch (e: Exception) {
            Log.w(tag, "Invalid authorized_keys file; denying SSH key authentication", e)
            emptyList()
        }
    }

    private fun isPortAvailable(port: Int): Boolean {
        return try {
            ServerSocket(port).use { true }
        } catch (e: Exception) {
            false
        }
    }
}

/**
 * Interactive Shell Command for PTY sessions connected to /system/bin/sh
 */
class MatrixShellCommand(
    private val linuxEnv: LinuxEnvironment,
    private val config: ServerConfig
) : Command, Runnable {

    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null
    private var errorStream: OutputStream? = null
    private var exitCallback: ExitCallback? = null
    private var process: Process? = null
    private var thread: Thread? = null

    override fun setInputStream(`in`: InputStream) {
        this.inputStream = `in`
    }

    override fun setOutputStream(out: OutputStream) {
        this.outputStream = out
    }

    override fun setErrorStream(err: OutputStream) {
        this.errorStream = err
    }

    override fun setExitCallback(callback: ExitCallback) {
        this.exitCallback = callback
    }

    override fun start(channel: ChannelSession, env: Environment) {
        thread = Thread(this, "MatrixShellSession").apply { start() }
    }

    override fun run() {
        try {
            val pb = ProcessBuilder("/system/bin/sh", "-i")
            pb.directory(linuxEnv.homeDir)
            val pEnv = pb.environment()
            pEnv["HOME"] = linuxEnv.homeDir.absolutePath
            pEnv["PATH"] = "${linuxEnv.binDir.absolutePath}:/sbin:/system/sbin:/system/bin:/system/xbin:/odm/bin:/vendor/bin"
            pEnv["USER"] = config.username
            pEnv["LOGNAME"] = config.username
            pEnv["SHELL"] = "/system/bin/sh"
            pEnv["TERM"] = "xterm-256color"
            pEnv["MATRIX_SERVER"] = "1"
            pEnv["PROJECTS_DIR"] = linuxEnv.projectsDir.absolutePath

            process = pb.start()
            val proc = process ?: return

            // Output banner to client
            outputStream?.write("\r\n\u001B[32;1m=== MATRIX SERVER ANDROID SHELL ===\u001B[0m\r\n".toByteArray())
            outputStream?.write("\u001B[32mLogged in as: ${config.username}\u001B[0m\r\n".toByteArray())
            outputStream?.write("\u001B[32mType 'matrix-cli' for system controls and projects.\u001B[0m\r\n\r\n".toByteArray())
            outputStream?.flush()

            // Stream proc stdout -> SSH outputStream
            val outThread = Thread {
                try {
                    proc.inputStream.copyTo(outputStream!!)
                } catch (_: Exception) {}
            }
            outThread.start()

            // Stream proc stderr -> SSH errorStream
            val errThread = Thread {
                try {
                    proc.errorStream.copyTo(errorStream ?: outputStream!!)
                } catch (_: Exception) {}
            }
            errThread.start()

            // Stream SSH inputStream -> proc stdin
            val inThread = Thread {
                try {
                    inputStream?.copyTo(proc.outputStream)
                } catch (_: Exception) {}
            }
            inThread.start()

            val exitCode = proc.waitFor()
            exitCallback?.onExit(exitCode)
        } catch (e: Exception) {
            outputStream?.write("\r\nShell process error: ${e.message}\r\n".toByteArray())
            outputStream?.flush()
            exitCallback?.onExit(-1, e.message)
        }
    }

    override fun destroy(channel: ChannelSession) {
        process?.destroy()
        thread?.interrupt()
    }
}

/**
 * Non-interactive Command execution (e.g., `ssh user@host "command"`)
 */
class MatrixExecCommand(
    private val linuxEnv: LinuxEnvironment,
    private val config: ServerConfig,
    private val commandString: String,
    // Injectable only for desktop JVM integration tests. Android always uses its own shell.
    private val shellExecutable: String = "/system/bin/sh"
) : Command, Runnable {

    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null
    private var errorStream: OutputStream? = null
    private var exitCallback: ExitCallback? = null
    private var process: Process? = null
    private var thread: Thread? = null

    override fun setInputStream(`in`: InputStream) { this.inputStream = `in` }
    override fun setOutputStream(out: OutputStream) { this.outputStream = out }
    override fun setErrorStream(err: OutputStream) { this.errorStream = err }
    override fun setExitCallback(callback: ExitCallback) { this.exitCallback = callback }

    override fun start(channel: ChannelSession, env: Environment) {
        thread = Thread(this, "MatrixExecSession").apply { start() }
    }

    override fun run() {
        try {
            val pb = ProcessBuilder(shellExecutable, "-c", commandString)
            pb.directory(linuxEnv.homeDir)
            val pEnv = pb.environment()
            pEnv["HOME"] = linuxEnv.homeDir.absolutePath
            pEnv["PATH"] = "${linuxEnv.binDir.absolutePath}:/sbin:/system/sbin:/system/bin:/system/xbin:/odm/bin:/vendor/bin"
            pEnv["USER"] = config.username
            pEnv["TERM"] = "xterm-256color"
            pEnv["MATRIX_SERVER"] = "1"

            process = pb.start()
            val proc = process ?: return

            val outThread = Thread {
                try { proc.inputStream.copyTo(outputStream!!) } catch (_: Exception) {}
            }
            outThread.start()

            val errThread = Thread {
                try { proc.errorStream.copyTo(errorStream ?: outputStream!!) } catch (_: Exception) {}
            }
            errThread.start()

            // SSH exec clients (including Maximus Panels) invoke `sh -s` and send
            // their scripts through the SSH channel's stdin. Without this pump,
            // `sh -s` blocks indefinitely or receives no script.
            val inThread = Thread({
                try {
                    proc.outputStream.use { stdin ->
                        inputStream?.copyTo(stdin)
                    }
                } catch (_: java.io.IOException) {
                    // The child may close stdin early; its exit status is authoritative.
                }
            }, "MatrixExecStdin").apply {
                isDaemon = true
                start()
            }

            val exitCode = proc.waitFor()
            // Do not report completion until stdout and stderr have drained.
            outThread.join(2000)
            errThread.join(2000)
            outputStream?.flush()
            exitCallback?.onExit(exitCode)
        } catch (e: Exception) {
            outputStream?.write("Execution failed: ${e.message}\n".toByteArray())
            outputStream?.flush()
            exitCallback?.onExit(-1, e.message)
        }
    }

    override fun destroy(channel: ChannelSession) {
        process?.destroy()
        thread?.interrupt()
    }
}
