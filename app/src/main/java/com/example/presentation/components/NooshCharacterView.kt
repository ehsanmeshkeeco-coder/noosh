package com.example.presentation.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.presentation.theme.NooshPrimary
import com.example.presentation.theme.NooshPrimaryLight

@Composable
fun NooshCharacterView(
    modifier: Modifier = Modifier,
    size: Dp = 80.dp,
    isCelebrating: Boolean = false
) {
    val infiniteTransition = rememberInfiniteTransition(label = "character_bounce")
    val bounceY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (isCelebrating) -12f else -6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bounce"
    )

    Box(
        modifier = modifier
            .size(size)
            .offset(y = bounceY.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val width = this.size.width
            val height = this.size.height

            // Droplet Body Path
            val dropPath = Path().apply {
                moveTo(width * 0.5f, height * 0.12f)
                cubicTo(
                    width * 0.2f, height * 0.45f,
                    width * 0.15f, height * 0.65f,
                    width * 0.15f, height * 0.75f
                )
                cubicTo(
                    width * 0.15f, height * 0.95f,
                    width * 0.85f, height * 0.95f,
                    width * 0.85f, height * 0.75f
                )
                cubicTo(
                    width * 0.85f, height * 0.65f,
                    width * 0.8f, height * 0.45f,
                    width * 0.5f, height * 0.12f
                )
                close()
            }

            // Fill Droplet Gradient
            drawPath(
                path = dropPath,
                brush = Brush.verticalGradient(
                    colors = listOf(NooshPrimaryLight, NooshPrimary),
                    startY = height * 0.12f,
                    endY = height * 0.95f
                ),
                style = Fill
            )

            // Outline
            drawPath(
                path = dropPath,
                color = Color(0xFF1E88E5),
                style = Stroke(width = 3.dp.toPx())
            )

            // Light reflection on top-left
            val reflectionPath = Path().apply {
                moveTo(width * 0.32f, height * 0.45f)
                cubicTo(
                    width * 0.28f, height * 0.55f,
                    width * 0.28f, height * 0.68f,
                    width * 0.35f, height * 0.75f
                )
            }
            drawPath(
                path = reflectionPath,
                color = Color.White.copy(alpha = 0.6f),
                style = Stroke(width = 3.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
            )

            // Cute Eyes
            val eyeRadius = width * 0.055f
            val leftEyeCenter = Offset(width * 0.38f, height * 0.62f)
            val rightEyeCenter = Offset(width * 0.62f, height * 0.62f)

            // Eyes (Sparkling)
            drawCircle(color = Color(0xFF0F172A), radius = eyeRadius, center = leftEyeCenter)
            drawCircle(color = Color(0xFF0F172A), radius = eyeRadius, center = rightEyeCenter)
            // Catchlights
            drawCircle(color = Color.White, radius = eyeRadius * 0.4f, center = leftEyeCenter.minus(Offset(2f, 2f)))
            drawCircle(color = Color.White, radius = eyeRadius * 0.4f, center = rightEyeCenter.minus(Offset(2f, 2f)))

            // Cheeks (Blush)
            drawCircle(
                color = Color(0xFFFF8FA3).copy(alpha = 0.6f),
                radius = eyeRadius * 0.9f,
                center = Offset(width * 0.28f, height * 0.68f)
            )
            drawCircle(
                color = Color(0xFFFF8FA3).copy(alpha = 0.6f),
                radius = eyeRadius * 0.9f,
                center = Offset(width * 0.72f, height * 0.68f)
            )

            // Smile
            val smilePath = Path().apply {
                moveTo(width * 0.42f, height * 0.72f)
                quadraticBezierTo(
                    width * 0.5f, height * 0.80f,
                    width * 0.58f, height * 0.72f
                )
            }
            drawPath(
                path = smilePath,
                color = Color(0xFF0F172A),
                style = Stroke(width = 2.5.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
            )

            // If celebrating, draw small gold star or heart at head
            if (isCelebrating) {
                drawCircle(
                    color = Color(0xFFFFD166),
                    radius = width * 0.08f,
                    center = Offset(width * 0.78f, height * 0.22f)
                )
            }
        }
    }
}
