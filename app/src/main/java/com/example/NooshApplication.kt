package com.example

import android.app.Application
import com.example.alarms.ReminderScheduler
import com.example.data.local.room.database.NooshDatabase
import com.example.data.remote.clerk.ClerkAuthManager
import com.example.data.remote.supabase.SupabaseClient
import com.example.data.remote.sync.SyncManager
import com.example.data.repository.ReminderRepositoryImpl
import com.example.data.repository.UserRepositoryImpl
import com.example.data.repository.WaterRepositoryImpl
import com.example.domain.companion.HealthCompanionManager
import com.example.domain.manager.InactivityDetector
import com.example.domain.repository.HealthRepository
import com.example.domain.repository.ReminderRepository
import com.example.domain.repository.SyncRepository
import com.example.domain.repository.UserRepository
import com.example.domain.repository.WaterRepository
import com.example.domain.usecase.AddWaterIntakeUseCase
import com.example.domain.usecase.GetDashboardDataUseCase
import com.example.fcm.FcmTokenManager
import com.example.notifications.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class NooshApplication : Application() {

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val database: NooshDatabase by lazy {
        NooshDatabase.getInstance(this, applicationScope)
    }

    val waterRepository: WaterRepository by lazy {
        WaterRepositoryImpl(
            waterIntakeDao = database.waterIntakeDao(),
            dailySummaryDao = database.dailyWaterSummaryDao(),
            userProfileDao = database.userProfileDao(),
            reminderDao = database.reminderDao(),
            syncOutboxDao = database.syncOutboxDao(),
            database = database
        )
    }

    val userRepository: UserRepository by lazy {
        UserRepositoryImpl(
            userProfileDao = database.userProfileDao()
        )
    }

    val reminderRepository: ReminderRepository by lazy {
        ReminderRepositoryImpl(
            reminderDao = database.reminderDao()
        )
    }

    val supabaseClient: SupabaseClient by lazy {
        SupabaseClient(
            supabaseUrl = "",
            supabaseKey = ""
        )
    }

    val inactivityDetector: InactivityDetector by lazy {
        InactivityDetector(defaultThresholdMinutes = 120)
    }

    val healthCompanionManager: HealthCompanionManager by lazy {
        HealthCompanionManager(
            healthAlertEventDao = database.healthAlertEventDao(),
            healthCompanionDao = database.healthCompanionDao(),
            supabaseClient = supabaseClient,
            waterRepository = waterRepository,
            userRepository = userRepository,
            reminderRepository = reminderRepository,
            inactivityDetector = inactivityDetector,
            scope = applicationScope,
            syncOutboxDao = database.syncOutboxDao(),
            database = database
        )
    }

    val fcmTokenManager: FcmTokenManager by lazy {
        FcmTokenManager(
            context = this,
            supabaseClient = supabaseClient,
            userRepository = userRepository,
            scope = applicationScope
        )
    }

    val syncManager: SyncManager by lazy {
        SyncManager(
            context = this,
            waterIntakeDao = database.waterIntakeDao(),
            supabaseClient = supabaseClient,
            syncOutboxDao = database.syncOutboxDao(),
            healthAlertEventDao = database.healthAlertEventDao(),
            waterRepository = waterRepository,
            userRepository = userRepository
        )
    }

    val networkMonitor: com.example.core.network.NetworkMonitor by lazy {
        com.example.core.network.NetworkMonitor(
            context = this,
            scope = applicationScope,
            onNetworkRestored = {
                com.example.data.remote.sync.SyncWorker.enqueueImmediateSync(this)
                applicationScope.launch(Dispatchers.IO) {
                    syncManager.processOutboxSync()
                }
            }
        )
    }

    val syncRepository: SyncRepository
        get() = syncManager

    val healthRepository: HealthRepository
        get() = syncManager

    val clerkAuthManager: ClerkAuthManager by lazy {
        ClerkAuthManager(
            context = this,
            publishableKey = ""
        )
    }

    val reminderScheduler: ReminderScheduler by lazy {
        ReminderScheduler(
            context = this,
            reminderRepository = reminderRepository,
            userRepository = userRepository
        )
    }

    val addWaterIntakeUseCase: AddWaterIntakeUseCase by lazy {
        AddWaterIntakeUseCase(
            waterRepository = waterRepository,
            userRepository = userRepository,
            reminderRepository = reminderRepository,
            syncRepository = syncRepository,
            healthRepository = healthRepository,
            healthCompanionManager = healthCompanionManager
        )
    }

    val getDashboardDataUseCase: GetDashboardDataUseCase by lazy {
        GetDashboardDataUseCase(
            waterRepository = waterRepository,
            userRepository = userRepository,
            reminderRepository = reminderRepository
        )
    }

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createNotificationChannel(this)
        fcmTokenManager.initTokenRegistration()

        networkMonitor.startMonitoring()
        com.example.data.remote.sync.SyncWorker.schedulePeriodicSync(this)

        applicationScope.launch(Dispatchers.IO) {
            val profile = userRepository.getUserProfile()
            if (profile.reminderEnabled) {
                reminderScheduler.scheduleNextPendingReminder()
            }
        }
    }
}
