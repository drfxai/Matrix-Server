# Matrix Server v1.0.0 — Maximus VPN SSH integration (Build 2)

**Release type: Android 7+ TEST BUILD (prerelease).** This is not a production-signed release.

This update includes opt-in, authenticated SSH `direct-tcpip` forwarding, so **Maximus VPN can use Matrix Server as a TCP-only SSH proxy** through its Mihomo engine. Matrix Server is an Android SSH/SFTP daemon, *not* a VLESS, Xray, WireGuard, or full UDP VPN server.

## Download

| Android package | Asset | Intended devices |
| --- | --- | --- |
| ARM64 APK | [MatrixServer-arm64-v8a.apk](https://github.com/drfxai/Matrix-Server/releases/download/v1.0.0-maximus-ssh-test/MatrixServer-arm64-v8a.apk) | 64-bit ARM Android phones |
| Universal APK | [MatrixServer-universal.apk](https://github.com/drfxai/Matrix-Server/releases/download/v1.0.0-maximus-ssh-test/MatrixServer-universal.apk) | Supported ARM and x86 architectures |
| Integrity manifest | [SHA256SUMS.txt](https://github.com/drfxai/Matrix-Server/releases/download/v1.0.0-maximus-ssh-test/SHA256SUMS.txt) | Verify downloaded APK integrity |

**App ID:** `com.aistudio.matrixserver.kxmt` · **Public versionName:** `1.0` · **Internal versionCode:** `2` · **Minimum Android:** 7.0 (API 24).

## What's changed

- **VPN TCP Forwarding** dashboard switch (OFF by default) permits authenticated SSH outbound TCP forwarding with restricted destination admission.
- **Copy Maximus VPN Profile** exports Mihomo-compatible YAML with the actual SSH endpoint, username, password and pinned SSH host public key.
- Fixed startup failure handling so a failed SSH daemon can be restarted; clarified that a local listener check does not mean the server is reachable from outside.
- Added profile and security-policy tests, Android Lint and security checks.

## Getting started

1. On the **Matrix Server phone**, configure a strong, unique SSH password and enable password authentication. This is required for profile export.
2. **Stop** its SSH service, turn on **VPN TCP Forwarding**, then **start** the service again.
3. Make the Matrix Server phone reachable over a **trusted LAN or separately configured private overlay**; a phone behind cellular CGNAT generally cannot act as a directly reachable public server.
4. Tap **Copy Maximus VPN Profile**. The exported YAML contains a live password: keep it private and **clear your clipboard** after import.
5. On a **different device**, open Maximus VPN, import the YAML through its Mihomo/Clash configuration pathway, select the SSH outbound and test TCP connectivity.

Full instructions and a physical-device checklist: [MAXIMUS_VPN_SETUP.md](https://github.com/drfxai/Matrix-Server/blob/main/MAXIMUS_VPN_SETUP.md).

## Important security and compatibility information

- **Debug-signed test APKs**: certificate continuity with a previously installed app has **not** been verified. An APK signed with a different certificate cannot update the existing install. **Back up application data, credentials and SSH authorized keys before attempting an uninstall.** Uninstalling can irreversibly erase those app-private settings.
- Use only on trusted LANs or authenticated private overlays. Do **not** expose the SSH listener publicly via router port forwarding: this app does not ship a hardened public-facing firewall or brute-force rate limiter.
- This feature is **TCP only**, with **no UDP, QUIC, WireGuard, VLESS, automatic NAT traversal, or public SSH hosting**. Destination filtering is not a substitute for an independent firewall and cannot prevent all DNS-rebinding cases.
- SSH login, host-key continuity, physical Android background behavior and **end-to-end Maximus VPN connectivity on actual devices remain unverified**. Passing CI means the app built and its automated checks passed, not that remote VPN service was proven.

## Build provenance

- **Verified source commit:** [`431f5f8a4923cb7d2431ec0ae533b314ad507e02`](https://github.com/drfxai/Matrix-Server/commit/431f5f8a4923cb7d2431ec0ae533b314ad507e02)
- **Android CI:** [run 39588762215](https://github.com/drfxai/Matrix-Server/actions/runs/39588762215) — unit tests, lint, APK signing and integrity checks, security-source job, both artifact uploads: **success**.
- **Verified publication:** [run 39589180591](https://github.com/drfxai/Matrix-Server/actions/runs/39589180591) — **success**.
- Published binary hashes (SHA-256):
  - `MatrixServer-arm64-v8a.apk`: `93dfcb3970057a1b5b296bc4d011e35f86f8739510c9dca868bcc6ac7440496f`
  - `MatrixServer-universal.apk`: `bcb7505fd77c8f8a51a0c13b434dd8d4b815431c017355ca20bb9b18be729ea1`

This release is a **test/prerelease**. The legacy [Android 7 test release](https://github.com/drfxai/Matrix-Server/releases/tag/v1.0.0-android7-test) is retained as a historical download and does **not** include these improvements.
