package com.example.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.alarms.ReminderScheduler
import com.example.data.remote.clerk.AuthState
import com.example.data.remote.clerk.ClerkAuthManager
import com.clerk.android.Clerk
import com.example.domain.companion.HealthCompanionManager
import com.example.domain.model.AlertFilterPolicy
import com.example.domain.model.AlertSeverity
import com.example.domain.model.HealthAlertEvent
import com.example.domain.model.HealthCompanionConnection
import com.example.domain.model.HealthCompanionStatus
import com.example.domain.model.HealthEventType
import com.example.domain.model.MonthlyReport
import com.example.domain.model.Reminder
import com.example.domain.model.UserProfile
import com.example.domain.model.WeeklyReport
import com.example.domain.repository.ReminderRepository
import com.example.domain.repository.SyncRepository
import com.example.domain.repository.UserRepository
import com.example.domain.repository.WaterRepository
import com.example.domain.usecase.AddWaterIntakeUseCase
import com.example.domain.usecase.DashboardState
import com.example.domain.usecase.GetDashboardDataUseCase
import com.example.fcm.FcmTokenManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(
    private val getDashboardDataUseCase: GetDashboardDataUseCase,
    private val addWaterIntakeUseCase: AddWaterIntakeUseCase,
    private val waterRepository: WaterRepository,
    private val userRepository: UserRepository,
    private val reminderRepository: ReminderRepository,
    private val syncRepository: SyncRepository,
    private val clerkAuthManager: ClerkAuthManager,
    private val reminderScheduler: ReminderScheduler,
    private val healthCompanionManager: HealthCompanionManager? = null,
    private val fcmTokenManager: FcmTokenManager? = null,
    private val supabaseClient: com.example.data.remote.supabase.SupabaseClient? = null
) : ViewModel() {

    val dashboardState: StateFlow<DashboardState?> = getDashboardDataUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val weeklyReport: StateFlow<WeeklyReport?> = waterRepository.getWeeklyReportFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _monthlyReport = MutableStateFlow<MonthlyReport?>(null)
    val monthlyReport: StateFlow<MonthlyReport?> = _monthlyReport.asStateFlow()

    val reminders: StateFlow<List<Reminder>> = reminderRepository.getTodayRemindersFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val authState: StateFlow<AuthState> = clerkAuthManager.authState

    private val _celebrationEvent = MutableStateFlow<Pair<Int, Boolean>?>(null)
    val celebrationEvent: StateFlow<Pair<Int, Boolean>?> = _celebrationEvent.asStateFlow()

    private val _syncStatus = MutableStateFlow("همگام با حافظه محلی و سرور")
    val syncStatus: StateFlow<String> = _syncStatus.asStateFlow()

    // Section 53 & 55: Health Companion & FCM Live Status
    val companionEvents: StateFlow<List<HealthAlertEvent>> = (healthCompanionManager?.getRecentEventsFlow(30) ?: emptyFlow())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeCompanion: StateFlow<HealthCompanionConnection?> = (healthCompanionManager?.getActiveCompanionFlow() ?: emptyFlow())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _companionStatus = MutableStateFlow<HealthCompanionStatus?>(null)
    val companionStatus: StateFlow<HealthCompanionStatus?> = _companionStatus.asStateFlow()

    init {
        loadMonthlyReport()
        refreshCompanionStatus()
    }

    fun loadMonthlyReport() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val report = waterRepository.getMonthlyReport()
                _monthlyReport.value = report
            } catch (e: Exception) {
                // Keep default
            }
        }
    }

    fun refreshCompanionStatus() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                healthCompanionManager?.let { manager ->
                    _companionStatus.value = manager.getCompanionStatus()
                }
            } catch (ignored: Exception) {}
        }
    }

    fun addWater(
        amountMl: Int,
        source: String = "app_quick",
        reminderId: String? = null,
        rescheduleMinutes: Int? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = addWaterIntakeUseCase(amountMl, source, reminderId)
            _celebrationEvent.value = Pair(amountMl, result.isGoalJustAchieved)
            if (rescheduleMinutes != null) {
                reminderScheduler.scheduleRetryAlarm(
                    reminderId = reminderId ?: "reschedule_${System.currentTimeMillis()}",
                    retryMinutes = rescheduleMinutes
                )
            }
            loadMonthlyReport()
            refreshCompanionStatus()
        }
    }

    fun updateDailyGoal(goalMl: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            userRepository.updateGoal(goalMl)
            refreshCompanionStatus()
        }
    }

    fun updateProfile(profile: UserProfile) {
        viewModelScope.launch(Dispatchers.IO) {
            userRepository.updateProfile(profile)
            refreshCompanionStatus()
        }
    }

    fun updateReminderSettings(
        enabled: Boolean,
        intervalMinutes: Int,
        startTime: String,
        endTime: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            userRepository.updateReminderSettings(
                enabled = enabled,
                intervalMinutes = intervalMinutes,
                startTime = startTime,
                endTime = endTime
            )
            if (enabled) {
                reminderScheduler.scheduleNextPendingReminder()
            } else {
                reminderScheduler.cancelAllAlarms()
            }
            refreshCompanionStatus()
        }
    }

    fun triggerTestReminder(context: android.content.Context) {
        sendTestNotification(context)
    }

    fun updateProfileSettings(
        name: String,
        dailyGoalMl: Int,
        reminderInterval: Int,
        wakeUp: String,
        sleep: String,
        sound: Boolean,
        vibrate: Boolean,
        graceDay: Boolean
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = userRepository.getUserProfile()
            val updated = current.copy(
                name = name,
                dailyWaterGoalMl = dailyGoalMl,
                reminderIntervalMinutes = reminderInterval,
                wakeUpTime = wakeUp,
                sleepTime = sleep,
                soundEnabled = sound,
                vibrateEnabled = vibrate,
                graceDayEnabled = graceDay
            )
            userRepository.updateProfile(updated)
            reminderScheduler.scheduleNextPendingReminder()
            refreshCompanionStatus()
        }
    }

    fun toggleReminders(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            userRepository.setReminderEnabled(enabled)
            if (enabled) {
                reminderScheduler.scheduleNextPendingReminder()
            } else {
                reminderScheduler.cancelAllAlarms()
            }
        }
    }

    fun snoozeReminder(reminderId: String, minutes: Int = 15) {
        viewModelScope.launch(Dispatchers.IO) {
            reminderRepository.snoozeReminder(reminderId, minutes)
            reminderScheduler.scheduleRetryAlarm(reminderId, retryMinutes = minutes)
            healthCompanionManager?.recordAndDispatchEvent(
                eventType = HealthEventType.REMINDER_SNOOZED,
                severity = AlertSeverity.LOW
            )
            refreshCompanionStatus()
        }
    }

    fun sendTestNotification(context: android.content.Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val profile = userRepository.getUserProfile()
            com.example.notifications.NotificationHelper.showWaterReminderNotification(
                context = context.applicationContext,
                personName = profile.name,
                reminderId = "test_reminder_${System.currentTimeMillis()}",
                amountMl = 250,
                soundEnabled = profile.soundEnabled,
                vibrateEnabled = profile.vibrateEnabled
            )
        }
    }

    // Health Companion Actions
    fun connectCompanion(userId: String, name: String, policy: AlertFilterPolicy) {
        viewModelScope.launch(Dispatchers.IO) {
            healthCompanionManager?.connectCompanion(userId, name, policy)
            refreshCompanionStatus()
        }
    }

    fun disconnectCompanion(connectionId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            healthCompanionManager?.disconnectCompanion(connectionId)
            refreshCompanionStatus()
        }
    }

    fun togglePauseCompanion(connectionId: String, isCurrentlyConnected: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            healthCompanionManager?.pauseAlerts(connectionId, isCurrentlyConnected)
            refreshCompanionStatus()
        }
    }

    fun updateCompanionPolicy(connectionId: String, policy: AlertFilterPolicy) {
        viewModelScope.launch(Dispatchers.IO) {
            healthCompanionManager?.updateAlertPolicy(connectionId, policy)
        }
    }

    fun simulateReminderMissed() {
        viewModelScope.launch(Dispatchers.IO) {
            healthCompanionManager?.recordAndDispatchEvent(
                eventType = HealthEventType.REMINDER_MISSED,
                severity = AlertSeverity.MEDIUM,
                missedReminderCount = 1
            )
            refreshCompanionStatus()
        }
    }

    fun simulateLongInactivity() {
        viewModelScope.launch(Dispatchers.IO) {
            healthCompanionManager?.recordAndDispatchEvent(
                eventType = HealthEventType.LONG_INACTIVITY,
                severity = AlertSeverity.HIGH
            )
            refreshCompanionStatus()
        }
    }

    fun syncNow() {
        viewModelScope.launch(Dispatchers.IO) {
            _syncStatus.value = "در حال ارسال اطلاعات..."
            val success = syncRepository.syncPendingIntakes()
            healthCompanionManager?.retryPendingEvents()
            if (success) {
                _syncStatus.value = "همگام‌سازی ابری با موفقیت انجام شد ✓"
            } else {
                _syncStatus.value = "در انتظار اتصال سرور (داده‌ها به صورت آفلاین امن هستند)"
            }
            refreshCompanionStatus()
        }
    }

    fun signInWithEmail(email: String, name: String) {
        clerkAuthManager.signInWithEmail(email, name)
        viewModelScope.launch(Dispatchers.IO) {
            val profile = userRepository.getUserProfile()
            val updated = profile.copy(name = name, email = email, clerkUserId = Clerk.getUser()?.id ?: "")
            userRepository.updateProfile(updated)
            fcmTokenManager?.onUserLogin(updated.id)
        }
    }

    fun continueAsGuest() {
        clerkAuthManager.continueAsGuest()
    }

    fun signOut() {
        clerkAuthManager.signOut()
        fcmTokenManager?.onUserLogout()
    }

    fun createCompanionRoom(onCodeGenerated: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val code = healthCompanionManager?.createInviteRoom() ?: (100000..999999).random().toString()
            withContext(Dispatchers.Main) {
                onCodeGenerated(code)
            }
            refreshCompanionStatus()
        }
    }

    fun joinCompanionRoom(roomCode: String, companionName: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val connection = healthCompanionManager?.joinInviteRoom(roomCode, companionName)
            refreshCompanionStatus()
            withContext(Dispatchers.Main) {
                onComplete(connection != null)
            }
        }
    }

    fun uploadAvatar(imageBytes: ByteArray, localUriString: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val profile = userRepository.getUserProfile()
            // Immediately store localUriString so UI displays it without waiting
            userRepository.updateProfile(profile.copy(profileImageUrl = localUriString))
            // Then attempt Supabase Storage upload
            val remoteUrl = supabaseClient?.uploadAvatar(profile.id, imageBytes)
            if (remoteUrl != null) {
                userRepository.updateProfile(profile.copy(profileImageUrl = remoteUrl))
            }
        }
    }

    fun dismissCelebration() {
        _celebrationEvent.value = null
    }
}

class MainViewModelFactory(
    private val getDashboardDataUseCase: GetDashboardDataUseCase,
    private val addWaterIntakeUseCase: AddWaterIntakeUseCase,
    private val waterRepository: WaterRepository,
    private val userRepository: UserRepository,
    private val reminderRepository: ReminderRepository,
    private val syncRepository: SyncRepository,
    private val clerkAuthManager: ClerkAuthManager,
    private val reminderScheduler: ReminderScheduler,
    private val healthCompanionManager: HealthCompanionManager? = null,
    private val fcmTokenManager: FcmTokenManager? = null,
    private val supabaseClient: com.example.data.remote.supabase.SupabaseClient? = null
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            return MainViewModel(
                getDashboardDataUseCase = getDashboardDataUseCase,
                addWaterIntakeUseCase = addWaterIntakeUseCase,
                waterRepository = waterRepository,
                userRepository = userRepository,
                reminderRepository = reminderRepository,
                syncRepository = syncRepository,
                clerkAuthManager = clerkAuthManager,
                reminderScheduler = reminderScheduler,
                healthCompanionManager = healthCompanionManager,
                fcmTokenManager = fcmTokenManager,
                supabaseClient = supabaseClient
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
