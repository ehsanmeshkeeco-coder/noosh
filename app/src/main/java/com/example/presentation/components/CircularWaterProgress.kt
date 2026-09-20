package com.example.presentation.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.util.DateTimeUtils
import com.example.presentation.theme.NooshAccent
import com.example.presentation.theme.NooshPrimary
import com.example.presentation.theme.NooshSubtleBlue
import com.example.presentation.theme.NooshWaveEnd
import com.example.presentation.theme.NooshWaveStart
import kotlin.math.sin

@Composable
fun CircularWaterProgress(
    percentage: Int,
    consumedMl: Int,
    goalMl: Int,
    modifier: Modifier = Modifier,
    size: Dp = 230.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "wave_anim")
    val waveOffset1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_offset_1"
    )

    val waveOffset2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_offset_2"
    )

    val progressFraction = (percentage / 100f).coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .size(size)
            .testTag("circular_water_progress"),
        contentAlignment = Alignment.Center
    ) {
        // Outer soft glow ring
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(NooshSubtleBlue.copy(alpha = 0.5f))
                .border(6.dp, NooshSubtleBlue, CircleShape)
        )

        // Wave Canvas inside Circle
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp)
                .clip(CircleShape)
        ) {
            val width = this.size.width
            val height = this.size.height
            val waterHeight = height * progressFraction
            val waterTopY = height - waterHeight

            val clipCircle = Path().apply {
                addOval(androidx.compose.ui.geometry.Rect(0f, 0f, width, height))
            }

            clipPath(clipCircle) {
                // Background behind water
                drawRect(color = Color(0xFFF0F8FF))

                // Back Wave (Lighter blue)
                val backWavePath = Path().apply {
                    moveTo(0f, height)
                    lineTo(0f, waterTopY)
                    val waveAmplitude = 12f
                    val waveLength = width / 1.2f
                    var x = 0f
                    while (x <= width) {
                        val y = waterTopY + waveAmplitude * sin((x / waveLength * 2 * Math.PI + waveOffset2).toFloat())
                        lineTo(x, y)
                        x += 4f
                    }
                    lineTo(width, height)
                    close()
                }
                drawPath(
                    path = backWavePath,
                    color = NooshWaveStart.copy(alpha = 0.55f)
                )

                // Front Wave (Gradient vibrant blue)
                val frontWavePath = Path().apply {
                    moveTo(0f, height)
                    lineTo(0f, waterTopY)
                    val waveAmplitude = 14f
                    val waveLength = width / 1.0f
                    var x = 0f
                    while (x <= width) {
                        val y = waterTopY + waveAmplitude * sin((x / waveLength * 2 * Math.PI + waveOffset1).toFloat())
                        lineTo(x, y)
                        x += 4f
                    }
                    lineTo(width, height)
                    close()
                }
                drawPath(
                    path = frontWavePath,
                    brush = Brush.verticalGradient(
                        colors = listOf(NooshWaveStart, NooshWaveEnd),
                        startY = waterTopY - 10f,
                        endY = height
                    )
                )
            }
        }

        // Percentage and Info overlay inside circle
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            val isHighWater = percentage > 55
            val textColor = if (isHighWater) Color.White else MaterialTheme.colorScheme.onBackground
            val subtextColor = if (isHighWater) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant

            Text(
                text = "${DateTimeUtils.toPersianDigits(percentage.toString())}٪",
                fontSize = 42.sp,
                fontWeight = FontWeight.Black,
                color = textColor
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${DateTimeUtils.toPersianDigits(consumedMl.toString())} / ${DateTimeUtils.toPersianDigits(goalMl.toString())} ml",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = subtextColor
            )
            Spacer(modifier = Modifier.height(2.dp))
            val glasses = consumedMl / 250
            val goalGlasses = (goalMl / 250).coerceAtLeast(1)
            Text(
                text = "${DateTimeUtils.toPersianDigits(glasses.toString())} از ${DateTimeUtils.toPersianDigits(goalGlasses.toString())} لیوان",
                fontSize = 12.sp,
                color = subtextColor
            )
        }
    }
}
