package com.example.data.local.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.domain.model.AlertFilterPolicy
import com.example.domain.model.CompanionConnectionStatus
import com.example.domain.model.HealthCompanionConnection

@Entity(tableName = "health_companion_connections")
data class HealthCompanionEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val companionUserId: String,
    val companionName: String,
    val status: String,
    val alertPolicy: String,
    val lastActiveAt: Long,
    val createdAt: Long,
    val updatedAt: Long
) {
    fun toDomain(): HealthCompanionConnection {
        return HealthCompanionConnection(
            id = id,
            userId = userId,
            companionUserId = companionUserId,
            companionName = companionName,
            status = try { CompanionConnectionStatus.valueOf(status) } catch (e: Exception) { CompanionConnectionStatus.CONNECTED },
            alertPolicy = try { AlertFilterPolicy.valueOf(alertPolicy) } catch (e: Exception) { AlertFilterPolicy.ALL },
            lastActiveAt = lastActiveAt,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    companion object {
        fun fromDomain(companion: HealthCompanionConnection): HealthCompanionEntity {
            return HealthCompanionEntity(
                id = companion.id,
                userId = companion.userId,
                companionUserId = companion.companionUserId,
                companionName = companion.companionName,
                status = companion.status.name,
                alertPolicy = companion.alertPolicy.name,
                lastActiveAt = companion.lastActiveAt,
                createdAt = companion.createdAt,
                updatedAt = companion.updatedAt
            )
        }
    }
}
