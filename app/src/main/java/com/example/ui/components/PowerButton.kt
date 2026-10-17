package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ServerState
import com.example.ui.theme.MatrixBlack
import com.example.ui.theme.MatrixDarkSurface
import com.example.ui.theme.MatrixGreen
import com.example.ui.theme.MatrixGreenDark
import com.example.ui.theme.MatrixRed
import com.example.ui.theme.MatrixTextSecondary
import com.example.ui.theme.MatrixYellow

@Composable
fun PowerButton(
    serverState: ServerState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isRunning = serverState.isRunning
    val isTransitioning = serverState.isTransitioning

    // Infinite breathing glow pulse when active or starting
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (isRunning || isTransitioning) 1.06f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val activeColor = when (serverState) {
        ServerState.ON -> MatrixGreen
        ServerState.STARTING, ServerState.STOPPING -> MatrixYellow
        ServerState.DEGRADED -> MatrixYellow
        ServerState.OFF -> MatrixRed
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(160.dp)
                .scale(pulseScale)
        ) {
            // Outer radiant glow ring
            Box(
                modifier = Modifier
                    .size(154.dp)
                    .clip(CircleShape)
                    .background(activeColor.copy(alpha = if (isRunning) 0.15f else 0.05f))
                    .border(
                        width = 2.dp,
                        brush = Brush.radialGradient(
                            colors = listOf(activeColor, activeColor.copy(alpha = 0.2f))
                        ),
                        shape = CircleShape
                    )
            )

            // Inner button surface
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(130.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(MatrixDarkSurface, MatrixBlack)
                        )
                    )
                    .border(
                        width = 3.dp,
                        color = activeColor,
                        shape = CircleShape
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = true, radius = 65.dp),
                        onClick = onClick
                    )
                    .testTag("power_button")
            ) {
                if (isTransitioning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(60.dp),
                        color = activeColor,
                        strokeWidth = 3.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.PowerSettingsNew,
                        contentDescription = "Server Power",
                        tint = activeColor,
                        modifier = Modifier.size(54.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = when (serverState) {
                ServerState.ON -> "STOP SERVER"
                ServerState.STARTING -> "INITIALIZING..."
                ServerState.STOPPING -> "SHUTTING DOWN..."
                ServerState.DEGRADED -> "SERVER DEGRADED"
                ServerState.OFF -> "START SERVER"
            },
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp,
            letterSpacing = 1.sp,
            color = activeColor
        )

        Text(
            text = when (serverState) {
                ServerState.ON -> "Tap to gracefully shutdown daemon"
                ServerState.STARTING -> "Binding port & verifying environment"
                ServerState.STOPPING -> "Terminating child processes..."
                ServerState.DEGRADED -> "SSH listener offline or degraded"
                ServerState.OFF -> "Tap to launch Linux userspace & SSH"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MatrixTextSecondary,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
