package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.filled.VpnLock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ConnectionHealth
import com.example.data.model.ConnectionIssueType
import com.example.data.model.ServerStatusType
import com.example.ui.theme.MatrixBlack
import com.example.ui.theme.MatrixBlue
import com.example.ui.theme.MatrixDarkSurface
import com.example.ui.theme.MatrixGreen
import com.example.ui.theme.MatrixGreenDark
import com.example.ui.theme.MatrixGreenSubtle
import com.example.ui.theme.MatrixRed
import com.example.ui.theme.MatrixSurfaceBorder
import com.example.ui.theme.MatrixSurfaceVariant
import com.example.ui.theme.MatrixTextPrimary
import com.example.ui.theme.MatrixTextSecondary
import com.example.ui.theme.MatrixYellow

/**
 * Pulsing multi-ring live status beacon with concentric sonar waves.
 */
@Composable
fun LiveStatusBeacon(
    status: ServerStatusType,
    modifier: Modifier = Modifier,
    sizeDp: Int = 24
) {
    val indicatorColor = when (status) {
        ServerStatusType.RUNNING -> MatrixGreen
        ServerStatusType.CONNECTION_ISSUE -> MatrixYellow
        ServerStatusType.STARTING, ServerStatusType.STOPPING -> MatrixBlue
        ServerStatusType.STOPPED -> MatrixRed
    }

    val isPulsing = status == ServerStatusType.RUNNING || status == ServerStatusType.CONNECTION_ISSUE || status == ServerStatusType.STARTING

    val infiniteTransition = rememberInfiniteTransition(label = "beaconPulseTransition")

    val waveScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = if (isPulsing) 2.2f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "waveScale"
    )

    val waveAlpha by infiniteTransition.animateFloat(
        initialValue = if (isPulsing) 0.6f else 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "waveAlpha"
    )

    val corePulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = if (isPulsing) 1.15f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "corePulse"
    )

    Box(
        modifier = modifier.size(sizeDp.dp),
        contentAlignment = Alignment.Center
    ) {
        // Expanding ring sonar wave
        if (isPulsing) {
            Canvas(modifier = Modifier.size(sizeDp.dp)) {
                drawCircle(
                    color = indicatorColor.copy(alpha = waveAlpha),
                    radius = (size.minDimension / 2f) * waveScale,
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }

        // Inner glowing core
        Box(
            modifier = Modifier
                .size((sizeDp * 0.55).dp)
                .scale(corePulse)
                .clip(CircleShape)
                .background(indicatorColor)
                .border(1.5.dp, MatrixBlack, CircleShape)
        )
    }
}

/**
 * Hero real-time status card displayed prominently on the console/dashboard.
 * Shows live daemon state, real-time socket probe latency, network reachability,
 * and actionable connection issue alerts.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RealtimeStatusCard(
    health: ConnectionHealth,
    onProbeNow: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showHealthDialog by remember { mutableStateOf(false) }

    val statusColor = health.indicatorColor
    val borderColor by animateColorAsState(
        targetValue = when (health.status) {
            ServerStatusType.RUNNING -> MatrixGreen.copy(alpha = 0.6f)
            ServerStatusType.CONNECTION_ISSUE -> MatrixYellow.copy(alpha = 0.8f)
            ServerStatusType.STOPPED -> MatrixSurfaceBorder
            ServerStatusType.STARTING, ServerStatusType.STOPPING -> MatrixBlue.copy(alpha = 0.6f)
        },
        animationSpec = tween(300),
        label = "cardBorderColor"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.5.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable { showHealthDialog = true }
            .testTag("realtime_status_card"),
        colors = CardDefaults.cardColors(containerColor = MatrixDarkSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Beacon, Status Label & Live Probe Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LiveStatusBeacon(status = health.status, sizeDp = 26)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = when (health.status) {
                                    ServerStatusType.RUNNING -> "SSH SERVER: ONLINE"
                                    ServerStatusType.CONNECTION_ISSUE -> "CONNECTION ISSUE"
                                    ServerStatusType.STOPPED -> "SSH SERVER: STOPPED"
                                    ServerStatusType.STARTING -> "STARTING DAEMON..."
                                    ServerStatusType.STOPPING -> "HALTING DAEMON..."
                                },
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = statusColor,
                                letterSpacing = 0.5.sp
                            )
                        }
                        Text(
                            text = health.headline,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = MatrixTextPrimary
                        )
                    }
                }

                // Tappable health inspection pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(MatrixSurfaceVariant)
                        .border(1.dp, MatrixSurfaceBorder, RoundedCornerShape(6.dp))
                        .clickable { showHealthDialog = true }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Inspect Health",
                            tint = MatrixGreen,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "INSPECT",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MatrixGreen
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Real-Time Health Telemetry Grid Chips
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Socket Probe Chip
                TelemetryChip(
                    label = "Socket Probe",
                    value = if (health.isSocketResponsive) "${health.socketProbeLatencyMs}ms (PASS)" else if (health.status == ServerStatusType.STOPPED) "N/A" else "REFUSED",
                    isGood = health.isSocketResponsive,
                    icon = Icons.Default.Speed
                )

                // Network Interface Chip
                TelemetryChip(
                    label = "Interface",
                    value = if (health.isWifiConnected) "Wi-Fi (${health.ipAddress})" else if (health.isVpnActive) "VPN (${health.vpnIpAddress})" else if (health.ipAddress != "127.0.0.1") health.ipAddress else "Localhost Only",
                    isGood = health.ipAddress != "127.0.0.1" || health.isVpnActive,
                    icon = if (health.isWifiConnected) Icons.Default.Wifi else if (health.isVpnActive) Icons.Default.VpnLock else Icons.Default.WifiOff
                )

                // Bound Port Chip
                TelemetryChip(
                    label = "Port",
                    value = "${health.port}",
                    isGood = health.status == ServerStatusType.RUNNING,
                    icon = Icons.Default.Settings
                )

                // Active Clients Chip
                TelemetryChip(
                    label = "Clients",
                    value = "${health.activeSessionsCount} active",
                    isGood = true,
                    icon = Icons.Default.Security
                )
            }

            // Connection Issue Warning Banner
            if (health.isIssue) {
                Spacer(modifier = Modifier.height(14.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MatrixYellow.copy(alpha = 0.12f))
                        .border(1.dp, MatrixYellow.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Connection Warning",
                                tint = MatrixYellow,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = when (health.issueType) {
                                    ConnectionIssueType.NO_NETWORK -> "UNREACHABLE OVER LOCAL NETWORK"
                                    ConnectionIssueType.PORT_IN_USE -> "PORT COLLISION"
                                    ConnectionIssueType.SOCKET_TIMEOUT -> "DAEMON SOCKET TIMEOUT"
                                    ConnectionIssueType.DAEMON_ERROR -> "DAEMON ERROR"
                                    ConnectionIssueType.NONE -> "CONNECTION ALERT"
                                },
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = MatrixYellow
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = health.detailMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            fontSize = 11.sp,
                            color = MatrixTextPrimary,
                            lineHeight = 15.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = onProbeNow,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MatrixYellow,
                                    contentColor = MatrixBlack
                                ),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = MatrixBlack
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "RE-TEST PROBE",
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    color = MatrixBlack
                                )
                            }

                            if (health.issueType == ConnectionIssueType.PORT_IN_USE) {
                                OutlinedButton(
                                    onClick = onNavigateToSettings,
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.height(34.dp).border(1.dp, MatrixYellow, RoundedCornerShape(6.dp)),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MatrixYellow)
                                ) {
                                    Text(
                                        text = "CHANGE PORT",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showHealthDialog) {
        ConnectionHealthDialog(
            health = health,
            onProbeNow = onProbeNow,
            onDismiss = { showHealthDialog = false }
        )
    }
}

/**
 * Compact app-wide persistent status indicator bar displayed at the top of screens.
 */
@Composable
fun GlobalStatusBar(
    health: ConnectionHealth,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(MatrixDarkSurface)
            .border(1.dp, MatrixSurfaceBorder)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp)
            .testTag("global_status_bar")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                LiveStatusBeacon(status = health.status, sizeDp = 14)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = when (health.status) {
                        ServerStatusType.RUNNING -> "SSH: ${health.ipAddress}:${health.port}"
                        ServerStatusType.CONNECTION_ISSUE -> "SSH: ⚠️ ${health.headline.uppercase()}"
                        ServerStatusType.STOPPED -> "SSH: STOPPED"
                        ServerStatusType.STARTING -> "SSH: STARTING..."
                        ServerStatusType.STOPPING -> "SSH: STOPPING..."
                    },
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = health.indicatorColor
                )

                if (health.vpnIpAddress != null) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "· VPN",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = MatrixBlue
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (health.status == ServerStatusType.RUNNING) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(MatrixSurfaceVariant)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${health.activeSessionsCount} CLIENTS",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = MatrixGreen
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                }

                Text(
                    text = "DETAILS >",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = MatrixTextSecondary
                )
            }
        }
    }
}

/**
 * Interactive detailed health diagnostic modal with real-time socket probe,
 * network interfaces breakdown, and one-tap re-test.
 */
@Composable
fun ConnectionHealthDialog(
    health: ConnectionHealth,
    onProbeNow: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MatrixDarkSurface,
        modifier = Modifier.border(1.dp, health.indicatorColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                LiveStatusBeacon(status = health.status, sizeDp = 20)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "CONNECTION HEALTH",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = health.indicatorColor
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = health.headline,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = MatrixTextPrimary
                )
                Text(
                    text = health.detailMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 11.sp,
                    color = MatrixTextSecondary,
                    modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                )

                // Detailed inspection checklist
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MatrixBlack)
                        .border(1.dp, MatrixSurfaceBorder, RoundedCornerShape(8.dp))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HealthCheckItem(
                        title = "Local TCP Socket Probe",
                        status = if (health.isSocketResponsive) "PASS (${health.socketProbeLatencyMs}ms)" else if (health.status == ServerStatusType.STOPPED) "IDLE" else "FAILED",
                        isOk = health.isSocketResponsive
                    )

                    HealthCheckItem(
                        title = "Wi-Fi / LAN Interface",
                        status = if (health.isWifiConnected) "Connected (${health.ipAddress})" else if (health.ipAddress != "127.0.0.1") health.ipAddress else "Disconnected",
                        isOk = health.ipAddress != "127.0.0.1"
                    )

                    HealthCheckItem(
                        title = "Mesh VPN (Tailscale)",
                        status = health.vpnIpAddress ?: "Not Detected",
                        isOk = health.vpnIpAddress != null
                    )

                    HealthCheckItem(
                        title = "SSH Listening Port",
                        status = "Port ${health.port} (0.0.0.0)",
                        isOk = health.status == ServerStatusType.RUNNING
                    )

                    HealthCheckItem(
                        title = "Active SSH Sessions",
                        status = "${health.activeSessionsCount} connected",
                        isOk = true
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onProbeNow,
                colors = ButtonDefaults.buttonColors(containerColor = MatrixGreen, contentColor = MatrixBlack),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.height(40.dp)
            ) {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp), tint = MatrixBlack)
                Spacer(modifier = Modifier.width(4.dp))
                Text("RUN LIVE PROBE", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MatrixBlack)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.height(40.dp).border(1.dp, MatrixSurfaceBorder, RoundedCornerShape(6.dp)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MatrixTextSecondary)
            ) {
                Text("CLOSE", fontFamily = FontFamily.Monospace, fontSize = 11.sp)
            }
        }
    )
}

@Composable
private fun TelemetryChip(
    label: String,
    value: String,
    isGood: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(MatrixBlack)
            .border(0.8.dp, if (isGood) MatrixSurfaceBorder else MatrixYellow.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 5.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isGood) MatrixGreen else MatrixYellow,
                modifier = Modifier.size(13.dp)
            )
            Column {
                Text(
                    text = label.uppercase(),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 8.sp,
                    color = MatrixTextSecondary,
                    lineHeight = 10.sp
                )
                Text(
                    text = value,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    color = if (isGood) MatrixTextPrimary else MatrixYellow,
                    lineHeight = 12.sp
                )
            }
        }
    }
}

@Composable
private fun HealthCheckItem(
    title: String,
    status: String,
    isOk: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            color = MatrixTextSecondary
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = status,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = if (isOk) MatrixGreen else MatrixYellow
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = if (isOk) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = null,
                tint = if (isOk) MatrixGreen else MatrixYellow,
                modifier = Modifier.size(13.dp)
            )
        }
    }
}
