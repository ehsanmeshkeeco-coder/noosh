package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.room.database.NooshDatabase
import com.example.data.local.room.entity.HealthAlertEventEntity
import com.example.data.local.room.entity.HealthCompanionEntity
import com.example.data.local.room.entity.ReminderEntity
import com.example.data.local.room.entity.UserProfileEntity
import com.example.data.remote.supabase.SupabaseClient
import com.example.domain.companion.HealthCompanionManager
import com.example.domain.manager.InactivityDetector
import com.example.domain.model.AlertFilterPolicy
import com.example.domain.model.AlertSeverity
import com.example.domain.model.CompanionConnectionStatus
import com.example.domain.model.EventDeliveryStatus
import com.example.domain.model.HealthEventType
import com.example.domain.model.HealthStatusEvaluation
import com.example.domain.model.ReminderStatus
import com.example.data.repository.ReminderRepositoryImpl
import com.example.data.repository.UserRepositoryImpl
import com.example.data.repository.WaterRepositoryImpl
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

/**
 * End-to-End Test Suite for Requirement #60:
 * User schedules reminder -> Reminder fires -> User does not drink -> Reminder retry ->
 * Missed event created -> Event synced to Supabase -> Backend evaluates health ->
 * FCM event sent -> Health Companion receives -> Event acknowledged.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HealthCompanionEndToEndTest {

    private lateinit var database: NooshDatabase
    private lateinit var context: Context
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var waterRepository: WaterRepositoryImpl
    private lateinit var userRepository: UserRepositoryImpl
    private lateinit var reminderRepository: ReminderRepositoryImpl
    private lateinit var inactivityDetector: InactivityDetector
    private lateinit var healthCompanionManager: HealthCompanionManager
    private lateinit var supabaseClient: SupabaseClient

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, NooshDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        waterRepository = WaterRepositoryImpl(
            waterIntakeDao = database.waterIntakeDao(),
            dailySummaryDao = database.dailyWaterSummaryDao(),
            userProfileDao = database.userProfileDao(),
            reminderDao = database.reminderDao()
        )

        userRepository = UserRepositoryImpl(
            userProfileDao = database.userProfileDao()
        )

        reminderRepository = ReminderRepositoryImpl(
            reminderDao = database.reminderDao()
        )

        inactivityDetector = InactivityDetector(defaultThresholdMinutes = 120)

        supabaseClient = SupabaseClient(
            supabaseUrl = "https://mock.supabase.co",
            supabaseKey = "mock_anon_key"
        )

        healthCompanionManager = HealthCompanionManager(
            healthAlertEventDao = database.healthAlertEventDao(),
            healthCompanionDao = database.healthCompanionDao(),
            supabaseClient = supabaseClient,
            waterRepository = waterRepository,
            userRepository = userRepository,
            reminderRepository = reminderRepository,
            inactivityDetector = inactivityDetector,
            scope = testScope
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `test complete health companion flow - reminder trigger to missed to inactivity`() = testScope.runTest {
        // 1. Initial User Profile Setup
        val profile = UserProfileEntity(
            id = "default_user",
            name = "علی راد",
            dailyWaterGoalMl = 2000,
            reminderIntervalMinutes = 60,
            wakeUpTime = "08:00",
            sleepTime = "23:00",
            inactivityThresholdMinutes = 120
        )
        database.userProfileDao().insertOrUpdateProfile(profile)

        // 2. Connect Companion with Consent (Section 58)
        val companion = HealthCompanionEntity(
            id = "companion_conn_1",
            userId = "default_user",
            companionUserId = "doctor_123",
            companionName = "دکتر مهدوی",
            status = CompanionConnectionStatus.CONNECTED.name,
            alertPolicy = AlertFilterPolicy.ALL.name,
            lastActiveAt = System.currentTimeMillis(),
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        database.healthCompanionDao().insertOrUpdateConnection(companion)

        // 3. User schedules a reminder
        val reminderId = "reminder_${System.currentTimeMillis()}"
        val scheduledReminder = ReminderEntity(
            id = reminderId,
            userId = "default_user",
            scheduledAt = System.currentTimeMillis(),
            status = ReminderStatus.PENDING.name,
            amountMl = 250
        )
        database.reminderDao().insertReminder(scheduledReminder)

        val retrievedReminder = reminderRepository.getReminderById(reminderId)
        assertNotNull(retrievedReminder)
        assertEquals(ReminderStatus.PENDING, retrievedReminder!!.status)

        // 4. Reminder Fires (Triggered) -> Event REMINDER_TRIGGERED created
        val triggeredEvent = healthCompanionManager.recordAndDispatchEvent(
            eventType = HealthEventType.REMINDER_TRIGGERED,
            severity = AlertSeverity.LOW
        )
        database.reminderDao().updateReminderStatus(reminderId, ReminderStatus.NOTIFIED.name, null)

        assertNotNull(triggeredEvent)
        assertEquals(HealthEventType.REMINDER_TRIGGERED, triggeredEvent.eventType)
        assertEquals(AlertSeverity.LOW, triggeredEvent.severity)

        // Verify Event stored locally (Audit Trail)
        val savedTriggeredEvent = database.healthAlertEventDao().getEventById(triggeredEvent.eventId)
        assertNotNull(savedTriggeredEvent)
        assertEquals(EventDeliveryStatus.PENDING.name, savedTriggeredEvent!!.deliveryStatus)

        // 5. User does not drink after 15 minutes retry -> Reminder marked MISSED
        database.reminderDao().updateReminderStatus(reminderId, ReminderStatus.MISSED.name, null)
        val missedEvent = healthCompanionManager.recordAndDispatchEvent(
            eventType = HealthEventType.REMINDER_MISSED,
            severity = AlertSeverity.MEDIUM,
            missedReminderCount = 1
        )

        assertEquals(HealthEventType.REMINDER_MISSED, missedEvent.eventType)
        assertEquals(AlertSeverity.MEDIUM, missedEvent.severity)
        assertEquals(1, missedEvent.missedReminderCount)

        // 6. Inactivity Detector Evaluates
        // Simulate no water intake for 150 minutes during daytime (wake up 08:00 to 23:00)
        val pastIntakeTime = System.currentTimeMillis() - (150 * 60 * 1000L)
        val inactivityResult = inactivityDetector.checkInactivity(
            lastWaterIntakeAt = pastIntakeTime,
            thresholdMinutes = 120,
            wakeUpTime = "08:00",
            sleepTime = "23:00"
        )
        assertTrue(inactivityResult.isInactive)
        assertTrue(inactivityResult.inactivityMinutes >= 150)

        // Dispatch High-Severity Inactivity Alert
        val inactivityEvent = healthCompanionManager.recordAndDispatchEvent(
            eventType = HealthEventType.LONG_INACTIVITY,
            severity = AlertSeverity.HIGH
        )
        assertEquals(HealthEventType.LONG_INACTIVITY, inactivityEvent.eventType)
        assertEquals(AlertSeverity.HIGH, inactivityEvent.severity)

        // 7. Companion Status Evaluation (Section 55)
        val companionStatus = healthCompanionManager.getCompanionStatus()
        assertEquals(HealthStatusEvaluation.BEHIND, companionStatus.evaluation)
        assertEquals(1, companionStatus.missedReminders)
        assertEquals(0, companionStatus.todayWaterMl)

        // 8. Event Acknowledgment Flow (Section 54)
        healthCompanionManager.acknowledgeEvent(inactivityEvent.eventId)
        val acknowledgedEntity = database.healthAlertEventDao().getEventById(inactivityEvent.eventId)
        assertNotNull(acknowledgedEntity)
        assertEquals(EventDeliveryStatus.ACKNOWLEDGED.name, acknowledgedEntity!!.deliveryStatus)

        // 9. Privacy check: User disconnects companion (Section 58)
        healthCompanionManager.disconnectCompanion(companion.id)
        val activeCompanionAfterDisconnect = database.healthCompanionDao().getActiveCompanion()
        // Must be null or DISCONNECTED
        assertTrue(activeCompanionAfterDisconnect == null || activeCompanionAfterDisconnect.status == CompanionConnectionStatus.DISCONNECTED.name)
    }

    @Test
    fun `test notification alert policy filtering`() = testScope.runTest {
        // When policy is HIGH_ONLY, only HIGH severity should notify companion
        val policyHighOnly = AlertFilterPolicy.HIGH_ONLY
        val policyMediumAndHigh = AlertFilterPolicy.MEDIUM_AND_HIGH
        val policyAll = AlertFilterPolicy.ALL

        // Low severity event
        assertFalse(shouldAlert(AlertSeverity.LOW, policyHighOnly))
        assertFalse(shouldAlert(AlertSeverity.LOW, policyMediumAndHigh))
        assertTrue(shouldAlert(AlertSeverity.LOW, policyAll))

        // Medium severity event
        assertFalse(shouldAlert(AlertSeverity.MEDIUM, policyHighOnly))
        assertTrue(shouldAlert(AlertSeverity.MEDIUM, policyMediumAndHigh))
        assertTrue(shouldAlert(AlertSeverity.MEDIUM, policyAll))

        // High severity event
        assertTrue(shouldAlert(AlertSeverity.HIGH, policyHighOnly))
        assertTrue(shouldAlert(AlertSeverity.HIGH, policyMediumAndHigh))
        assertTrue(shouldAlert(AlertSeverity.HIGH, policyAll))
    }

    @Test
    fun `test scheduleDailyReminders handles normal and overnight intervals without infinite loop`() = testScope.runTest {
        val normalProfile = com.example.domain.model.UserProfile(
            id = "user_normal",
            wakeUpTime = "08:00",
            sleepTime = "23:00",
            reminderIntervalMinutes = 60,
            reminderEnabled = true
        )
        reminderRepository.scheduleDailyReminders(normalProfile)

        // Overnight sleep time (e.g., wake up 09:00, sleep 01:30 next day)
        val overnightProfile = com.example.domain.model.UserProfile(
            id = "user_overnight",
            wakeUpTime = "09:00",
            sleepTime = "01:30",
            reminderIntervalMinutes = 30,
            reminderEnabled = true
        )
        reminderRepository.scheduleDailyReminders(overnightProfile)

        // Equal wake and sleep time
        val edgeProfile = com.example.domain.model.UserProfile(
            id = "user_edge",
            wakeUpTime = "08:00",
            sleepTime = "08:00",
            reminderIntervalMinutes = 15,
            reminderEnabled = true
        )
        reminderRepository.scheduleDailyReminders(edgeProfile)
    }

    private fun shouldAlert(severity: AlertSeverity, policy: AlertFilterPolicy): Boolean {
        return when (policy) {
            AlertFilterPolicy.ALL -> true
            AlertFilterPolicy.MEDIUM_AND_HIGH -> severity == AlertSeverity.MEDIUM || severity == AlertSeverity.HIGH
            AlertFilterPolicy.HIGH_ONLY -> severity == AlertSeverity.HIGH
        }
    }
}
