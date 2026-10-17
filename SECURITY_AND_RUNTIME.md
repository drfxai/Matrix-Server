# Matrix Server — security and runtime status

## Actual execution model

The application provides an Android app-sandboxed shell via \`/system/bin/sh\`
and an Apache MINA SSHD network listener. It **does not** provide root access,
a Linux distribution, a container, a working \`apt\` repository, or Python/Node.js
interpreters by default. Earlier demo wrappers which printed simulated results
are no longer installed, and recognized previously generated shims are removed.

Real Python, Node.js, compiler toolchains, and package management need a separate,
ABI-compatible verified runtime with its own lifecycle, integrity verification,
resource limits, and explicit user approval. Until such a runtime is delivered,
do not describe projects requiring those binaries as runnable.

## SSH security behavior

- Server startup is denied if there is no configured strong SSH password and
  no valid authorized SSH public key.
- Password login defaults **off**, with no built-in password. Initial setup
  explicitly requires a 12–128 character user password.
- The legacy plaintext password is migrated into Android Keystore-backed AES-GCM
  encryption when sufficiently strong, and the old plaintext record is deleted.
  Weak legacy passwords are not migrated, and lose password-auth privileges.
- Public-key authentication compares the offered key with parsed entries in
  \`~/.ssh/authorized_keys\`; empty or malformed files grant no access.
- Android backups are disabled to prevent copying credentials and host keys.
- The SSH daemon listens on all interfaces to support LAN connections.
  Use only trusted networks, protect the device, and avoid exposing the SSH
  port to the public Internet. No perimeter firewall is provided by the app.
- Persisted auth configuration is applied to a running daemon only after restart.

## Validation needed before production release

A physical-device test must verify successful and unsuccessful password/key
login, SFTP directory confinement, device reboot, host-key continuity,
concurrent sessions, brute-force rate limits, and upgrades from older builds.
No signed APK or real Linux userspace is implied by this source change.
