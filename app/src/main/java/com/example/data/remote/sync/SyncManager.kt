package com.example.data.remote.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.example.core.util.DateTimeUtils
import com.example.data.local.room.dao.HealthAlertEventDao
import com.example.data.local.room.dao.SyncOutboxDao
import com.example.data.local.room.dao.WaterIntakeDao
import com.example.data.local.room.entity.SyncEntityType
import com.example.data.local.room.entity.SyncOutboxEntity
import com.example.data.local.room.entity.SyncOutboxStatus
import com.example.data.remote.supabase.SupabaseClient
import com.example.data.remote.supabase.SupabaseHealthSyncEvent
import com.example.data.remote.supabase.SupabaseWaterIntake
import com.example.domain.model.AlertSeverity
import com.example.domain.model.EventDeliveryStatus
import com.example.domain.model.HealthAlertEvent
import com.example.domain.model.HealthEventType
import com.example.domain.repository.HealthRepository
import com.example.domain.repository.SyncRepository
import com.example.domain.repository.UserRepository
import com.example.domain.repository.WaterRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Section 63-75: Offline Data Synchronization & Outbox Engine
 */
class SyncManager(
    private val context: Context,
    private val waterIntakeDao: WaterIntakeDao,
    private val supabaseClient: SupabaseClient,
    private val syncOutboxDao: SyncOutboxDao? = null,
    private val healthAlertEventDao: HealthAlertEventDao? = null,
    private val waterRepository: WaterRepository? = null,
    private val userRepository: UserRepository? = null
) : SyncRepository, HealthRepository {

    var simulatedOnline: Boolean? = null

    override fun isOnline(): Boolean {
        simulatedOnline?.let { return it }
        return checkSystemOnline()
    }

    private fun checkSystemOnline(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    override fun getPendingOutboxCountFlow(): Flow<Int> {
        return syncOutboxDao?.getPendingCountFlow() ?: emptyFlow()
    }

    /**
     * Section 64, 66, 67, 72, 73: Process Outbox with ordering, idempotency, exponential backoff,
     * and partial failure resilience.
     */
    override suspend fun processOutboxSync(): Boolean = withContext(Dispatchers.IO) {
        if (!isOnline() || !supabaseClient.isConfigured) {
            Log.d(TAG, "Device offline or Supabase not configured. Skipping outbox processing.")
            return@withContext false
        }

        val outbox = syncOutboxDao ?: return@withContext syncPendingIntakes()
        val now = System.currentTimeMillis()
        // Section 66: Sync Ordering (createdAt ASC)
        val pendingEntries = outbox.getPendingOutboxEntries(now)
        if (pendingEntries.isEmpty()) {
            Log.d(TAG, "No pending outbox entries to synchronize.")
            return@withContext true
        }

        var allSuccessful = true
        val affectedDates = mutableSetOf<String>()

        for (entry in pendingEntries) {
            // Mark entry SYNCING
            outbox.updateAttempt(
                id = entry.id,
                status = SyncOutboxStatus.SYNCING.name,
                lastAttemptAt = now,
                nextRetryAt = entry.nextRetryAt,
                error = null,
                attemptCount = entry.attemptCount
            )

            val success = try {
                when (entry.entityType) {
                    SyncEntityType.WATER_INTAKE.name -> {
                        val json = JSONObject(entry.payload)
                        val dto = SupabaseWaterIntake(
                            id = json.getString("id"),
                            userId = json.getString("userId"),
                            amountMl = json.getInt("amountMl"),
                            consumedAt = json.getLong("consumedAt"),
                            source = json.optString("source", "app_quick"),
                            createdAt = json.optLong("createdAt", System.currentTimeMillis())
                        )
                        val uploaded = supabaseClient.insertWaterIntake(dto)
                        if (uploaded) {
                            waterIntakeDao.markAsSynced(dto.id)
                            val dateStr = DateTimeUtils.formatDate(dto.consumedAt)
                            affectedDates.add(dateStr)
                        }
                        uploaded
                    }
                    SyncEntityType.HEALTH_ALERT_EVENT.name -> {
                        val json = JSONObject(entry.payload)
                        val event = HealthAlertEvent(
                            eventId = json.getString("eventId"),
                            eventType = HealthEventType.valueOf(json.getString("eventType")),
                            userId = json.getString("userId"),
                            timestamp = json.getLong("timestamp"),
                            date = json.getString("date"),
                            currentWaterMl = json.getInt("currentWaterMl"),
                            dailyGoalMl = json.getInt("dailyGoalMl"),
                            goalPercentage = json.getInt("goalPercentage"),
                            lastWaterIntakeAt = if (json.has("lastWaterIntakeAt") && !json.isNull("lastWaterIntakeAt")) json.getLong("lastWaterIntakeAt") else null,
                            missedReminderCount = json.optInt("missedReminderCount", 0),
                            streak = json.optInt("streak", 1),
                            severity = AlertSeverity.valueOf(json.getString("severity")),
                            deliveryStatus = EventDeliveryStatus.SENT,
                            sentAt = now
                        )
                        val uploaded = supabaseClient.insertHealthAlertEvent(event)
                        if (uploaded) {
                            healthAlertEventDao?.markEventSent(event.eventId, now)
                        }
                        uploaded
                    }
                    SyncEntityType.REMINDER_EVENT.name -> {
                        true
                    }
                    else -> true
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing outbox entry ${entry.id}: ${e.message}")
                false
            }

            if (success) {
                outbox.markSynced(entry.id)
                Log.d(TAG, "Successfully synced outbox entry ${entry.id} (${entry.entityType})")
            } else {
                allSuccessful = false
                val newAttemptCount = entry.attemptCount + 1
                val delay = calculateBackoffDelayMillis(newAttemptCount)
                val nextRetry = now + delay
                val newStatus = if (newAttemptCount >= MAX_ATTEMPTS) {
                    SyncOutboxStatus.FAILED.name
                } else {
                    SyncOutboxStatus.RETRYING.name
                }
                outbox.updateAttempt(
                    id = entry.id,
                    status = newStatus,
                    lastAttemptAt = now,
                    nextRetryAt = nextRetry,
                    error = "Sync attempt failed",
                    attemptCount = newAttemptCount
                )
                Log.w(TAG, "Outbox entry ${entry.id} failed. Attempt: $newAttemptCount, status: $newStatus, nextRetry: $nextRetry")
                // Section 73: Partial Sync Failure - Do not discard or roll back previous successful records!
            }
        }

        // Section 69: Historical Health State & Analytics Recalculation
        if (affectedDates.isNotEmpty() && waterRepository != null) {
            try {
                for (date in affectedDates) {
                    waterRepository.recalculateDailySummary(date)
                }
                val todayTotal = waterRepository.getTodayTotalMl()
                val profile = userRepository?.getUserProfile()
                val goal = profile?.dailyWaterGoalMl ?: 2000
                val streak = waterRepository.calculateStreak()
                syncHealthData(
                    currentDailyIntakeMl = todayTotal,
                    dailyGoalMl = goal,
                    completedReminders = 0,
                    missedReminders = 0,
                    streakDays = streak.currentStreak
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error recalculating historical health state: ${e.message}")
            }
        }

        allSuccessful
    }

    override suspend fun syncPendingIntakes(): Boolean = withContext(Dispatchers.IO) {
        if (!isOnline() || !supabaseClient.isConfigured) {
            Log.d(TAG, "Offline or Supabase not configured. Skipping network sync.")
            return@withContext false
        }

        try {
            val unsynced = waterIntakeDao.getUnsyncedIntakes()
            var allSuccessful = true
            for (item in unsynced) {
                val dto = SupabaseWaterIntake(
                    id = item.id,
                    userId = item.userId,
                    amountMl = item.amountMl,
                    consumedAt = item.consumedAt,
                    source = item.source,
                    createdAt = item.createdAt
                )
                val success = supabaseClient.insertWaterIntake(dto)
                if (success) {
                    waterIntakeDao.markAsSynced(item.id)
                } else {
                    allSuccessful = false
                }
            }
            allSuccessful
        } catch (e: Exception) {
            Log.e(TAG, "Sync failed: ${e.message}")
            false
        }
    }

    // Section 74: Server Reconciliation
    override suspend fun pullRemoteUpdates(): Boolean = withContext(Dispatchers.IO) {
        if (!isOnline() || !supabaseClient.isConfigured) return@withContext false
        try {
            val remoteIntakes = supabaseClient.fetchWaterIntakes("default_user", 0)
            for (remote in remoteIntakes) {
                val local = waterIntakeDao.getIntakeById(remote.id)
                if (local == null) {
                    val entity = com.example.data.local.room.entity.WaterIntakeEntity(
                        id = remote.id,
                        userId = remote.userId,
                        amountMl = remote.amountMl,
                        consumedAt = remote.consumedAt,
                        source = remote.source,
                        createdAt = remote.createdAt,
                        synced = true
                    )
                    waterIntakeDao.insertIntake(entity)
                    val dateStr = DateTimeUtils.formatDate(remote.consumedAt)
                    waterRepository?.recalculateDailySummary(dateStr)
                }
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Reconciliation failed: ${e.message}")
            false
        }
    }

    override suspend fun syncHealthData(
        currentDailyIntakeMl: Int,
        dailyGoalMl: Int,
        completedReminders: Int,
        missedReminders: Int,
        streakDays: Int
    ): Boolean = withContext(Dispatchers.IO) {
        if (!isOnline() || !supabaseClient.isConfigured) return@withContext false
        val event = SupabaseHealthSyncEvent(
            userId = "default_user",
            dailyIntakeMl = currentDailyIntakeMl,
            dailyGoalMl = dailyGoalMl,
            streakDays = streakDays
        )
        supabaseClient.syncHealthEvent(event)
    }

    fun triggerImmediateSync() {
        SyncWorker.enqueueImmediateSync(context)
    }

    /**
     * Section 72: Exponential Backoff Strategy
     * Attempt 1 -> 0s (immediate)
     * Attempt 2 -> 30s
     * Attempt 3 -> 120s (2 minutes)
     * Attempt 4 -> 600s (10 minutes)
     * Attempt 5 -> 1800s (30 minutes)
     */
    fun calculateBackoffDelayMillis(attemptCount: Int): Long {
        return when (attemptCount) {
            0 -> 0L
            1 -> 30_000L
            2 -> 120_000L
            3 -> 600_000L
            4 -> 1_800_000L
            else -> 1_800_000L
        }
    }

    companion object {
        private const val TAG = "SyncManager"
        const val MAX_ATTEMPTS = 5
    }
}
