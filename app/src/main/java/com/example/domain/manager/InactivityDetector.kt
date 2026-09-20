package com.example.domain.manager

import java.util.Calendar

data class InactivityCheckResult(
    val isInactive: Boolean,
    val inactivityMinutes: Long,
    val thresholdMinutes: Int,
    val isWithinActiveHours: Boolean
)

class InactivityDetector(
    val defaultThresholdMinutes: Int = 120 // 2 hours default configurable
) {
    fun checkInactivity(
        lastWaterIntakeAt: Long?,
        thresholdMinutes: Int = defaultThresholdMinutes,
        wakeUpTime: String = "08:00",
        sleepTime: String = "23:00",
        currentTime: Long = System.currentTimeMillis()
    ): InactivityCheckResult {
        val isWithinActive = isWithinActiveHours(wakeUpTime, sleepTime, currentTime)
        if (!isWithinActive) {
            // User is sleeping, inactivity is normal and suppressed
            return InactivityCheckResult(
                isInactive = false,
                inactivityMinutes = 0,
                thresholdMinutes = thresholdMinutes,
                isWithinActiveHours = false
            )
        }

        val effectiveLastDrink = lastWaterIntakeAt ?: getTodayStartTime(wakeUpTime, currentTime)
        val diffMs = (currentTime - effectiveLastDrink).coerceAtLeast(0)
        val diffMinutes = diffMs / (1000 * 60)

        val isInactive = diffMinutes >= thresholdMinutes

        return InactivityCheckResult(
            isInactive = isInactive,
            inactivityMinutes = diffMinutes,
            thresholdMinutes = thresholdMinutes,
            isWithinActiveHours = true
        )
    }

    private fun isWithinActiveHours(wakeUp: String, sleep: String, currentTime: Long): Boolean {
        val cal = Calendar.getInstance().apply { timeInMillis = currentTime }
        val currentMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)

        val wakeParts = wakeUp.split(":").mapNotNull { it.toIntOrNull() }
        val sleepParts = sleep.split(":").mapNotNull { it.toIntOrNull() }

        val wakeMinutes = if (wakeParts.size == 2) wakeParts[0] * 60 + wakeParts[1] else 8 * 60
        val sleepMinutes = if (sleepParts.size == 2) sleepParts[0] * 60 + sleepParts[1] else 23 * 60

        return if (wakeMinutes <= sleepMinutes) {
            currentMinutes in wakeMinutes..sleepMinutes
        } else {
            currentMinutes >= wakeMinutes || currentMinutes <= sleepMinutes
        }
    }

    private fun getTodayStartTime(wakeUp: String, currentTime: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = currentTime
            val wakeParts = wakeUp.split(":").mapNotNull { it.toIntOrNull() }
            val h = if (wakeParts.isNotEmpty()) wakeParts[0] else 8
            val m = if (wakeParts.size > 1) wakeParts[1] else 0
            set(Calendar.HOUR_OF_DAY, h)
            set(Calendar.MINUTE, m)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }
}
