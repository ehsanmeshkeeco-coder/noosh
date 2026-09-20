package com.example.presentation.screens

import android.widget.Toast
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
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.core.util.DateTimeUtils
import com.example.domain.model.Reminder
import com.example.domain.model.ReminderStatus
import com.example.presentation.theme.NooshPrimary
import com.example.presentation.theme.NooshSubtleBlue
import com.example.presentation.theme.SuccessGreen
import com.example.presentation.viewmodel.MainViewModel

@Composable
fun RemindersScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val dashboardState by viewModel.dashboardState.collectAsState()
    val remindersList by viewModel.reminders.collectAsState()

    val profile = dashboardState?.profile

    var isEnabled by remember(profile) { mutableStateOf(profile?.reminderEnabled ?: true) }
    var selectedInterval by remember(profile) { mutableIntStateOf(profile?.reminderIntervalMinutes ?: 60) }
    var startTime by remember(profile) { mutableStateOf(profile?.wakeUpTime ?: "08:00") }
    var endTime by remember(profile) { mutableStateOf(profile?.sleepTime ?: "23:00") }
    var soundEnabled by remember(profile) { mutableStateOf(profile?.soundEnabled ?: true) }
    var vibrateEnabled by remember(profile) { mutableStateOf(profile?.vibrateEnabled ?: true) }
    var inactivityMinutes by remember(profile) { mutableStateOf(profile?.inactivityThresholdMinutes ?: 120) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("reminders_screen"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))

            // Screen Title
            Text(
                text = stringResource(R.string.settings_title),
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "تنظیم بازه‌های زمانی هوشمند یادآوری آب",
                fontSize = 13.sp,
                color = Color(0xFF64748B),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp)
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Master Toggle Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
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
                                .background(if (isEnabled) NooshSubtleBlue else Color(0xFFF1F5F9)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Alarm,
                                contentDescription = null,
                                tint = if (isEnabled) NooshPrimary else Color(0xFF94A3B8),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.reminders_toggle),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = if (isEnabled) "یادآورها فعال هستند" else "یادآورها موقتاً غیرفعالند",
                                fontSize = 12.sp,
                                color = if (isEnabled) SuccessGreen else Color(0xFF94A3B8)
                            )
                        }
                    }

                    Switch(
                        checked = isEnabled,
                        onCheckedChange = { checked ->
                            isEnabled = checked
                            viewModel.updateReminderSettings(checked, selectedInterval, startTime, endTime)
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = NooshPrimary)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Intervals selection
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.reminder_interval),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(30, 45, 60, 90, 120).forEach { mins ->
                            val isSelected = selectedInterval == mins
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedInterval = mins
                                    viewModel.updateReminderSettings(isEnabled, mins, startTime, endTime)
                                },
                                label = { Text(text = "${DateTimeUtils.toPersianDigits(mins.toString())} دقیقه", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = NooshPrimary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Start and End times
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.reminder_start_time),
                                fontSize = 12.sp,
                                color = Color(0xFF64748B)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "ساعت ${DateTimeUtils.toPersianDigits(startTime)}",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.reminder_end_time),
                                fontSize = 12.sp,
                                color = Color(0xFF64748B)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "ساعت ${DateTimeUtils.toPersianDigits(endTime)}",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Inactivity Threshold Card (Section 56)
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "تشخیص عدم فعالیت طولانی",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "اگر در زمان بیداری بیش از این زمان آبی مصرف نشود، هشدار عدم فعالیت برای همراه ارسال می‌شود.",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(90, 120, 180, 240).forEach { mins ->
                            val isSelected = inactivityMinutes == mins
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    inactivityMinutes = mins
                                    profile?.let {
                                        viewModel.updateProfile(it.copy(inactivityThresholdMinutes = mins))
                                    }
                                },
                                label = {
                                    Text(
                                        text = "${DateTimeUtils.toPersianDigits((mins / 60.0).toString().removeSuffix(".0"))} ساعت",
                                        fontSize = 12.sp
                                    )
                                },
                                modifier = Modifier.weight(1f),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = NooshPrimary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Sound & Vibration Preferences
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.VolumeUp, contentDescription = null, tint = NooshPrimary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(text = stringResource(R.string.sound_toggle), fontSize = 14.sp)
                        }
                        Switch(
                            checked = soundEnabled,
                            onCheckedChange = { checked ->
                                soundEnabled = checked
                                profile?.let { viewModel.updateProfile(it.copy(soundEnabled = checked)) }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Vibration, contentDescription = null, tint = NooshPrimary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(text = stringResource(R.string.vibration_toggle), fontSize = 14.sp)
                        }
                        Switch(
                            checked = vibrateEnabled,
                            onCheckedChange = { checked ->
                                vibrateEnabled = checked
                                profile?.let { viewModel.updateProfile(it.copy(vibrateEnabled = checked)) }
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Test Notification Trigger Button
            OutlinedButton(
                onClick = {
                    viewModel.triggerTestReminder(context)
                    Toast.makeText(context, "اعلان تست ارسال شد 🔔", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("btn_test_notification"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = null,
                    tint = NooshPrimary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "ارسال اعلان تست جهت بررسی دکمه «یک لیوان آب خوردم»",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = NooshPrimary
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "برنامه یادآورهای امروز",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))
        }

        // Reminders list
        if (remindersList.isEmpty()) {
            item {
                Text(
                    text = "برنامه یادآورهای امروز آماده است. در زمان‌های مشخص اعلان دریافت خواهید کرد 💧",
                    fontSize = 13.sp,
                    color = Color(0xFF64748B),
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            }
        } else {
            items(remindersList) { reminder ->
                ReminderItemCard(reminder = reminder)
            }
        }

        item {
            Spacer(modifier = Modifier.height(90.dp))
        }
    }
}

@Composable
private fun ReminderItemCard(reminder: Reminder) {
    val isCompleted = reminder.status == ReminderStatus.COMPLETED
    val isNotified = reminder.status == ReminderStatus.NOTIFIED
    val isSnoozed = reminder.status == ReminderStatus.SNOOZED

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
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = if (isCompleted) SuccessGreen else NooshPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "ساعت ${DateTimeUtils.formatDate(reminder.scheduledAt)}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = "${DateTimeUtils.toPersianDigits(reminder.amountMl.toString())} میلی‌لیتر (یک لیوان)",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )
                }
            }

            val statusText = when {
                isCompleted -> "نوشیده شد ✓"
                isNotified -> "اعلان شده 🔔"
                isSnoozed -> "به تعویق افتاده"
                else -> "در انتظار"
            }
            val statusColor = when {
                isCompleted -> SuccessGreen
                isNotified -> NooshPrimary
                isSnoozed -> Color(0xFFF59E0B)
                else -> Color(0xFF94A3B8)
            }

            Text(
                text = statusText,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = statusColor
            )
        }
    }
}
