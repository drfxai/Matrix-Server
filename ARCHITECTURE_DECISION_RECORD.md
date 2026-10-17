# Architecture Decision Record (ADR)
## MATRIX SERVER: Android-to-Linux Architecture & Engineering Strategy

### Status
Accepted & Implemented

### Context & Problem Statement
Android OS enforces sandboxing, SELinux constraints, seccomp filters, and process lifecycle restrictions. Transforming an unrooted Android device into a reliable, remotely accessible Linux development server requires choosing an architecture that provides:
1. Genuine SSHv2 remote shell & SFTP file transfer capabilities accessible from Windows, macOS, and Linux clients.
2. Interactive Linux command execution (`/system/bin/sh`, Toybox/Toolbox POSIX toolchain, environment variables, piping, PTY handling).
3. Supervised background project execution (Python, Node.js, Shell, HTTP services).
4. Uninterrupted background operation with screen-off and battery discharge via Android Foreground Services and Partial WakeLocks.
5. High security defaults (SSH public-key auth preference, non-privileged port, sandboxed storage, zero plain-text leaks).

### Evaluated Options

#### Option 1: Embedded Apache MINA SSHD + Native Android POSIX Userspace + Kotlin Process Supervisor (SELECTED)
- **Mechanism:** Apache MINA SSHD runs directly inside Android's native JVM runtime within an active Foreground Service (`MatrixServerService`). It binds to port 2222, executes POSIX `/system/bin/sh` with configured `PATH`, `HOME`, `TERM=xterm-256color`, and spawns interactive PTY sessions. Built-in SFTP subsystem provides remote file transfer. A Kotlin `ProcessSupervisor` manages background projects with stdout/stderr capture, exit codes, and bounded auto-restart.
- **Restrictions:** Non-privileged ports (>= 1024), standard Android app sandbox (`/data/user/0/com.aistudio.matrixserver.kxmt/files`).
- **Permissions:** `INTERNET`, `ACCESS_NETWORK_STATE`, `FOREGROUND_SERVICE`, `WAKE_LOCK`, `POST_NOTIFICATIONS`. No root required.
- **Reliability:** Highest. Zero native ABI segmentation faults or 16KB page-size incompatibilities across modern Android releases (Android 8 to 15+).
- **Background Persistence:** Backed by native `PARTIAL_WAKE_LOCK` and Foreground Service notification.

#### Option 2: Termux Companion Integration
- **Mechanism:** Rely on external Termux APK and Termux:Tasker via Android Intents.
- **Drawbacks:** Requires sideloading external APKs from F-Droid or GitHub, external permission grants, fragile IPC, and breaks "Install -> Guided Setup -> One-Tap Start" mission.

#### Option 3: Full QEMU / KVM Virtual Machine
- **Mechanism:** Run an emulated x86_64 or aarch64 virtual machine on top of Android.
- **Drawbacks:** Massive CPU overhead, huge battery drain (10x higher), 1GB+ disk image downloads, unusable on mid-range devices.

#### Option 4: Pure Root / chroot
- **Mechanism:** Run direct chroot into a rootfs with root privileges.
- **Drawbacks:** 95%+ of user devices are non-rooted; breaches Google Play security guidelines; risks device bricking.

### Decision
Implement **Option 1**: A dedicated, fully integrated Android-to-Linux server architecture built on Apache MINA SSHD with native POSIX process spawning, integrated SFTP, real process supervision, in-app interactive terminal, Room database persistence, and adaptive background management.
