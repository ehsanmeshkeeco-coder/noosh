package com.example.alarms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.NooshApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == Intent.ACTION_TIME_CHANGED ||
            action == Intent.ACTION_TIMEZONE_CHANGED
        ) {
            val pendingResult = goAsync()
            val app = context.applicationContext as? NooshApplication
            if (app != null) {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val profile = app.userRepository.getUserProfile()
                        if (profile.reminderEnabled) {
                            app.reminderRepository.scheduleDailyReminders(profile)
                            app.reminderScheduler.scheduleNextPendingReminder()
                        }
                    } finally {
                        pendingResult.finish()
                    }
                }
            } else {
                pendingResult.finish()
            }
        }
    }
}
