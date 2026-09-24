package com.example.domain.repository

import com.example.domain.model.DailySummary
import com.example.domain.model.DayIntake
import com.example.domain.model.MonthlyReport
import com.example.domain.model.Reminder
import com.example.domain.model.ReminderStatus
import com.example.domain.model.StreakInfo
import com.example.domain.model.UserProfile
import com.example.domain.model.WaterIntake
import com.example.domain.model.WeeklyReport
import kotlinx.coroutines.flow.Flow

interface WaterRepository {
    fun getTodayIntakesFlow(userId: String = "default_user"): Flow<List<WaterIntake>>
    fun getTodayTotalMlFlow(userId: String = "default_user"): Flow<Int>
    suspend fun addWaterIntake(amountMl: Int, source: String = "app_quick", reminderId: String? = null): WaterIntake
    suspend fun getTodayTotalMl(userId: String = "default_user"): Int
    suspend fun getTodayWaterIntakes(userId: String = "default_user"): List<WaterIntake>
    suspend fun getDailySummary(date: String, userId: String = "default_user"): DailySummary?
    suspend fun recalculateDailySummary(date: String, userId: String = "default_user"): DailySummary?
    fun getWeeklyReportFlow(userId: String = "default_user"): Flow<WeeklyReport>
    suspend fun getMonthlyReport(monthOffset: Int = 0, userId: String = "default_user"): MonthlyReport
    suspend fun calculateStreak(userId: String = "default_user"): StreakInfo
    suspend fun getDaysGoalAchievedCount(userId: String = "default_user"): Int
}

interface UserRepository {
    fun getUserProfileFlow(userId: String = "default_user"): Flow<UserProfile?>
    suspend fun getUserProfile(userId: String = "default_user"): UserProfile
    suspend fun updateProfile(profile: UserProfile)
    suspend fun updateGoal(goalMl: Int, userId: String = "default_user")
    suspend fun setReminderEnabled(enabled: Boolean, userId: String = "default_user")
    suspend fun updateReminderSettings(
        enabled: Boolean,
        intervalMinutes: Int,
        startTime: String,
        endTime: String,
        userId: String = "default_user"
    )
}

interface ReminderRepository {
    fun getAllRemindersFlow(userId: String = "default_user"): Flow<List<Reminder>>
    fun getTodayRemindersFlow(userId: String = "default_user"): Flow<List<Reminder>>
    fun getNextReminderFlow(): Flow<Reminder?>
    suspend fun getTodayReminders(userId: String = "default_user"): List<Reminder>
    suspend fun getReminderById(id: String): Reminder?
    suspend fun getNextReminder(): Reminder?
    suspend fun getLastNotifiedReminder(): Reminder?
    suspend fun updateReminderStatus(id: String, status: ReminderStatus, completedAt: Long? = null)
    suspend fun completeReminder(id: String, amountMl: Int = 250)
    suspend fun snoozeReminder(id: String, snoozeMinutes: Int = 15)
    suspend fun stallReminder(id: String, reason: String, delayMinutes: Int = 15)
    suspend fun scheduleDailyReminders(profile: UserProfile)
    suspend fun insertReminder(reminder: Reminder)
}

interface HealthRepository {
    suspend fun syncHealthData(
        currentDailyIntakeMl: Int,
        dailyGoalMl: Int,
        completedReminders: Int,
        missedReminders: Int,
        streakDays: Int
    ): Boolean
}

interface SyncRepository {
    suspend fun syncPendingIntakes(): Boolean
    suspend fun processOutboxSync(): Boolean
    suspend fun pullRemoteUpdates(): Boolean
    fun isOnline(): Boolean
    fun getPendingOutboxCountFlow(): Flow<Int>
}
