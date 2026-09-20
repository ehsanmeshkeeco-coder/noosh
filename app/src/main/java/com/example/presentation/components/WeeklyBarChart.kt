package com.example.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.util.DateTimeUtils
import com.example.domain.model.DayIntake
import com.example.presentation.theme.NooshPrimary
import com.example.presentation.theme.NooshPrimaryLight
import com.example.presentation.theme.NooshSubtleBlue

@Composable
fun WeeklyBarChart(
    days: List<DayIntake>,
    averageMl: Int,
    goalMl: Int = 2000,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("weekly_bar_chart")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "نمودار ۷ روز گذشته",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Text(
                    text = "هدف: ${DateTimeUtils.toPersianDigits(goalMl.toString())} میلی‌لیتر",
                    fontSize = 12.sp,
                    color = Color(0xFF64748B),
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Chart Canvas area
            val maxDisplayAmount = maxOf(goalMl, days.maxOfOrNull { it.amountMl } ?: goalMl, 2500)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                // Goal reference line
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val goalY = h - (goalMl.toFloat() / maxDisplayAmount * h)

                    drawLine(
                        color = Color(0xFF94A3B8),
                        start = Offset(0f, goalY),
                        end = Offset(w, goalY),
                        strokeWidth = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f)
                    )
                }

                // Bars
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom
                ) {
                    days.forEach { day ->
                        DayBarItem(
                            day = day,
                            maxAmount = maxDisplayAmount
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(NooshPrimary, RoundedCornerShape(2.dp))
                )
                Text(
                    text = "میزان مصرف آب",
                    fontSize = 11.sp,
                    color = Color(0xFF64748B),
                    modifier = Modifier.padding(start = 6.dp, end = 16.dp)
                )

                Box(
                    modifier = Modifier
                        .width(16.dp)
                        .height(2.dp)
                        .background(Color(0xFF94A3B8))
                )
                Text(
                    text = "خط هدف روزانه",
                    fontSize = 11.sp,
                    color = Color(0xFF64748B),
                    modifier = Modifier.padding(start = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun DayBarItem(
    day: DayIntake,
    maxAmount: Int
) {
    val barHeightFraction = (day.amountMl.toFloat() / maxAmount).coerceIn(0.04f, 1f)
    val isGoalAchieved = day.amountMl >= day.goalMl

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(36.dp)
    ) {
        // Amount label above bar if non-zero
        if (day.amountMl > 0) {
            Text(
                text = DateTimeUtils.toPersianDigits((day.amountMl / 250).toString()),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = if (day.isToday) NooshPrimary else Color(0xFF64748B)
            )
        } else {
            Spacer(modifier = Modifier.height(14.dp))
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Vertical Bar
        Box(
            modifier = Modifier
                .width(18.dp)
                .height(120.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            // Background bar track
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                    .background(NooshSubtleBlue.copy(alpha = 0.4f))
            )

            // Fill Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(barHeightFraction)
                    .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                    .background(
                        if (isGoalAchieved) {
                            Brush.verticalGradient(listOf(NooshPrimaryLight, NooshPrimary))
                        } else if (day.isToday) {
                            Brush.verticalGradient(listOf(NooshPrimary, Color(0xFF0284C7)))
                        } else {
                            Brush.verticalGradient(listOf(Color(0xFF93C5FD), Color(0xFF60A5FA)))
                        }
                    )
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Day Name
        Text(
            text = day.dayName,
            fontSize = 11.sp,
            fontWeight = if (day.isToday) FontWeight.Black else FontWeight.Medium,
            color = if (day.isToday) NooshPrimary else Color(0xFF475569)
        )
    }
}
