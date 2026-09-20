package com.example.domain.model

import java.util.UUID

enum class HealthEventType {
    WATER_CONSUMED,
    REMINDER_TRIGGERED,
    REMINDER_MISSED,
    REMINDER_SNOOZED,
    GOAL_REACHED,
    LOW_DAILY_PROGRESS,
    LONG_INACTIVITY,
    DAILY_GOAL_MISSED
}

enum class AlertSeverity {
    LOW,
    MEDIUM,
    HIGH
}

enum class EventDeliveryStatus {
    PENDING,
    SENT,
    ACKNOWLEDGED,
    FAILED
}

data class HealthAlertEvent(
    val eventId: String = UUID.randomUUID().toString(),
    val eventType: HealthEventType,
    val userId: String,
    val timestamp: Long = System.currentTimeMillis(),
    val date: String,
    val currentWaterMl: Int,
    val dailyGoalMl: Int,
    val goalPercentage: Int,
    val lastWaterIntakeAt: Long?,
    val missedReminderCount: Int,
    val streak: Int,
    val severity: AlertSeverity,
    val deliveryStatus: EventDeliveryStatus = EventDeliveryStatus.PENDING,
    val sentAt: Long? = null,
    val acknowledgedAt: Long? = null,
    val retryCount: Int = 0,
    val errorMessage: String? = null
)

enum class CompanionConnectionStatus {
    CONNECTED,
    PAUSED,
    DISCONNECTED
}

enum class AlertFilterPolicy {
    ALL,                // LOW, MEDIUM, HIGH
    MEDIUM_AND_HIGH,    // MEDIUM, HIGH
    HIGH_ONLY           // HIGH only
}

enum class HealthStatusEvaluation {
    ON_TRACK,
    BEHIND,
    GOAL_REACHED
}

data class HealthCompanionConnection(
    val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val companionUserId: String,
    val companionName: String,
    val status: CompanionConnectionStatus = CompanionConnectionStatus.CONNECTED,
    val alertPolicy: AlertFilterPolicy = AlertFilterPolicy.ALL,
    val lastActiveAt: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class HealthCompanionStatus(
    val evaluation: HealthStatusEvaluation,
    val todayWaterMl: Int,
    val dailyGoalMl: Int,
    val goalPercentage: Int,
    val lastDrinkTimeAgoMinutes: Long?,
    val completedReminders: Int,
    val missedReminders: Int,
    val currentStreak: Int,
    val connection: HealthCompanionConnection?
)
