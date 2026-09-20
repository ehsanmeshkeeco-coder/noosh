package com.example.data.repository

import com.example.core.util.DateTimeUtils
import com.example.data.local.room.dao.ReminderDao
import com.example.data.local.room.entity.ReminderEntity
import com.example.domain.model.Reminder
import com.example.domain.model.ReminderStatus
import com.example.domain.model.UserProfile
import com.example.domain.repository.ReminderRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

class ReminderRepositoryImpl(
    private val reminderDao: ReminderDao
) : ReminderRepository {

    override fun getAllRemindersFlow(userId: String): Flow<List<Reminder>> {
        return reminderDao.getAllRemindersFlow(userId).map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getTodayRemindersFlow(userId: String): Flow<List<Reminder>> {
        val start = DateTimeUtils.getStartOfDayMillis()
        val end = DateTimeUtils.getEndOfOfDayMillis()
        return reminderDao.getRemindersForDayFlow(userId, start, end).map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun getTodayReminders(userId: String): List<Reminder> {
        val start = DateTimeUtils.getStartOfDayMillis()
        val end = DateTimeUtils.getEndOfOfDayMillis()
        return reminderDao.getRemindersForDay(userId, start, end).map { it.toDomain() }
    }

    override suspend fun getReminderById(id: String): Reminder? {
        return reminderDao.getReminderById(id)?.toDomain()
    }

    override fun getNextReminderFlow(): Flow<Reminder?> {
        return reminderDao.getNextUpcomingReminderFlow().map { it?.toDomain() }
    }

    override suspend fun getNextReminder(): Reminder? {
        return reminderDao.getNextUpcomingReminder()?.toDomain()
    }

    override suspend fun getLastNotifiedReminder(): Reminder? {
        return reminderDao.getLastNotifiedReminder()?.toDomain()
    }

    override suspend fun updateReminderStatus(
        id: String,
        status: ReminderStatus,
        completedAt: Long?
    ) {
        reminderDao.updateReminderStatus(id, status.name, completedAt)
    }

    override suspend fun completeReminder(id: String, amountMl: Int) {
        val now = System.currentTimeMillis()
        reminderDao.updateReminderStatus(id, ReminderStatus.COMPLETED.name, now)
    }

    override suspend fun snoozeReminder(id: String, snoozeMinutes: Int) {
        val reminder = reminderDao.getReminderById(id) ?: return
        val now = System.currentTimeMillis()
        val nextTime = now + (snoozeMinutes * 60 * 1000L)
        val updated = reminder.copy(
            status = ReminderStatus.SNOOZED.name,
            retryCount = reminder.retryCount + 1,
            nextReminderAt = nextTime
        )
        reminderDao.updateReminder(updated)
    }

    override suspend fun scheduleDailyReminders(profile: UserProfile) {
        if (!profile.reminderEnabled) {
            reminderDao.deletePendingRemindersAfter(System.currentTimeMillis())
            return
        }

        val today = LocalDate.now(DateTimeUtils.zoneId)
        val startTime = DateTimeUtils.parseTime(profile.wakeUpTime)
        val endTime = DateTimeUtils.parseTime(profile.sleepTime)
        val intervalMinutes = profile.reminderIntervalMinutes.coerceAtLeast(15)

        var currentTime = startTime
        val now = System.currentTimeMillis()
        val newReminders = mutableListOf<ReminderEntity>()

        while (currentTime.isBefore(endTime) || currentTime == endTime) {
            val scheduledMillis = today.atTime(currentTime).atZone(DateTimeUtils.zoneId).toInstant().toEpochMilli()
            if (scheduledMillis > now) {
                newReminders.add(
                    ReminderEntity(
                        id = UUID.randomUUID().toString(),
                        userId = profile.id,
                        scheduledAt = scheduledMillis,
                        status = ReminderStatus.PENDING.name,
                        amountMl = 250
                    )
                )
            }
            currentTime = currentTime.plusMinutes(intervalMinutes.toLong())
        }

        // Delete future pending reminders and insert new plan
        reminderDao.deletePendingRemindersAfter(now)
        if (newReminders.isNotEmpty()) {
            reminderDao.insertReminders(newReminders)
        }
    }

    override suspend fun insertReminder(reminder: Reminder) {
        reminderDao.insertReminder(ReminderEntity.fromDomain(reminder))
    }
}
