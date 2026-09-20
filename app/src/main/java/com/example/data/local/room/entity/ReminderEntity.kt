package com.example.data.local.room.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.domain.model.Reminder
import com.example.domain.model.ReminderStatus
import java.util.UUID

@Entity(
    tableName = "reminders",
    indices = [
        Index(value = ["userId"]),
        Index(value = ["scheduledAt"]),
        Index(value = ["status"])
    ]
)
data class ReminderEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String = "default_user",
    val scheduledAt: Long,
    val triggeredAt: Long? = null,
    val completedAt: Long? = null,
    val status: String = ReminderStatus.PENDING.name,
    val retryCount: Int = 0,
    val nextReminderAt: Long? = null,
    val amountMl: Int = 250
) {
    fun toDomain(): Reminder = Reminder(
        id = id,
        userId = userId,
        scheduledAt = scheduledAt,
        triggeredAt = triggeredAt,
        completedAt = completedAt,
        status = try {
            ReminderStatus.valueOf(status)
        } catch (e: Exception) {
            ReminderStatus.PENDING
        },
        retryCount = retryCount,
        nextReminderAt = nextReminderAt,
        amountMl = amountMl
    )

    companion object {
        fun fromDomain(domain: Reminder): ReminderEntity = ReminderEntity(
            id = domain.id,
            userId = domain.userId,
            scheduledAt = domain.scheduledAt,
            triggeredAt = domain.triggeredAt,
            completedAt = domain.completedAt,
            status = domain.status.name,
            retryCount = domain.retryCount,
            nextReminderAt = domain.nextReminderAt,
            amountMl = domain.amountMl
        )
    }
}
