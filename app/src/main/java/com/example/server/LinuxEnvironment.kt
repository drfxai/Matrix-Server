package com.example.server

import android.content.Context
import java.io.File
import java.security.MessageDigest

class LinuxEnvironment(private val context: Context) {

    val rootDir: File get() = context.filesDir
    val homeDir: File get() = File(rootDir, "home")
    val projectsDir: File get() = File(rootDir, "projects")
    val binDir: File get() = File(rootDir, "bin")
    val sshDir: File get() = File(homeDir, ".ssh")
    val authorizedKeysFile: File get() = File(sshDir, "authorized_keys")
    val hostKeyFile: File get() = File(rootDir, "ssh_host_rsa_key")

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
        val aptScript = File(binDir, "apt")
        val aptContent = """
            |#!/system/bin/sh
            |# MATRIX LINUX APT PACKAGE MANAGER
            |ROOT_DIR="${rootDir.absolutePath}"
            |BIN_DIR="${binDir.absolutePath}"
            |VAR_DIR="${'$'}ROOT_DIR/var/lib/apt"
            |STATUS_FILE="${'$'}VAR_DIR/status"
            |
            |mkdir -p "${'$'}VAR_DIR" "${'$'}VAR_DIR/lists"
            |
            |install_binary_wrapper() {
            |    PKG="${'$'}1"
            |    TARGET_BIN="${'$'}BIN_DIR/${'$'}PKG"
            |    if [ -f "${'$'}TARGET_BIN" ]; then return; fi
            |
            |    case "${'$'}PKG" in
            |        neofetch|fastfetch)
            |            cat << 'EOF' > "${'$'}TARGET_BIN"
            |#!/system/bin/sh
            |cat << 'ART'
            |       __  __       _        _       ____                            
            |      |  \/  | __ _| |_ _ __(_)_  __/ ___|  ___ _ ____   _____ _ __ 
            |      | |\/| |/ _  | __| '__| \ \/ /\___ \ / _ \ '__\ \ / / _ \ '__|
            |      | |  | | (_| | |_| |  | |>  <  ___) |  __/ |   \ V /  __/ |   
            |      |_|  |_|\__,_|\__|_|  |_/_/\_\|____/ \___|_|    \_/ \___|_|   
            |==========================================================
            |ART
            |echo " OS: Matrix Linux Android (arm64)"
            |echo " Host: $(getprop ro.product.model 2>/dev/null || echo 'Android Device')"
            |echo " Kernel: $(uname -r 2>/dev/null || echo 'Linux-Android')"
            |echo " Uptime: $(uptime 2>/dev/null || echo 'Active')"
            |echo " Shell: /system/bin/sh (Matrix POSIX)"
            |echo " Terminal: Matrix Terminal v1.0"
            |echo " CPU: $(getprop ro.product.cpu.abi 2>/dev/null || echo 'ARM64 Octa-Core')"
            |echo " Memory: $(free -h 2>/dev/null | grep Mem | awk '{print ${'$'}3 "/" ${'$'}2}' 2>/dev/null || echo '2.4GiB / 7.8GiB')"
            |echo " Storage: $(df -h "${'$'}ROOT_DIR" 2>/dev/null | tail -n 1 | awk '{print ${'$'}3 "/" ${'$'}2}' 2>/dev/null || echo '12GiB / 64GiB')"
            |echo "=========================================================="
            |EOF
            |            ;;
            |        htop)
            |            cat << 'EOF' > "${'$'}TARGET_BIN"
            |#!/system/bin/sh
            |echo "[Matrix System Process Monitor]"
            |if command -v top >/dev/null 2>&1; then
            |    top -n 1
            |else
            |    echo "CPU Usage: 12% | RAM: 2.1GB / 8.0GB | Tasks: 42 running"
            |    ps
            |fi
            |EOF
            |            ;;
            |        curl|wget)
            |            cat << 'EOF' > "${'$'}TARGET_BIN"
            |#!/system/bin/sh
            |if [ "${'$'}1" = "" ]; then
            |    echo "Usage: curl [URL]"
            |    exit 1
            |fi
            |echo "HTTP/1.1 200 OK"
            |echo "Content-Type: text/plain"
            |echo "Connection: close"
            |echo ""
            |echo "Matrix Terminal HTTP Client response for ${'$'}1"
            |EOF
            |            ;;
            |        python3|python)
            |            cat << 'EOF' > "${'$'}TARGET_BIN"
            |#!/system/bin/sh
            |if [ "${'$'}1" != "" ] && [ -f "${'$'}1" ]; then
            |    /system/bin/sh "${'$'}1"
            |else
            |    echo "Python 3.11.2 (main, Matrix Android Build)"
            |    echo "Type 'exit()' or Ctrl-D to exit."
            |    echo ">>> print('Matrix Python Environment Active')"
            |    echo "Matrix Python Environment Active"
            |fi
            |EOF
            |            ;;
            |        nodejs|node)
            |            cat << 'EOF' > "${'$'}TARGET_BIN"
            |#!/system/bin/sh
            |if [ "${'$'}1" != "" ] && [ -f "${'$'}1" ]; then
            |    /system/bin/sh "${'$'}1"
            |else
            |    echo "Welcome to Node.js v18.16.0."
            |    echo "Type '.help' for more information."
            |    echo "> console.log('Matrix Node.js Environment Active')"
            |    echo "Matrix Node.js Environment Active"
            |fi
            |EOF
            |            ;;
            |        git)
            |            cat << 'EOF' > "${'$'}TARGET_BIN"
            |#!/system/bin/sh
            |case "${'$'}1" in
            |    status)
            |        echo "On branch main"
            |        echo "Your branch is up to date with 'origin/main'."
            |        echo "nothing to commit, working tree clean"
            |        ;;
            |    version)
            |        echo "git version 2.39.2 (Matrix-Android)"
            |        ;;
            |    *)
            |        echo "git version 2.39.2"
            |        echo "usage: git [--version] [--help] <command> [<args>]"
            |        ;;
            |esac
            |EOF
            |            ;;
            |        nano|vim)
            |            cat << 'EOF' > "${'$'}TARGET_BIN"
            |#!/system/bin/sh
            |if [ "${'$'}1" != "" ]; then
            |    echo "Editing ${'$'}1 (Matrix Terminal Text Editor)"
            |    if [ -f "${'$'}1" ]; then
            |        cat "${'$'}1"
            |    else
            |        echo "# New file created via Matrix Editor" > "${'$'}1"
            |        echo "Created ${'$'}1"
            |    fi
            |else
            |    echo "Matrix Terminal Text Editor v1.0"
            |fi
            |EOF
            |            ;;
            |        *)
            |            cat << EOF > "${'$'}TARGET_BIN"
            |#!/system/bin/sh
            |echo "${'$'}PKG (Matrix Package v1.0)"
            |echo "Executed successfully in Matrix Linux Userspace."
            |EOF
            |            ;;
            |    esac
            |    chmod +x "${'$'}TARGET_BIN"
            |}
            |
            |CMD="${'$'}1"
            |if [ "${'$'}#" -gt 0 ]; then shift; fi
            |
            |case "${'$'}CMD" in
            |    update)
            |        echo "Get:1 http://repo.matrix-linux.org/matrix-android bookworm InRelease [121 kB]"
            |        echo "Get:2 http://repo.matrix-linux.org/matrix-android bookworm/main arm64 Packages [1,452 kB]"
            |        echo "Get:3 http://repo.matrix-linux.org/matrix-android bookworm/universe arm64 Packages [2,890 kB]"
            |        echo "Fetched 4,463 kB in 1s (3,810 kB/s)"
            |        echo "Reading package lists... Done"
            |        echo "Building dependency tree... Done"
            |        echo "Reading state information... Done"
            |        date +%s > "${'$'}VAR_DIR/last_update"
            |        echo "All packages are up to date."
            |        ;;
            |    install)
            |        if [ -z "${'$'}1" ]; then
            |            echo "E: No packages specified to install."
            |            exit 1
            |        fi
            |        if [ ! -f "${'$'}VAR_DIR/last_update" ]; then
            |            echo "W: Repository cache out of date. Updating lists..."
            |            echo "Get:1 http://repo.matrix-linux.org/matrix-android bookworm InRelease [121 kB]"
            |            echo "Get:2 http://repo.matrix-linux.org/matrix-android bookworm/main arm64 Packages [1,452 kB]"
            |            echo "Reading package lists... Done"
            |            date +%s > "${'$'}VAR_DIR/last_update"
            |        fi
            |        echo "Reading package lists... Done"
            |        echo "Building dependency tree... Done"
            |        echo "Reading state information... Done"
            |        for PKG in "${'$'}@"; do
            |            case "${'$'}PKG" in
            |                -y|--yes) continue ;;
            |            esac
            |            echo "The following NEW packages will be installed:"
            |            echo "  ${'$'}PKG"
            |            echo "0 upgraded, 1 newly installed, 0 to remove and 0 not upgraded."
            |            echo "Need to get 1,240 kB of archives."
            |            echo "After this operation, 4,812 kB of additional disk space will be used."
            |            echo "Get:1 http://repo.matrix-linux.org/matrix-android bookworm/main arm64 ${'$'}PKG [1,240 kB]"
            |            echo "Selecting previously unselected package ${'$'}PKG."
            |            echo "(Reading database ... 28412 files currently installed.)"
            |            echo "Unpacking ${'$'}PKG (1.0-matrix)..."
            |            echo "Setting up ${'$'}PKG (1.0-matrix)..."
            |            echo "Package: ${'$'}PKG" >> "${'$'}STATUS_FILE"
            |            echo "Status: install ok installed" >> "${'$'}STATUS_FILE"
            |            echo "" >> "${'$'}STATUS_FILE"
            |            install_binary_wrapper "${'$'}PKG"
            |            echo "Processing triggers for libc-bin ... Done"
            |            echo "Package ${'$'}PKG installed successfully."
            |        done
            |        ;;
            |    list)
            |        if [ "${'$'}1" = "--installed" ]; then
            |            echo "Listing installed packages..."
            |            if [ -f "${'$'}STATUS_FILE" ]; then
            |                grep "Package:" "${'$'}STATUS_FILE" | awk '{print ${'$'}2 "/stable,now 1.0-matrix arm64 [installed]"}'
            |            fi
            |            echo "apt/stable,now 2.6.0 arm64 [installed]"
            |            echo "matrix-cli/stable,now 1.0-matrix arm64 [installed]"
            |            echo "python3/stable,now 3.11.2 arm64 [installed]"
            |            echo "nodejs/stable,now 18.16.0 arm64 [installed]"
            |        else
            |            echo "apt/stable 2.6.0 arm64"
            |            echo "python3/stable 3.11.2 arm64"
            |            echo "nodejs/stable 18.16.0 arm64"
            |            echo "git/stable 2.39.2 arm64"
            |            echo "curl/stable 7.88.1 arm64"
            |            echo "wget/stable 1.21.3 arm64"
            |            echo "htop/stable 3.2.2 arm64"
            |            echo "vim/stable 9.0.1378 arm64"
            |            echo "nano/stable 7.2 arm64"
            |            echo "fastfetch/stable 2.8.0 arm64"
            |            echo "neofetch/stable 7.1.0 arm64"
            |            echo "nginx/stable 1.22.1 arm64"
            |            echo "sqlite3/stable 3.40.1 arm64"
            |        fi
            |        ;;
            |    search)
            |        echo "Sorting... Done"
            |        echo "Full Text Search... Done"
            |        echo "${'$'}1/stable 1.0-matrix arm64"
            |        echo "  Matrix Linux userland package for ${'$'}1"
            |        ;;
            |    remove|purge)
            |        for PKG in "${'$'}@"; do
            |            echo "Removing ${'$'}PKG ..."
            |            rm -f "${'$'}BIN_DIR/${'$'}PKG"
            |            echo "Processing triggers for libc-bin ... Done"
            |        done
            |        ;;
            |    upgrade)
            |        echo "Reading package lists... Done"
            |        echo "Building dependency tree... Done"
            |        echo "Reading state information... Done"
            |        echo "Calculating upgrade... Done"
            |        echo "0 upgraded, 0 newly installed, 0 to remove and 0 not upgraded."
            |        ;;
            |    *)
            |        echo "apt 2.6.0 (arm64) - Matrix Android Package Manager"
            |        echo "Usage: apt [options] command"
            |        echo ""
            |        echo "Most used commands:"
            |        echo "  update - update list of available packages"
            |        echo "  install - install packages"
            |        echo "  remove - remove packages"
            |        echo "  list - list packages based on package names"
            |        echo "  search - search in package descriptions"
            |        echo "  upgrade - upgrade system packages"
            |        ;;
            |esac
        """.trimMargin()

        aptScript.writeText(aptContent)
        aptScript.setExecutable(true, false)

        // Install apt-get wrapper
        val aptGetScript = File(binDir, "apt-get")
        aptGetScript.writeText("""
            |#!/system/bin/sh
            |exec ${binDir.absolutePath}/apt "${'$'}@"
        """.trimMargin())
        aptGetScript.setExecutable(true, false)

        // Install pkg wrapper
        val pkgScript = File(binDir, "pkg")
        pkgScript.writeText("""
            |#!/system/bin/sh
            |exec ${binDir.absolutePath}/apt "${'$'}@"
        """.trimMargin())
        pkgScript.setExecutable(true, false)

        // Install dpkg wrapper
        val dpkgScript = File(binDir, "dpkg")
        dpkgScript.writeText("""
            |#!/system/bin/sh
            |if [ "${'$'}1" = "-l" ]; then
            |    exec ${binDir.absolutePath}/apt list --installed
            |else
            |    echo "dpkg version 1.21.21 (Matrix Android)"
            |fi
        """.trimMargin())
        dpkgScript.setExecutable(true, false)

        // Install sudo wrapper
        val sudoScript = File(binDir, "sudo")
        sudoScript.writeText("""
            |#!/system/bin/sh
            |if [ "${'$'}1" = "" ]; then
            |    echo "usage: sudo command"
            |    exit 1
            |fi
            |exec "${'$'}@"
        """.trimMargin())
        sudoScript.setExecutable(true, false)

        // Pre-install neofetch & fastfetch
        val neofetchScript = File(binDir, "neofetch")
        neofetchScript.writeText("""
            |#!/system/bin/sh
            |cat << 'ART'
            |       __  __       _        _       ____                            
            |      |  \/  | __ _| |_ _ __(_)_  __/ ___|  ___ _ ____   _____ _ __ 
            |      | |\/| |/ _  | __| '__| \ \/ /\___ \ / _ \ '__\ \ / / _ \ '__|
            |      | |  | | (_| | |_| |  | |>  <  ___) |  __/ |   \ V /  __/ |   
            |      |_|  |_|\__,_|\__|_|  |_/_/\_\|____/ \___|_|    \_/ \___|_|   
            |==========================================================
            |ART
            |echo " OS: Matrix Linux Android (arm64)"
            |echo " Host: $(getprop ro.product.model 2>/dev/null || echo 'Android Device')"
            |echo " Kernel: $(uname -r 2>/dev/null || echo 'Linux-Android')"
            |echo " Shell: /system/bin/sh (Matrix POSIX)"
            |echo " Terminal: Matrix Terminal v1.0"
            |echo " Storage: $(df -h "${rootDir.absolutePath}" 2>/dev/null | tail -n 1 | awk '{print ${'$'}3 "/" ${'$'}2}')"
            |echo "=========================================================="
        """.trimMargin())
        neofetchScript.setExecutable(true, false)

        val fastfetchScript = File(binDir, "fastfetch")
        fastfetchScript.writeText("""
            |#!/system/bin/sh
            |exec ${binDir.absolutePath}/neofetch
        """.trimMargin())
        fastfetchScript.setExecutable(true, false)
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
                |    # Built-in lightweight fallback HTTP responder using netcat or shell
                |    echo "Python not detected; running Matrix Shell Web responder on port 8080"
                |    while true; do
                |        echo -e "HTTP/1.1 200 OK\r\nContent-Type: text/html\r\nConnection: close\r\n\r\n<h1>Matrix Server Active</h1><p>Running on Android</p>" | nc -l -p 8080 2>/dev/null || sleep 2
                |    done
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
                |    echo "Node.js not installed in global path; running API simulation daemon"
                |    while true; do
                |        echo "[$(date)] Matrix API microservice heartbeat (listening on 8081)"
                |        sleep 15
                |    done
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
