package com.example.data.local.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.room.entity.ReminderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders WHERE userId = :userId ORDER BY scheduledAt ASC")
    fun getAllRemindersFlow(userId: String = "default_user"): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE userId = :userId AND scheduledAt >= :startTime AND scheduledAt < :endTime ORDER BY scheduledAt ASC")
    fun getRemindersForDayFlow(userId: String, startTime: Long, endTime: Long): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE userId = :userId AND scheduledAt >= :startTime AND scheduledAt < :endTime ORDER BY scheduledAt ASC")
    suspend fun getRemindersForDay(userId: String, startTime: Long, endTime: Long): List<ReminderEntity>

    @Query("SELECT * FROM reminders WHERE id = :id LIMIT 1")
    suspend fun getReminderById(id: String): ReminderEntity?

    @Query("SELECT * FROM reminders WHERE status IN ('PENDING', 'SNOOZED', 'NOTIFIED') AND scheduledAt > :now ORDER BY scheduledAt ASC LIMIT 1")
    fun getNextUpcomingReminderFlow(now: Long = System.currentTimeMillis()): Flow<ReminderEntity?>

    @Query("SELECT * FROM reminders WHERE status IN ('PENDING', 'SNOOZED', 'NOTIFIED') AND scheduledAt > :now ORDER BY scheduledAt ASC LIMIT 1")
    suspend fun getNextUpcomingReminder(now: Long = System.currentTimeMillis()): ReminderEntity?

    @Query("SELECT * FROM reminders WHERE status = 'NOTIFIED' ORDER BY triggeredAt DESC LIMIT 1")
    suspend fun getLastNotifiedReminder(): ReminderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: ReminderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminders(reminders: List<ReminderEntity>)

    @Update
    suspend fun updateReminder(reminder: ReminderEntity)

    @Query("UPDATE reminders SET status = :status, completedAt = :completedAt WHERE id = :id")
    suspend fun updateReminderStatus(id: String, status: String, completedAt: Long?)

    @Query("DELETE FROM reminders WHERE scheduledAt >= :startTime AND status = 'PENDING'")
    suspend fun deletePendingRemindersAfter(startTime: Long)

    @Query("DELETE FROM reminders WHERE userId = :userId")
    suspend fun clearRemindersForUser(userId: String)
}
