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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.util.DateTimeUtils
import com.example.domain.model.DayIntake
import com.example.presentation.components.RechartsWeeklyVisualization
import com.example.presentation.theme.NooshPrimary
import com.example.presentation.theme.NooshSubtleBlue
import com.example.presentation.theme.SuccessGreen
import com.example.presentation.viewmodel.MainViewModel

/**
 * Dedicated Screen using the Recharts library to visualize the last 7 days
 * of water consumption data with a clean line chart and detailed metrics.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RechartsWeeklyTrendScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val weeklyReport by viewModel.weeklyReport.collectAsState()
    val dashboardState by viewModel.dashboardState.collectAsState()
    val dailyGoalMl = dashboardState?.profile?.dailyWaterGoalMl ?: 2000

    val report = weeklyReport
    val days = report?.days ?: emptyList()
    val totalLiters = (report?.totalAmountMl ?: 0) / 1000f
    val avgMl = report?.dailyAverageMl ?: 0
    val completionPct = report?.goalCompletionPercentage ?: 0
    val trendPct = report?.trendVsLastWeekPercent ?: 0

    val bestDay = days.maxByOrNull { it.amountMl }
    val lowestDay = days.minByOrNull { it.amountMl }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "روند خطی ۷ روزه Recharts",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "تحلیل بصری مصرف آب با نمودار خطی Recharts",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("btn_back_trend")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "بازگشت",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        modifier = modifier
            .fillMaxSize()
            .testTag("recharts_weekly_trend_screen")
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))

                // Metrics Highlights Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TrendMetricCard(
                        title = "مجموع ۷ روز",
                        value = "${DateTimeUtils.toPersianDigits(String.format("%.1f", totalLiters))} لیتر",
                        color = NooshPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    TrendMetricCard(
                        title = "میانگین روزانه",
                        value = "${DateTimeUtils.toPersianDigits(avgMl.toString())} ml",
                        color = Color(0xFF0284C7),
                        modifier = Modifier.weight(1f)
                    )
                    TrendMetricCard(
                        title = "تحقق هدف",
                        value = "${DateTimeUtils.toPersianDigits(completionPct.toString())}٪",
                        color = SuccessGreen,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Recharts Clean Line Chart Card
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("recharts_line_chart_card")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
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
                                        .background(NooshSubtleBlue),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ShowChart,
                                        contentDescription = null,
                                        tint = NooshPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "نمودار خطی هیدراتاسیون Recharts",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F172A)
                                    )
                                    Text(
                                        text = "روند مصرف ۷ روز اخیر همراه با خط هدف",
                                        fontSize = 11.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFE0F2FE))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "Recharts JS",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0369A1)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Recharts Interactive WebView Component
                        RechartsWeeklyVisualization(
                            days = days,
                            goalMl = dailyGoalMl,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Weekly Trend Analysis Card
                Card(
                    shape = RoundedCornerShape(16.dp),
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
                            imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                            contentDescription = null,
                            tint = SuccessGreen,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "تحلیل روند مصرف: ${DateTimeUtils.toPersianDigits(trendPct.toString())}٪ تغییر نسبت به هفته پیش",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF065F46)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "حفظ خط افقی پیوسته نزدیک به هدف ۲۰۰۰ میلی‌لیتر نشان‌دهنده هیدراتاسیون متعادل و منظم است.",
                                fontSize = 11.sp,
                                color = Color(0xFF047857),
                                lineHeight = 16.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Extremes: Best vs Lowest Day
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "🌟 اوج مصرف هفته",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF0284C7)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = bestDay?.let { "${it.dayName}: ${DateTimeUtils.toPersianDigits(it.amountMl.toString())}ml" } ?: "-",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF0F172A)
                            )
                        }
                    }

                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "📉 کمترین مصرف هفته",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF64748B)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = lowestDay?.let { "${it.dayName}: ${DateTimeUtils.toPersianDigits(it.amountMl.toString())}ml" } ?: "-",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF0F172A)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Breakdown of the 7 Days
                Text(
                    text = "جزئیات روزانه ۷ روز اخیر",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))
            }

            items(days) { day ->
                DailyDetailRow(day = day, dailyGoalMl = dailyGoalMl)
                Spacer(modifier = Modifier.height(8.dp))
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun TrendMetricCard(
    title: String,
    value: String,
    color: Color,
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
                text = value,
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                color = color
            )
        }
    }
}

@Composable
private fun DailyDetailRow(
    day: DayIntake,
    dailyGoalMl: Int
) {
    val isGoalAchieved = day.amountMl >= dailyGoalMl
    val progress = (day.amountMl.toFloat() / dailyGoalMl.toFloat()).coerceIn(0f, 1f)

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (day.isToday) Color(0xFFF0F9FF) else Color.White
        ),
        border = if (day.isToday) androidx.compose.foundation.BorderStroke(1.dp, NooshPrimary.copy(alpha = 0.4f)) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (isGoalAchieved) Color(0xFFDCFCE7) else Color(0xFFF1F5F9)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isGoalAchieved) Icons.Default.CheckCircle else Icons.Default.LocalDrink,
                    contentDescription = null,
                    tint = if (isGoalAchieved) SuccessGreen else Color(0xFF64748B),
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (day.isToday) "${day.dayName} (امروز)" else day.dayName,
                        fontSize = 13.sp,
                        fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Medium,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = "${DateTimeUtils.toPersianDigits(day.amountMl.toString())} / ${DateTimeUtils.toPersianDigits(dailyGoalMl.toString())} ml",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isGoalAchieved) SuccessGreen else Color(0xFF0284C7)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = if (isGoalAchieved) SuccessGreen else NooshPrimary,
                    trackColor = Color(0xFFE2E8F0)
                )
            }
        }
    }
}
