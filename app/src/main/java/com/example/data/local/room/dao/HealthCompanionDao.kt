package com.example.data.local.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.room.entity.HealthCompanionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HealthCompanionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateConnection(companion: HealthCompanionEntity)

    @Update
    suspend fun updateConnection(companion: HealthCompanionEntity)

    @Query("SELECT * FROM health_companion_connections WHERE status != 'DISCONNECTED' ORDER BY updatedAt DESC LIMIT 1")
    fun getActiveCompanionFlow(): Flow<HealthCompanionEntity?>

    @Query("SELECT * FROM health_companion_connections WHERE status != 'DISCONNECTED' ORDER BY updatedAt DESC LIMIT 1")
    suspend fun getActiveCompanion(): HealthCompanionEntity?

    @Query("SELECT * FROM health_companion_connections ORDER BY updatedAt DESC")
    suspend fun getAllConnections(): List<HealthCompanionEntity>

    @Query("UPDATE health_companion_connections SET status = 'DISCONNECTED', updatedAt = :time WHERE id = :id")
    suspend fun disconnectCompanion(id: String, time: Long = System.currentTimeMillis())

    @Query("UPDATE health_companion_connections SET status = :status, updatedAt = :time WHERE id = :id")
    suspend fun updateStatus(id: String, status: String, time: Long = System.currentTimeMillis())

    @Query("UPDATE health_companion_connections SET alertPolicy = :policy, updatedAt = :time WHERE id = :id")
    suspend fun updateAlertPolicy(id: String, policy: String, time: Long = System.currentTimeMillis())
}
