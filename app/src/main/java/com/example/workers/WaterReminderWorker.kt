package com.example.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.NooshApplication
import com.example.notifications.NotificationHelper
import java.util.Calendar

/**
 * WorkManager-based CoroutineWorker that runs periodically in the background
 * to remind the user to drink water with a soft, non-intrusive notification.
 * Provides interactive shade actions directly executable without opening the app.
 */
class WaterReminderWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val WORK_NAME_PERIODIC = "noosh_periodic_water_reminder_work"
        const val WORK_NAME_SNOOZE = "noosh_snooze_water_reminder_work"
        const val WORK_NAME_TEST = "noosh_test_water_reminder_work"
        const val TAG_WATER_REMINDER = "tag_water_reminder"
    }

    override suspend fun doWork(): Result {
        val app = context.applicationContext as? NooshApplication ?: return Result.success()

        if (isStopped) {
            return Result.retry()
        }

        try {
            val profile = app.userRepository.getUserProfile()

            // 1. Check if reminders are enabled
            if (!profile.reminderEnabled) {
                return Result.success()
            }

            // 2. Check quiet hours (sleep time)
            val wakeUp = profile.wakeUpTime.ifBlank { "08:00" }
            val sleep = profile.sleepTime.ifBlank { "23:00" }
            if (isQuietHours(wakeUp, sleep)) {
                return Result.success()
            }

            // 3. Check if user already drank water very recently (e.g., within 20 mins)
            val todayIntakes = app.waterRepository.getTodayWaterIntakes()
            val twentyMinutesAgo = System.currentTimeMillis() - (20 * 60 * 1000)
            val recentlyDrank = todayIntakes.any { it.consumedAt > twentyMinutesAgo }
            if (recentlyDrank) {
                return Result.success()
            }

            // 4. Construct personalized, soft reminder
            val userName = profile.name.takeIf {
                it.isNotBlank() && it != "کاربر مهمان" && it != "کاربر نوش" && it != "کاربر گرامی"
            } ?: "دوست من"

            val softMessages = listOf(
                "یک جرعه سلامتی! زمان هیدراته شدن بدن شما فرا رسیده 🌱",
                "نوشیدن یک لیوان آب گوارا به شادابی ذهن و بدنتان کمک می‌کند 💧",
                "دوست من، نوشیدن منظم آب باعث درخشش پوست و رفع خستگی می‌شود ✨",
                "جرعه‌ای آب خنک بنوشید و روزتان را با طراوت و انرژی ادامه دهید 🌊",
                "کلیه‌ها و عضلات شما برای بازدهی عالی هم‌اکنون به آب نیاز دارند 🧠"
            )
            val message = softMessages.random()

            if (isStopped) {
                return Result.retry()
            }

            // 5. Trigger interactive soft notification with shade actions
            NotificationHelper.showSoftWaterReminderNotification(
                context = context,
                personName = userName,
                customBody = "$userName عزیز، $message",
                reminderId = "wm_reminder_${System.currentTimeMillis()}",
                amountMl = 250,
                soundEnabled = profile.soundEnabled,
                vibrateEnabled = profile.vibrateEnabled
            )

            return Result.success()
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            return Result.failure()
        }
    }

    private fun isQuietHours(wakeUpTime: String, sleepTime: String): Boolean {
        return try {
            val now = Calendar.getInstance()
            val currentMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)

            val wakeParts = wakeUpTime.split(":").map { it.trim().toInt() }
            val sleepParts = sleepTime.split(":").map { it.trim().toInt() }
            val wakeMinutes = wakeParts[0] * 60 + wakeParts[1]
            val sleepMinutes = sleepParts[0] * 60 + sleepParts[1]

            if (sleepMinutes > wakeMinutes) {
                // e.g., 08:00 to 23:00 -> quiet hours are outside this range
                currentMinutes < wakeMinutes || currentMinutes >= sleepMinutes
            } else {
                // e.g., crosses midnight
                currentMinutes in sleepMinutes until wakeMinutes
            }
        } catch (e: Exception) {
            false
        }
    }
}
