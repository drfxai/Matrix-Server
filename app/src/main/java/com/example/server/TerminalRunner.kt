package com.example.server

import android.content.Context
import com.example.data.model.TerminalLine
import com.example.data.model.TerminalLineType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

class TerminalRunner(
    private val context: Context,
    private val linuxEnv: LinuxEnvironment
) {
    private var currentDir: File = linuxEnv.homeDir

    private val _outputLines = MutableStateFlow<List<TerminalLine>>(emptyList())
    val outputLines: StateFlow<List<TerminalLine>> = _outputLines.asStateFlow()

    private val _isExecuting = MutableStateFlow(false)
    val isExecuting: StateFlow<Boolean> = _isExecuting.asStateFlow()

    private val commandHistory = mutableListOf<String>()

    init {
        linuxEnv.initializeEnvironment()
        appendLine("MATRIX ANDROID SHELL v1.0", TerminalLineType.SYSTEM)
        appendLine("Environment: POSIX userspace (/system/bin/sh)", TerminalLineType.SYSTEM)
        appendLine("Type 'help' or 'matrix-cli' for system information.", TerminalLineType.SYSTEM)
        appendPromptLine()
    }

    suspend fun executeCommand(commandStr: String) = withContext(Dispatchers.IO) {
        val trimmed = commandStr.trim()
        if (trimmed.isEmpty()) return@withContext

        // Initialize app-private Android shell files; no Linux distribution is bundled.
        linuxEnv.initializeEnvironment()

        commandHistory.add(trimmed)
        appendLine(trimmed, TerminalLineType.PROMPT)
        _isExecuting.value = true

        try {
            when {
                trimmed == "clear" -> {
                    _outputLines.value = emptyList()
                    appendPromptLine()
                    return@withContext
                }
                trimmed == "help" -> {
                    appendLine("Available Android Shell Commands:", TerminalLineType.SYSTEM)
                    appendLine("  matrix-cli [status|projects] - Local status and project listing", TerminalLineType.STDOUT)
                    appendLine("  apt, Python and Node.js require a separately installed compatible runtime.", TerminalLineType.STDOUT)
                    appendLine("  ls, ps, top, cat, mkdir, cp, rm - Native POSIX commands", TerminalLineType.STDOUT)
                    appendLine("  uname -a, df -h, free, id - Hardware and OS diagnostics", TerminalLineType.STDOUT)
                    appendLine("  clear - Clear terminal screen", TerminalLineType.STDOUT)
                    appendPromptLine()
                    return@withContext
                }
                trimmed.startsWith("cd ") -> {
                    val target = trimmed.substring(3).trim()
                    handleCd(target)
                    appendPromptLine()
                    return@withContext
                }
                trimmed == "cd" -> {
                    currentDir = linuxEnv.homeDir
                    appendPromptLine()
                    return@withContext
                }
            }

            // Command execution via POSIX sh
            val pb = ProcessBuilder("/system/bin/sh", "-c", trimmed)
            pb.directory(currentDir)
            val env = pb.environment()
            env["HOME"] = linuxEnv.homeDir.absolutePath
            env["PATH"] = "${linuxEnv.binDir.absolutePath}:/sbin:/system/sbin:/system/bin:/system/xbin:/odm/bin:/vendor/bin"
            env["USER"] = "matrix"
            env["SHELL"] = "/system/bin/sh"
            env["TERM"] = "xterm-256color"
            env["MATRIX_SERVER"] = "1"

            val process = pb.start()

            val stdoutReader = BufferedReader(InputStreamReader(process.inputStream))
            val stderrReader = BufferedReader(InputStreamReader(process.errorStream))

            var outLine: String?
            while (stdoutReader.readLine().also { outLine = it } != null) {
                outLine?.let { appendLine(it, TerminalLineType.STDOUT) }
            }

            var errLine: String?
            while (stderrReader.readLine().also { errLine = it } != null) {
                errLine?.let { appendLine(it, TerminalLineType.STDERR) }
            }

            val exitCode = process.waitFor()
            if (exitCode != 0) {
                appendLine("[Process exited with code $exitCode]", TerminalLineType.STDERR)
            }
        } catch (e: Exception) {
            appendLine("Command error: ${e.message}", TerminalLineType.STDERR)
        } finally {
            _isExecuting.value = false
            appendPromptLine()
        }
    }

    private fun handleCd(pathStr: String) {
        val target = if (pathStr.startsWith("/")) {
            File(pathStr)
        } else if (pathStr.startsWith("~")) {
            File(linuxEnv.homeDir, pathStr.removePrefix("~").removePrefix("/"))
        } else {
            File(currentDir, pathStr)
        }

        if (target.exists() && target.isDirectory) {
            currentDir = target.canonicalFile
        } else {
            appendLine("cd: no such file or directory: $pathStr", TerminalLineType.STDERR)
        }
    }

    fun clearScreen() {
        _outputLines.value = emptyList()
        appendPromptLine()
    }

    private fun appendPromptLine() {
        val relDir = if (currentDir == linuxEnv.homeDir) "~" else currentDir.name
        appendLine("matrix@android:$relDir$ ", TerminalLineType.PROMPT)
    }

    private fun appendLine(text: String, type: TerminalLineType) {
        val current = _outputLines.value.toMutableList()
        current.add(TerminalLine(text = text, type = type))
        if (current.size > 500) {
            current.removeAt(0)
        }
        _outputLines.value = current
    }
}
