package com.example.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.core.util.DateTimeUtils
import com.example.presentation.components.CelebrationDialog
import com.example.presentation.components.CircularWaterProgress
import com.example.presentation.components.GamificationDialog
import com.example.presentation.components.GlassRowIndicator
import com.example.presentation.components.LevelProgressCard
import com.example.presentation.components.LevelUpDialog
import com.example.presentation.components.QuickAddSheet
import com.example.presentation.theme.FlameOrange
import com.example.presentation.theme.NooshPrimary
import com.example.presentation.theme.NooshSubtleBlue
import com.example.presentation.viewmodel.MainViewModel

@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    onProgressRingPositioned: ((Rect) -> Unit)? = null,
    onQuickAddPositioned: ((Rect) -> Unit)? = null
) {
    val dashboardState by viewModel.dashboardState.collectAsState()
    val celebrationEvent by viewModel.celebrationEvent.collectAsState()
    val badges by viewModel.badges.collectAsState()
    val levelUpEvent by viewModel.levelUpEvent.collectAsState()
    val xpToastEvent by viewModel.xpToastEvent.collectAsState()

    var showQuickAddSheet by remember { mutableStateOf(false) }
    var showGamificationDialog by remember { mutableStateOf(false) }

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

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Theme Switcher Button (Dark / Light Mode)
                        val themeMode = state?.profile?.themeMode ?: "system"
                        val isSystemDark = isSystemInDarkTheme()
                        val isDarkActive = when (themeMode) {
                            "dark" -> true
                            "light" -> false
                            else -> isSystemDark
                        }

                        Surface(
                            onClick = { viewModel.toggleThemeMode() },
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier.testTag("btn_theme_toggle")
                        ) {
                            Box(
                                modifier = Modifier.size(38.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isDarkActive) Icons.Default.DarkMode else Icons.Default.LightMode,
                                    contentDescription = if (isDarkActive) "تغییر به حالت روز (روشن)" else "تغییر به حالت شب (تاریک)",
                                    tint = if (isDarkActive) Color(0xFFFBBF24) else Color(0xFFF59E0B),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
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
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Gamification Level Progress Card
                state?.levelInfo?.let { levelInfo ->
                    LevelProgressCard(
                        levelInfo = levelInfo,
                        onViewBadgesClick = { showGamificationDialog = true }
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Main Circular Water Progress with wave
                Box(
                    modifier = Modifier.onGloballyPositioned { coordinates ->
                        onProgressRingPositioned?.invoke(coordinates.boundsInRoot())
                    }
                ) {
                    CircularWaterProgress(
                        percentage = state?.percentage ?: 0,
                        consumedMl = state?.totalConsumedMl ?: 0,
                        goalMl = state?.goalMl ?: 2000,
                        size = 230.dp
                    )
                }

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
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                    ),
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
                                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Alarm,
                                    contentDescription = "یادآور",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = stringResource(R.string.next_reminder_label),
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                    color = MaterialTheme.colorScheme.onSurface
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .onGloballyPositioned { coordinates ->
                            onQuickAddPositioned?.invoke(coordinates.boundsInRoot())
                        },
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("today_intakes_header"),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "تاریخچه مصرف‌های امروز",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Text(
                        text = "${DateTimeUtils.toPersianDigits((state?.recentIntakes?.size ?: 0).toString())} نوبت ثبت شده",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
            }

            // Timeline items list
            val intakes = state?.recentIntakes ?: emptyList()
            if (intakes.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                            .testTag("empty_intakes_view")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = "💧", fontSize = 28.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "هنوز برای امروز آبی ثبت نشده است",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "با نوشیدن اولین لیوان آب، روزتان را با نشاط آغاز کنید",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(intakes, key = { it.id }) { intake ->
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

        // Gamification Details Dialog (Badges & Level Roadmap)
        if (showGamificationDialog && state != null) {
            GamificationDialog(
                levelInfo = state.levelInfo,
                badges = badges,
                onDismiss = { showGamificationDialog = false }
            )
        }

        // Level-Up Celebration Dialog
        levelUpEvent?.let { (newLevel, levelTitle, levelEmoji) ->
            LevelUpDialog(
                newLevel = newLevel,
                levelTitle = levelTitle,
                levelEmoji = levelEmoji,
                onDismiss = { viewModel.dismissLevelUpDialog() }
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
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .testTag("intake_timeline_item")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.WaterDrop,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Column {
                    Text(
                        text = "+${DateTimeUtils.toPersianDigits(amountMl.toString())} میلی‌لیتر",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    val sourceLabel = when (source) {
                        "notification" -> "از نوار اعلان"
                        "widget" -> "از ویجت صفحه اصلی"
                        "app_custom" -> "ثبت سفارشی"
                        else -> "ثبت سریع برنامه"
                    }
                    Text(
                        text = sourceLabel,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
            ) {
                Text(
                    text = "ساعت ${DateTimeUtils.toPersianDigits(time)}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}
