# MATRIX SERVER
## Professional Android-to-Linux Development Server

Turn any Android smartphone into a high-performance, remotely accessible Linux development server with real OpenSSH access, persistent project execution, intelligent background management, and one-tap activation.

---

## ⚡ Quick Start (One-Tap Activation)

1. **Install & Launch** MATRIX SERVER on your Android phone.
2. Complete the **3-step Setup Wizard** (sets your username and password).
3. Tap the glowing **START SERVER** button on the Console.
4. Tap the **Copy SSH Command** button.
5. On your computer (Windows, macOS, or Linux), paste and run the command:
   ```bash
   ssh matrix@192.168.1.120 -p 2222
   ```
6. Enter your password (`matrix1337` by default) or authenticate using your SSH key.
7. You are now inside the phone's native Linux userspace! Run real Linux commands, manage projects, and deploy services.

---

## 🖥️ Client Connection Instructions

### Windows (PowerShell, Windows Terminal, or CMD)
```powershell
ssh matrix@<PHONE_IP> -p 2222
```

### macOS & Linux Terminal
```bash
ssh matrix@<PHONE_IP> -p 2222
```

### Remote SFTP (FileZilla, WinSCP, Cyberduck)
- **Protocol:** SFTP (SSH File Transfer Protocol)
- **Host:** `<PHONE_IP>`
- **Port:** `2222`
- **Username:** `matrix`
- **Password:** `<YOUR_PASSWORD>`

### VS Code Remote - SSH Extension
Add to your `~/.ssh/config`:
```ssh-config
Host MatrixPhone
    HostName <PHONE_IP>
    User matrix
    Port 2222
```

---

## 🌐 Remote Access Anywhere via Tailscale Private VPN

To access your phone's server outside your home Wi-Fi (from cellular data, coffee shops, or remote networks) without port forwarding or risking public internet exposure:

1. Install the official **Tailscale** app on your Android phone and log in.
2. Install **Tailscale** on your laptop or workstation and log into the same network.
3. MATRIX SERVER automatically detects your Tailscale `100.x.y.z` IP and displays it on the Dashboard.
4. Connect from anywhere in the world:
   ```bash
   ssh matrix@100.x.y.z -p 2222
   ```

---

## 🚀 Managed Projects & Runtimes

MATRIX SERVER includes a robust **Process Supervisor** that executes development workloads in the background, logs stdout/stderr in real-time, and provides bounded exponential backoff auto-restarts on crash:

- **Matrix Web Server (Python):** Serves an HTTP dashboard on port `8080`.
- **API Microservice (Node.js):** REST API endpoints on port `8081`.
- **System Worker (Shell):** Background automation script logging hardware and storage metrics.
- **Custom Projects:** Create any custom Python script, Node.js service, or Shell daemon with custom ports and auto-restart policies.

---

## 🔋 Screen-Off Persistence & Battery Optimization

MATRIX SERVER is specifically engineered to keep running when the phone's screen is locked or turned off:
- **Foreground Service:** Runs an active Android foreground service with a persistent ongoing notification showing live status and quick actions ("STOP SERVER", "COPY SSH").
- **Partial WakeLock:** Automatically holds `PowerManager.PARTIAL_WAKE_LOCK` to keep the CPU active without draining the screen display.
- **Battery Optimization Exemption:** In Android Settings > Apps > MATRIX SERVER > Battery, select **Unrestricted** to prevent the OS from killing background background processes during deep sleep.
- **Low-Power Mode:** Toggleable in Settings to reduce canvas animations and polling intervals.

---

## 🔒 Security & Privacy

- **Non-Privileged Port:** Listens on port `2222` by default.
- **No Plaintext Leaks:** Passwords and keys are never displayed in notifications or exported logs.
- **Public-Key Authentication:** Full support for `~/.ssh/authorized_keys` and Ed25519/RSA keys.
- **Host Key Verification:** SHA-256 host key fingerprint is generated on first launch and displayed on the Console for verification.
