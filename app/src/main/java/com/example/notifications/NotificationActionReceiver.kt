package com.example.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.example.NooshApplication
import com.example.R
import com.example.widgets.WaterProgressWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == NotificationHelper.ACTION_DRANK_WATER) {
            val pendingResult = goAsync()
            val reminderId = intent.getStringExtra(NotificationHelper.EXTRA_REMINDER_ID)
            val amountMl = intent.getIntExtra(NotificationHelper.EXTRA_AMOUNT_ML, 250)

            // Dismiss notification immediately
            NotificationHelper.cancelNotification(context)

            val app = context.applicationContext as? NooshApplication
            if (app != null) {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        app.addWaterIntakeUseCase(
                            amountMl = amountMl,
                            source = "notification",
                            reminderId = reminderId
                        )
                        
                        // Update widgets
                        WaterProgressWidgetProvider.updateAllWidgets(context)

                        // Reschedule next reminders
                        app.reminderScheduler.scheduleNextPendingReminder()

                        launch(Dispatchers.Main) {
                            Toast.makeText(
                                context,
                                context.getString(R.string.congratulations_subtitle),
                                Toast.LENGTH_SHORT
                            ).show()
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
