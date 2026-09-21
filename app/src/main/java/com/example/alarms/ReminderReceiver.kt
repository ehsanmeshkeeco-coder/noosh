package com.example.alarms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.NooshApplication
import com.example.domain.model.ReminderStatus
import com.example.notifications.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_REMINDER_TRIGGERED) {
            val pendingResult = goAsync()
            val reminderId = intent.getStringExtra(EXTRA_REMINDER_ID) ?: ""
            val amountMl = intent.getIntExtra(EXTRA_AMOUNT_ML, 250)
            val isRetry = intent.getBooleanExtra(EXTRA_IS_RETRY, false)

            Log.d(TAG, "Reminder triggered: $reminderId, isRetry: $isRetry")

            val app = context.applicationContext as? NooshApplication
            if (app != null) {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val profile = app.userRepository.getUserProfile()
                        if (!profile.reminderEnabled) {
                            return@launch
                        }

                        // If not retry, record REMINDER_TRIGGERED and schedule 15-minute retry check
                        if (!isRetry) {
                            // First reminder: Normal notification with sound and vibration
                            NotificationHelper.showWaterReminderNotification(
                                context = context,
                                personName = profile.name,
                                reminderId = reminderId,
                                amountMl = amountMl,
                                soundEnabled = profile.soundEnabled,
                                vibrateEnabled = profile.vibrateEnabled
                            )

                            // Update status to NOTIFIED
                            app.reminderRepository.updateReminderStatus(
                                id = reminderId,
                                status = ReminderStatus.NOTIFIED,
                                completedAt = null
                            )

                            app.healthCompanionManager.recordAndDispatchEvent(
                                eventType = com.example.domain.model.HealthEventType.REMINDER_TRIGGERED,
                                severity = com.example.domain.model.AlertSeverity.LOW
                            )
                            app.reminderScheduler.scheduleRetryAlarm(reminderId, retryMinutes = 15)
                        } else {
                            // 15 minutes passed: Check if user hasn't consumed water yet
                            val currentReminder = app.reminderRepository.getReminderById(reminderId)
                            if (currentReminder != null && (currentReminder.status == ReminderStatus.NOTIFIED || currentReminder.status == ReminderStatus.PENDING)) {
                                // Start active ringing loop (audio + vibration) and show full screen alarm UI
                                WaterAlarmRingingService.start(
                                    context = context,
                                    reminderId = reminderId,
                                    personName = profile.name,
                                    amountMl = amountMl
                                )

                                app.healthCompanionManager.recordAndDispatchEvent(
                                    eventType = com.example.domain.model.HealthEventType.REMINDER_MISSED,
                                    severity = com.example.domain.model.AlertSeverity.MEDIUM,
                                    missedReminderCount = 1
                                )
                                // Check if user has long inactivity
                                app.healthCompanionManager.checkAndTriggerInactivity()
                            }
                            // Schedule next normal pending reminder
                            app.reminderScheduler.scheduleNextPendingReminder()
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error handling reminder alarm: ${e.message}")
                    } finally {
                        pendingResult.finish()
                    }
                }
            } else {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_REMINDER_TRIGGERED = "com.example.action.REMINDER_TRIGGERED"
        const val EXTRA_REMINDER_ID = "extra_reminder_id"
        const val EXTRA_AMOUNT_ML = "extra_amount_ml"
        const val EXTRA_IS_RETRY = "extra_is_retry"
        private const val TAG = "ReminderReceiver"
    }
}
