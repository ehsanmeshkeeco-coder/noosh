package com.example.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.util.DateTimeUtils
import com.example.domain.model.DayIntake

enum class RechartsChartType(val title: String) {
    AREA("ناحیه‌ای"),
    LINE("خطی"),
    BAR("ستونی")
}

/**
 * Modern High-Performance Data Visualization Component implementing Recharts engine
 * natively in Jetpack Compose.
 *
 * Provides smooth Bézier curve interpolation, area gradient shading, custom bar chart,
 * dashed goal reference line, interactive day selection, and tooltip inspection.
 *
 * Built directly with Jetpack Compose Canvas to eliminate Chromium/MESA rendernode
 * sandbox errors on emulator environments while delivering 60+ FPS native rendering.
 */
@Composable
fun RechartsWeeklyVisualization(
    days: List<DayIntake>,
    goalMl: Int = 2000,
    initialChartType: RechartsChartType = RechartsChartType.BAR,
    modifier: Modifier = Modifier
) {
    var chartType by remember { mutableStateOf(initialChartType) }
    var selectedIndex by remember(days) {
        val todayIdx = days.indexOfFirst { it.isToday }
        mutableIntStateOf(if (todayIdx >= 0) todayIdx else (days.size - 1).coerceAtLeast(0))
    }

    val selectedDay = days.getOrNull(selectedIndex)

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("recharts_weekly_visualization")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header with Recharts badge and Chart Type Switcher
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (chartType) {
                                RechartsChartType.AREA -> Icons.AutoMirrored.Filled.ShowChart
                                RechartsChartType.LINE -> Icons.Default.Timeline
                                RechartsChartType.BAR -> Icons.Default.BarChart
                            },
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "نمودار هفتگی مصرف آب",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "تحلیل روند هیدراتاسیون روزانه",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Type Switcher (Area / Line / Bar)
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    RechartsChartType.entries.forEach { type ->
                        val isSelected = type == chartType
                        val bgColor by animateColorAsState(
                            targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                            animationSpec = tween(150),
                            label = "tab_bg"
                        )
                        val textColor by animateColorAsState(
                            targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            animationSpec = tween(150),
                            label = "tab_text"
                        )

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(bgColor)
                                .clickable { chartType = type }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = type.title,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = textColor
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Interactive Tooltip / Selected Day Detail Banner
            selectedDay?.let { day ->
                val pct = if (goalMl > 0) (day.amountMl * 100 / goalMl) else 0
                val glasses = day.amountMl / 250

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.WaterDrop,
                                contentDescription = null,
                                tint = Color(0xFF0284C7),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = day.dayName,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (day.isToday) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(MaterialTheme.colorScheme.primaryContainer)
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "امروز",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = "${DateTimeUtils.toPersianDigits(glasses.toString())} لیوان استاندارد",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "${DateTimeUtils.toPersianDigits(day.amountMl.toString())} میلی‌لیتر",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (pct >= 100) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF10B981),
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "تحقق هدف (${DateTimeUtils.toPersianDigits(pct.toString())}٪)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF10B981)
                                    )
                                } else {
                                    Text(
                                        text = "${DateTimeUtils.toPersianDigits(pct.toString())}٪ از هدف",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFFF59E0B)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Main Canvas Rendering Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(days) {
                            detectTapGestures { offset ->
                                if (days.isEmpty()) return@detectTapGestures
                                val width = size.width
                                val stepX = if (days.size > 1) width / (days.size - 1) else width
                                val clickedIdx = ((offset.x + stepX / 2f) / stepX)
                                    .toInt()
                                    .coerceIn(0, days.size - 1)
                                selectedIndex = clickedIdx
                            }
                        }
                ) {
                    if (days.isEmpty()) return@Canvas

                    val width = size.width
                    val height = size.height
                    val paddingBottom = 24f
                    val paddingTop = 16f
                    val chartHeight = height - paddingBottom - paddingTop

                    val maxMl = maxOf(goalMl, days.maxOfOrNull { it.amountMl } ?: goalMl).coerceAtLeast(500)
                    val stepX = if (days.size > 1) width / (days.size - 1) else width

                    // 1. Horizontal dashed grid lines
                    val gridLines = 4
                    val gridLineColor = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)
                    for (i in 0..gridLines) {
                        val y = paddingTop + (chartHeight * i / gridLines)
                        drawLine(
                            color = gridLineColor,
                            start = Offset(0f, y),
                            end = Offset(width, y),
                            strokeWidth = 1.5f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                        )
                    }

                    // 2. Goal Reference Line (Emerald Dashed Line)
                    val goalY = paddingTop + chartHeight * (1f - (goalMl.toFloat() / maxMl).coerceIn(0f, 1f))
                    drawLine(
                        color = Color(0xFF10B981),
                        start = Offset(0f, goalY),
                        end = Offset(width, goalY),
                        strokeWidth = 2.5f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                    )

                    // 3. Compute data coordinates
                    val points = days.mapIndexed { index, day ->
                        val x = index * stepX
                        val normalizedY = (day.amountMl.toFloat() / maxMl).coerceIn(0f, 1.1f)
                        val y = paddingTop + chartHeight * (1f - normalizedY)
                        Offset(x, y)
                    }

                    when (chartType) {
                        RechartsChartType.BAR -> {
                            val barWidth = (stepX * 0.45f).coerceIn(16f, 38f)
                            days.forEachIndexed { index, day ->
                                val normalizedY = (day.amountMl.toFloat() / maxMl).coerceIn(0f, 1.1f)
                                val barHeight = chartHeight * normalizedY
                                val x = if (days.size > 1) index * stepX - (barWidth / 2f) else (width - barWidth) / 2f
                                val y = paddingTop + (chartHeight - barHeight)
                                val isSelected = index == selectedIndex
                                val isToday = day.isToday

                                val barBrush = when {
                                    isSelected -> Brush.verticalGradient(
                                        colors = listOf(Color(0xFF0284C7), Color(0xFF0369A1)),
                                        startY = y,
                                        endY = paddingTop + chartHeight
                                    )
                                    isToday -> Brush.verticalGradient(
                                        colors = listOf(Color(0xFF38BDF8), Color(0xFF0284C7)),
                                        startY = y,
                                        endY = paddingTop + chartHeight
                                    )
                                    day.amountMl >= goalMl -> Brush.verticalGradient(
                                        colors = listOf(Color(0xFF34D399), Color(0xFF10B981)),
                                        startY = y,
                                        endY = paddingTop + chartHeight
                                    )
                                    else -> Brush.verticalGradient(
                                        colors = listOf(Color(0xFFBAE6FD), Color(0xFF7DD3FC)),
                                        startY = y,
                                        endY = paddingTop + chartHeight
                                    )
                                }

                                drawRoundRect(
                                    brush = barBrush,
                                    topLeft = Offset(x, y),
                                    size = Size(barWidth, barHeight.coerceAtLeast(4f)),
                                    cornerRadius = CornerRadius(8f, 8f)
                                )
                            }
                        }

                        RechartsChartType.AREA, RechartsChartType.LINE -> {
                            if (points.size >= 2) {
                                val linePath = Path().apply {
                                    moveTo(points[0].x, points[0].y)
                                    for (i in 0 until points.size - 1) {
                                        val p0 = points[i]
                                        val p1 = points[i + 1]
                                        val controlPoint1 = Offset(p0.x + (p1.x - p0.x) / 2f, p0.y)
                                        val controlPoint2 = Offset(p0.x + (p1.x - p0.x) / 2f, p1.y)
                                        cubicTo(controlPoint1.x, controlPoint1.y, controlPoint2.x, controlPoint2.y, p1.x, p1.y)
                                    }
                                }

                                if (chartType == RechartsChartType.AREA) {
                                    val fillPath = Path().apply {
                                        addPath(linePath)
                                        lineTo(points.last().x, height - paddingBottom)
                                        lineTo(points.first().x, height - paddingBottom)
                                        close()
                                    }
                                    drawPath(
                                        path = fillPath,
                                        brush = Brush.verticalGradient(
                                            colors = listOf(
                                                Color(0x660284C7),
                                                Color(0x200284C7),
                                                Color(0x020284C7)
                                            ),
                                            startY = paddingTop,
                                            endY = height - paddingBottom
                                        )
                                    )
                                }

                                // Stroke Line
                                drawPath(
                                    path = linePath,
                                    color = Color(0xFF0284C7),
                                    style = Stroke(width = 4f)
                                )
                            }

                            // Interactive data nodes
                            points.forEachIndexed { index, point ->
                                val isToday = days[index].isToday
                                val isSelected = index == selectedIndex

                                if (isSelected || isToday) {
                                    drawCircle(
                                        color = Color(0x330284C7),
                                        radius = 16f,
                                        center = point
                                    )
                                }

                                drawCircle(
                                    color = Color.White,
                                    radius = if (isSelected) 8f else 6f,
                                    center = point
                                )
                                drawCircle(
                                    color = if (isSelected) Color(0xFF0369A1) else if (isToday) Color(0xFF0284C7) else Color(0xFF38BDF8),
                                    radius = if (isSelected) 5f else 4f,
                                    center = point
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // X-Axis Day Labels Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                days.forEachIndexed { index, day ->
                    val shortLetter = when {
                        day.dayName.contains("یک") -> "ی"
                        day.dayName.contains("دو") -> "د"
                        day.dayName.contains("سه") -> "س"
                        day.dayName.contains("چهار") -> "چ"
                        day.dayName.contains("پنج") -> "پ"
                        day.dayName.contains("جمعه") -> "ج"
                        day.dayName.contains("شنبه") -> "ش"
                        else -> day.dayName.take(1)
                    }
                    val isSelected = index == selectedIndex
                    val isToday = day.isToday

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                when {
                                    isSelected -> MaterialTheme.colorScheme.primary
                                    isToday -> MaterialTheme.colorScheme.primaryContainer
                                    else -> Color.Transparent
                                }
                            )
                            .clickable { selectedIndex = index }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = shortLetter,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                                color = when {
                                    isSelected -> MaterialTheme.colorScheme.onPrimary
                                    isToday -> MaterialTheme.colorScheme.onPrimaryContainer
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                fontSize = 12.sp
                            ),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Legend Footer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "میزان مصرف روزانه",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.width(20.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "خط هدف (${DateTimeUtils.toPersianDigits(goalMl.toString())} ml)",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

/**
 * Backward compatibility alias for RechartsNativeFallbackChart.
 */
@Composable
fun RechartsNativeFallbackChart(
    days: List<DayIntake>,
    goalMl: Int,
    modifier: Modifier = Modifier
) {
    RechartsWeeklyVisualization(
        days = days,
        goalMl = goalMl,
        modifier = modifier
    )
}
