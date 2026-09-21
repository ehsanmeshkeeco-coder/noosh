package com.example.alarms

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.R
import com.example.notifications.NotificationActionReceiver
import com.example.notifications.NotificationHelper

/**
 * Foreground Service that plays a pleasant, persistent alarm sound and vibration loop
 * while presenting the Fullscreen Water Alarm Activity until the user drinks or stalls.
 */
class WaterAlarmRingingService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        ensureForegroundChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_STOP_ALARM) {
            stopRingingAndSelf()
            return START_NOT_STICKY
        }

        val reminderId = intent?.getStringExtra(EXTRA_REMINDER_ID) ?: ""
        val personName = intent?.getStringExtra(EXTRA_PERSON_NAME) ?: "کاربر گرامی"
        val amountMl = intent?.getIntExtra(EXTRA_AMOUNT_ML, 250) ?: 250

        startForegroundNotification(reminderId, personName, amountMl)
        startAlarmAudioAndVibration()

        return START_NOT_STICKY
    }

    private fun startForegroundNotification(reminderId: String, personName: String, amountMl: Int) {
        val fullScreenIntent = Intent(this, WaterAlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(WaterAlarmActivity.EXTRA_REMINDER_ID, reminderId)
            putExtra(WaterAlarmActivity.EXTRA_PERSON_NAME, personName)
            putExtra(WaterAlarmActivity.EXTRA_AMOUNT_ML, amountMl)
        }

        val fullScreenPendingIntent = PendingIntent.getActivity(
            this,
            101,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val drinkIntent = Intent(this, NotificationActionReceiver::class.java).apply {
            this.action = NotificationHelper.ACTION_DRANK_WATER
            putExtra(NotificationHelper.EXTRA_REMINDER_ID, reminderId)
            putExtra(NotificationHelper.EXTRA_AMOUNT_ML, amountMl)
        }
        val drinkPendingIntent = PendingIntent.getBroadcast(
            this,
            102,
            drinkIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stallIntent = Intent(this, NotificationActionReceiver::class.java).apply {
            this.action = NotificationHelper.ACTION_STALL_REMINDER
            putExtra(NotificationHelper.EXTRA_REMINDER_ID, reminderId)
            putExtra(NotificationHelper.EXTRA_STALL_REASON, "مشغله")
        }
        val stallPendingIntent = PendingIntent.getBroadcast(
            this,
            103,
            stallIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = "⏰ زنگ یادآور نوشیدن آب"
        val body = "$personName عزیز، وقت نوشیدن $amountMl میلی‌لیتر آب است!"

        val notification: Notification = NotificationCompat.Builder(this, SERVICE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_water)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setContentIntent(fullScreenPendingIntent)
            .setOngoing(true)
            .setAutoCancel(false)
            .addAction(R.drawable.ic_notification_water, "همین الان می‌نوشم 💧", drinkPendingIntent)
            .addAction(R.drawable.ic_notification_water, "به تعویق انداختن (۱۵ دقیقه)", stallPendingIntent)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    private fun startAlarmAudioAndVibration() {
        try {
            if (mediaPlayer == null) {
                // Use a gentle but audible alarm or notification sound
                var alertUri: Uri? = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                if (alertUri == null) {
                    alertUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                }
                if (alertUri == null) {
                    alertUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                }

                mediaPlayer = MediaPlayer().apply {
                    setDataSource(applicationContext, alertUri!!)
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    isLooping = true
                    // Gentle volume ramp (not painfully jarring, but clear and continuous)
                    setVolume(0.85f, 0.85f)
                    prepare()
                    start()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start alarm sound: ${e.message}")
        }

        try {
            vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                manager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }

            // Gentle rhythmic repeating pulse: wait 0ms, vibrate 400ms, pause 300ms, vibrate 400ms, pause 1000ms
            val pattern = longArrayOf(0, 400, 300, 400, 1000)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, 0)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start vibrator: ${e.message}")
        }
    }

    private fun stopRingingAndSelf() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping media player: ${e.message}")
        }

        try {
            vibrator?.cancel()
            vibrator = null
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling vibrator: ${e.message}")
        }

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        stopRingingAndSelf()
        super.onDestroy()
    }

    private fun ensureForegroundChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                SERVICE_CHANNEL_ID,
                "زنگ پیوسته آب",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "هشدار تمام صفحه و زنگ مستمر نوشیدن آب"
                setSound(null, null) // Audio handled via MediaPlayer for volume loop control
                enableVibration(false) // Handled via Vibrator loop
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val SERVICE_CHANNEL_ID = "noosh_water_alarm_ringing_service_channel"
        const val NOTIFICATION_ID = 2002

        const val ACTION_START_ALARM = "com.example.action.START_WATER_ALARM"
        const val ACTION_STOP_ALARM = "com.example.action.STOP_WATER_ALARM"

        const val EXTRA_REMINDER_ID = "extra_reminder_id"
        const val EXTRA_PERSON_NAME = "extra_person_name"
        const val EXTRA_AMOUNT_ML = "extra_amount_ml"
        private const val TAG = "WaterAlarmService"

        fun start(context: Context, reminderId: String, personName: String, amountMl: Int) {
            val intent = Intent(context, WaterAlarmRingingService::class.java).apply {
                action = ACTION_START_ALARM
                putExtra(EXTRA_REMINDER_ID, reminderId)
                putExtra(EXTRA_PERSON_NAME, personName)
                putExtra(EXTRA_AMOUNT_ML, amountMl)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, WaterAlarmRingingService::class.java).apply {
                action = ACTION_STOP_ALARM
            }
            context.startService(intent)
        }
    }
}
