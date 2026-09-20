package com.example.fcm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.NooshApplication
import com.example.R
import com.example.domain.model.AlertSeverity
import com.example.domain.model.HealthEventType
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NooshFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "FCM onNewToken received: ${token.take(10)}...")
        val app = applicationContext as? NooshApplication
        app?.fcmTokenManager?.onNewToken(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "FCM Message received from: ${remoteMessage.from}")

        val data = remoteMessage.data
        if (data.isNotEmpty()) {
            val eventTypeStr = data["eventType"] ?: return
            val eventId = data["eventId"] ?: ""
            val severityStr = data["severity"] ?: "LOW"
            val currentWaterMl = data["currentWaterMl"]?.toIntOrNull() ?: 0
            val dailyGoalMl = data["dailyGoalMl"]?.toIntOrNull() ?: 2000
            val goalPercentage = data["goalPercentage"]?.toIntOrNull() ?: 0

            val app = applicationContext as? NooshApplication
            if (app != null) {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        // Section 54: Acknowledge event to avoid duplicates
                        if (eventId.isNotBlank()) {
                            app.healthCompanionManager.acknowledgeEvent(eventId)
                        }

                        // Check Companion Alert Notification Policy
                        val severity = try { AlertSeverity.valueOf(severityStr) } catch (e: Exception) { AlertSeverity.LOW }
                        val eventType = try { HealthEventType.valueOf(eventTypeStr) } catch (e: Exception) { HealthEventType.WATER_CONSUMED }

                        // Display companion notification for MEDIUM and HIGH severity alerts
                        if (severity != AlertSeverity.LOW) {
                            showCompanionAlertNotification(
                                context = this@NooshFirebaseMessagingService,
                                eventType = eventType,
                                severity = severity,
                                currentWaterMl = currentWaterMl,
                                dailyGoalMl = dailyGoalMl,
                                percentage = goalPercentage
                            )
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error handling FCM message: ${e.message}")
                    }
                }
            }
        }
    }

    private fun showCompanionAlertNotification(
        context: Context,
        eventType: HealthEventType,
        severity: AlertSeverity,
        currentWaterMl: Int,
        dailyGoalMl: Int,
        percentage: Int
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "noosh_companion_alerts"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "هشدارهای همراه سلامت (Companion Alerts)",
                if (severity == AlertSeverity.HIGH) NotificationManager.IMPORTANCE_HIGH else NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "اعلان‌های وضعیت هیدراتاسیون و هشدارهای عدم فعالیت"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val title = when (eventType) {
            HealthEventType.LONG_INACTIVITY -> "⚠️ هشدار عدم فعالیت طولانی"
            HealthEventType.REMINDER_MISSED -> "یادآوری فراموش‌شده آب"
            HealthEventType.DAILY_GOAL_MISSED -> "هدف روزانه تکمیل نشد"
            HealthEventType.GOAL_REACHED -> "🎉 هدف روزانه محقق شد!"
            else -> "اطلاعیه همراه سلامت"
        }

        val message = when (eventType) {
            HealthEventType.LONG_INACTIVITY -> "مدت زیادی است آبی ثبت نشده است ($currentWaterMl از $dailyGoalMl ml). لطفاً یادآوری کنید."
            HealthEventType.REMINDER_MISSED -> "کاربر یادآور اخیر را پاسخ نداده است ($percentage٪ از هدف امروز)."
            HealthEventType.GOAL_REACHED -> "کاربر با موفقیت به هدف $dailyGoalMl میلی‌لیتری امروز رسید!"
            else -> "وضعیت فعلی آب: $currentWaterMl ml"
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_noosh_logo)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(if (severity == AlertSeverity.HIGH) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify((System.currentTimeMillis() % 10000).toInt(), notification)
    }

    companion object {
        private const val TAG = "NooshFcmService"
    }
}
