package com.example.data.local.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.room.entity.DailyWaterSummaryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyWaterSummaryDao {
    @Query("SELECT * FROM daily_water_summaries WHERE date = :date AND userId = :userId LIMIT 1")
    fun getSummaryForDateFlow(date: String, userId: String = "default_user"): Flow<DailyWaterSummaryEntity?>

    @Query("SELECT * FROM daily_water_summaries WHERE date = :date AND userId = :userId LIMIT 1")
    suspend fun getSummaryForDate(date: String, userId: String = "default_user"): DailyWaterSummaryEntity?

    @Query("SELECT * FROM daily_water_summaries WHERE userId = :userId AND date >= :startDate AND date <= :endDate ORDER BY date ASC")
    fun getSummariesBetweenFlow(userId: String, startDate: String, endDate: String): Flow<List<DailyWaterSummaryEntity>>

    @Query("SELECT * FROM daily_water_summaries WHERE userId = :userId AND date >= :startDate AND date <= :endDate ORDER BY date ASC")
    suspend fun getSummariesBetween(userId: String, startDate: String, endDate: String): List<DailyWaterSummaryEntity>

    @Query("SELECT * FROM daily_water_summaries WHERE userId = :userId ORDER BY date DESC LIMIT :limit")
    suspend fun getRecentSummaries(userId: String, limit: Int = 30): List<DailyWaterSummaryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateSummary(summary: DailyWaterSummaryEntity)

    @Update
    suspend fun updateSummary(summary: DailyWaterSummaryEntity)
}
