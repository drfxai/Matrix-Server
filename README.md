# Matrix Server

**Android SSH/SFTP host and app-sandboxed process supervisor.**

Matrix Server provides an SSH daemon on an Android phone using Apache MINA SSHD.
After successful authentication, sessions run Android's native `/system/bin/sh`
with the app's permissions. This is **not a rooted Linux machine, Linux distro,
container, VPS, or built-in Python/Node.js hosting platform**.

## Supported today

- SSH interactive shell and remote command execution in Android's app sandbox.
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
