package com.example.data.local.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.domain.model.DailySummary

@Entity(tableName = "daily_water_summaries")
data class DailyWaterSummaryEntity(
    @PrimaryKey val date: String, // Format: YYYY-MM-DD
    val userId: String = "default_user",
    val totalConsumedMl: Int = 0,
    val goalMl: Int = 2000,
    val percentage: Int = 0,
    val completedReminders: Int = 0,
    val missedReminders: Int = 0
) {
    fun toDomain(): DailySummary = DailySummary(
        date = date,
        userId = userId,
        totalConsumedMl = totalConsumedMl,
        goalMl = goalMl,
        percentage = percentage,
        completedReminders = completedReminders,
        missedReminders = missedReminders
    )

    companion object {
        fun fromDomain(domain: DailySummary): DailyWaterSummaryEntity = DailyWaterSummaryEntity(
            date = domain.date,
            userId = domain.userId,
            totalConsumedMl = domain.totalConsumedMl,
            goalMl = domain.goalMl,
            percentage = domain.percentage,
            completedReminders = domain.completedReminders,
            missedReminders = domain.missedReminders
        )
    }
}
