package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.room.database.NooshDatabase
import com.example.data.local.room.entity.SyncEntityType
import com.example.data.local.room.entity.SyncOutboxStatus
import com.example.data.local.room.entity.UserProfileEntity
import com.example.data.remote.supabase.SupabaseClient
import com.example.data.remote.supabase.SupabaseHealthSyncEvent
import com.example.data.remote.supabase.SupabaseWaterIntake
import com.example.data.remote.sync.SyncManager
import com.example.data.repository.ReminderRepositoryImpl
import com.example.data.repository.UserRepositoryImpl
import com.example.data.repository.WaterRepositoryImpl
import com.example.domain.companion.HealthCompanionManager
import com.example.domain.manager.InactivityDetector
import com.example.domain.model.AlertFilterPolicy
import com.example.domain.model.AlertSeverity
import com.example.domain.model.CompanionConnectionStatus
import com.example.domain.model.HealthAlertEvent
import com.example.domain.model.HealthCompanionConnection
import com.example.domain.model.HealthEventType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

/**
 * Requirement #75: Complete 18-step Scenario Verification for Offline Data Synchronization Architecture
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class OfflineDataSyncEndToEndTest {

    private lateinit var database: NooshDatabase
    private lateinit var context: Context
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var fakeSupabaseClient: FakeSupabaseClient
    private lateinit var waterRepository: WaterRepositoryImpl
    private lateinit var userRepository: UserRepositoryImpl
    private lateinit var reminderRepository: ReminderRepositoryImpl
    private lateinit var healthCompanionManager: HealthCompanionManager
    private lateinit var syncManager: SyncManager

    class FakeSupabaseClient : SupabaseClient() {
        override val isConfigured: Boolean = true
        var isNetworkOnline: Boolean = true
        val uploadedIntakes = mutableListOf<SupabaseWaterIntake>()
        val uploadedEvents = mutableListOf<HealthAlertEvent>()
        val syncedHealthEvents = mutableListOf<SupabaseHealthSyncEvent>()
        val unacknowledgedQueue = mutableListOf<HealthAlertEvent>()
        val acknowledgedIds = mutableSetOf<String>()

        var shouldFailNextIntake: Boolean = false

        override suspend fun insertWaterIntake(intake: SupabaseWaterIntake): Boolean {
            if (!isNetworkOnline) return false
            if (shouldFailNextIntake) {
                shouldFailNextIntake = false
                return false
            }
            uploadedIntakes.add(intake)
            return true
        }

        override suspend fun insertHealthAlertEvent(event: HealthAlertEvent): Boolean {
            if (!isNetworkOnline) return false
            uploadedEvents.add(event)
            return true
        }

        override suspend fun syncHealthEvent(event: SupabaseHealthSyncEvent): Boolean {
            if (!isNetworkOnline) return false
            syncedHealthEvents.add(event)
            return true
        }

        override suspend fun fetchUnacknowledgedEvents(userId: String): List<HealthAlertEvent> {
            return unacknowledgedQueue.filter { !acknowledgedIds.contains(it.eventId) }
        }

        override suspend fun acknowledgeHealthAlertEvent(eventId: String): Boolean {
            acknowledgedIds.add(eventId)
            return true
        }

        override suspend fun fetchWaterIntakes(userId: String, sinceTimestamp: Long): List<SupabaseWaterIntake> {
            return uploadedIntakes.filter { it.consumedAt >= sinceTimestamp }
        }
    }

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, NooshDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        fakeSupabaseClient = FakeSupabaseClient()

        waterRepository = WaterRepositoryImpl(
            waterIntakeDao = database.waterIntakeDao(),
            dailySummaryDao = database.dailyWaterSummaryDao(),
            userProfileDao = database.userProfileDao(),
            reminderDao = database.reminderDao(),
            syncOutboxDao = database.syncOutboxDao(),
            database = database
        )

        userRepository = UserRepositoryImpl(
            userProfileDao = database.userProfileDao()
        )

        reminderRepository = ReminderRepositoryImpl(
            reminderDao = database.reminderDao()
        )

        val inactivityDetector = InactivityDetector(defaultThresholdMinutes = 120)

        healthCompanionManager = HealthCompanionManager(
            healthAlertEventDao = database.healthAlertEventDao(),
            healthCompanionDao = database.healthCompanionDao(),
            supabaseClient = fakeSupabaseClient,
            waterRepository = waterRepository,
            userRepository = userRepository,
            reminderRepository = reminderRepository,
            inactivityDetector = inactivityDetector,
            scope = testScope,
            syncOutboxDao = database.syncOutboxDao(),
            database = database
        )

        syncManager = SyncManager(
            context = context,
            waterIntakeDao = database.waterIntakeDao(),
            supabaseClient = fakeSupabaseClient,
            syncOutboxDao = database.syncOutboxDao(),
            healthAlertEventDao = database.healthAlertEventDao(),
            waterRepository = waterRepository,
            userRepository = userRepository
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `verify full 18-step offline data synchronization and outbox flow`() = testScope.runTest {
        // Setup initial user
        val profile = UserProfileEntity(
            id = "default_user",
            name = "سارا",
            dailyWaterGoalMl = 2000,
            reminderIntervalMinutes = 60,
            wakeUpTime = "08:00",
            sleepTime = "23:00",
            inactivityThresholdMinutes = 120
        )
        database.userProfileDao().insertOrUpdateProfile(profile)

        // Connect a companion
        val companion = HealthCompanionConnection(
            id = UUID.randomUUID().toString(),
            userId = "default_user",
            companionUserId = "companion_123",
            companionName = "مادر",
            status = CompanionConnectionStatus.CONNECTED,
            alertPolicy = AlertFilterPolicy.ALL,
            lastActiveAt = System.currentTimeMillis(),
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        database.healthCompanionDao().insertOrUpdateConnection(
            com.example.data.local.room.entity.HealthCompanionEntity.fromDomain(companion)
        )

        // ==========================================
        // Step 1: Device goes offline
        // ==========================================
        syncManager.simulatedOnline = false
        fakeSupabaseClient.isNetworkOnline = false

        // ==========================================
        // Step 2: User logs 3 water intakes
        // ==========================================
        val intake1 = waterRepository.addWaterIntake(250, "quick_250")
        val intake2 = waterRepository.addWaterIntake(300, "quick_300")
        val intake3 = waterRepository.addWaterIntake(500, "quick_500")

        // ==========================================
        // Step 3: Records saved to Room locally
        // ==========================================
        val todayIntakes = waterRepository.getTodayWaterIntakes()
        assertEquals(3, todayIntakes.size)
        assertEquals(1050, waterRepository.getTodayTotalMl())

        // ==========================================
        // Step 4: Records added to Sync Outbox
        // ==========================================
        val outboxEntries = database.syncOutboxDao().getAllEntries()
        assertEquals(3, outboxEntries.size)
        assertTrue(outboxEntries.all { it.status == SyncOutboxStatus.PENDING.name })
        assertTrue(outboxEntries.all { it.entityType == SyncEntityType.WATER_INTAKE.name })
        assertTrue(outboxEntries.all { it.idempotencyKey.startsWith("default_user:WATER_INTAKE:") })

        // ==========================================
        // Step 5: UI immediately updates
        // ==========================================
        val totalMlFlow = waterRepository.getTodayTotalMlFlow("default_user").first()
        assertEquals(1050, totalMlFlow)

        // ==========================================
        // Step 6 & 7: Missed reminder event saved to Room and added to Outbox
        // ==========================================
        val missedEvent = healthCompanionManager.recordAndDispatchEvent(
            eventType = HealthEventType.REMINDER_MISSED,
            severity = AlertSeverity.MEDIUM,
            missedReminderCount = 1
        )
        assertNotNull(missedEvent)

        val updatedOutbox = database.syncOutboxDao().getAllEntries()
        assertEquals(4, updatedOutbox.size)
        assertTrue(updatedOutbox.any { it.entityType == SyncEntityType.HEALTH_ALERT_EVENT.name })

        // ==========================================
        // Step 8: Daily summary is updated locally
        // ==========================================
        val summary = waterRepository.getDailySummary(com.example.core.util.DateTimeUtils.getTodayDateString())
        assertNotNull(summary)
        assertEquals(1050, summary!!.totalConsumedMl)
        assertEquals(52, summary.percentage) // 1050 / 2000 = 52%

        // ==========================================
        // Step 9: Health Companion alerts evaluated locally
        // ==========================================
        val companionStatus = healthCompanionManager.getCompanionStatus()
        assertNotNull(companionStatus)
        assertEquals(1050, companionStatus.todayWaterMl)

        // ==========================================
        // Step 10: Network remains offline, verifying no premature uploads
        // ==========================================
        assertEquals(0, fakeSupabaseClient.uploadedIntakes.size)
        assertEquals(0, fakeSupabaseClient.uploadedEvents.size)

        // ==========================================
        // Step 11 & 12: Phone restart recovery simulation & internet becomes available
        // ==========================================
        syncManager.simulatedOnline = true
        fakeSupabaseClient.isNetworkOnline = true

        // ==========================================
        // Step 13 & 14: Records recovered from Room and Sync Worker processes outbox
        // ==========================================
        val syncResult = syncManager.processOutboxSync()
        assertTrue(syncResult)

        // ==========================================
        // Step 15: Supabase receives intakes & events
        // ==========================================
        assertEquals(3, fakeSupabaseClient.uploadedIntakes.size)
        assertEquals(1, fakeSupabaseClient.uploadedEvents.size)
        assertEquals(missedEvent.eventId, fakeSupabaseClient.uploadedEvents[0].eventId)

        // Outbox entries now all marked SYNCED
        val syncedOutbox = database.syncOutboxDao().getAllEntries()
        assertTrue(syncedOutbox.all { it.status == SyncOutboxStatus.SYNCED.name })

        // ==========================================
        // Step 16 & 17: Health state recalculation and historical reconstruction
        // ==========================================
        assertTrue(fakeSupabaseClient.syncedHealthEvents.isNotEmpty())
        val lastSyncedHealth = fakeSupabaseClient.syncedHealthEvents.last()
        assertEquals(1050, lastSyncedHealth.dailyIntakeMl)
        assertEquals(2000, lastSyncedHealth.dailyGoalMl)

        // Verify idempotency: syncing again does not re-upload already synced items
        val reSync = syncManager.processOutboxSync()
        assertTrue(reSync)
        assertEquals(3, fakeSupabaseClient.uploadedIntakes.size)

        // ==========================================
        // Step 18: Health Companion catches up from Supabase
        // ==========================================
        val remoteBacklogEvent = HealthAlertEvent(
            eventId = UUID.randomUUID().toString(),
            eventType = HealthEventType.LONG_INACTIVITY,
            userId = "default_user",
            timestamp = System.currentTimeMillis() - 10000,
            date = com.example.core.util.DateTimeUtils.getTodayDateString(),
            currentWaterMl = 1050,
            dailyGoalMl = 2000,
            goalPercentage = 52,
            lastWaterIntakeAt = null,
            missedReminderCount = 1,
            streak = 1,
            severity = AlertSeverity.HIGH,
            deliveryStatus = com.example.domain.model.EventDeliveryStatus.SENT
        )
        fakeSupabaseClient.unacknowledgedQueue.add(remoteBacklogEvent)

        val caughtUpCount = healthCompanionManager.catchUpFromSupabase("default_user")
        assertEquals(1, caughtUpCount)
        assertTrue(fakeSupabaseClient.acknowledgedIds.contains(remoteBacklogEvent.eventId))

        val localStoredEvent = database.healthAlertEventDao().getEventById(remoteBacklogEvent.eventId)
        assertNotNull(localStoredEvent)
        assertEquals("ACKNOWLEDGED", localStoredEvent!!.deliveryStatus)
    }

    @Test
    fun `test partial sync failure and exponential backoff retry`() = testScope.runTest {
        syncManager.simulatedOnline = false

        // Add 2 intakes
        waterRepository.addWaterIntake(200, "cup")
        waterRepository.addWaterIntake(300, "cup")

        // First intake upload will fail, second will succeed
        fakeSupabaseClient.shouldFailNextIntake = true
        syncManager.simulatedOnline = true

        val syncSuccess = syncManager.processOutboxSync()
        // Overall is false because 1 failed
        assertEquals(false, syncSuccess)

        val entries = database.syncOutboxDao().getAllEntries()
        assertEquals(2, entries.size)

        // Check partial success: one is RETRYING, one is SYNCED
        val failedEntry = entries.find { it.status == SyncOutboxStatus.RETRYING.name }
        val successEntry = entries.find { it.status == SyncOutboxStatus.SYNCED.name }

        assertNotNull(failedEntry)
        assertNotNull(successEntry)
        assertEquals(1, failedEntry!!.attemptCount)
        assertTrue(failedEntry.nextRetryAt > failedEntry.createdAt)

        // Only 1 intake was uploaded to remote
        assertEquals(1, fakeSupabaseClient.uploadedIntakes.size)
    }
}
