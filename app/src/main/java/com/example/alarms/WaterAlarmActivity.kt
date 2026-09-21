package com.example.alarms

import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AlarmOff
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocalDrink
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.NooshApplication
import com.example.presentation.components.NooshCharacterView
import com.example.presentation.theme.NooshPrimary
import com.example.presentation.theme.NooshTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Dedicated Fullscreen Alarm Activity displayed when the water alarm triggers.
 * Appears over lockscreen and wakes up the screen with a ringing loop,
 * presenting distinct "Drink Now" and "Stall / Snooze" actions.
 */
class WaterAlarmActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        turnScreenOnAndShowOverKeyguard()

        val reminderId = intent.getStringExtra(EXTRA_REMINDER_ID) ?: ""
        val personName = intent.getStringExtra(EXTRA_PERSON_NAME) ?: "کاربر گرامی"
        val amountMl = intent.getIntExtra(EXTRA_AMOUNT_ML, 250)

        setContent {
            NooshTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF0F172A) // Deep dark night background
                ) {
                    WaterAlarmScreenContent(
                        personName = personName,
                        amountMl = amountMl,
                        onDrinkNow = {
                            handleDrinkNow(reminderId, amountMl)
                        },
                        onStall = {
                            handleStall(reminderId)
                        }
                    )
                }
            }
        }
    }

    private fun handleDrinkNow(reminderId: String, amountMl: Int) {
        WaterAlarmRingingService.stop(this)

        val app = application as? NooshApplication
        if (app != null) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    app.addWaterIntakeUseCase(
                        amountMl = amountMl,
                        source = "alarm_screen",
                        reminderId = reminderId.ifBlank { null }
                    )
                    app.reminderScheduler.scheduleNextPendingReminder()
                    launch(Dispatchers.Main) {
                        Toast.makeText(this@WaterAlarmActivity, "نوش جان! $amountMl میلی‌لیتر آب ثبت شد 💧", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                } catch (e: Exception) {
                    launch(Dispatchers.Main) { finish() }
                }
            }
        } else {
            finish()
        }
    }

    private fun handleStall(reminderId: String) {
        WaterAlarmRingingService.stop(this)

        val app = application as? NooshApplication
        if (app != null) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    if (reminderId.isNotBlank()) {
                        app.reminderRepository.stallReminder(reminderId, reason = "مشغله", delayMinutes = 15)
                    }
                    app.reminderScheduler.scheduleNextPendingReminder()
                    launch(Dispatchers.Main) {
                        Toast.makeText(this@WaterAlarmActivity, "یادآور ۱۵ دقیقه به تعویق افتاد و جبران خواهد شد.", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                } catch (e: Exception) {
                    launch(Dispatchers.Main) { finish() }
                }
            }
        } else {
            finish()
        }
    }

    private fun turnScreenOnAndShowOverKeyguard() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            keyguardManager?.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }
    }

    companion object {
        const val EXTRA_REMINDER_ID = "extra_reminder_id"
        const val EXTRA_PERSON_NAME = "extra_person_name"
        const val EXTRA_AMOUNT_ML = "extra_amount_ml"
    }
}

@Composable
fun WaterAlarmScreenContent(
    personName: String,
    amountMl: Int,
    onDrinkNow: () -> Unit,
    onStall: () -> Unit
) {
    // Subtle pulsating animation for water droplet icon
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp, vertical = 40.dp)
            .testTag("water_alarm_screen"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top status pill
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(30.dp))
                .background(Color(0xFF1E293B))
                .padding(horizontal = 18.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Alarm,
                    contentDescription = null,
                    tint = Color(0xFF38BDF8),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "زنگ هوشمند یادآوری آب",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFE2E8F0)
                )
            }
        }

        // Center visual with character and pulsating droplet
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF0284C7).copy(alpha = 0.5f),
                                Color(0xFF0369A1).copy(alpha = 0.15f),
                                Color.Transparent
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFF38BDF8), NooshPrimary)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalDrink,
                        contentDescription = "آب",
                        tint = Color.White,
                        modifier = Modifier.size(50.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "زمان نوشیدن آب فرا رسید 💧",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "$personName عزیز، بدنتان برای نشاط و سلامت به $amountMl میلی‌لیتر آب تازه نیاز دارد.",
                fontSize = 15.sp,
                color = Color(0xFF94A3B8),
                textAlign = TextAlign.Center,
                lineHeight = 22.sp,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }

        // Bottom Action Buttons: Drink Now & Stall
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Button(
                onClick = onDrinkNow,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .testTag("alarm_drink_now_button"),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF0284C7)
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "همین الان می‌نوشم ($amountMl ml)",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            OutlinedButton(
                onClick = onStall,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("alarm_stall_button"),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFFCBD5E1)
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF475569))
            ) {
                Icon(
                    imageVector = Icons.Default.AlarmOff,
                    contentDescription = null,
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "به تعویق انداختن (۱۵ دقیقه بعد)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFE2E8F0)
                )
            }
        }
    }
}
