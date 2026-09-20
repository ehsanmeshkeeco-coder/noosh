package com.example.data.local.room.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

enum class SyncOutboxStatus {
    PENDING,
    SYNCING,
    SYNCED,
    FAILED,
    RETRYING
}

enum class SyncEntityType {
    WATER_INTAKE,
    REMINDER_EVENT,
    HEALTH_ALERT_EVENT,
    USER_PROFILE
}

enum class SyncOperation {
    INSERT,
    UPDATE,
    DELETE
}

/**
 * Section 64: Sync Outbox Pattern
 * Every important offline-capable operation must create an Outbox record inside
 * the same Room transaction as the original data change.
 */
@Entity(
    tableName = "sync_outbox",
    indices = [
        Index(value = ["status", "nextRetryAt", "createdAt"]),
        Index(value = ["idempotencyKey"], unique = true)
    ]
)
data class SyncOutboxEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val entityType: String, // SyncEntityType.name
    val entityId: String,
    val operation: String, // SyncOperation.name
    val payload: String, // JSON
    val createdAt: Long = System.currentTimeMillis(),
    val attemptCount: Int = 0,
    val lastAttemptAt: Long? = null,
    val nextRetryAt: Long = createdAt,
    val status: String = SyncOutboxStatus.PENDING.name,
    val idempotencyKey: String,
    val lastError: String? = null
)
