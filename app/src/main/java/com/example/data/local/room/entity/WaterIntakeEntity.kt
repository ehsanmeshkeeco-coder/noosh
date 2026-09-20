package com.example.data.local.room.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.domain.model.WaterIntake
import java.util.UUID

@Entity(
    tableName = "water_intakes",
    indices = [
        Index(value = ["userId"]),
        Index(value = ["consumedAt"]),
        Index(value = ["synced"])
    ]
)
data class WaterIntakeEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String = "default_user",
    val amountMl: Int,
    val consumedAt: Long = System.currentTimeMillis(),
    val source: String = "app_quick",
    val reminderId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val synced: Boolean = false,
    val remoteId: String? = null
) {
    fun toDomain(): WaterIntake = WaterIntake(
        id = id,
        userId = userId,
        amountMl = amountMl,
        consumedAt = consumedAt,
        source = source,
        reminderId = reminderId,
        createdAt = createdAt,
        synced = synced,
        remoteId = remoteId
    )

    companion object {
        fun fromDomain(domain: WaterIntake): WaterIntakeEntity = WaterIntakeEntity(
            id = domain.id,
            userId = domain.userId,
            amountMl = domain.amountMl,
            consumedAt = domain.consumedAt,
            source = domain.source,
            reminderId = domain.reminderId,
            createdAt = domain.createdAt,
            synced = domain.synced,
            remoteId = domain.remoteId
        )
    }
}
