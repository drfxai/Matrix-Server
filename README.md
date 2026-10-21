# Matrix Server

**Android SSH/SFTP host and app-sandboxed process supervisor.**

Matrix Server provides an SSH daemon on an Android phone using Apache MINA SSHD.
After successful authentication, sessions run Android's native `/system/bin/sh`
with the app's permissions. This is **not a rooted Linux machine, Linux distro,
container, VPS, or built-in Python/Node.js hosting platform**.

## Supported today

- SSH interactive shell and remote command execution in Android's app sandbox.
- Opt-in authenticated SSH TCP forwarding for Maximus VPN's Mihomo SSH outbound (TCP only, public destinations only); see [Maximus VPN setup](MAXIMUS_VPN_SETUP.md).
- SFTP access under Android application permissions (not an enforced chroot).
- User-configured SSH password and approved public keys.
- Persistent foreground service, optional WakeLock, and local project supervision.
- Android dashboard, terminal, file tools and diagnostics.

## Connect from another device

1. Install the Android app and complete its initial configuration.
2. Choose a unique SSH username and strong password of 12–128 characters.
   There is **no built-in or default SSH password**.
3. Start the server from the application dashboard.
4. Identify the phone's LAN IP address. Keep both devices on a trusted network.
5. From your computer, run:

```bash
ssh -p 2222 matrix@PHONE_LAN_IP
```

Replace the port, username and IP address with values shown in the app.
To transfer files, use `sftp -P 2222 matrix@PHONE_LAN_IP`.
Verify the host-key fingerprint reported in the app when it is available;
do not accept an unexpected host-key rotation without investigation.

## SSH security

- All authentication methods are disabled or denied until the user configures
  a valid password or a valid approved public key.
- Existing weak legacy passwords are rejected; approved strong legacy passwords
  are migrated out of plaintext preferences to Android Keystore AES-GCM storage.
- App-managed public keys are parsed, fingerprinted and persisted atomically.
- Android backups are disabled for this application.
- The SSH listener binds to network interfaces for LAN access. Do **not** forward
  its port directly to the internet. Use a private secure tunnel or VPN if remote
  connectivity is needed, and test it on your own network.
- Restrict network reachability with your router or firewall. This app does not
  currently implement a built-in connection firewall or brute-force rate limiter.
- Restart the SSH service after changing authentication settings.

## What this is not

The Android shell offers platform tools permitted to the application UID.
`apt`, `dpkg`, `pip`, `python`, `node`, and other Unix packages are **not installed**
by Matrix Server. Project templates requiring these programs will fail cleanly
until a compatible, separately installed runtime is available.

Installing a rootless Linux distribution is a separate engineering milestone:
it requires verified ARM64 binaries, isolated filesystem management, package
downloads, resource controls and tests on a physical device.

## Build and verify

The GitHub Actions workflow `.github/workflows/android-ci.yml` builds a
**debug-signed APK**, runs unit tests and Android Lint, and attaches the APK
to successful runs. These are test artifacts, not production releases.

Physical-device SSH/SFTP login, host-key continuity, secure upgrades, Android
background behavior and network attack testing are still required before
a production signed APK is published.

See [Security and Runtime Status](SECURITY_AND_RUNTIME.md) for additional details.

## Current Matrix Server + Maximus VPN SSH test release

**Updated build: Matrix Server v1.0.0 (internal build 2, 2026-10-21).**

- [Download verified test release (ARM64 / Universal APK)](https://github.com/drfxai/Matrix-Server/releases/tag/v1.0.0-maximus-ssh-test).
- [Release notes, checksums, upgrade warnings and Maximus VPN import guide](RELEASE_NOTES_MAXIMUS.md).
- The old `v1.0.0-android7-test` tag is historical and does **not** contain the Maximus VPN TCP forwarding improvements.

**These are debug-signed test APKs, not production releases.** Signing-certificate
continuity for in-place upgrade has not been established. Do not uninstall an
existing installation without first safeguarding its app data and SSH keys.
Real-phone end-to-end VPN testing remains outstanding.

## Pre-release test APKs for Android 7.0 and newer

The debug CI build generates two downloadable APK artifacts, with identical
application ID and version but different CPU packaging:

- `MatrixServer-arm64-v8a.apk`: for ARM64 phones running Android 7.0 (API 24) or later.
- `MatrixServer-universal.apk`: for Android 7.0 (API 24) or later, with
  supported ARM64, ARMv7 (32-bit), x86, and x86_64 native libraries.

The APKs are attached as **separate GitHub Actions artifacts** in successful
builds of the Android CI workflow. Download the ZIP for the desired artifact and
extract the APK before installing.

The build uses `desugar_jdk_libs_nio` because parts of the SSHD implementation
use `java.nio.file`, which Android 7 does not implement natively.

**Important:** These packages are debug-signed, and must not be mistaken for
a production release. They cannot install as an upgrade over an app signed by
a different key. Do not uninstall an existing deployment without backing up
its app data and authorized keys first; these are protected by Android app
storage and may be irretrievably lost on uninstall.

Passing unit tests and Android Lint is not the same as executing the app on
a real Android 7 or 8 device. SSH login, key authentication, SFTP and background
service behavior must be tested on representative devices before declaring
backwards compatibility production-ready.
