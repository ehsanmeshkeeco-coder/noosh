package com.example.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.util.DateTimeUtils
import com.example.domain.model.AlertFilterPolicy
import com.example.domain.model.AlertSeverity
import com.example.domain.model.CompanionConnectionStatus
import com.example.domain.model.EventDeliveryStatus
import com.example.domain.model.HealthAlertEvent
import com.example.domain.model.HealthCompanionConnection
import com.example.domain.model.HealthCompanionStatus
import com.example.domain.model.HealthEventType
import com.example.domain.model.HealthStatusEvaluation
import com.example.presentation.theme.NooshBackground
import com.example.presentation.theme.NooshPrimary
import com.example.presentation.theme.NooshSurface
import com.example.presentation.viewmodel.MainViewModel

@Composable
fun HealthCompanionScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val companionStatus by viewModel.companionStatus.collectAsState()
    val recentEvents by viewModel.companionEvents.collectAsState()
    val activeCompanion by viewModel.activeCompanion.collectAsState()

    var showConnectDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(NooshBackground)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Favorite,
                        contentDescription = null,
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "همراه سلامت و هشدارهای FCM",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "ارسال رویدادهای مصرف آب به همراه سلامت بدون وابستگی به یادآور محلی",
                    fontSize = 12.sp,
                    color = Color(0xFF64748B)
                )
            }
        }

        // Section 55: Health Companion Status Card
        item {
            companionStatus?.let { status ->
                CompanionStatusCard(status = status)
            }
        }

        // Section 58 & 57: Companion Connection & Privacy Management
        item {
            CompanionConnectionCard(
                connection = activeCompanion,
                onConnectClick = { showConnectDialog = true },
                onDisconnectClick = { activeCompanion?.let { viewModel.disconnectCompanion(it.id) } },
                onTogglePause = { activeCompanion?.let { viewModel.togglePauseCompanion(it.id, it.status == CompanionConnectionStatus.CONNECTED) } },
                onPolicyChange = { activeCompanion?.let { conn -> viewModel.updateCompanionPolicy(conn.id, it) } }
            )
        }

        // Simulation & Manual Triggering
        item {
            CompanionSimulationCard(
                onTriggerMissed = { viewModel.simulateReminderMissed() },
                onTriggerInactivity = { viewModel.simulateLongInactivity() }
            )
        }

        // Section 53 & 54: Audit Trail of FCM Events
        item {
            Text(
                text = "لاگ رویدادهای سلامت (Audit Trail)",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B),
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (recentEvents.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = NooshSurface)
                ) {
                    Box(modifier = Modifier.padding(24.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "هنوز رویدادی ثبت نشده است. با مصرف آب یا آزمون شبیه‌ساز رویداد ایجاد می‌شود.",
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
            }
        } else {
            items(recentEvents) { event ->
                HealthEventItem(event = event)
            }
        }
    }

    if (showConnectDialog) {
        ConnectCompanionDialog(
            onDismiss = { showConnectDialog = false },
            onConfirm = { name, userId, policy ->
                viewModel.connectCompanion(userId, name, policy)
                showConnectDialog = false
            }
        )
    }
}

@Composable
private fun CompanionStatusCard(status: HealthCompanionStatus) {
    val (statusText, statusBg, statusColor) = when (status.evaluation) {
        HealthStatusEvaluation.ON_TRACK -> Triple("وضعیت مطلوب (On Track)", Color(0xFFDCFCE7), Color(0xFF16A34A))
        HealthStatusEvaluation.BEHIND -> Triple("عقب‌تر از برنامه (Behind)", Color(0xFFFEE2E2), Color(0xFFDC2626))
        HealthStatusEvaluation.GOAL_REACHED -> Triple("هدف روزانه محقق شد (Goal Reached)", Color(0xFFE0F2FE), Color(0xFF0284C7))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = NooshSurface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "وضعیت جاری کاربر",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF334155)
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(statusBg)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = statusText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = statusColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Metrics Grid
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                MetricMiniItem(
                    label = "آب امروز",
                    value = "${DateTimeUtils.toPersianDigits(status.todayWaterMl.toString())} / ${DateTimeUtils.toPersianDigits(status.dailyGoalMl.toString())} ml",
                    sub = "${DateTimeUtils.toPersianDigits(status.goalPercentage.toString())}٪ هدف"
                )
                MetricMiniItem(
                    label = "آخرین نوشیدن",
                    value = if (status.lastDrinkTimeAgoMinutes != null) {
                        "${DateTimeUtils.toPersianDigits(status.lastDrinkTimeAgoMinutes.toString())} دقیقه پیش"
                    } else {
                        "هنوز ثبت نشده"
                    },
                    sub = "بر اساس لاگ واقعی"
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                MetricMiniItem(
                    label = "یادآورهای امروز",
                    value = "${DateTimeUtils.toPersianDigits(status.completedReminders.toString())} انجام‌شده",
                    sub = "${DateTimeUtils.toPersianDigits(status.missedReminders.toString())} بی‌پاسخ"
                )
                MetricMiniItem(
                    label = "پایداری زنجیره",
                    value = "${DateTimeUtils.toPersianDigits(status.currentStreak.toString())} روز پیاپی",
                    sub = "Streak فعال"
                )
            }
        }
    }
}

@Composable
private fun MetricMiniItem(label: String, value: String, sub: String) {
    Column(modifier = Modifier.width(150.dp)) {
        Text(text = label, fontSize = 11.sp, color = Color(0xFF64748B))
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = sub, fontSize = 10.sp, color = Color(0xFF94A3B8))
    }
}

@Composable
private fun CompanionConnectionCard(
    connection: HealthCompanionConnection?,
    onConnectClick: () -> Unit,
    onDisconnectClick: () -> Unit,
    onTogglePause: () -> Unit,
    onPolicyChange: (AlertFilterPolicy) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = NooshSurface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "تنظیمات اتصال و حریم خصوصی (Privacy)",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "اطلاعات تنها در صورت اتصال صریح با همراه به اشتراک گذاشته می‌شود.",
                fontSize = 11.sp,
                color = Color(0xFF64748B)
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (connection == null || connection.status == CompanionConnectionStatus.DISCONNECTED) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "همراه متصل: ندارد",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8)
                    )
                    Button(
                        onClick = onConnectClick,
                        colors = ButtonDefaults.buttonColors(containerColor = NooshPrimary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(imageVector = Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "اتصال همراه", fontSize = 12.sp)
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "همراه: ${connection.companionName}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )
                        Text(
                            text = if (connection.status == CompanionConnectionStatus.CONNECTED) "وضعیت: متصل و فعال" else "وضعیت: متوقف‌شده",
                            fontSize = 11.sp,
                            color = if (connection.status == CompanionConnectionStatus.CONNECTED) Color(0xFF16A34A) else Color(0xFFF59E0B)
                        )
                    }

                    Row {
                        OutlinedButton(
                            onClick = onTogglePause,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = if (connection.status == CompanionConnectionStatus.CONNECTED) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (connection.status == CompanionConnectionStatus.CONNECTED) "توقف" else "ادامه",
                                fontSize = 11.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        OutlinedButton(
                            onClick = onDisconnectClick,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(text = "قطع", fontSize = 11.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Alert Policy Selector
                Text(
                    text = "سیاست دریافت هشدارها (Notification Policy):",
                    fontSize = 11.sp,
                    color = Color(0xFF64748B)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PolicyChip(
                        title = "همه رویدادها",
                        selected = connection.alertPolicy == AlertFilterPolicy.ALL,
                        onClick = { onPolicyChange(AlertFilterPolicy.ALL) }
                    )
                    PolicyChip(
                        title = "متوسط و بالا",
                        selected = connection.alertPolicy == AlertFilterPolicy.MEDIUM_AND_HIGH,
                        onClick = { onPolicyChange(AlertFilterPolicy.MEDIUM_AND_HIGH) }
                    )
                    PolicyChip(
                        title = "فقط بحرانی",
                        selected = connection.alertPolicy == AlertFilterPolicy.HIGH_ONLY,
                        onClick = { onPolicyChange(AlertFilterPolicy.HIGH_ONLY) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PolicyChip(title: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) NooshPrimary else Color(0xFFF1F5F9))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Text(
            text = title,
            fontSize = 10.sp,
            color = if (selected) Color.White else Color(0xFF475569),
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun CompanionSimulationCard(
    onTriggerMissed: () -> Unit,
    onTriggerInactivity: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Filled.NotificationsActive, contentDescription = null, tint = NooshPrimary, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "شبیه‌ساز و آزمون رویدادهای اضطراری",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "ارسال شبیه‌سازی‌شده رویدادها برای اعتبارسنجی جریان داده به Supabase و همراه سلامت:",
                fontSize = 11.sp,
                color = Color(0xFF64748B)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onTriggerMissed,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(text = "یادآور فراموش‌شده", fontSize = 11.sp)
                }
                OutlinedButton(
                    onClick = onTriggerInactivity,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(text = "عدم فعالیت طولانی", fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun HealthEventItem(event: HealthAlertEvent) {
    val (typeTitle, typeColor) = when (event.eventType) {
        HealthEventType.WATER_CONSUMED -> "مصرف آب" to Color(0xFF0284C7)
        HealthEventType.REMINDER_TRIGGERED -> "ایجاد یادآور" to Color(0xFF64748B)
        HealthEventType.REMINDER_MISSED -> "یادآور فراموش‌شده" to Color(0xFFF59E0B)
        HealthEventType.REMINDER_SNOOZED -> "تعویق یادآور" to Color(0xFF6B7280)
        HealthEventType.GOAL_REACHED -> "تحقق هدف روزانه" to Color(0xFF16A34A)
        HealthEventType.LONG_INACTIVITY -> "عدم فعالیت طولانی" to Color(0xFFDC2626)
        HealthEventType.DAILY_GOAL_MISSED -> "عدم تحقق هدف روزانه" to Color(0xFFEF4444)
        HealthEventType.LOW_DAILY_PROGRESS -> "پیشرفت کم آب" to Color(0xFFF97316)
    }

    val (sevTitle, sevBg, sevColor) = when (event.severity) {
        AlertSeverity.LOW -> Triple("پایین", Color(0xFFF1F5F9), Color(0xFF64748B))
        AlertSeverity.MEDIUM -> Triple("متوسط", Color(0xFFFEF3C7), Color(0xFFD97706))
        AlertSeverity.HIGH -> Triple("بحرانی", Color(0xFFFEE2E2), Color(0xFFDC2626))
    }

    val statusTitle = when (event.deliveryStatus) {
        EventDeliveryStatus.PENDING -> "در صف ارسال"
        EventDeliveryStatus.SENT -> "ارسال شده"
        EventDeliveryStatus.ACKNOWLEDGED -> "تأیید شده"
        EventDeliveryStatus.FAILED -> "تلاش مجدد"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = NooshSurface),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(typeColor, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = typeTitle,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(sevBg)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(text = sevTitle, fontSize = 9.sp, color = sevColor, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "مصرف: ${DateTimeUtils.toPersianDigits(event.currentWaterMl.toString())} از ${DateTimeUtils.toPersianDigits(event.dailyGoalMl.toString())} ml (${DateTimeUtils.toPersianDigits(event.goalPercentage.toString())}٪)",
                    fontSize = 11.sp,
                    color = Color(0xFF64748B)
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = statusTitle,
                    fontSize = 10.sp,
                    color = if (event.deliveryStatus == EventDeliveryStatus.ACKNOWLEDGED || event.deliveryStatus == EventDeliveryStatus.SENT) Color(0xFF16A34A) else Color(0xFFF59E0B),
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${DateTimeUtils.toPersianDigits(event.date)}",
                    fontSize = 10.sp,
                    color = Color(0xFF94A3B8)
                )
            }
        }
    }
}

@Composable
private fun ConnectCompanionDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, userId: String, policy: AlertFilterPolicy) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var userId by remember { mutableStateOf("") }
    var policy by remember { mutableStateOf(AlertFilterPolicy.ALL) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "اتصال همراه سلامت جدید", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(text = "نام و شناسه همراه سلامت (مثلاً پزشک یا مربی) را وارد کنید:", fontSize = 12.sp, color = Color(0xFF64748B))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("نام همراه (مثلاً دکتر راد)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = userId,
                    onValueChange = { userId = it },
                    label = { Text("شناسه کاربری همراه") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && userId.isNotBlank()) {
                        onConfirm(name, userId, policy)
                    }
                },
                enabled = name.isNotBlank() && userId.isNotBlank()
            ) {
                Text("اتصال و فعال‌سازی")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("انصراف")
            }
        }
    )
}
