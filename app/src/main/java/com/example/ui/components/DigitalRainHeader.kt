package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import com.example.ui.theme.MatrixGreen
import com.example.ui.theme.MatrixGreenDark
import kotlin.random.Random

@Composable
fun DigitalRainHeader(
    lowPowerMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    if (lowPowerMode) {
        Canvas(modifier = modifier.fillMaxWidth().height(60.dp)) {
            // Static cyber scanline
            drawLine(
                color = MatrixGreenDark.copy(alpha = 0.3f),
                start = Offset(0f, size.height - 2f),
                end = Offset(size.width, size.height - 2f),
                strokeWidth = 2f
            )
        }
        return
    }

    val infiniteTransition = rememberInfiniteTransition(label = "digitalRain")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rainPhase"
    )

    // Pre-calculated column positions
    val columnSeeds = remember {
        List(24) { index ->
            Triple(
                Random.nextFloat(), // speed multiplier
                Random.nextInt(5, 12), // length
                Random.nextFloat() // phase offset
            )
        }
    }

    Canvas(modifier = modifier.fillMaxWidth().height(70.dp)) {
        val colWidth = size.width / columnSeeds.size
        val paint = android.graphics.Paint().apply {
            textSize = 28f
            typeface = android.graphics.Typeface.MONOSPACE
            isAntiAlias = true
        }

        val chars = "010101XYZΩλπµ$#@+*><=~"

        columnSeeds.forEachIndexed { colIdx, (speed, len, offset) ->
            val colX = colIdx * colWidth + (colWidth / 2f)
            val currentProgress = (phase * speed + offset) % 1.0f
            val headY = currentProgress * (size.height + len * 25f)

            for (i in 0 until len) {
                val charY = headY - (i * 22f)
                if (charY in 0f..size.height) {
                    val alpha = (1.0f - (i.toFloat() / len.toFloat())).coerceIn(0.1f, 0.9f)
                    val charIdx = ((colIdx * 7 + i * 3) % chars.length).toInt()
                    val c = chars[charIdx].toString()

                    if (i == 0) {
                        // Head character is white/bright green
                        paint.color = android.graphics.Color.argb((alpha * 255).toInt(), 220, 255, 230)
                    } else {
                        paint.color = android.graphics.Color.argb((alpha * 180).toInt(), 0, 255, 102)
                    }

                    drawContext.canvas.nativeCanvas.drawText(c, colX, charY, paint)
                }
            }
        }

        // Bottom green border line
        drawLine(
            color = MatrixGreen.copy(alpha = 0.25f),
            start = Offset(0f, size.height - 1f),
            end = Offset(size.width, size.height - 1f),
            strokeWidth = 2f
        )
    }
}
