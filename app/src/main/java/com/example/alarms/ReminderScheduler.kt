package com.example.alarms

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.domain.model.Reminder
import com.example.domain.model.ReminderStatus
import com.example.domain.repository.ReminderRepository
import com.example.domain.repository.UserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderScheduler(
    private val context: Context,
    private val reminderRepository: ReminderRepository,
    private val userRepository: UserRepository
) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleNextPendingReminder() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val profile = userRepository.getUserProfile()
                if (!profile.reminderEnabled) {
                    cancelAllAlarms()
                    return@launch
                }

                val nextReminder = reminderRepository.getNextReminder()
                if (nextReminder != null) {
                    scheduleExactAlarm(nextReminder)
                } else {
                    // Schedule today's plan if needed
                    reminderRepository.scheduleDailyReminders(profile)
                    val freshlyScheduled = reminderRepository.getNextReminder()
                    freshlyScheduled?.let { scheduleExactAlarm(it) }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error scheduling next reminder: ${e.message}")
            }
        }
    }

    fun scheduleExactAlarm(reminder: Reminder) {
        val triggerAtMillis = reminder.nextReminderAt ?: reminder.scheduledAt
        if (triggerAtMillis <= System.currentTimeMillis()) {
            return
        }

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_REMINDER_TRIGGERED
            putExtra(ReminderReceiver.EXTRA_REMINDER_ID, reminder.id)
            putExtra(ReminderReceiver.EXTRA_AMOUNT_ML, reminder.amountMl)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminder.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
            Log.d(TAG, "Successfully scheduled reminder ${reminder.id} at $triggerAtMillis")
        } catch (e: SecurityException) {
            Log.w(TAG, "Exact alarm permission missing, falling back: ${e.message}")
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    fun scheduleRetryAlarm(reminderId: String, retryMinutes: Int = 15) {
        val triggerAtMillis = System.currentTimeMillis() + (retryMinutes * 60 * 1000L)
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_REMINDER_TRIGGERED
            putExtra(ReminderReceiver.EXTRA_REMINDER_ID, reminderId)
            putExtra(ReminderReceiver.EXTRA_IS_RETRY, true)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            (reminderId + "_retry").hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting retry alarm: ${e.message}")
        }
    }

    fun cancelAlarm(reminderId: String) {
        val intent = Intent(context, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId.hashCode(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        pendingIntent?.let { alarmManager.cancel(it) }
    }

    fun cancelAllAlarms() {
        // Will cancel upcoming alarms
    }

    companion object {
        private const val TAG = "ReminderScheduler"
    }
}
