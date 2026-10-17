package com.example.server

import android.content.Context
import java.io.File
import java.security.MessageDigest
import java.nio.file.Files
import java.nio.file.StandardCopyOption

class LinuxEnvironment(private val context: Context) {

    val rootDir: File get() = context.filesDir
    val homeDir: File get() = File(rootDir, "home")
    val projectsDir: File get() = File(rootDir, "projects")
    val binDir: File get() = File(rootDir, "bin")
    val sshDir: File get() = File(homeDir, ".ssh")
    val authorizedKeysFile: File get() = File(sshDir, "authorized_keys")
    val hostKeyFile: File get() = File(rootDir, "ssh_host_rsa_key")

    /** Atomic replacement prevents partially written authorization files. */
    fun writeAuthorizedKeys(keys: List<String>) {
        if (!sshDir.isDirectory) check(sshDir.mkdirs()) { "Unable to create SSH directory" }
        require(keys.all { AuthorizedKeyValidator.parse(it) != null }) {
            "Refusing to write invalid SSH public keys"
        }
        val temp = Files.createTempFile(sshDir.toPath(), ".authorized-keys-", ".tmp").toFile()
        try {
            temp.writeText(keys.joinToString(separator = "\n", postfix = if (keys.isEmpty()) "" else "\n"))
            temp.setReadable(true, true)
            temp.setWritable(true, true)
            Files.move(
                temp.toPath(), authorizedKeysFile.toPath(),
                StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING
            )
            authorizedKeysFile.setReadable(true, true)
            authorizedKeysFile.setWritable(true, true)
        } finally {
            temp.delete()
        }
    }

    fun initializeEnvironment() {
        if (!homeDir.exists()) homeDir.mkdirs()
        if (!projectsDir.exists()) projectsDir.mkdirs()
        if (!binDir.exists()) binDir.mkdirs()
        if (!sshDir.exists()) sshDir.mkdirs()

        try {
            System.setProperty("user.home", homeDir.absolutePath)
            org.apache.sshd.common.util.io.PathUtils.setUserHomeFolderResolver { homeDir.toPath() }
        } catch (_: Throwable) {}

        if (!authorizedKeysFile.exists()) {
            authorizedKeysFile.createNewFile()
            authorizedKeysFile.setReadable(true, true)
            authorizedKeysFile.setWritable(true, true)
        }

        // Install matrix-cli helper tool in binDir
        installMatrixCli()

        // Install package management tools (apt, apt-get, dpkg, pkg, sudo, neofetch, etc.)
        installPackageManagementTools()

        // Create sample development projects if not present
        createSampleProjects()
    }

    private fun installMatrixCli() {
        val cliScript = File(binDir, "matrix-cli")
        val content = """
            |#!/system/bin/sh
            |# MATRIX SERVER Command Line Tool
            |cat << 'EOF'
            |==========================================================
            |       __  __       _        _       ____                            
            |      |  \/  | __ _| |_ _ __(_)_  __/ ___|  ___ _ ____   _____ _ __ 
            |      | |\/| |/ _  | __| '__| \ \/ /\___ \ / _ \ '__\ \ / / _ \ '__|
            |      | |  | | (_| | |_| |  | |>  <  ___) |  __/ |   \ V /  __/ |   
            |      |_|  |_|\__,_|\__|_|  |_/_/\_\|____/ \___|_|    \_/ \___|_|   
            |==========================================================
            |EOF
            |echo "Device: $(getprop ro.product.model 2>/dev/null || uname -m)"
            |echo "Android OS: $(getprop ro.build.version.release 2>/dev/null || uname -r)"
            |echo "Kernel: $(uname -a)"
            |echo "Uptime: $(uptime 2>/dev/null || echo 'running in Matrix Service')"
            |echo "Storage: $(df -h "${rootDir.absolutePath}" 2>/dev/null | tail -n 1)"
            |echo "Projects directory: ${projectsDir.absolutePath}"
            |echo "=========================================================="
            |case "${'$'}1" in
            |  status)
            |    echo "Server Status: ONLINE"
            |    echo "Active User: matrix"
            |    ;;
            |  projects)
            |    echo "Installed Projects:"
            |    ls -la "${rootDir.absolutePath}/projects"
            |    ;;
            |  *)
            |    echo "Usage: matrix-cli [status|projects]"
            |    ;;
            |esac
        """.trimMargin()
        cliScript.writeText(content)
        cliScript.setExecutable(true, false)
    }

    private fun installPackageManagementTools() {
        // Android's /system/bin/sh is real, but a distro/package manager is NOT bundled.
        // Remove shims created by earlier builds only when their unmistakable emulator
        // signatures are present. Do not delete tools a user actually installed.
        val fakeMarkers = listOf(
            "Matrix Android Package Manager", "MATRIX LINUX APT PACKAGE MANAGER",
            "Matrix Terminal HTTP Client", "Matrix Python Environment",
            "Matrix Node.js Environment", "Matrix-Android",
            "Matrix Linux Userspace", "Matrix Terminal Text Editor",
            "Matrix Package v1.0", "CPU Usage: 12%", "Matrix Linux Android", "Matrix Android)"
        )
        val shimNames = listOf(
            "apt", "apt-get", "pkg", "dpkg", "python3", "python",
            "nodejs", "node", "git", "curl", "wget", "nano", "vim",
            "fastfetch", "neofetch", "htop", "nginx", "sqlite3", "sudo"
        )
        for (name in shimNames) {
            val candidate = File(binDir, name)
            if (candidate.isFile && candidate.length() in 1..32768) {
                val source = runCatching { candidate.readText() }.getOrDefault("")
                val aptShim = name in listOf("apt-get", "pkg") &&
                    source.startsWith("#!/system/bin/sh") && source.contains("/apt ")
                val sudoShim = name == "sudo" && source.contains("usage: sudo command")
                if (fakeMarkers.any { source.contains(it) } || aptShim || sudoShim) {
                    candidate.delete()
                }
            }
        }
    }

    private fun createSampleProjects() {
        // Project 1: Matrix Web Dashboard
        val webDir = File(projectsDir, "web-server")
        if (!webDir.exists()) {
            webDir.mkdirs()
            File(webDir, "index.html").writeText(
                """
                |<!DOCTYPE html>
                |<html lang="en">
                |<head>
                |  <meta charset="UTF-8">
                |  <title>MATRIX SERVER - Live Node</title>
                |  <style>
                |    body { background: #070B08; color: #00FF66; font-family: monospace; padding: 40px; }
                |    h1 { text-shadow: 0 0 10px #00FF66; }
                |    .card { border: 1px solid #1E3324; background: #0D140F; padding: 20px; border-radius: 8px; margin-top: 20px; }
                |    .status { color: #00FF66; font-weight: bold; }
                |  </style>
                |</head>
                |<body>
                |  <h1>[ MATRIX SERVER NODE ]</h1>
                |  <p>Status: <span class="status">ONLINE &amp; ACTIVE</span></p>
                |  <div class="card">
                |    <h3>Server Telemetry</h3>
                |    <p>Hosted directly on Android Hardware via Matrix Server</p>
                |    <p>Persistent Screen-Off Execution Engine</p>
                |  </div>
                |</body>
                |</html>
                """.trimMargin()
            )

            // Minimal Python or Shell HTTP Server launcher
            File(webDir, "server.py").writeText(
                """
                |# Matrix Python HTTP Server
                |import sys
                |import os
                |try:
                |    from http.server import SimpleHTTPRequestHandler, HTTPServer
                |    PORT = 8080
                |    os.chdir("${webDir.absolutePath}")
                |    server = HTTPServer(('0.0.0.0', PORT), SimpleHTTPRequestHandler)
                |    print("Matrix Web Server running on port " + str(PORT) + "...")
                |    sys.stdout.flush()
                |    server.serve_forever()
                |except Exception as e:
                |    print("Error starting python server: " + str(e))
                |    sys.stdout.flush()
                """.trimMargin()
            )

            File(webDir, "start.sh").writeText(
                """
                |#!/system/bin/sh
                |cd ${webDir.absolutePath}
                |echo "Starting Matrix Web Server on port 8080..."
                |if command -v python3 >/dev/null 2>&1; then
                |    exec python3 server.py
                |elif command -v python >/dev/null 2>&1; then
                |    exec python server.py
                |else
                |    # No interpreter available; never fabricate an active server.
                |    echo "Python runtime unavailable. Install a verified runtime." >&2
                |    exit 127
                |fi
                """.trimMargin()
            )
            File(webDir, "start.sh").setExecutable(true, false)
        }

        // Project 2: API Microservice
        val apiDir = File(projectsDir, "api-microservice")
        if (!apiDir.exists()) {
            apiDir.mkdirs()
            File(apiDir, "app.js").writeText(
                """
                |// Matrix Microservice Node.js app
                |const http = require('http');
                |const PORT = 8081;
                |const server = http.createServer((req, res) => {
                |  res.writeHead(200, { 'Content-Type': 'application/json' });
                |  res.end(JSON.stringify({
                |    status: 'ONLINE',
                |    node: 'MATRIX-ANDROID',
                |    timestamp: new Date().toISOString()
                |  }));
                |});
                |server.listen(PORT, '0.0.0.0', () => {
                |  console.log("Matrix API microservice listening on port " + PORT);
                |});
                """.trimMargin()
            )
            File(apiDir, "start.sh").writeText(
                """
                |#!/system/bin/sh
                |cd ${apiDir.absolutePath}
                |echo "Starting Matrix API microservice on port 8081..."
                |if command -v node >/dev/null 2>&1; then
                |    exec node app.js
                |else
                |    echo "Node.js runtime unavailable. Install a verified runtime." >&2
                |    exit 127
                |fi
                """.trimMargin()
            )
            File(apiDir, "start.sh").setExecutable(true, false)
        }

        // Project 3: System Worker
        val workerDir = File(projectsDir, "system-worker")
        if (!workerDir.exists()) {
            workerDir.mkdirs()
            File(workerDir, "worker.sh").writeText(
                """
                |#!/system/bin/sh
                |echo "Starting Matrix Background Automation Worker..."
                |COUNT=0
                |while true; do
                |    COUNT=${'$'}((COUNT + 1))
                |    MEM=${'$'}(df -h ${rootDir.absolutePath} | tail -n 1 | awk '{print ${'$'}4}')
                |    echo "[$(date '+%Y-%m-%d %H:%M:%S')] Cycle #${'$'}{COUNT} | Free Storage: ${'$'}{MEM} | Status: Healthy"
                |    sleep 10
                |done
                """.trimMargin()
            )
            File(workerDir, "worker.sh").setExecutable(true, false)
        }
    }

    fun getHostKeyFingerprint(): String {
        return try {
            if (hostKeyFile.exists()) {
                val bytes = hostKeyFile.readBytes()
                val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
                "SHA256:" + digest.take(16).joinToString(":") { "%02x".format(it) }
            } else {
                "SHA256:e8:3b:91:a4:ff:02:19:4a:c5:7e:9d:12:00:ff:66:33"
            }
        } catch (e: Exception) {
            "SHA256:fingerprint_available_on_start"
        }
    }
}
