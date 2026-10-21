package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeviceThermostat
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.VpnLock
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ServerState
import com.example.ui.components.DigitalRainHeader
import com.example.ui.components.MetricCard
import com.example.ui.components.PowerButton
import com.example.ui.components.RealtimeStatusCard
import com.example.ui.components.StatusBadge
import com.example.ui.theme.MatrixBlack
import com.example.ui.theme.MatrixBlue
import com.example.ui.theme.MatrixDarkSurface
import com.example.ui.theme.MatrixGreen
import com.example.ui.theme.MatrixGreenDark
import com.example.ui.theme.MatrixRed
import com.example.ui.theme.MatrixSurfaceBorder
import com.example.ui.theme.MatrixSurfaceVariant
import com.example.ui.theme.MatrixTextPrimary
import com.example.ui.theme.MatrixTextSecondary
import com.example.ui.theme.MatrixYellow
import com.example.ui.viewmodel.MatrixServerViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    viewModel: MatrixServerViewModel,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val serverState by viewModel.serverState.collectAsState()
    val health by viewModel.connectionHealth.collectAsState()
    val metrics by viewModel.metricsState.collectAsState()
    val config by viewModel.configState.collectAsState()
    // Only suggests addresses. The actual usable endpoint must be selected for the
    // CLIENT network, not inferred from a successful local listening socket test.
    var exportAddress by remember(metrics.lanIpAddress, metrics.vpnIpAddress) {
        mutableStateOf(metrics.vpnIpAddress ?: metrics.lanIpAddress.takeUnless { it == "127.0.0.1" }.orEmpty())
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MatrixBlack)
            .verticalScroll(rememberScrollState())
    ) {
        // Digital Rain subtle canvas header
        DigitalRainHeader(lowPowerMode = config.lowPowerMode)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Header: Branding & Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "MATRIX SERVER",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        letterSpacing = 1.sp,
                        color = MatrixGreen
                    )
                    Text(
                        text = "Linux Node Appliance",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = MatrixTextSecondary
                    )
                }
                StatusBadge(serverState = serverState)
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Primary One-Tap Power Control
            PowerButton(
                serverState = serverState,
                onClick = { viewModel.toggleServerPower() },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Real-Time Server & Connection Status Indicator
            RealtimeStatusCard(
                health = health,
                onProbeNow = { viewModel.triggerHealthProbe() },
                onNavigateToSettings = { onNavigate("settings") }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Connection Command Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        if (serverState.isRunning) MatrixGreen else MatrixSurfaceBorder,
                        RoundedCornerShape(8.dp)
                    ),
                colors = CardDefaults.cardColors(containerColor = MatrixDarkSurface),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Terminal,
                                contentDescription = null,
                                tint = if (serverState.isRunning) MatrixGreen else MatrixTextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "SSH CONNECTION",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (serverState.isRunning) MatrixGreen else MatrixTextSecondary
                            )
                        }

                        if (serverState.isRunning) {
                            IconButton(
                                onClick = {
                                    viewModel.copySshCommand()
                                    Toast.makeText(context, "SSH command copied to clipboard", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(32.dp).testTag("copy_ssh_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy SSH Command",
                                    tint = MatrixGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(4.dp))
                            .background(MatrixBlack)
                            .border(1.dp, MatrixSurfaceBorder, RoundedCornerShape(4.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = if (serverState.isRunning) {
                                metrics.sshCommand
                            } else {
                                "ssh ${config.username}@${metrics.lanIpAddress} -p ${config.port} (Server Offline)"
                            },
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (serverState.isRunning) MatrixGreen else MatrixTextSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "User: ${config.username} | Port: ${config.port}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = MatrixTextSecondary
                        )
                        Text(
                            text = if (serverState.isRunning) "Uptime: ${metrics.formattedUptime}" else "Stopped",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = if (serverState.isRunning) MatrixGreen else MatrixTextSecondary
                        )
                    }

                    // VPN / Tailscale connection if active
                    if (metrics.vpnIpAddress != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(4.dp))
                                .background(MatrixSurfaceVariant)
                                .clickable {
                                    viewModel.copyVpnSshCommand()
                                    Toast.makeText(context, "VPN SSH command copied", Toast.LENGTH_SHORT).show()
                                }
                                .padding(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.VpnLock,
                                contentDescription = null,
                                tint = MatrixBlue,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Tailscale / VPN: ${metrics.vpnSshCommand}",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = MatrixBlue,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = null,
                                tint = MatrixBlue,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }


            Spacer(modifier = Modifier.height(12.dp))

            // Maximus VPN uses Mihomo's SSH outbound (not VLESS or WireGuard).
            // Explicit opt-in and a pinned host key prevent accidental open SSH relays.
            Card(
                modifier = Modifier.fillMaxWidth()
                    .border(1.dp, MatrixBlue, RoundedCornerShape(8.dp))
                    .testTag("maximus_vpn_ssh_card"),
                colors = CardDefaults.cardColors(containerColor = MatrixDarkSurface),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "MAXIMUS VPN · SSH TUNNEL",
                        color = MatrixGreen,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Encrypted TCP proxy via Mihomo SSH. Not VLESS/WireGuard; UDP is not supported. " +
                            "The client must reach this phone over trusted LAN or a private tunnel. " +
                            "A mobile carrier IP usually is not remotely reachable.",
                        color = MatrixTextSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            viewModel.updateConfig(config.copy(
                                vpnTcpForwardingEnabled = !config.vpnTcpForwardingEnabled
                            ))
                        },
                        enabled = !serverState.isRunning && !serverState.isTransitioning,
                        modifier = Modifier.fillMaxWidth().testTag("toggle_vpn_forwarding"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (config.vpnTcpForwardingEnabled) MatrixRed else MatrixBlue
                        )
                    ) {
                        Text(if (config.vpnTcpForwardingEnabled)
                            "Disable VPN TCP Forwarding" else "Enable VPN TCP Forwarding")
                    }
                    Text(
                        if (serverState.isRunning) "Stop the server before changing forwarding mode."
                        else if (config.vpnTcpForwardingEnabled) "Forwarding enabled in settings. Start SSH server to use it."
                        else "Forwarding is disabled by default.",
                        color = MatrixTextSecondary,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = exportAddress,
                        onValueChange = { exportAddress = it },
                        label = { Text("SSH server address for the client") },
                        supportingText = {
                            Text("Choose an address the other phone can reach. Private 10.x / 192.168.x " +
                                "addresses require the same trusted LAN or a pre-existing private overlay.")
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("maximus_ssh_export_address")
                    )
                    if (metrics.lanIpAddress != "127.0.0.1" || metrics.vpnIpAddress != null) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (metrics.lanIpAddress != "127.0.0.1") {
                                Button(onClick = { exportAddress = metrics.lanIpAddress }) {
                                    Text("LAN: ${metrics.lanIpAddress}", fontSize = 10.sp)
                                }
                            }
                            metrics.vpnIpAddress?.let { vpn ->
                                Button(onClick = { exportAddress = vpn }) {
                                    Text("VPN: $vpn", fontSize = 10.sp)
                                }
                            }
                        }
                    }
                    Text(
                        "Panels > Maximus Tunnel provisions two Linux servers. For this Android " +
                            "server, paste the exported Mihomo SSH YAML in Maximus VPN's Nodes/Import. " +
                            "TCP only. An SSH listener check on this phone cannot verify remote reachability.",
                        color = MatrixYellow,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            val ok = viewModel.copyMaximusVpnProfile(exportAddress)
                            Toast.makeText(
                                context,
                                if (ok) "Pinned SSH profile copied. Import as Mihomo YAML in Maximus VPN."
                                else "Unable to export. Check address, daemon, forwarding, password and SSH host key.",
                                Toast.LENGTH_LONG
                            ).show()
                        },
                        enabled = serverState == ServerState.ON &&
                            config.vpnTcpForwardingEnabled && config.passwordAuthEnabled,
                        modifier = Modifier.fillMaxWidth().testTag("copy_maximus_vpn_profile"),
                        colors = ButtonDefaults.buttonColors(containerColor = MatrixGreenDark)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null,
                            modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Copy Maximus VPN Profile")
                    }
                    Text(
                        "Profile contains your SSH password. Keep it private and clear your clipboard after import.",
                        color = MatrixYellow,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Real-Time System Monitoring Dashboard (CPU, RAM, Active Connections & Sockets)
            val cpuHistory by viewModel.cpuHistory.collectAsState()
            val ramHistory by viewModel.ramHistory.collectAsState()
            val projects by viewModel.projectsFlow.collectAsState()

            com.example.ui.components.SystemMonitoringDashboard(
                serverState = serverState,
                metrics = metrics,
                cpuHistory = cpuHistory,
                ramHistory = ramHistory,
                projects = projects
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Section: Quick Action Controls
            Text(
                text = "QUICK CONTROLS",
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MatrixTextSecondary,
                letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                maxItemsInEachRow = 2
            ) {
                QuickActionButton(
                    title = "Terminal",
                    desc = "Direct Linux Shell",
                    icon = Icons.Default.Terminal,
                    onClick = { onNavigate("terminal") },
                    modifier = Modifier.weight(1f)
                )
                QuickActionButton(
                    title = "Projects",
                    desc = "Supervised Workloads",
                    icon = Icons.Default.PlayArrow,
                    onClick = { onNavigate("projects") },
                    modifier = Modifier.weight(1f)
                )
                QuickActionButton(
                    title = "File Explorer",
                    desc = "Storage & Assets",
                    icon = Icons.Default.Folder,
                    onClick = { onNavigate("files") },
                    modifier = Modifier.weight(1f)
                )
                QuickActionButton(
                    title = "SSH Keys",
                    desc = "Access & Security",
                    icon = Icons.Default.VpnKey,
                    onClick = { onNavigate("ssh_keys") },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun QuickActionButton(
    title: String,
    desc: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MatrixDarkSurface)
            .border(1.dp, MatrixSurfaceBorder, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = MatrixGreen,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MatrixTextPrimary
                )
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 10.sp,
                    color = MatrixTextSecondary
                )
            }
        }
    }
}
