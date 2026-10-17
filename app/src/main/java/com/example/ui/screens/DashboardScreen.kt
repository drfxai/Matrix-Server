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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
