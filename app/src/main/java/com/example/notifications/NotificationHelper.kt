package com.example.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R

object NotificationHelper {
    const val CHANNEL_ID = "noosh_water_reminder_channel"
    const val URGENT_CHANNEL_ID = "noosh_water_urgent_reminder_channel"
    const val SOFT_CHANNEL_ID = "noosh_soft_water_reminder_channel"
    const val NOTIFICATION_ID = 1001
    const val URGENT_NOTIFICATION_ID = 1002
    const val SOFT_NOTIFICATION_ID = 1003

    const val ACTION_DRANK_WATER = "com.example.action.DRANK_WATER"
    const val ACTION_STALL_REMINDER = "com.example.action.STALL_REMINDER"
    const val ACTION_SNOOZE_REMINDER = "com.example.action.SNOOZE_REMINDER"
    const val EXTRA_REMINDER_ID = "extra_reminder_id"
    const val EXTRA_AMOUNT_ML = "extra_amount_ml"
    const val EXTRA_STALL_REASON = "extra_stall_reason"
    const val EXTRA_DELAY_MINUTES = "extra_delay_minutes"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val name = context.getString(R.string.notification_channel_name)
            val descriptionText = context.getString(R.string.notification_channel_desc)
            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .build()

            val channel = NotificationChannel(CHANNEL_ID, name, NotificationManager.IMPORTANCE_HIGH).apply {
                description = descriptionText
                enableLights(true)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 300, 200, 300)
                setSound(soundUri, audioAttributes)
            }
            notificationManager.createNotificationChannel(channel)

            // Soft Background Reminder Channel (gentle chime & soft vibration)
            val softChannel = NotificationChannel(
                SOFT_CHANNEL_ID,
                "یادآور ملایم نوشیدن آب (WorkManager)",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "یادآورهای دوره‌ای پس‌زمینه با دکمه‌های کنترلی مستقیم در نوار اعلانات"
                enableLights(true)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 150, 100, 150)
                setSound(soundUri, audioAttributes)
            }
            notificationManager.createNotificationChannel(softChannel)

            // Urgent channel with alarm-type sound and repeating vibration
            val alarmSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                ?: soundUri
            val urgentAudioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()

            val urgentChannel = NotificationChannel(
                URGENT_CHANNEL_ID,
                "زنگ هشدار تأخیر آب",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "زنگ پیوسته در صورت عدم مصرف آب پس از ۱۵ دقیقه"
                enableLights(true)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500, 200, 500)
                setSound(alarmSoundUri, urgentAudioAttributes)
            }
            notificationManager.createNotificationChannel(urgentChannel)
        }
    }

    fun showWaterReminderNotification(
        context: Context,
        personName: String,
        reminderId: String,
        amountMl: Int = 250,
        soundEnabled: Boolean = true,
        vibrateEnabled: Boolean = true
    ) {
        createNotificationChannel(context)

        // Intent to open app when clicking notification body
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            0,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action Intent for "I drank water" without opening full activity
        val actionIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = ACTION_DRANK_WATER
            putExtra(EXTRA_REMINDER_ID, reminderId)
            putExtra(EXTRA_AMOUNT_ML, amountMl)
        }
        val actionPendingIntent = PendingIntent.getBroadcast(
            context,
            1,
            actionIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = context.getString(R.string.notification_title)
        val body = context.getString(R.string.notification_body_template, personName)
        val actionTitle = context.getString(R.string.notification_action_drank)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_water)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(contentPendingIntent)
            .setAutoCancel(true)
            .addAction(
                R.drawable.ic_notification_water,
                actionTitle,
                actionPendingIntent
            )

        if (soundEnabled) {
            builder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
        } else {
            builder.setSound(null)
        }

        if (vibrateEnabled) {
            builder.setVibrate(longArrayOf(0, 300, 200, 300))
        } else {
            builder.setVibrate(longArrayOf(0))
        }

        try {
            val manager = NotificationManagerCompat.from(context)
            manager.notify(NOTIFICATION_ID, builder.build())
        } catch (e: SecurityException) {
            // Android 13+ POST_NOTIFICATIONS permission might not be granted yet
        }
    }

    /**
     * Escalated ringing notification triggered after 15 minutes if user didn't drink.
     * Rings continuously/persistently with an alarm sound until user drinks or stalls.
     */
    fun showUrgentEscalatedNotification(
        context: Context,
        personName: String,
        reminderId: String,
        amountMl: Int = 250
    ) {
        createNotificationChannel(context)

        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            2,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action 1: Drink Now
        val drinkIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = ACTION_DRANK_WATER
            putExtra(EXTRA_REMINDER_ID, reminderId)
            putExtra(EXTRA_AMOUNT_ML, amountMl)
        }
        val drinkPendingIntent = PendingIntent.getBroadcast(
            context,
            3,
            drinkIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action 2: Stall (Snooze and compensate in next turn)
        val stallIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = ACTION_STALL_REMINDER
            putExtra(EXTRA_REMINDER_ID, reminderId)
            putExtra(EXTRA_STALL_REASON, "مشغله کاری")
        }
        val stallPendingIntent = PendingIntent.getBroadcast(
            context,
            4,
            stallIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = context.getString(R.string.notification_urgent_title)
        val body = context.getString(R.string.notification_urgent_body, personName)
        val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

        val builder = NotificationCompat.Builder(context, URGENT_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_water)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setSound(alarmSound)
            .setVibrate(longArrayOf(0, 500, 200, 500, 200, 500))
            .setContentIntent(contentPendingIntent)
            .setOngoing(true)
            .setAutoCancel(false)
            .addAction(R.drawable.ic_notification_water, context.getString(R.string.notification_action_drink_now), drinkPendingIntent)
            .addAction(R.drawable.ic_notification_water, context.getString(R.string.notification_action_stall), stallPendingIntent)

        try {
            val manager = NotificationManagerCompat.from(context)
            manager.notify(URGENT_NOTIFICATION_ID, builder.build())
        } catch (e: SecurityException) {
            // ignore
        }
    }

    /**
     * Soft, periodic background reminder triggered by WorkManager.
     * Features direct interactive actions in the notification shade so the user
     * can log water intake (250ml or 500ml) or snooze without opening the app.
     */
    fun showSoftWaterReminderNotification(
        context: Context,
        personName: String,
        customBody: String? = null,
        reminderId: String = "workmanager_reminder_${System.currentTimeMillis()}",
        amountMl: Int = 250,
        soundEnabled: Boolean = true,
        vibrateEnabled: Boolean = true
    ) {
        createNotificationChannel(context)

        // Content intent: Opens MainActivity when tapping notification body
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            10,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action 1: "نوشیدم (۲۵۰ml) 💧" (Quick regular glass)
        val drink250Intent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = ACTION_DRANK_WATER
            putExtra(EXTRA_REMINDER_ID, reminderId)
            putExtra(EXTRA_AMOUNT_ML, 250)
        }
        val drink250PendingIntent = PendingIntent.getBroadcast(
            context,
            11,
            drink250Intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action 2: "لیوان بزرگ (۵۰۰ml) 🥛" (Large glass / bottle)
        val drink500Intent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = ACTION_DRANK_WATER
            putExtra(EXTRA_REMINDER_ID, reminderId)
            putExtra(EXTRA_AMOUNT_ML, 500)
        }
        val drink500PendingIntent = PendingIntent.getBroadcast(
            context,
            12,
            drink500Intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action 3: "۱۰ دقیقه بعد ⏳" (Snooze from notification shade)
        val snoozeIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = ACTION_SNOOZE_REMINDER
            putExtra(EXTRA_REMINDER_ID, reminderId)
            putExtra(EXTRA_DELAY_MINUTES, 10)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            13,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = context.getString(R.string.notification_soft_title)
        val body = customBody ?: context.getString(R.string.notification_soft_body_template, personName)

        val builder = NotificationCompat.Builder(context, SOFT_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_water)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(contentPendingIntent)
            .setAutoCancel(true)
            .addAction(
                R.drawable.ic_notification_water,
                context.getString(R.string.notification_action_drink_250),
                drink250PendingIntent
            )
            .addAction(
                R.drawable.ic_notification_water,
                context.getString(R.string.notification_action_drink_500),
                drink500PendingIntent
            )
            .addAction(
                R.drawable.ic_notification_water,
                context.getString(R.string.notification_action_snooze_10),
                snoozePendingIntent
            )

        if (soundEnabled) {
            builder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
        } else {
            builder.setSound(null)
        }

        if (vibrateEnabled) {
            builder.setVibrate(longArrayOf(0, 150, 100, 150))
        } else {
            builder.setVibrate(longArrayOf(0))
        }

        try {
            val manager = NotificationManagerCompat.from(context)
            manager.notify(SOFT_NOTIFICATION_ID, builder.build())
        } catch (e: SecurityException) {
            // Android 13+ permission catch
        }
    }

    fun cancelNotification(context: Context) {
        val manager = NotificationManagerCompat.from(context)
        manager.cancel(NOTIFICATION_ID)
        manager.cancel(URGENT_NOTIFICATION_ID)
        manager.cancel(SOFT_NOTIFICATION_ID)
    }
}
