package com.example.data.local.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.room.entity.WaterIntakeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WaterIntakeDao {
    @Query("SELECT * FROM water_intakes WHERE userId = :userId ORDER BY consumedAt DESC")
    fun getAllWaterIntakes(userId: String = "default_user"): Flow<List<WaterIntakeEntity>>

    @Query("SELECT * FROM water_intakes WHERE userId = :userId AND consumedAt >= :startTime AND consumedAt < :endTime ORDER BY consumedAt DESC")
    fun getIntakesBetweenFlow(userId: String, startTime: Long, endTime: Long): Flow<List<WaterIntakeEntity>>

    @Query("SELECT * FROM water_intakes WHERE userId = :userId AND consumedAt >= :startTime AND consumedAt < :endTime ORDER BY consumedAt DESC")
    suspend fun getIntakesBetween(userId: String, startTime: Long, endTime: Long): List<WaterIntakeEntity>

    @Query("SELECT COALESCE(SUM(amountMl), 0) FROM water_intakes WHERE userId = :userId AND consumedAt >= :startTime AND consumedAt < :endTime")
    fun getTotalIntakeBetweenFlow(userId: String, startTime: Long, endTime: Long): Flow<Int>

    @Query("SELECT COALESCE(SUM(amountMl), 0) FROM water_intakes WHERE userId = :userId AND consumedAt >= :startTime AND consumedAt < :endTime")
    suspend fun getTotalIntakeBetween(userId: String, startTime: Long, endTime: Long): Int

    @Query("SELECT * FROM water_intakes WHERE synced = 0")
    suspend fun getUnsyncedIntakes(): List<WaterIntakeEntity>

    @Query("SELECT * FROM water_intakes WHERE id = :id")
    suspend fun getIntakeById(id: String): WaterIntakeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIntake(intake: WaterIntakeEntity)

    @Update
    suspend fun updateIntake(intake: WaterIntakeEntity)

    @Query("UPDATE water_intakes SET synced = 1 WHERE id = :id")
    suspend fun markAsSynced(id: String)

    @Query("DELETE FROM water_intakes WHERE id = :id")
    suspend fun deleteIntake(id: String)
}
