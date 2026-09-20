package com.example.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.presentation.components.CelebrationDialog
import com.example.presentation.components.CircularWaterProgress
import com.example.presentation.components.GlassRowIndicator
import com.example.presentation.components.QuickAddSheet
import com.example.presentation.theme.FlameOrange
import com.example.presentation.theme.NooshPrimary
import com.example.presentation.theme.NooshSubtleBlue
import com.example.presentation.viewmodel.MainViewModel

@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val dashboardState by viewModel.dashboardState.collectAsState()
    val celebrationEvent by viewModel.celebrationEvent.collectAsState()
    var showQuickAddSheet by remember { mutableStateOf(false) }

    val state = dashboardState

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .testTag("dashboard_screen"),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Spacer(modifier = Modifier.height(16.dp))

                // Top Header: Greeting & Health Streak Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "${stringResource(R.string.greeting_hello)} ${state?.profile?.name ?: "کاربر گرامی"} 👋",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = stringResource(R.string.greeting_subtitle),
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Streak Badge
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(FlameOrange.copy(alpha = 0.12f))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = "زنجیره",
                            tint = FlameOrange,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${DateTimeUtils.toPersianDigits((state?.streak?.currentStreak ?: 1).toString())} روز",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = FlameOrange
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Main Circular Water Progress with wave
                CircularWaterProgress(
                    percentage = state?.percentage ?: 0,
                    consumedMl = state?.totalConsumedMl ?: 0,
                    goalMl = state?.goalMl ?: 2000,
                    size = 230.dp
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Glass Row Indicator (Glasses)
                GlassRowIndicator(
                    consumedGlasses = state?.glassesConsumed ?: 0,
                    totalGoalGlasses = state?.totalGlassesGoal ?: 8,
                    onGlassClick = {
                        viewModel.addWater(250, "app_quick")
                    }
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Next Reminder Card
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("next_reminder_card")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(NooshSubtleBlue),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Alarm,
                                    contentDescription = "یادآور",
                                    tint = NooshPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = stringResource(R.string.next_reminder_label),
                                    fontSize = 13.sp,
                                    color = Color(0xFF64748B)
                                )
                                val nextTime = state?.nextReminder?.scheduledAt
                                Text(
                                    text = if (nextTime != null) {
                                        "ساعت ${DateTimeUtils.formatDate(nextTime)}"
                                    } else {
                                        stringResource(R.string.no_reminders_left)
                                    },
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                            }
                        }

                        // Snooze 15 min button if next reminder exists
                        state?.nextReminder?.let { reminder ->
                            OutlinedButton(
                                onClick = { viewModel.snoozeReminder(reminder.id, 15) },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Text(text = "۱۵ دقیقه بعد", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { viewModel.addWater(250, "app_quick") },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("btn_add_one_glass"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NooshPrimary)
                    ) {
                        Icon(
                            imageVector = Icons.Default.WaterDrop,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.btn_add_water),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedButton(
                        onClick = { showQuickAddSheet = true },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("btn_custom_add"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.btn_custom_add),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Today's Activity Log Heading
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "مصرف‌های امروز",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "${DateTimeUtils.toPersianDigits((state?.recentIntakes?.size ?: 0).toString())} نوبت",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            // Timeline items
            val intakes = state?.recentIntakes ?: emptyList()
            if (intakes.isEmpty()) {
                item {
                    Text(
                        text = "هنوز برای امروز آبی ثبت نشده است. اولین لیوان را بنوشید 💧",
                        fontSize = 13.sp,
                        color = Color(0xFF94A3B8),
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }
            } else {
                items(intakes) { intake ->
                    IntakeTimelineItem(
                        time = DateTimeUtils.formatDate(intake.consumedAt),
                        amountMl = intake.amountMl,
                        source = intake.source
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(90.dp))
            }
        }

        // Quick Add Bottom Sheet
        if (showQuickAddSheet) {
            QuickAddSheet(
                onDismiss = { showQuickAddSheet = false },
                onAddWater = { amount, rescheduleMinutes ->
                    showQuickAddSheet = false
                    viewModel.addWater(
                        amountMl = amount,
                        source = "app_custom",
                        rescheduleMinutes = rescheduleMinutes
                    )
                }
            )
        }

        // Celebration Dialog
        celebrationEvent?.let { (amount, isGoalAchieved) ->
            CelebrationDialog(
                amountMl = amount,
                isGoalAchieved = isGoalAchieved,
                onDismiss = { viewModel.dismissCelebration() }
            )
        }
    }
}

@Composable
private fun IntakeTimelineItem(
    time: String,
    amountMl: Int,
    source: String
) {
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
                .padding(horizontal = 14.dp, vertical = 10.dp),
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
                        imageVector = Icons.Default.WaterDrop,
                        contentDescription = null,
                        tint = NooshPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "+${DateTimeUtils.toPersianDigits(amountMl.toString())} میلی‌لیتر",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    val sourceLabel = when (source) {
                        "notification" -> "از اعلان"
                        "widget" -> "از ویجت"
                        else -> "از اپلیکیشن"
                    }
                    Text(
                        text = sourceLabel,
                        fontSize = 11.sp,
                        color = Color(0xFF64748B)
                    )
                }
            }

            Text(
                text = DateTimeUtils.toPersianDigits(time),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF64748B)
            )
        }
    }
}
