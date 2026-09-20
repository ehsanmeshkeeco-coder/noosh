package com.example.domain.companion

import android.util.Log
import androidx.room.withTransaction
import com.example.core.util.DateTimeUtils
import com.example.data.local.room.dao.HealthAlertEventDao
import com.example.data.local.room.dao.HealthCompanionDao
import com.example.data.local.room.dao.SyncOutboxDao
import com.example.data.local.room.database.NooshDatabase
import com.example.data.local.room.entity.HealthAlertEventEntity
import com.example.data.local.room.entity.HealthCompanionEntity
import com.example.data.local.room.entity.SyncEntityType
import com.example.data.local.room.entity.SyncOperation
import com.example.data.local.room.entity.SyncOutboxEntity
import com.example.data.local.room.entity.SyncOutboxStatus
import com.example.data.remote.supabase.SupabaseClient
import com.example.domain.manager.InactivityDetector
import com.example.domain.model.AlertFilterPolicy
import com.example.domain.model.AlertSeverity
import com.example.domain.model.CompanionConnectionStatus
import com.example.domain.model.EventDeliveryStatus
import com.example.domain.model.HealthAlertEvent
import com.example.domain.model.HealthCompanionConnection
import com.example.domain.model.HealthCompanionStatus
import com.example.domain.model.HealthEventType
import com.example.domain.model.HealthStatusEvaluation
import com.example.domain.repository.ReminderRepository
import com.example.domain.repository.UserRepository
import com.example.domain.repository.WaterRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.Calendar
import java.util.UUID

class HealthCompanionManager(
    private val healthAlertEventDao: HealthAlertEventDao,
    private val healthCompanionDao: HealthCompanionDao,
    private val supabaseClient: SupabaseClient,
    private val waterRepository: WaterRepository,
    private val userRepository: UserRepository,
    private val reminderRepository: ReminderRepository,
    private val inactivityDetector: InactivityDetector,
    private val scope: CoroutineScope,
    private val syncOutboxDao: SyncOutboxDao? = null,
    private val database: NooshDatabase? = null
) {

    fun getRecentEventsFlow(limit: Int = 20): Flow<List<HealthAlertEvent>> {
        return healthAlertEventDao.getRecentEventsFlow(limit).map { list ->
            list.map { it.toDomain() }
        }
    }

    fun getActiveCompanionFlow(): Flow<HealthCompanionConnection?> {
        return healthCompanionDao.getActiveCompanionFlow().map { it?.toDomain() }
    }

    suspend fun getCompanionStatus(): HealthCompanionStatus {
        val todayIntakes = waterRepository.getTodayWaterIntakes()
        val totalWater = todayIntakes.sumOf { it.amountMl }
        val profile = userRepository.getUserProfile()
        val goal = profile.dailyWaterGoalMl.coerceAtLeast(1)
        val percentage = ((totalWater.toFloat() / goal) * 100).toInt()

        val reminders = reminderRepository.getTodayReminders()
        val completedReminders = reminders.count { it.status == com.example.domain.model.ReminderStatus.COMPLETED }
        val missedReminders = reminders.count { it.status == com.example.domain.model.ReminderStatus.MISSED }

        val lastIntake = todayIntakes.maxByOrNull { it.consumedAt }
        val lastDrinkTimeAgoMinutes: Long? = lastIntake?.let {
            val diff = (System.currentTimeMillis() - it.consumedAt).coerceAtLeast(0L)
            diff / (1000L * 60L)
        }

        val evaluation = when {
            totalWater >= goal -> HealthStatusEvaluation.GOAL_REACHED
            missedReminders >= 1 || (lastDrinkTimeAgoMinutes != null && lastDrinkTimeAgoMinutes > 120) -> HealthStatusEvaluation.BEHIND
            else -> HealthStatusEvaluation.ON_TRACK
        }

        val connection = healthCompanionDao.getActiveCompanion()?.toDomain()

        return HealthCompanionStatus(
            evaluation = evaluation,
            todayWaterMl = totalWater,
            dailyGoalMl = goal,
            goalPercentage = percentage,
            lastDrinkTimeAgoMinutes = lastDrinkTimeAgoMinutes,
            completedReminders = completedReminders,
            missedReminders = missedReminders,
            currentStreak = 1,
            connection = connection
        )
    }

    // Section 49 & 50: Create and reliably dispatch Health Event
    suspend fun recordAndDispatchEvent(
        eventType: HealthEventType,
        severity: AlertSeverity,
        missedReminderCount: Int = 0
    ): HealthAlertEvent {
        val todayIntakes = waterRepository.getTodayWaterIntakes()
        val totalWater = todayIntakes.sumOf { it.amountMl }
        val profile = userRepository.getUserProfile()
        val goal = profile.dailyWaterGoalMl.coerceAtLeast(1)
        val percentage = ((totalWater.toFloat() / goal) * 100).toInt()
        val lastIntake = todayIntakes.maxByOrNull { it.consumedAt }

        val event = HealthAlertEvent(
            eventId = UUID.randomUUID().toString(),
            eventType = eventType,
            userId = profile.id,
            timestamp = System.currentTimeMillis(),
            date = DateTimeUtils.getTodayDateString(),
            currentWaterMl = totalWater,
            dailyGoalMl = goal,
            goalPercentage = percentage,
            lastWaterIntakeAt = lastIntake?.consumedAt,
            missedReminderCount = missedReminderCount,
            streak = 1,
            severity = severity,
            deliveryStatus = EventDeliveryStatus.PENDING
        )

        // 1. Persist locally first (Offline-First and Audit Trail) with atomic Outbox (Section 64)
        if (database != null) {
            database.withTransaction {
                persistEventWithOutbox(event)
            }
        } else {
            persistEventWithOutbox(event)
        }

        // 2. Check if companion is connected & allows this severity
        val activeCompanion = healthCompanionDao.getActiveCompanion()?.toDomain()
        val shouldDispatch = if (activeCompanion != null && activeCompanion.status == CompanionConnectionStatus.CONNECTED) {
            when (activeCompanion.alertPolicy) {
                AlertFilterPolicy.ALL -> true
                AlertFilterPolicy.MEDIUM_AND_HIGH -> severity != AlertSeverity.LOW
                AlertFilterPolicy.HIGH_ONLY -> severity == AlertSeverity.HIGH
            }
        } else {
            false // No authorized companion or alerts paused
        }

        // 3. Dispatch to remote Supabase backend / Edge Function
        if (shouldDispatch) {
            scope.launch {
                dispatchRemoteEvent(event)
            }
        }

        return event
    }

    private suspend fun persistEventWithOutbox(event: HealthAlertEvent) {
        healthAlertEventDao.insertEvent(HealthAlertEventEntity.fromDomain(event))
        syncOutboxDao?.let { outbox ->
            val payload = JSONObject().apply {
                put("eventId", event.eventId)
                put("eventType", event.eventType.name)
                put("userId", event.userId)
                put("timestamp", event.timestamp)
                put("date", event.date)
                put("currentWaterMl", event.currentWaterMl)
                put("dailyGoalMl", event.dailyGoalMl)
                put("goalPercentage", event.goalPercentage)
                put("lastWaterIntakeAt", event.lastWaterIntakeAt)
                put("missedReminderCount", event.missedReminderCount)
                put("streak", event.streak)
                put("severity", event.severity.name)
            }.toString()

            val outboxEntity = SyncOutboxEntity(
                id = UUID.randomUUID().toString(),
                userId = event.userId,
                entityType = SyncEntityType.HEALTH_ALERT_EVENT.name,
                entityId = event.eventId,
                operation = SyncOperation.INSERT.name,
                payload = payload,
                createdAt = event.timestamp,
                attemptCount = 0,
                lastAttemptAt = null,
                nextRetryAt = event.timestamp,
                status = SyncOutboxStatus.PENDING.name,
                idempotencyKey = "${event.userId}:HEALTH_ALERT_EVENT:${event.eventId}",
                lastError = null
            )
            outbox.insertOutbox(outboxEntity)
        }
    }

    private suspend fun dispatchRemoteEvent(event: HealthAlertEvent) = withContext(Dispatchers.IO) {
        try {
            val success = supabaseClient.insertHealthAlertEvent(event)
            if (success) {
                healthAlertEventDao.markEventSent(event.eventId, System.currentTimeMillis())
                Log.d(TAG, "Event ${event.eventId} (${event.eventType}) dispatched to Supabase backend successfully.")
            } else {
                healthAlertEventDao.markEventFailed(event.eventId, "Remote call unsuccessful, will retry.")
            }
        } catch (e: Exception) {
            healthAlertEventDao.markEventFailed(event.eventId, e.message ?: "Unknown error")
            Log.e(TAG, "Failed to dispatch event to Supabase: ${e.message}")
        }
    }

    // Section 54: Idempotent Event Acknowledgement
    suspend fun acknowledgeEvent(eventId: String): Boolean {
        val existing = healthAlertEventDao.getEventById(eventId)
        if (existing != null && existing.deliveryStatus == EventDeliveryStatus.ACKNOWLEDGED.name) {
            // Already acknowledged, idempotent return true
            return true
        }

        val ackTime = System.currentTimeMillis()
        healthAlertEventDao.markEventAcknowledged(eventId, ackTime)
        supabaseClient.acknowledgeHealthAlertEvent(eventId)
        return true
    }

    // Section 56: Check for inactivity and trigger LONG_INACTIVITY if needed
    suspend fun checkAndTriggerInactivity(): Boolean {
        val profile = userRepository.getUserProfile()
        val todayIntakes = waterRepository.getTodayWaterIntakes()
        val lastIntake = todayIntakes.maxByOrNull { it.consumedAt }

        val result = inactivityDetector.checkInactivity(
            lastWaterIntakeAt = lastIntake?.consumedAt,
            thresholdMinutes = profile.inactivityThresholdMinutes,
            wakeUpTime = profile.wakeUpTime,
            sleepTime = profile.sleepTime
        )

        if (result.isInactive) {
            recordAndDispatchEvent(
                eventType = HealthEventType.LONG_INACTIVITY,
                severity = AlertSeverity.HIGH
            )
            return true
        }
        return false
    }

    // Section 58: Privacy & Connection Controls
    suspend fun connectCompanion(
        companionUserId: String,
        companionName: String,
        policy: AlertFilterPolicy = AlertFilterPolicy.ALL
    ): HealthCompanionConnection {
        val profile = userRepository.getUserProfile()
        val connection = HealthCompanionConnection(
            id = UUID.randomUUID().toString(),
            userId = profile.id,
            companionUserId = companionUserId,
            companionName = companionName,
            status = CompanionConnectionStatus.CONNECTED,
            alertPolicy = policy,
            lastActiveAt = System.currentTimeMillis(),
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        healthCompanionDao.insertOrUpdateConnection(HealthCompanionEntity.fromDomain(connection))
        supabaseClient.syncCompanionConnection(connection)
        return connection
    }

    suspend fun disconnectCompanion(connectionId: String) {
        healthCompanionDao.disconnectCompanion(connectionId)
    }

    suspend fun pauseAlerts(connectionId: String, isPaused: Boolean) {
        val newStatus = if (isPaused) CompanionConnectionStatus.PAUSED else CompanionConnectionStatus.CONNECTED
        healthCompanionDao.updateStatus(connectionId, newStatus.name)
    }

    suspend fun updateAlertPolicy(connectionId: String, policy: AlertFilterPolicy) {
        healthCompanionDao.updateAlertPolicy(connectionId, policy.name)
    }

    suspend fun createInviteRoom(): String {
        val profile = userRepository.getUserProfile()
        val roomCode = (100000..999999).random().toString()
        supabaseClient.createCompanionRoom(
            userId = profile.id,
            hostName = profile.name,
            roomCode = roomCode
        )
        return roomCode
    }

    suspend fun joinInviteRoom(roomCode: String, companionName: String): HealthCompanionConnection {
        val profile = userRepository.getUserProfile()
        val connection = supabaseClient.joinCompanionRoom(
            roomCode = roomCode,
            companionUserId = profile.id,
            companionName = companionName.ifBlank { "همراه سلامت" }
        ) ?: HealthCompanionConnection(
            id = UUID.randomUUID().toString(),
            userId = profile.id,
            companionUserId = "host_$roomCode",
            companionName = companionName.ifBlank { "همراه سلامت" },
            status = CompanionConnectionStatus.CONNECTED,
            alertPolicy = AlertFilterPolicy.ALL,
            lastActiveAt = System.currentTimeMillis(),
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        healthCompanionDao.insertOrUpdateConnection(HealthCompanionEntity.fromDomain(connection))
        return connection
    }

    // Section 54 & 59: Retry pending events
    suspend fun retryPendingEvents() {
        val pending = healthAlertEventDao.getPendingEvents()
        for (eventEntity in pending) {
            val event = eventEntity.toDomain()
            dispatchRemoteEvent(event)
        }
    }

    // Section 70: Health Companion Catch-Up
    // If Companion was offline when events occurred, it catches up from Supabase (source of truth)
    suspend fun catchUpFromSupabase(userId: String): Int = withContext(Dispatchers.IO) {
        if (!supabaseClient.isConfigured) return@withContext 0
        try {
            val unacknowledgedEvents = supabaseClient.fetchUnacknowledgedEvents(userId)
            var count = 0
            for (remoteEvent in unacknowledgedEvents) {
                val existing = healthAlertEventDao.getEventById(remoteEvent.eventId)
                if (existing == null) {
                    healthAlertEventDao.insertEvent(HealthAlertEventEntity.fromDomain(remoteEvent))
                }
                val ackSuccess = supabaseClient.acknowledgeHealthAlertEvent(remoteEvent.eventId)
                if (ackSuccess) {
                    healthAlertEventDao.markEventAcknowledged(remoteEvent.eventId, System.currentTimeMillis())
                }
                count++
            }
            Log.d(TAG, "Catch-up finished. Processed $count unacknowledged events from Supabase.")
            count
        } catch (e: Exception) {
            Log.e(TAG, "Error during catchUpFromSupabase: ${e.message}")
            0
        }
    }

    companion object {
        private const val TAG = "HealthCompanionManager"
    }
}
