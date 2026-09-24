package com.example.presentation.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.core.util.DateTimeUtils
import com.example.domain.model.DayIntake
import com.example.presentation.components.RechartsWeeklyVisualization
import com.example.presentation.components.WeeklyBarChart
import com.example.presentation.theme.NooshPrimary
import com.example.presentation.theme.NooshSubtleBlue
import com.example.presentation.theme.SuccessGreen
import com.example.presentation.viewmodel.MainViewModel
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset

@Composable
fun WeeklyAnalyticsScreen(
    viewModel: MainViewModel,
    onNavigateToRechartsTrend: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val weeklyReport by viewModel.weeklyReport.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("weekly_analytics_screen"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))

            // Title
            Text(
                text = stringResource(R.string.weekly_report_title),
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "تحلیل روند هیدراتاسیون در ۷ روز گذشته",
                fontSize = 13.sp,
                color = Color(0xFF64748B),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp)
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Metrics Row
            val report = weeklyReport
            val totalLiters = (report?.totalAmountMl ?: 0) / 1000f
            val avgMl = report?.dailyAverageMl ?: 0
            val completionPct = report?.goalCompletionPercentage ?: 0

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricCard(
                    title = "مجموع هفتگی",
                    value = "${String.format("%.1f", totalLiters)} لیتر",
                    subtitle = "۵۶ لیوان هدف",
                    modifier = Modifier.weight(1f)
                )

                MetricCard(
                    title = "میانگین روزانه",
                    value = "${DateTimeUtils.toPersianDigits(avgMl.toString())} میلی‌لیتر",
                    subtitle = "حدود ${(avgMl / 250)} لیوان",
                    modifier = Modifier.weight(1f)
                )

                MetricCard(
                    title = "تکمیل هدف",
                    value = "${DateTimeUtils.toPersianDigits(completionPct.toString())}٪",
                    subtitle = "عالی پیش می‌ری!",
                    valueColor = if (completionPct >= 80) SuccessGreen else NooshPrimary,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Positive trend banner
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFECFDF5)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.TrendingUp,
                        contentDescription = null,
                        tint = SuccessGreen,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "مصرف آب شما ۱۲٪ نسبت به هفته گذشته افزایش داشته است. این روند به سلامت کلیه‌ها و شادابی پوست شما کمک می‌کند 💧",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF065F46)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Chart View Mode Switcher
            var selectedChartMode by remember { mutableIntStateOf(0) }
            val days = report?.days ?: emptyList()

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                TabRow(
                    selectedTabIndex = selectedChartMode,
                    containerColor = Color.Transparent,
                    contentColor = NooshPrimary,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedChartMode]),
                            color = NooshPrimary,
                            height = 3.dp
                        )
                    }
                ) {
                    Tab(
                        selected = selectedChartMode == 0,
                        onClick = { selectedChartMode = 0 },
                        text = {
                            Text(
                                text = "نمودار هوشمند Recharts",
                                fontWeight = if (selectedChartMode == 0) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
                            )
                        }
                    )
                    Tab(
                        selected = selectedChartMode == 1,
                        onClick = { selectedChartMode = 1 },
                        text = {
                            Text(
                                text = "نمودار ستونی بومی",
                                fontWeight = if (selectedChartMode == 1) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (selectedChartMode == 0) {
                // Interactive Recharts Visualization
                RechartsWeeklyVisualization(
                    days = days,
                    goalMl = 2000,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                // Native Weekly Bar Chart
                WeeklyBarChart(
                    days = days,
                    averageMl = avgMl,
                    goalMl = 2000
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            androidx.compose.material3.OutlinedButton(
                onClick = onNavigateToRechartsTrend,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("btn_open_recharts_dedicated_screen"),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, NooshPrimary)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ShowChart,
                    contentDescription = null,
                    tint = NooshPrimary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "مشاهده در صفحه اختصاصی نمودار خطی Recharts ↗",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = NooshPrimary
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "ریزجزئیات روزانه",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))
        }

        // Daily breakdown list
        val daysList = weeklyReport?.days ?: emptyList()
        items(daysList) { day ->
            DailyDetailCard(day = day)
        }

        item {
            Spacer(modifier = Modifier.height(90.dp))
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    subtitle: String,
    valueColor: Color = NooshPrimary,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontSize = 11.sp,
                color = Color(0xFF64748B),
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = DateTimeUtils.toPersianDigits(value),
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                color = valueColor
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = Color(0xFF94A3B8)
            )
        }
    }
}

@Composable
private fun DailyDetailCard(day: DayIntake) {
    val isGoalReached = day.amountMl >= day.goalMl
    val glasses = day.amountMl / 250
    val goalGlasses = day.goalMl / 250

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (isGoalReached) Color(0xFFDCFCE7) else NooshSubtleBlue),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isGoalReached) Icons.Default.CheckCircle else Icons.Default.Info,
                        contentDescription = null,
                        tint = if (isGoalReached) SuccessGreen else NooshPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = if (day.isToday) "${day.dayName} (امروز)" else day.dayName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (day.isToday) NooshPrimary else Color(0xFF0F172A)
                    )
                    Text(
                        text = "${DateTimeUtils.toPersianDigits(glasses.toString())} از ${DateTimeUtils.toPersianDigits(goalGlasses.toString())} لیوان",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${DateTimeUtils.toPersianDigits(day.amountMl.toString())} میلی‌لیتر",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
                Text(
                    text = if (isGoalReached) "هدف کامل شد ✓" else "${DateTimeUtils.toPersianDigits((day.goalMl - day.amountMl).toString())} میلی‌لیتر مانده",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isGoalReached) SuccessGreen else Color(0xFF94A3B8)
                )
            }
        }
    }
}
