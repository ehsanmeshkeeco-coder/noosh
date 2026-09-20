package com.example.data.local.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.room.entity.SyncOutboxEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncOutboxDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOutbox(entry: SyncOutboxEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOutboxList(entries: List<SyncOutboxEntity>)

    @Update
    suspend fun updateOutbox(entry: SyncOutboxEntity)

    /**
     * Section 66: Sync Ordering (createdAt ASC)
     * Fetches entries that are ready for sync/retry based on nextRetryAt <= now.
     */
    @Query("SELECT * FROM sync_outbox WHERE status IN ('PENDING', 'RETRYING') AND nextRetryAt <= :now ORDER BY createdAt ASC")
    suspend fun getPendingOutboxEntries(now: Long = System.currentTimeMillis()): List<SyncOutboxEntity>

    @Query("SELECT * FROM sync_outbox WHERE status IN ('PENDING', 'RETRYING') ORDER BY createdAt ASC")
    suspend fun getAllPendingOrRetryingEntries(): List<SyncOutboxEntity>

    @Query("SELECT * FROM sync_outbox WHERE id = :id")
    suspend fun getEntryById(id: String): SyncOutboxEntity?

    @Query("SELECT * FROM sync_outbox WHERE idempotencyKey = :key")
    suspend fun getEntryByIdempotencyKey(key: String): SyncOutboxEntity?

    @Query("UPDATE sync_outbox SET status = :status, lastAttemptAt = :lastAttemptAt, attemptCount = :attemptCount, lastError = :error, nextRetryAt = :nextRetryAt WHERE id = :id")
    suspend fun updateAttempt(
        id: String,
        status: String,
        lastAttemptAt: Long,
        nextRetryAt: Long,
        error: String?,
        attemptCount: Int
    )

    @Query("UPDATE sync_outbox SET status = 'SYNCED', lastError = null WHERE id = :id")
    suspend fun markSynced(id: String)

    @Query("SELECT COUNT(*) FROM sync_outbox WHERE status IN ('PENDING', 'RETRYING')")
    suspend fun getPendingCount(): Int

    @Query("SELECT COUNT(*) FROM sync_outbox WHERE status IN ('PENDING', 'RETRYING')")
    fun getPendingCountFlow(): Flow<Int>

    @Query("SELECT * FROM sync_outbox ORDER BY createdAt DESC LIMIT :limit")
    suspend fun getRecentEntries(limit: Int = 50): List<SyncOutboxEntity>

    @Query("SELECT * FROM sync_outbox ORDER BY createdAt ASC")
    suspend fun getAllEntries(): List<SyncOutboxEntity>

    @Query("DELETE FROM sync_outbox WHERE status = 'SYNCED' AND createdAt < :olderThanTimestamp")
    suspend fun deleteSyncedOlderThan(olderThanTimestamp: Long)
}
