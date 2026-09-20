package com.example.domain.model

enum class ReminderStatus {
    PENDING,
    NOTIFIED,
    SNOOZED,
    COMPLETED,
    MISSED
}

data class Reminder(
    val id: String,
    val userId: String,
    val scheduledAt: Long,
    val triggeredAt: Long?,
    val completedAt: Long?,
    val status: ReminderStatus,
    val retryCount: Int,
    val nextReminderAt: Long?,
    val amountMl: Int
)

data class WaterIntake(
    val id: String,
    val userId: String,
    val amountMl: Int,
    val consumedAt: Long,
    val source: String,
    val reminderId: String?,
    val createdAt: Long,
    val synced: Boolean,
    val remoteId: String?
)

data class UserProfile(
    val id: String,
    val clerkUserId: String?,
    val name: String,
    val email: String,
    val profileImageUrl: String?,
    val dailyWaterGoalMl: Int,
    val reminderIntervalMinutes: Int,
    val reminderEnabled: Boolean,
    val wakeUpTime: String,
    val sleepTime: String,
    val createdAt: Long,
    val updatedAt: Long,
    val graceDayEnabled: Boolean = true,
    val soundEnabled: Boolean = true,
    val vibrateEnabled: Boolean = true,
    val inactivityThresholdMinutes: Int = 120
)

data class DailySummary(
    val date: String,
    val userId: String,
    val totalConsumedMl: Int,
    val goalMl: Int,
    val percentage: Int,
    val completedReminders: Int,
    val missedReminders: Int
)

data class DayIntake(
    val dayName: String,
    val date: String,
    val amountMl: Int,
    val goalMl: Int,
    val isToday: Boolean = false
)

data class WeeklyReport(
    val days: List<DayIntake>,
    val totalAmountMl: Int,
    val dailyAverageMl: Int,
    val goalCompletionPercentage: Int,
    val trendVsLastWeekPercent: Int
)

data class MonthlyReport(
    val monthTitle: String,
    val totalConsumedMl: Int,
    val dailyAverageMl: Int,
    val goalCompletionRate: Int,
    val bestDayDate: String,
    val bestDayAmountMl: Int,
    val lowestDayDate: String,
    val lowestDayAmountMl: Int,
    val completedReminders: Int,
    val missedReminders: Int,
    val currentStreakDays: Int,
    val longestStreakDays: Int,
    val comparisonWithPrevMonthPercent: Int,
    val dailyIntakes: List<Int>
)

data class StreakInfo(
    val currentStreak: Int,
    val longestStreak: Int,
    val isGraceDayUsed: Boolean
)
