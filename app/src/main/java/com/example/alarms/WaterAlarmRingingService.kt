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
import com.example.MainActivity
import com.example.R
import com.example.notifications.NotificationActionReceiver
import com.example.notifications.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Background Alarm Service that plays a soft, recurring notification sound and gentle vibration
 * to remind the user to drink water, persisting until the user interacts with the app.
 */
class WaterAlarmRingingService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var recurringSoundJob: Job? = null
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

        _isRingingFlow.value = true
        startForegroundNotification(reminderId, personName, amountMl)
        startSoftRecurringSoundLoop()

        return START_NOT_STICKY
    }

    private fun startForegroundNotification(reminderId: String, personName: String, amountMl: Int) {
        // Interacting with the app via tapping notification opens MainActivity and stops the alarm
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            this.action = ACTION_INTERACT_WITH_APP
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_STOP_ALARM, true)
            putExtra(EXTRA_REMINDER_ID, reminderId)
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            100,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Fullscreen alarm intent if locked
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

        val title = "💧 وقت نوشیدن آب است"
        val body = "$personName عزیز، لطفاً یک لیوان ($amountMl میلی‌لیتر) آب میل کنید."

        val notification: Notification = NotificationCompat.Builder(this, SERVICE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_water)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setContentIntent(openAppPendingIntent)
            .setOngoing(true)
            .setAutoCancel(false)
            .addAction(R.drawable.ic_notification_water, "نوشیدم 💧", drinkPendingIntent)
            .addAction(R.drawable.ic_notification_water, "ورود به برنامه", openAppPendingIntent)
            .addAction(R.drawable.ic_notification_water, "تعویق ۱۵ دقیقه", stallPendingIntent)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    /**
     * Plays a soft, recurring notification chime and gentle pulse every 3.5 seconds
     * until the user interacts with the app or dismisses it.
     */
    private fun startSoftRecurringSoundLoop() {
        recurringSoundJob?.cancel()
        initVibrator()

        val alertUri: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

        recurringSoundJob = serviceScope.launch {
            while (isActive) {
                try {
                    mediaPlayer?.release()
                    mediaPlayer = MediaPlayer().apply {
                        setDataSource(applicationContext, alertUri)
                        setAudioAttributes(
                            AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_ALARM)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                .build()
                        )
                        // Soft, gentle volume (persists softly without being deafening)
                        setVolume(0.52f, 0.52f)
                        prepare()
                        start()
                    }

                    // Gentle haptic feedback accompanying the chime
                    triggerSoftPulse()
                } catch (e: Exception) {
                    Log.e(TAG, "Failed playing recurring soft sound: ${e.message}")
                }

                // Wait 3.5 seconds before playing the next soft recurring reminder chime
                delay(3500)
            }
        }
    }

    private fun initVibrator() {
        if (vibrator == null) {
            vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                manager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        }
    }

    private fun triggerSoftPulse() {
        try {
            vibrator?.let { vib ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vib.vibrate(VibrationEffect.createOneShot(180, 100))
                } else {
                    @Suppress("DEPRECATION")
                    vib.vibrate(180)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error triggering soft pulse: ${e.message}")
        }
    }

    private fun stopRingingAndSelf() {
        _isRingingFlow.value = false
        recurringSoundJob?.cancel()
        recurringSoundJob = null

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
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun ensureForegroundChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                SERVICE_CHANNEL_ID,
                "زنگ ملایم یادآور نوشیدن آب",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "پخش صدای ملایم و تکرارشونده تا زمان تعامل با برنامه"
                setSound(null, null) // Handled via MediaPlayer for volume loop control
                enableVibration(false) // Handled via recurring soft vibrator
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val SERVICE_CHANNEL_ID = "noosh_water_alarm_ringing_service_channel"
        const val NOTIFICATION_ID = 2002

        const val ACTION_START_ALARM = "com.example.action.START_WATER_ALARM"
        const val ACTION_STOP_ALARM = "com.example.action.STOP_WATER_ALARM"
        const val ACTION_INTERACT_WITH_APP = "com.example.action.INTERACT_WITH_APP"

        const val EXTRA_REMINDER_ID = "extra_reminder_id"
        const val EXTRA_PERSON_NAME = "extra_person_name"
        const val EXTRA_AMOUNT_ML = "extra_amount_ml"
        const val EXTRA_STOP_ALARM = "extra_stop_alarm"
        private const val TAG = "WaterAlarmService"

        private val _isRingingFlow = MutableStateFlow(false)
        val isRingingFlow: StateFlow<Boolean> = _isRingingFlow.asStateFlow()

        fun start(context: Context, reminderId: String = "reminder_auto", personName: String = "کاربر گرامی", amountMl: Int = 250) {
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
            _isRingingFlow.value = false
            val intent = Intent(context, WaterAlarmRingingService::class.java).apply {
                action = ACTION_STOP_ALARM
            }
            context.startService(intent)
        }
    }
}
