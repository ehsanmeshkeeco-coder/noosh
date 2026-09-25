package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.core.util.DateTimeUtils
import com.example.data.local.room.database.NooshDatabase
import com.example.data.local.room.entity.DailyWaterSummaryEntity
import com.example.data.local.room.entity.HealthAlertEventEntity
import com.example.data.local.room.entity.HealthCompanionEntity
import com.example.data.local.room.entity.ReminderEntity
import com.example.data.local.room.entity.SyncEntityType
import com.example.data.local.room.entity.SyncOperation
import com.example.data.local.room.entity.SyncOutboxEntity
import com.example.data.local.room.entity.SyncOutboxStatus
import com.example.data.local.room.entity.WaterIntakeEntity
import com.example.data.remote.clerk.AuthState
import com.example.data.remote.clerk.ClerkAuthManager
import com.example.data.remote.clerk.ClerkUser
import com.example.data.remote.supabase.SupabaseClient
import com.example.data.remote.sync.SyncManager
import com.example.data.repository.ReminderRepositoryImpl
import com.example.data.repository.UserRepositoryImpl
import com.example.data.repository.WaterRepositoryImpl
import com.example.domain.calculator.HydrationGoalCalculator
import com.example.domain.companion.HealthCompanionManager
import com.example.domain.manager.InactivityDetector
import com.example.domain.model.AlertFilterPolicy
import com.example.domain.model.AlertSeverity
import com.example.domain.model.CompanionConnectionStatus
import com.example.domain.model.DayIntake
import com.example.domain.model.EventDeliveryStatus
import com.example.domain.model.HealthEventType
import com.example.domain.model.ReminderStatus
import com.example.domain.usecase.AddWaterIntakeUseCase
import com.example.domain.usecase.GetDashboardDataUseCase
import com.example.presentation.components.RechartsWeeklyVisualization
import com.example.presentation.theme.NooshTheme
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Master End-to-End Test Suite covering all Critical User Journeys (CUJs) and app scenarios:
 *
 * 1. Onboarding Wizard & Personalized Goal Calculator Scenario
 * 2. Water Logging, Glass Counters & Goal Achievement Scenario
 * 3. Weekly & Monthly Reports and Trends Scenario
 * 4. Smart Reminders & Notification Action Simulation Scenario
 * 5. Authentication, Persistent Session & Sign Out Scenario
 * 6. Health Companion Pairing & Inactivity Alert Scenario
 * 7. Offline-First Sync Outbox Queue Scenario
 * 8. Multi-Cup Container Sizing Scenario
 * 9. Recharts Visualization Rendering & Modes Scenario
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AllScenariosEndToEndTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var database: NooshDatabase
    private lateinit var context: Context
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var waterRepository: WaterRepositoryImpl
    private lateinit var userRepository: UserRepositoryImpl
    private lateinit var reminderRepository: ReminderRepositoryImpl
    private lateinit var syncManager: SyncManager
    private lateinit var healthCompanionManager: HealthCompanionManager
    private lateinit var addWaterIntakeUseCase: AddWaterIntakeUseCase
    private lateinit var getDashboardDataUseCase: GetDashboardDataUseCase
    private lateinit var clerkAuthManager: ClerkAuthManager
    private lateinit var gamificationRepository: com.example.domain.repository.GamificationRepository

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

        val fakeSupabase = SupabaseClient("https://fake.supabase.co", "fake-key")

        syncManager = SyncManager(
            context = context,
            waterIntakeDao = database.waterIntakeDao(),
            supabaseClient = fakeSupabase,
            syncOutboxDao = database.syncOutboxDao(),
            healthAlertEventDao = database.healthAlertEventDao(),
            waterRepository = waterRepository,
            userRepository = userRepository
        )

        val inactivityDetector = InactivityDetector(defaultThresholdMinutes = 120)

        healthCompanionManager = HealthCompanionManager(
            healthAlertEventDao = database.healthAlertEventDao(),
            healthCompanionDao = database.healthCompanionDao(),
            supabaseClient = fakeSupabase,
            waterRepository = waterRepository,
            userRepository = userRepository,
            reminderRepository = reminderRepository,
            inactivityDetector = inactivityDetector,
            scope = testScope,
            syncOutboxDao = database.syncOutboxDao(),
            database = database
        )

        gamificationRepository = com.example.data.repository.GamificationRepositoryImpl(
            gamificationDao = database.gamificationDao(),
            userProfileDao = database.userProfileDao()
        )

        addWaterIntakeUseCase = AddWaterIntakeUseCase(
            waterRepository = waterRepository,
            userRepository = userRepository,
            reminderRepository = reminderRepository,
            syncRepository = syncManager,
            healthRepository = syncManager,
            healthCompanionManager = healthCompanionManager,
            gamificationRepository = gamificationRepository
        )

        getDashboardDataUseCase = GetDashboardDataUseCase(
            waterRepository = waterRepository,
            userRepository = userRepository,
            reminderRepository = reminderRepository
        )

        clerkAuthManager = ClerkAuthManager(context, "pk_test_fake_key$")
        clerkAuthManager.signOut()
    }

    @After
    fun tearDown() {
        database.close()
    }

    // =========================================================================
    // Scenario 1: Onboarding Wizard & Personalized Goal Calculator CUJ
    // =========================================================================
    @Test
    fun test_01_onboarding_and_goal_calculator_scenario() = testScope.runTest {
        // 1. Initial State: Profile has default values and onboarding not completed
        val initialProfile = userRepository.getUserProfile()
        assertFalse(initialProfile.onboardingCompleted)

        // 2. User inputs their metrics: 75kg, active lifestyle, warm/dry climate
        val weightKg = 75f
        val activityLevel = "active" // +700 ml
        val climate = "warm_dry" // +400 ml

        val calculation = HydrationGoalCalculator.calculate(weightKg, activityLevel, climate)
        // Baseline: 75 * 35 = 2625 ml
        assertEquals(2625, calculation.baselineMl)
        assertEquals(700, calculation.activityAdditionMl)
        assertEquals(400, calculation.climateAdditionMl)
        // Total recommended: 2625 + 700 + 400 = 3725 -> rounded to 100 = 3700 ml
        assertTrue(calculation.recommendedGoalMl >= 3700)
        assertTrue(calculation.recommendedGlasses >= 14)

        // 3. Complete onboarding and save to local Room database
        val completedProfile = initialProfile.copy(
            name = "کاربر تستی",
            dailyWaterGoalMl = calculation.recommendedGoalMl,
            wakeUpTime = "07:30",
            sleepTime = "23:00",
            onboardingCompleted = true
        )
        userRepository.updateProfile(completedProfile)

        // 4. Verify persisted profile
        val updatedProfile = userRepository.getUserProfile()
        assertTrue(updatedProfile.onboardingCompleted)
        assertEquals("کاربر تستی", updatedProfile.name)
        assertEquals(calculation.recommendedGoalMl, updatedProfile.dailyWaterGoalMl)
        assertEquals("07:30", updatedProfile.wakeUpTime)
        assertEquals("23:00", updatedProfile.sleepTime)
    }

    // =========================================================================
    // Scenario 2: Water Logging, Glass Counters & Goal Achievement CUJ
    // =========================================================================
    @Test
    fun test_02_water_logging_glass_counters_and_goal_achievement_scenario() = testScope.runTest {
        // Set goal to 2000 ml
        val profile = userRepository.getUserProfile().copy(dailyWaterGoalMl = 2000)
        userRepository.updateProfile(profile)

        // 1. Initial daily state: 0 ml
        val todayTotal = waterRepository.getTodayTotalMl()
        assertEquals(0, todayTotal)

        // 2. Log first standard glass (250 ml)
        val result1 = addWaterIntakeUseCase(amountMl = 250, source = "quick_button")
        assertEquals(250, result1.newTotalMl)
        assertFalse(result1.isGoalJustAchieved)

        // Verify dashboard state
        var dashboard = getDashboardDataUseCase().first()
        assertEquals(250, dashboard.totalConsumedMl)
        assertEquals(1, dashboard.glassesConsumed)
        assertEquals(12, dashboard.percentage) // 250 / 2000 = 12.5% -> 12%

        // 3. Log a bottle (750 ml)
        val result2 = addWaterIntakeUseCase(amountMl = 750, source = "bottle")
        assertEquals(1000, result2.newTotalMl)
        assertFalse(result2.isGoalJustAchieved)

        dashboard = getDashboardDataUseCase().first()
        assertEquals(1000, dashboard.totalConsumedMl)
        assertEquals(4, dashboard.glassesConsumed)
        assertEquals(50, dashboard.percentage) // 1000 / 2000 = 50%

        // 4. Log another 1000 ml to reach exactly 2000 ml (Goal achieved!)
        val result3 = addWaterIntakeUseCase(amountMl = 1000, source = "custom_sheet")
        assertEquals(2000, result3.newTotalMl)
        assertTrue(result3.isGoalJustAchieved) // Celebration triggered!

        dashboard = getDashboardDataUseCase().first()
        assertEquals(2000, dashboard.totalConsumedMl)
        assertEquals(8, dashboard.glassesConsumed)
        assertEquals(100, dashboard.percentage)
        assertEquals(3, dashboard.recentIntakes.size)

        // 5. Delete an intake record (e.g. user entered by mistake)
        val firstIntake = dashboard.recentIntakes.last()
        database.waterIntakeDao().deleteIntake(firstIntake.id)

        val afterDeleteTotal = waterRepository.getTodayTotalMl()
        assertEquals(1750, afterDeleteTotal)

        dashboard = getDashboardDataUseCase().first()
        assertEquals(1750, dashboard.totalConsumedMl)
        assertEquals(7, dashboard.glassesConsumed)
        assertEquals(87, dashboard.percentage)
    }

    // =========================================================================
    // Scenario 3: Weekly & Monthly Analytics and Trends CUJ
    // =========================================================================
    @Test
    fun test_03_weekly_and_monthly_analytics_and_trends_scenario() = testScope.runTest {
        val today = System.currentTimeMillis()
        val oneDayMs = 86400000L

        // Insert intakes and daily summaries across the past 7 days
        val dailyAmounts = listOf(1500, 2000, 2500, 1800, 2200, 1200, 2000)
        val zoneId = DateTimeUtils.zoneId
        val formatter = java.time.format.DateTimeFormatter.ISO_LOCAL_DATE

        dailyAmounts.forEachIndexed { index, amount ->
            val timestamp = today - ((6 - index) * oneDayMs)
            val dateStr = java.time.Instant.ofEpochMilli(timestamp).atZone(zoneId).toLocalDate().format(formatter)
            database.waterIntakeDao().insertIntake(
                WaterIntakeEntity(
                    id = UUID.randomUUID().toString(),
                    userId = "default_user",
                    amountMl = amount,
                    consumedAt = timestamp,
                    source = "history_test",
                    reminderId = null,
                    createdAt = timestamp,
                    synced = false
                )
            )
            database.dailyWaterSummaryDao().insertOrUpdateSummary(
                DailyWaterSummaryEntity(
                    date = dateStr,
                    userId = "default_user",
                    totalConsumedMl = amount,
                    goalMl = 2000,
                    percentage = (amount * 100 / 2000).coerceAtMost(100),
                    completedReminders = 3,
                    missedReminders = 0
                )
            )
        }

        // Fetch Weekly Report Flow
        val report = waterRepository.getWeeklyReportFlow().first()
        assertNotNull(report)
        assertEquals(7, report.days.size)
        assertTrue(report.totalAmountMl > 0)
        assertTrue(report.dailyAverageMl > 0)

        // Best day has highest intake
        val bestDay = report.days.maxByOrNull { it.amountMl }
        assertNotNull(bestDay)
        assertTrue(bestDay!!.amountMl >= 2000)
    }

    // =========================================================================
    // Scenario 4: Smart Reminders & Schedule Window CUJ
    // =========================================================================
    @Test
    fun test_04_smart_reminders_and_schedule_scenario() = testScope.runTest {
        val now = System.currentTimeMillis()

        // Insert reminders throughout the day
        for (idx in 0 until 5) {
            database.reminderDao().insertReminder(
                ReminderEntity(
                    id = "rem_$idx",
                    userId = "default_user",
                    scheduledAt = now + (idx + 1) * 3600000L,
                    status = if (idx == 0) ReminderStatus.COMPLETED.name else ReminderStatus.PENDING.name,
                    amountMl = 250
                )
            )
        }

        // Next reminder should be "rem_1"
        val nextReminder = reminderRepository.getNextReminder()
        assertNotNull(nextReminder)
        assertEquals("rem_1", nextReminder?.id)
        assertEquals(ReminderStatus.PENDING, nextReminder?.status)

        // Complete the next reminder
        reminderRepository.updateReminderStatus("rem_1", ReminderStatus.COMPLETED)
        val updatedNextReminder = reminderRepository.getNextReminder()
        assertEquals("rem_2", updatedNextReminder?.id)
    }

    // =========================================================================
    // Scenario 5: Authentication, Persistent Session & Sign Out CUJ
    // =========================================================================
    @Test
    fun test_05_authentication_session_persistence_and_signout_scenario() = testScope.runTest {
        // Initial state: Unauthenticated
        assertEquals(AuthState.Unauthenticated, clerkAuthManager.authState.value)

        // Simulate successful sign-in
        val user = ClerkUser(
            id = "user_clerk_12345",
            firstName = "علی",
            email = "ali@example.com",
            avatarUrl = null,
            isGuest = false
        )
        clerkAuthManager.saveUser(user)

        // Verify active state
        val authState = clerkAuthManager.authState.value
        assertTrue(authState is AuthState.Authenticated)
        val authUser = (authState as AuthState.Authenticated).user
        assertEquals("user_clerk_12345", authUser.id)
        assertEquals("علی", authUser.firstName)
        assertEquals("ali@example.com", authUser.email)

        // Simulate app recreation / relaunch
        val newManager = ClerkAuthManager(context, "pk_test_fake_key$")
        val restoredState = newManager.authState.value
        assertTrue(restoredState is AuthState.Authenticated)
        assertEquals("user_clerk_12345", (restoredState as AuthState.Authenticated).user.id)

        // Sign out
        newManager.signOut()
        assertEquals(AuthState.Unauthenticated, newManager.authState.value)

        // Verify local storage is cleared
        val cleanManager = ClerkAuthManager(context, "pk_test_fake_key$")
        assertEquals(AuthState.Unauthenticated, cleanManager.authState.value)
    }

    // =========================================================================
    // Scenario 6: Health Companion Pairing & Inactivity Alert CUJ
    // =========================================================================
    @Test
    fun test_06_health_companion_pairing_and_alert_scenario() = testScope.runTest {
        val companionId = "comp_${UUID.randomUUID()}"
        val connection = HealthCompanionEntity(
            id = companionId,
            userId = "default_user",
            companionUserId = "companion_user_456",
            companionName = "مراقب سلامت",
            status = CompanionConnectionStatus.CONNECTED.name,
            alertPolicy = AlertFilterPolicy.ALL.name,
            lastActiveAt = System.currentTimeMillis(),
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        database.healthCompanionDao().insertOrUpdateConnection(connection)

        // Verify connection is established
        val loaded = database.healthCompanionDao().getActiveCompanion()
        assertNotNull(loaded)
        assertEquals(CompanionConnectionStatus.CONNECTED.name, loaded?.status)
        assertEquals("مراقب سلامت", loaded?.companionName)

        // Simulate inactivity detection triggering a health alert event
        val eventId = "evt_${UUID.randomUUID()}"
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val alertEvent = HealthAlertEventEntity(
            eventId = eventId,
            eventType = HealthEventType.LONG_INACTIVITY.name,
            userId = "default_user",
            timestamp = System.currentTimeMillis(),
            date = todayStr,
            currentWaterMl = 0,
            dailyGoalMl = 2000,
            goalPercentage = 0,
            lastWaterIntakeAt = null,
            missedReminderCount = 3,
            streak = 1,
            severity = AlertSeverity.HIGH.name,
            deliveryStatus = EventDeliveryStatus.PENDING.name,
            sentAt = null,
            acknowledgedAt = null,
            retryCount = 0,
            errorMessage = null
        )
        database.healthAlertEventDao().insertEvent(alertEvent)

        val pendingEvents = database.healthAlertEventDao().getPendingEvents()
        assertEquals(1, pendingEvents.size)
        assertEquals(eventId, pendingEvents[0].eventId)
        assertEquals(AlertSeverity.HIGH.name, pendingEvents[0].severity)

        // Companion acknowledges the alert
        healthCompanionManager.acknowledgeEvent(eventId)
        val eventAfterAck = database.healthAlertEventDao().getEventById(eventId)
        assertEquals(EventDeliveryStatus.ACKNOWLEDGED.name, eventAfterAck?.deliveryStatus)
    }

    // =========================================================================
    // Scenario 7: Offline-First Sync Outbox Queue CUJ
    // =========================================================================
    @Test
    fun test_07_offline_first_sync_outbox_queue_scenario() = testScope.runTest {
        val outboxItem = SyncOutboxEntity(
            id = UUID.randomUUID().toString(),
            userId = "default_user",
            entityType = SyncEntityType.WATER_INTAKE.name,
            entityId = "intake_offline_1",
            operation = SyncOperation.INSERT.name,
            payload = "{\"amountMl\":250}",
            status = SyncOutboxStatus.PENDING.name,
            attemptCount = 0,
            lastError = null,
            createdAt = System.currentTimeMillis(),
            idempotencyKey = UUID.randomUUID().toString()
        )
        database.syncOutboxDao().insertOutbox(outboxItem)

        // Verify outbox has pending items
        val pendingCount = database.syncOutboxDao().getPendingCount()
        assertEquals(1, pendingCount)

        val pending = database.syncOutboxDao().getPendingOutboxEntries()
        assertEquals(1, pending.size)
        assertEquals("intake_offline_1", pending[0].entityId)

        // Mark item as synced
        database.syncOutboxDao().markSynced(outboxItem.id)

        val remainingPending = database.syncOutboxDao().getPendingCount()
        assertEquals(0, remainingPending)
    }

    // =========================================================================
    // Scenario 8: Multi-Cup Container Sizing CUJ
    // =========================================================================
    @Test
    fun test_08_multi_cup_container_sizing_scenario() = testScope.runTest {
        // Cup sizes: 150ml (فنجان), 250ml (لیوان), 500ml (قمقمه), 750ml (بطری)
        val cupAmounts = listOf(150, 250, 500, 750)
        cupAmounts.forEach { amount ->
            addWaterIntakeUseCase(amountMl = amount, source = "preset_cup_$amount")
        }

        val total = waterRepository.getTodayTotalMl()
        // 150 + 250 + 500 + 750 = 1650 ml
        assertEquals(1650, total)

        val dashboard = getDashboardDataUseCase().first()
        assertEquals(1650, dashboard.totalConsumedMl)
        // 1650 / 250 = 6 standard glasses
        assertEquals(6, dashboard.glassesConsumed)
        assertEquals(4, dashboard.recentIntakes.size)
    }

    // =========================================================================
    // Scenario 9: Recharts Native Compose Visualization Rendering & Modes CUJ
    // =========================================================================
    @Test
    fun test_09_recharts_visualization_rendering_and_modes_scenario() {
        val sampleDays = listOf(
            DayIntake(dayName = "شنبه", date = "1403/07/01", amountMl = 1500, goalMl = 2000, isToday = false),
            DayIntake(dayName = "یکشنبه", date = "1403/07/02", amountMl = 2000, goalMl = 2000, isToday = false),
            DayIntake(dayName = "دوشنبه", date = "1403/07/03", amountMl = 2200, goalMl = 2000, isToday = false),
            DayIntake(dayName = "سه‌شنبه", date = "1403/07/04", amountMl = 1800, goalMl = 2000, isToday = false),
            DayIntake(dayName = "چهارشنبه", date = "1403/07/05", amountMl = 2500, goalMl = 2000, isToday = false),
            DayIntake(dayName = "پنج‌شنبه", date = "1403/07/06", amountMl = 1600, goalMl = 2000, isToday = false),
            DayIntake(dayName = "جمعه", date = "1403/07/07", amountMl = 2100, goalMl = 2000, isToday = true)
        )

        composeTestRule.setContent {
            NooshTheme {
                RechartsWeeklyVisualization(
                    days = sampleDays,
                    goalMl = 2000
                )
            }
        }

        // Verify visualization root is rendered
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("recharts_weekly_visualization").assertIsDisplayed()
    }

    // =========================================================================
    // Scenario 10: Gamification (XP, Levels, Badges) & Home Screen Widget CUJ
    // =========================================================================
    @Test
    fun test_10_gamification_and_home_widget_scenario() = testScope.runTest {
        // 1. Initial State: Level 1 (قطره تازه) with 0 XP
        val initialStats = gamificationRepository.getGamificationStats()
        assertEquals(1, initialStats.levelInfo.level)
        assertEquals("قطره تازه", initialStats.levelInfo.titleFa)
        assertEquals(0, initialStats.totalXp)
        assertTrue(initialStats.totalBadgesCount >= 8)

        // 2. Logging 250ml water should award XP and unlock "first_sip" badge
        val result = addWaterIntakeUseCase(amountMl = 250, source = "test_gamification")
        assertTrue(result.xpEarned >= 25)
        assertTrue(result.newTotalXp >= 25)

        val statsAfterIntake = gamificationRepository.getGamificationStats()
        assertTrue(statsAfterIntake.unlockedBadgesCount >= 1)
        val firstSip = statsAfterIntake.badges.find { it.id == "first_sip" }
        assertNotNull(firstSip)
        assertTrue(firstSip?.isUnlocked == true)

        // 3. Level threshold progression
        val level2Info = com.example.domain.gamification.GamificationManager.getLevelInfo(totalXp = 250)
        assertEquals(2, level2Info.level)
        assertEquals("جویبار پویا", level2Info.titleFa)

        val level3Info = com.example.domain.gamification.GamificationManager.getLevelInfo(totalXp = 600)
        assertEquals(3, level3Info.level)
        assertEquals("رود پرآب", level3Info.titleFa)

        // 4. Circular chart widget bitmap generation
        val chartBitmap = com.example.widgets.WaterProgressWidgetProvider.createCircularChartBitmap(percentage = 50)
        assertNotNull(chartBitmap)
        assertEquals(200, chartBitmap.width)
        assertEquals(200, chartBitmap.height)

        // 5. Render LevelProgressCard in Compose
        composeTestRule.setContent {
            NooshTheme {
                com.example.presentation.components.LevelProgressCard(
                    levelInfo = statsAfterIntake.levelInfo,
                    onViewBadgesClick = {}
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("level_progress_card").assertIsDisplayed()
    }
}
