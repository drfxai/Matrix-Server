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
4. In **MAXIMUS VPN · SSH TUNNEL** on the Matrix Server dashboard, choose
   the **SSH server address for the client**. Use **LAN** only when the other
   device is on a trusted LAN with direct reachability, or **VPN** only when
   *both devices are already connected to the same authenticated overlay*.
   You can also enter a verified DNS hostname or address manually.
   **10.247.210.65 is a private address, not a public internet server.**
   The app can inspect its own interfaces but cannot prove a remote client has a route.
5. Press **Copy Maximus VPN Profile**. This exports explicit Mihomo/Clash YAML with
   the **selected address**, SSH username/password, port and a pinned host public key.
6. On the **other device**, open Maximus VPN > Add server > import/advanced
   configuration, paste the generated Mihomo YAML, select the SSH profile,
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
- Import the YAML into Maximus VPN, verify the Mihomo executable is bundled,
  and test egress via its connection verification and IP-check tools.
- Confirm that UDP-only traffic is not silently claimed as supported.
- Verify failed authentication, wrong host key, private-IP forwarding rejection,
  screen-off behavior, network changes, and correct service shutdown.
- Repeat tests on Android 7+ and modern Android versions.

A green Android CI result confirms compilation/static tests only, not this
end-to-end physical device and remote-network verification.

## Maximus VPN Panels vs. SSH profile import — important distinction

**Panels > Maximus Tunnel** in the Maximus VPN Android app is intended for
**two Linux VPS machines** (Iran + abroad), administered over SSH using
`systemd`, root or sudo, and Xray/Hysteria installers. Matrix Server runs
in an **Android app sandbox** and cannot install or host those Linux daemons.
Seeing the Matrix Server SSH host in Panels is *not* evidence that Maximus Tunnel
can be provisioned on it.

For Matrix Server integration, use **Nodes / Import** for the YAML exported
by this Android app. This is a **TCP-only SSH-proxy path** using Mihomo, not
the two-VPS Maximus Tunnel feature.

### "Not reachable" at 10.247.210.65:2222

This means the **client** failed its TCP connection to the specified SSH port.
An address in `10.0.0.0/8` is private, and may belong to an overlay or LAN.
Check, in order:

1. Matrix Server is visibly RUNNING, its port is `2222` (or the configured
   port), its strong SSH password is enabled, and VPN TCP forwarding was
   enabled **before** starting the SSH daemon.
2. The address shown under the server's LAN or VPN interfaces is *still*
   `10.247.210.65`; VPN interface addresses can change.
3. Both devices already have a **working route** to that exact private IP.
   A VPN running *only* on the server device does not automatically give the
   client a route to it. A cellular CGNAT network will not forward unsolicited
   inbound TCP to your phone. An external rendezvous/relay or VPN mesh would
   be a separate service and is not included in Matrix Server.
4. Check access from the **client** to TCP port `2222` and confirm an SSH
   handshake with the pinned host key. The Matrix Server local-port
   diagnostics are only *local observations* and cannot replace this test.
5. Only after SSH reachability is established, import the **new** Mihomo YAML
   in Nodes / Import and test outbound TCP with the actual Mihomo runtime.

Never port-forward this experimental Android SSH service to the public internet.
