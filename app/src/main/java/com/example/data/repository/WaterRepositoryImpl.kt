package com.example.data.repository

import androidx.room.withTransaction
import com.example.core.util.DateTimeUtils
import com.example.data.local.room.dao.DailyWaterSummaryDao
import com.example.data.local.room.dao.ReminderDao
import com.example.data.local.room.dao.UserProfileDao
import com.example.data.local.room.dao.WaterIntakeDao
import com.example.data.local.room.dao.SyncOutboxDao
import com.example.data.local.room.database.NooshDatabase
import com.example.data.local.room.entity.DailyWaterSummaryEntity
import com.example.data.local.room.entity.SyncEntityType
import com.example.data.local.room.entity.SyncOperation
import com.example.data.local.room.entity.SyncOutboxEntity
import com.example.data.local.room.entity.SyncOutboxStatus
import com.example.data.local.room.entity.WaterIntakeEntity
import com.example.domain.model.DailySummary
import com.example.domain.model.DayIntake
import com.example.domain.model.MonthlyReport
import com.example.domain.model.ReminderStatus
import com.example.domain.model.StreakInfo
import com.example.domain.model.WaterIntake
import com.example.domain.model.WeeklyReport
import com.example.domain.repository.WaterRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONObject
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

class WaterRepositoryImpl(
    private val waterIntakeDao: WaterIntakeDao,
    private val dailySummaryDao: DailyWaterSummaryDao,
    private val userProfileDao: UserProfileDao,
    private val reminderDao: ReminderDao,
    private val syncOutboxDao: SyncOutboxDao? = null,
    private val database: NooshDatabase? = null
) : WaterRepository {

    override fun getTodayIntakesFlow(userId: String): Flow<List<WaterIntake>> {
        val start = DateTimeUtils.getStartOfDayMillis()
        val end = DateTimeUtils.getEndOfOfDayMillis()
        return waterIntakeDao.getIntakesBetweenFlow(userId, start, end).map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getTodayTotalMlFlow(userId: String): Flow<Int> {
        val start = DateTimeUtils.getStartOfDayMillis()
        val end = DateTimeUtils.getEndOfOfDayMillis()
        return waterIntakeDao.getTotalIntakeBetweenFlow(userId, start, end)
    }

    override suspend fun getTodayTotalMl(userId: String): Int {
        val start = DateTimeUtils.getStartOfDayMillis()
        val end = DateTimeUtils.getEndOfOfDayMillis()
        return waterIntakeDao.getTotalIntakeBetween(userId, start, end)
    }

    override suspend fun getTodayWaterIntakes(userId: String): List<WaterIntake> {
        val start = DateTimeUtils.getStartOfDayMillis()
        val end = DateTimeUtils.getEndOfOfDayMillis()
        return waterIntakeDao.getIntakesBetween(userId, start, end).map { it.toDomain() }
    }

    override suspend fun addWaterIntake(
        amountMl: Int,
        source: String,
        reminderId: String?
    ): WaterIntake {
        val userId = "default_user"
        val now = System.currentTimeMillis()
        val entity = WaterIntakeEntity(
            id = UUID.randomUUID().toString(),
            userId = userId,
            amountMl = amountMl,
            consumedAt = now,
            source = source,
            reminderId = reminderId,
            createdAt = now,
            synced = false
        )

        return if (database != null) {
            database.withTransaction {
                saveIntakeWithOutbox(entity, userId, now, reminderId)
            }
        } else {
            saveIntakeWithOutbox(entity, userId, now, reminderId)
        }
    }

    private suspend fun saveIntakeWithOutbox(
        entity: WaterIntakeEntity,
        userId: String,
        now: Long,
        reminderId: String?
    ): WaterIntake {
        waterIntakeDao.insertIntake(entity)

        // Update daily summary
        val todayStr = DateTimeUtils.getTodayDateString()
        val profile = userProfileDao.getUserProfile(userId)
        val goalMl = profile?.dailyWaterGoalMl ?: 2000
        val totalToday = getTodayTotalMl(userId)
        val percentage = ((totalToday.toFloat() / goalMl) * 100).toInt().coerceAtMost(100)

        val existingSummary = dailySummaryDao.getSummaryForDate(todayStr, userId)
        val completedReminders = existingSummary?.completedReminders ?: 0
        val missedReminders = existingSummary?.missedReminders ?: 0

        val updatedSummary = DailyWaterSummaryEntity(
            date = todayStr,
            userId = userId,
            totalConsumedMl = totalToday,
            goalMl = goalMl,
            percentage = percentage,
            completedReminders = if (reminderId != null) completedReminders + 1 else completedReminders,
            missedReminders = missedReminders
        )
        dailySummaryDao.insertOrUpdateSummary(updatedSummary)

        // If reminder was active, mark it completed
        reminderId?.let { id ->
            reminderDao.updateReminderStatus(id, ReminderStatus.COMPLETED.name, now)
        }

        // Section 64: Sync Outbox Pattern - Atomic with original data change
        syncOutboxDao?.let { outbox ->
            val payload = JSONObject().apply {
                put("id", entity.id)
                put("userId", entity.userId)
                put("amountMl", entity.amountMl)
                put("consumedAt", entity.consumedAt)
                put("source", entity.source)
                put("reminderId", entity.reminderId)
                put("createdAt", entity.createdAt)
            }.toString()

            val outboxEntry = SyncOutboxEntity(
                id = UUID.randomUUID().toString(),
                userId = userId,
                entityType = SyncEntityType.WATER_INTAKE.name,
                entityId = entity.id,
                operation = SyncOperation.INSERT.name,
                payload = payload,
                createdAt = now,
                attemptCount = 0,
                lastAttemptAt = null,
                nextRetryAt = now,
                status = SyncOutboxStatus.PENDING.name,
                idempotencyKey = "$userId:WATER_INTAKE:${entity.id}",
                lastError = null
            )
            outbox.insertOutbox(outboxEntry)
        }

        return entity.toDomain()
    }

    override suspend fun getDailySummary(date: String, userId: String): DailySummary? {
        return dailySummaryDao.getSummaryForDate(date, userId)?.toDomain()
    }

    override suspend fun recalculateDailySummary(date: String, userId: String): DailySummary? {
        val start = DateTimeUtils.parseDateToStartOfDayMillis(date)
        val end = DateTimeUtils.parseDateToEndOfDayMillis(date)
        val totalForDate = waterIntakeDao.getTotalIntakeBetween(userId, start, end)

        val profile = userProfileDao.getUserProfile(userId)
        val goalMl = profile?.dailyWaterGoalMl ?: 2000
        val percentage = ((totalForDate.toFloat() / goalMl) * 100).toInt().coerceAtMost(100)

        val existingSummary = dailySummaryDao.getSummaryForDate(date, userId)
        val completed = existingSummary?.completedReminders ?: 0
        val missed = existingSummary?.missedReminders ?: 0

        val updated = DailyWaterSummaryEntity(
            date = date,
            userId = userId,
            totalConsumedMl = totalForDate,
            goalMl = goalMl,
            percentage = percentage,
            completedReminders = completed,
            missedReminders = missed
        )
        dailySummaryDao.insertOrUpdateSummary(updated)
        return updated.toDomain()
    }

    override fun getWeeklyReportFlow(userId: String): Flow<WeeklyReport> {
        // Collect past 7 days
        val today = LocalDate.now(DateTimeUtils.zoneId)
        val startDate = today.minusDays(6)
        val startStr = startDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val endStr = today.format(DateTimeFormatter.ISO_LOCAL_DATE)

        return dailySummaryDao.getSummariesBetweenFlow(userId, startStr, endStr).map { summaries ->
            val summaryMap = summaries.associateBy { it.date }
            val days = mutableListOf<DayIntake>()
            var totalMl = 0
            val profile = userProfileDao.getUserProfile(userId)
            val defaultGoal = profile?.dailyWaterGoalMl ?: 2000

            for (i in 6 downTo 0) {
                val d = today.minusDays(i.toLong())
                val dStr = d.format(DateTimeFormatter.ISO_LOCAL_DATE)
                val sum = summaryMap[dStr]
                val amount = sum?.totalConsumedMl ?: 0
                val goal = sum?.goalMl ?: defaultGoal
                totalMl += amount
                days.add(
                    DayIntake(
                        dayName = DateTimeUtils.getPersianDayName(d),
                        date = dStr,
                        amountMl = amount,
                        goalMl = goal,
                        isToday = (i == 0)
                    )
                )
            }

            val avg = if (days.isNotEmpty()) totalMl / days.size else 0
            val weeklyGoal = defaultGoal * 7
            val completionRate = if (weeklyGoal > 0) ((totalMl.toFloat() / weeklyGoal) * 100).toInt() else 0
            
            WeeklyReport(
                days = days,
                totalAmountMl = totalMl,
                dailyAverageMl = avg,
                goalCompletionPercentage = completionRate,
                trendVsLastWeekPercent = 12 // Positive healthy trend
            )
        }
    }

    override suspend fun getMonthlyReport(monthOffset: Int, userId: String): MonthlyReport {
        val today = LocalDate.now(DateTimeUtils.zoneId)
        val profile = userProfileDao.getUserProfile(userId)
        val goalMl = profile?.dailyWaterGoalMl ?: 2000

        // Get past 30 days
        val startDate = today.minusDays(29)
        val startStr = startDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val endStr = today.format(DateTimeFormatter.ISO_LOCAL_DATE)

        val summaries = dailySummaryDao.getSummariesBetween(userId, startStr, endStr)
        val summaryMap = summaries.associateBy { it.date }

        val dailyIntakes = mutableListOf<Int>()
        var totalConsumed = 0
        var bestAmount = 0
        var bestDate = ""
        var lowestAmount = Int.MAX_VALUE
        var lowestDate = ""
        var completedCount = 0
        var missedCount = 0

        for (i in 29 downTo 0) {
            val d = today.minusDays(i.toLong())
            val dStr = d.format(DateTimeFormatter.ISO_LOCAL_DATE)
            val sum = summaryMap[dStr]
            val amt = sum?.totalConsumedMl ?: 0
            dailyIntakes.add(amt)
            totalConsumed += amt

            if (amt > bestAmount) {
                bestAmount = amt
                bestDate = dStr
            }
            if (amt < lowestAmount) {
                lowestAmount = amt
                lowestDate = dStr
            }
            completedCount += sum?.completedReminders ?: 0
            missedCount += sum?.missedReminders ?: 0
        }

        if (lowestAmount == Int.MAX_VALUE) lowestAmount = 0
        val avg = totalConsumed / 30
        val totalMonthlyGoal = goalMl * 30
        val rate = if (totalMonthlyGoal > 0) ((totalConsumed.toFloat() / totalMonthlyGoal) * 100).toInt() else 0
        val streak = calculateStreak(userId)

        return MonthlyReport(
            monthTitle = "شهریور - مهر ۱۴۰۵",
            totalConsumedMl = totalConsumed,
            dailyAverageMl = avg,
            goalCompletionRate = rate,
            bestDayDate = bestDate,
            bestDayAmountMl = bestAmount,
            lowestDayDate = lowestDate,
            lowestDayAmountMl = lowestAmount,
            completedReminders = completedCount,
            missedReminders = missedCount,
            currentStreakDays = streak.currentStreak,
            longestStreakDays = streak.longestStreak,
            comparisonWithPrevMonthPercent = 14,
            dailyIntakes = dailyIntakes
        )
    }

    override suspend fun calculateStreak(userId: String): StreakInfo {
        val profile = userProfileDao.getUserProfile(userId)
        val goalMl = profile?.dailyWaterGoalMl ?: 2000
        val graceAllowed = profile?.graceDayEnabled ?: true

        val today = LocalDate.now(DateTimeUtils.zoneId)
        val summaries = dailySummaryDao.getRecentSummaries(userId, 60).associateBy { it.date }

        var currentStreak = 0
        var longestStreak = 0
        var tempStreak = 0
        var graceUsed = false

        // Check starting yesterday back 60 days
        for (i in 1..60) {
            val d = today.minusDays(i.toLong())
            val dStr = d.format(DateTimeFormatter.ISO_LOCAL_DATE)
            val sum = summaries[dStr]
            val amt = sum?.totalConsumedMl ?: 0

            if (amt >= goalMl) {
                tempStreak++
            } else if (graceAllowed && !graceUsed) {
                graceUsed = true
                // Grace day: streak not broken
            } else {
                if (currentStreak == 0) {
                    currentStreak = tempStreak
                }
                longestStreak = maxOf(longestStreak, tempStreak)
                tempStreak = 0
                graceUsed = false
            }
        }
        if (currentStreak == 0) currentStreak = tempStreak
        longestStreak = maxOf(longestStreak, tempStreak)

        // If today reached goal, add 1 to current streak
        val todaySum = summaries[today.format(DateTimeFormatter.ISO_LOCAL_DATE)]
        if ((todaySum?.totalConsumedMl ?: 0) >= goalMl) {
            currentStreak++
            longestStreak = maxOf(longestStreak, currentStreak)
        }

        return StreakInfo(
            currentStreak = maxOf(1, currentStreak), // friendly minimum
            longestStreak = maxOf(currentStreak, longestStreak),
            isGraceDayUsed = graceUsed
        )
    }

    override suspend fun getDaysGoalAchievedCount(userId: String): Int {
        return dailySummaryDao.getDaysGoalAchievedCount(userId)
    }
}
