package com.example.workers

import android.content.Context
import android.util.Log
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Helper object to manage and schedule WorkManager-based background reminders
 * to ensure persistent, battery-efficient periodic notifications.
 */
object WaterReminderWorkScheduler {

    private const val TAG = "WaterReminderWorkScheduler"

    /**
     * Schedules periodic water reminder background work.
     * WorkManager supports periodic intervals >= 15 minutes.
     */
    fun schedulePeriodicReminders(
        context: Context,
        intervalMinutes: Int = 60
    ) {
        try {
            val safeInterval = intervalMinutes.coerceAtLeast(15).toLong()
            val flexInterval = (safeInterval / 3).coerceAtLeast(5)

            val periodicWorkRequest = PeriodicWorkRequestBuilder<WaterReminderWorker>(
                safeInterval,
                TimeUnit.MINUTES,
                flexInterval,
                TimeUnit.MINUTES
            )
                .addTag(WaterReminderWorker.TAG_WATER_REMINDER)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WaterReminderWorker.WORK_NAME_PERIODIC,
                ExistingPeriodicWorkPolicy.UPDATE,
                periodicWorkRequest
            )
        } catch (e: Throwable) {
            Log.w(TAG, "WorkManager periodic scheduling skipped or unavailable: ${e.message}")
        }
    }

    /**
     * Schedules a delayed snooze reminder (e.g. 10 minutes) when user clicks snooze on shade.
     */
    fun scheduleSnoozeReminder(
        context: Context,
        delayMinutes: Long = 10
    ) {
        try {
            val snoozeWorkRequest = OneTimeWorkRequestBuilder<WaterReminderWorker>()
                .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
                .addTag(WaterReminderWorker.TAG_WATER_REMINDER)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                WaterReminderWorker.WORK_NAME_SNOOZE,
                ExistingWorkPolicy.REPLACE,
                snoozeWorkRequest
            )
        } catch (e: Throwable) {
            Log.w(TAG, "WorkManager snooze scheduling skipped: ${e.message}")
        }
    }

    /**
     * Immediately triggers a one-time test reminder so the user can verify
     * the notification shade actions right away.
     */
    fun triggerImmediateTestReminder(context: Context) {
        try {
            val testWorkRequest = OneTimeWorkRequestBuilder<WaterReminderWorker>()
                .addTag(WaterReminderWorker.TAG_WATER_REMINDER)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                WaterReminderWorker.WORK_NAME_TEST,
                ExistingWorkPolicy.REPLACE,
                testWorkRequest
            )
        } catch (e: Throwable) {
            Log.w(TAG, "WorkManager test scheduling skipped: ${e.message}")
        }
    }

    /**
     * Cancels all scheduled reminder work.
     */
    fun cancelAllReminders(context: Context) {
        try {
            val workManager = WorkManager.getInstance(context)
            workManager.cancelUniqueWork(WaterReminderWorker.WORK_NAME_PERIODIC)
            workManager.cancelUniqueWork(WaterReminderWorker.WORK_NAME_SNOOZE)
            workManager.cancelAllWorkByTag(WaterReminderWorker.TAG_WATER_REMINDER)
        } catch (e: Throwable) {
            Log.w(TAG, "WorkManager cancellation skipped: ${e.message}")
        }
    }
}

