package com.example.data.local.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.room.entity.HealthAlertEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HealthAlertEventDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: HealthAlertEventEntity)

    @Update
    suspend fun updateEvent(event: HealthAlertEventEntity)

    @Query("SELECT * FROM health_alert_events WHERE eventId = :eventId LIMIT 1")
    suspend fun getEventById(eventId: String): HealthAlertEventEntity?

    @Query("SELECT * FROM health_alert_events WHERE deliveryStatus = 'PENDING' ORDER BY timestamp ASC")
    suspend fun getPendingEvents(): List<HealthAlertEventEntity>

    @Query("SELECT * FROM health_alert_events ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentEventsFlow(limit: Int = 30): Flow<List<HealthAlertEventEntity>>

    @Query("SELECT * FROM health_alert_events ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentEvents(limit: Int = 30): List<HealthAlertEventEntity>

    @Query("UPDATE health_alert_events SET deliveryStatus = 'SENT', sentAt = :sentAt WHERE eventId = :eventId AND deliveryStatus != 'ACKNOWLEDGED'")
    suspend fun markEventSent(eventId: String, sentAt: Long)

    @Query("UPDATE health_alert_events SET deliveryStatus = 'ACKNOWLEDGED', acknowledgedAt = :ackAt WHERE eventId = :eventId")
    suspend fun markEventAcknowledged(eventId: String, ackAt: Long)

    @Query("UPDATE health_alert_events SET deliveryStatus = 'FAILED', retryCount = retryCount + 1, errorMessage = :error WHERE eventId = :eventId AND deliveryStatus != 'ACKNOWLEDGED'")
    suspend fun markEventFailed(eventId: String, error: String)

    @Query("DELETE FROM health_alert_events WHERE timestamp < :olderThanTimestamp AND deliveryStatus = 'ACKNOWLEDGED'")
    suspend fun cleanOldAcknowledgedEvents(olderThanTimestamp: Long)
}
