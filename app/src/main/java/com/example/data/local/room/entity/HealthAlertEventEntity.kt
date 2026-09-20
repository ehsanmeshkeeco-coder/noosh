package com.example.data.local.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.domain.model.AlertSeverity
import com.example.domain.model.EventDeliveryStatus
import com.example.domain.model.HealthAlertEvent
import com.example.domain.model.HealthEventType

@Entity(tableName = "health_alert_events")
data class HealthAlertEventEntity(
    @PrimaryKey
    val eventId: String,
    val eventType: String,
    val userId: String,
    val timestamp: Long,
    val date: String,
    val currentWaterMl: Int,
    val dailyGoalMl: Int,
    val goalPercentage: Int,
    val lastWaterIntakeAt: Long?,
    val missedReminderCount: Int,
    val streak: Int,
    val severity: String,
    val deliveryStatus: String,
    val sentAt: Long?,
    val acknowledgedAt: Long?,
    val retryCount: Int,
    val errorMessage: String?
) {
    fun toDomain(): HealthAlertEvent {
        return HealthAlertEvent(
            eventId = eventId,
            eventType = try { HealthEventType.valueOf(eventType) } catch (e: Exception) { HealthEventType.WATER_CONSUMED },
            userId = userId,
            timestamp = timestamp,
            date = date,
            currentWaterMl = currentWaterMl,
            dailyGoalMl = dailyGoalMl,
            goalPercentage = goalPercentage,
            lastWaterIntakeAt = lastWaterIntakeAt,
            missedReminderCount = missedReminderCount,
            streak = streak,
            severity = try { AlertSeverity.valueOf(severity) } catch (e: Exception) { AlertSeverity.LOW },
            deliveryStatus = try { EventDeliveryStatus.valueOf(deliveryStatus) } catch (e: Exception) { EventDeliveryStatus.PENDING },
            sentAt = sentAt,
            acknowledgedAt = acknowledgedAt,
            retryCount = retryCount,
            errorMessage = errorMessage
        )
    }

    companion object {
        fun fromDomain(event: HealthAlertEvent): HealthAlertEventEntity {
            return HealthAlertEventEntity(
                eventId = event.eventId,
                eventType = event.eventType.name,
                userId = event.userId,
                timestamp = event.timestamp,
                date = event.date,
                currentWaterMl = event.currentWaterMl,
                dailyGoalMl = event.dailyGoalMl,
                goalPercentage = event.goalPercentage,
                lastWaterIntakeAt = event.lastWaterIntakeAt,
                missedReminderCount = event.missedReminderCount,
                streak = event.streak,
                severity = event.severity.name,
                deliveryStatus = event.deliveryStatus.name,
                sentAt = event.sentAt,
                acknowledgedAt = event.acknowledgedAt,
                retryCount = event.retryCount,
                errorMessage = event.errorMessage
            )
        }
    }
}
