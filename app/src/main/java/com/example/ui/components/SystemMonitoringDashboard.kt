package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.DeviceThermostat
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.VpnLock
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ProjectItem
import com.example.data.model.ServerMetrics
import com.example.data.model.ServerState
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
import com.example.util.ActiveSocketInfo
import com.example.util.NetworkInterfaceInfo
import com.example.util.NetworkUtils

@Composable
fun SystemMonitoringDashboard(
    serverState: ServerState,
    metrics: ServerMetrics,
    cpuHistory: List<Float>,
    ramHistory: List<Float>,
    projects: List<ProjectItem>,
    modifier: Modifier = Modifier
) {
    val isRunning = serverState.isRunning

    val activeInterfaces = rememberActiveInterfaces()
    val activeSockets = NetworkUtils.getActiveSockets(metrics.sshPort, isRunning, projects)

    val cpuColor by animateColorAsState(
        targetValue = when {
            metrics.cpuUsage > 85f -> MatrixRed
            metrics.cpuUsage > 60f -> MatrixYellow
            else -> MatrixGreen
        },
        label = "cpuColor"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("system_monitoring_dashboard"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Section Title Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Speed,
                    contentDescription = null,
                    tint = MatrixGreen,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "REAL-TIME TELEMETRY & NETWORK",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    letterSpacing = 0.5.sp,
                    color = MatrixGreen
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(MatrixGreen.copy(alpha = 0.15f))
                    .border(0.8.dp, MatrixGreen, RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "LIVE (2s)",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = MatrixGreen
                )
            }
        }

        // CARD 1: REAL-TIME CPU USAGE & HISTORICAL SPARKLINE
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MatrixSurfaceBorder, RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = MatrixDarkSurface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(cpuColor.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = null,
                                tint = cpuColor,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "CPU LOAD & FREQUENCY",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MatrixTextPrimary
                            )
                            Text(
                                text = "Proportional kernel execution load",
                                style = MaterialTheme.typography.bodyMedium,
                                fontSize = 10.sp,
                                color = MatrixTextSecondary
                            )
                        }
                    }

                    Text(
                        text = String.format("%.1f%%", metrics.cpuUsage),
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = cpuColor
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Real-time Sparkline Canvas Graph
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(MatrixBlack)
                        .border(1.dp, MatrixSurfaceBorder, RoundedCornerShape(6.dp))
                        .padding(horizontal = 4.dp, vertical = 6.dp)
                ) {
                    CpuSparklineCanvas(history = cpuHistory, lineColor = cpuColor)
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.DeviceThermostat, contentDescription = null, tint = MatrixTextSecondary, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Thermal: ${metrics.thermalStatus}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = MatrixTextSecondary
                        )
                    }

                    Text(
                        text = if (metrics.cpuUsage > 85f) "HIGH LOAD WARNING" else "NORMAL LOAD",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        color = cpuColor
                    )
                }
            }
        }

        // CARD 2: RAM CONSUMPTION & BREAKDOWN
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MatrixSurfaceBorder, RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = MatrixDarkSurface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                val ramPct = if (metrics.memoryTotalMb > 0) (metrics.memoryUsedMb.toFloat() / metrics.memoryTotalMb.toFloat()) * 100f else 0f
                val ramColor = if (ramPct > 85f) MatrixRed else MatrixGreen

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(ramColor.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Memory,
                                contentDescription = null,
                                tint = ramColor,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "RAM MEMORY CONSUMPTION",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MatrixTextPrimary
                            )
                            Text(
                                text = "${metrics.memoryUsedMb} MB / ${metrics.memoryTotalMb} MB Allocated",
                                style = MaterialTheme.typography.bodyMedium,
                                fontSize = 10.sp,
                                color = MatrixTextSecondary
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(ramColor.copy(alpha = 0.15f))
                            .border(0.8.dp, ramColor, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = String.format("%.0f%% USED", ramPct),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = ramColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Linear Memory allocation progress bar
                LinearProgressIndicator(
                    progress = { (ramPct / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = ramColor,
                    trackColor = MatrixSurfaceVariant
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Matrix App Memory: ~${(metrics.memoryUsedMb * 0.25f).toInt()} MB",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = MatrixTextSecondary
                    )
                    Text(
                        text = "System Available: ${(metrics.memoryTotalMb - metrics.memoryUsedMb).coerceAtLeast(0)} MB",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = MatrixGreen
                    )
                }
            }
        }

        // CARD 3: ACTIVE NETWORK CONNECTIONS & LISTENING SOCKETS
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MatrixSurfaceBorder, RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = MatrixDarkSurface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(MatrixBlue.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.NetworkCheck,
                                contentDescription = null,
                                tint = MatrixBlue,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "NETWORK INTERFACES & CONNECTIONS",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MatrixTextPrimary
                            )
                            Text(
                                text = "Active sockets and IP network bindings",
                                style = MaterialTheme.typography.bodyMedium,
                                fontSize = 10.sp,
                                color = MatrixTextSecondary
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(MatrixBlue.copy(alpha = 0.15f))
                            .border(0.8.dp, MatrixBlue, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${activeInterfaces.size} IFACE",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = MatrixBlue
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Network Interfaces List
                Text(
                    text = "ACTIVE INTERFACES",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MatrixTextSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    activeInterfaces.forEach { iface ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(MatrixBlack)
                                .border(1.dp, MatrixSurfaceBorder, RoundedCornerShape(6.dp))
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = when (iface.type) {
                                        "VPN MESH" -> Icons.Default.VpnLock
                                        "WIFI" -> Icons.Default.Wifi
                                        else -> Icons.Default.Lan
                                    },
                                    contentDescription = null,
                                    tint = if (iface.type == "VPN MESH") MatrixBlue else MatrixGreen,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${iface.name} (${iface.type})",
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = MatrixTextPrimary
                                )
                            }

                            Text(
                                text = iface.ipAddress,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = if (iface.type == "VPN MESH") MatrixBlue else MatrixGreen
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Active Listening Sockets
                Text(
                    text = "LISTENING DAEMON SOCKETS (${activeSockets.size})",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MatrixTextSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))

                if (activeSockets.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(MatrixBlack)
                            .padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Server offline. No listening sockets active.",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.5.sp,
                            color = MatrixTextSecondary
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        activeSockets.forEach { sock ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MatrixBlack)
                                    .border(1.dp, MatrixGreen.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(MatrixGreen)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = sock.serviceName,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = MatrixGreen
                                    )
                                }

                                Text(
                                    text = "${sock.protocol} ${sock.bindAddress}:${sock.port}",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.5.sp,
                                    color = MatrixTextPrimary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Active SSH Client Sessions
                Text(
                    text = "ACTIVE SSH CLIENT SESSIONS",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MatrixTextSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))

                if (!isRunning || metrics.activeSessionsCount == 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(MatrixBlack)
                            .padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isRunning) "0 Active Client Sessions. Ready for incoming SSH connections." else "Server offline.",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.5.sp,
                            color = MatrixTextSecondary
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(MatrixGreen.copy(alpha = 0.12f))
                            .border(1.dp, MatrixGreen.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Terminal, contentDescription = null, tint = MatrixGreen, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "1 Active Client Session (SSH-2.0)",
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = MatrixGreen
                                )
                                Text(
                                    text = "Client: matrix@${metrics.lanIpAddress} (Channel: PTY Shell)",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.5.sp,
                                    color = MatrixTextPrimary
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(MatrixGreen)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "ACTIVE",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp,
                                color = MatrixBlack
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CpuSparklineCanvas(
    history: List<Float>,
    lineColor: Color
) {
    Canvas(modifier = Modifier.fillMaxWidth().height(48.dp)) {
        if (history.isEmpty()) return@Canvas

        val width = size.width
        val height = size.height
        val maxVal = 100f
        val stepX = width / (history.size - 1).coerceAtLeast(1)

        val points = history.mapIndexed { index, value ->
            val x = index * stepX
            val y = height - ((value / maxVal) * height).coerceIn(0f, height)
            Offset(x, y)
        }

        val strokePath = Path().apply {
            moveTo(points.first().x, points.first().y)
            for (i in 1 until points.size) {
                val prev = points[i - 1]
                val current = points[i]
                val controlX = (prev.x + current.x) / 2f
                cubicTo(controlX, prev.y, controlX, current.y, current.x, current.y)
            }
        }

        val fillPath = Path().apply {
            addPath(strokePath)
            lineTo(width, height)
            lineTo(0f, height)
            close()
        }

        // Draw gradient under curve
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(lineColor.copy(alpha = 0.35f), Color.Transparent),
                startY = 0f,
                endY = height
            )
        )

        // Draw sparkline curve
        drawPath(
            path = strokePath,
            color = lineColor,
            style = Stroke(width = 2.dp.toPx())
        )

        // Draw current value indicator dot
        points.lastOrNull()?.let { lastPoint ->
            drawCircle(
                color = lineColor,
                radius = 3.5.dp.toPx(),
                center = lastPoint
            )
        }
    }
}

@Composable
private fun rememberActiveInterfaces(): List<NetworkInterfaceInfo> {
    return androidx.compose.runtime.remember { NetworkUtils.getActiveInterfaces() }
}
