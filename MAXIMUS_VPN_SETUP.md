# Maximus VPN interoperability — authenticated SSH outbound

Matrix Server is an **SSH/SFTP daemon running in an Android application sandbox**.
It is not an Xray, VLESS, WireGuard, Shadowsocks, or public VPS server. To route
**TCP** connections through it from another device, Maximus VPN can use its
bundled Mihomo SSH outbound.

## Configure on the Matrix Server phone

1. Configure a strong unique SSH password under Settings, or manage SSH keys.
   Automatic export currently requires **password authentication**.
2. Stop the SSH service. On the dashboard, enable **VPN TCP Forwarding**.
   It is OFF by default. Restart the SSH service.
3. Check that both devices have network reachability (same trusted LAN, or a
   separately configured private overlay with reachable phone address).
   A cellular/CGNAT IP is generally not directly reachable from the internet.
4. Press **Copy Maximus VPN Profile**. This exports JSON that is valid as
   Mihomo/Clash YAML. It includes the SSH username, password, endpoint,
   and a **pinned host public key** from the active SSH daemon.
5. On the **other device**, open Maximus VPN > Add server > import/advanced
   configuration, paste the generated Mihomo YAML/JSON, select the SSH profile,
   and connect using the bundled Mihomo engine.

Be careful: the export contains a live password. Do not upload it to public
repositories, logs or Telegram chats. Clear the clipboard after import.
Revoking the SSH password invalidates previously imported configurations.

## What is actually supported

- SSH authentication + **direct-tcpip** outbound SSH channels for **TCP only**.
- Remote TCP listening, remote forwarding, X11 and SSH agent forwarding denied.
- An outbound admission check rejects loopback, private, link-local and several
  non-public ranges. **This is not a hard network firewall**; DNS rebinding is
  possible, so deploy only behind a trusted LAN or authenticated overlay.
- No UDP relay, UDP-based DNS, QUIC or generic WireGuard/VLESS server.
- A local socket probe only checks listening readiness. It does *not* certify
  external routing, VPN usability, public reachability or actual internet access.
- The sshd process runs with the permissions of the Android app, not root.
- Never open/port-forward the SSH listener directly to the public internet;
  a hardened, tested firewall and rate limiting are **not** shipped here.

## External verification checklist (physical devices)

- Confirm SSH login and pinned host key from a second device.
- Confirm a direct-tcpip channel to a publicly reachable **TCP** endpoint.
- Import the JSON into Maximus VPN, verify the Mihomo executable is bundled,
  and test egress via its connection verification and IP-check tools.
- Confirm that UDP-only traffic is not silently claimed as supported.
- Verify failed authentication, wrong host key, private-IP forwarding rejection,
  screen-off behavior, network changes, and correct service shutdown.
- Repeat tests on Android 7+ and modern Android versions.

A green Android CI result confirms compilation/static tests only, not this
end-to-end physical device and remote-network verification.
