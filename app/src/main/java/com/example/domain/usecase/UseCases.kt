package com.example.domain.usecase

import com.clerk.android.Clerk
import com.example.domain.companion.HealthCompanionManager
import com.example.domain.model.AlertSeverity
import com.example.domain.model.HealthEventType
import com.example.domain.model.Reminder
import com.example.domain.model.StreakInfo
import com.example.domain.model.UserProfile
import com.example.domain.model.WaterIntake
import com.example.domain.repository.HealthRepository
import com.example.domain.repository.ReminderRepository
import com.example.domain.repository.SyncRepository
import com.example.domain.repository.UserRepository
import com.example.domain.repository.WaterRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

data class AddWaterResult(
    val intake: WaterIntake,
    val newTotalMl: Int,
    val goalMl: Int,
    val isGoalJustAchieved: Boolean
)

class AddWaterIntakeUseCase(
    private val waterRepository: WaterRepository,
    private val userRepository: UserRepository,
    private val reminderRepository: ReminderRepository,
    private val syncRepository: SyncRepository,
    private val healthRepository: HealthRepository,
    private val healthCompanionManager: HealthCompanionManager? = null
) {
    suspend operator fun invoke(
        amountMl: Int,
        source: String = "app_quick",
        reminderId: String? = null
    ): AddWaterResult {
        val profile = userRepository.getUserProfile()
        val previousTotal = waterRepository.getTodayTotalMl()
        val intake = waterRepository.addWaterIntake(amountMl, source, reminderId)
        val newTotal = previousTotal + amountMl
        val goalMl = profile.dailyWaterGoalMl
        val isGoalJustAchieved = previousTotal < goalMl && newTotal >= goalMl

        // Trigger non-blocking cloud and health sync
        try {
            syncRepository.syncPendingIntakes()
            healthRepository.syncHealthData(
                currentDailyIntakeMl = newTotal,
                dailyGoalMl = goalMl,
                completedReminders = if (reminderId != null) 1 else 0,
                missedReminders = 0,
                streakDays = 1
            )

            // Dispatch Health Companion events
            healthCompanionManager?.let { manager ->
                manager.recordAndDispatchEvent(
                    eventType = HealthEventType.WATER_CONSUMED,
                    severity = AlertSeverity.LOW
                )
                if (isGoalJustAchieved) {
                    manager.recordAndDispatchEvent(
                        eventType = HealthEventType.GOAL_REACHED,
                        severity = AlertSeverity.MEDIUM
                    )
                }
            }
        } catch (ignored: Exception) {}

        return AddWaterResult(
            intake = intake,
            newTotalMl = newTotal,
            goalMl = goalMl,
            isGoalJustAchieved = isGoalJustAchieved
        )
    }
}

data class DashboardState(
    val profile: UserProfile,
    val totalConsumedMl: Int,
    val goalMl: Int,
    val percentage: Int,
    val glassesConsumed: Int,
    val totalGlassesGoal: Int,
    val nextReminder: Reminder?,
    val recentIntakes: List<WaterIntake>,
    val streak: StreakInfo
)

class GetDashboardDataUseCase(
    private val waterRepository: WaterRepository,
    private val userRepository: UserRepository,
    private val reminderRepository: ReminderRepository
) {
    operator fun invoke(): Flow<DashboardState> {
        return combine(
            userRepository.getUserProfileFlow(),
            waterRepository.getTodayTotalMlFlow(),
            waterRepository.getTodayIntakesFlow(),
            reminderRepository.getNextReminderFlow()
        ) { profileOrNull, totalMl, intakes, nextReminder ->
            val profile = profileOrNull ?: UserProfile(
                id = "default_user",
                clerkUserId = Clerk.getUser()?.id ?: "",
                name = Clerk.getUser()?.firstName ?: "کاربر گرامی",
                email = "user@noosh.app",
                profileImageUrl = null,
                dailyWaterGoalMl = 2000,
                reminderIntervalMinutes = 60,
                reminderEnabled = true,
                wakeUpTime = "08:00",
                sleepTime = "23:00",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )

            val goal = profile.dailyWaterGoalMl
            val pct = if (goal > 0) ((totalMl.toFloat() / goal) * 100).toInt().coerceAtMost(100) else 0
            val glasses = (totalMl / 250)
            val goalGlasses = (goal / 250).coerceAtLeast(1)

            DashboardState(
                profile = profile,
                totalConsumedMl = totalMl,
                goalMl = goal,
                percentage = pct,
                glassesConsumed = glasses,
                totalGlassesGoal = goalGlasses,
                nextReminder = nextReminder,
                recentIntakes = intakes,
                streak = StreakInfo(currentStreak = 1, longestStreak = 1, isGraceDayUsed = false)
            )
        }
    }
}
