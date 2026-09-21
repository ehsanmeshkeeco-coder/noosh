package com.example.domain.usecase

import kotlin.math.roundToInt

/**
 * Scientific water intake calculation based on user biometrics:
 * weight (kg), height (cm), age (years), gender (male, female, other),
 * activity level, and climate.
 *
 * Base recommendation:
 * - Baseline: 35 ml per kg of body weight
 * - Gender factor: Male (+250ml), Female (baseline), Other (baseline)
 * - Age adjustment:
 *     < 18: ~30-35 ml/kg
 *     18 - 55: 35 ml/kg
 *     55 - 65: 30-32 ml/kg
 *     > 65: 28-30 ml/kg
 * - Height factor: slight adjustment for surface area (if height > 180cm +150ml, if height < 155cm -100ml)
 * - Rounded to nearest 50ml, clamped between 1200ml and 4500ml
 */
object WaterCalculationAlgorithm {

    data class CalculationResult(
        val dailyWaterGoalMl: Int,
        val recommendedGlasses: Int,
        val recommendedIntervalMinutes: Int,
        val explanation: String
    )

    fun calculateDailyGoal(
        weightKg: Float,
        heightCm: Float,
        age: Int,
        gender: String, // "male", "female", "other"
        wakeUpTime: String = "08:00",
        sleepTime: String = "23:00"
    ): CalculationResult {
        val safeWeight = if (weightKg <= 0f) 70f else weightKg.coerceIn(30f, 250f)
        val safeHeight = if (heightCm <= 0f) 170f else heightCm.coerceIn(100f, 230f)
        val safeAge = if (age <= 0) 25 else age.coerceIn(10, 100)

        // 1. Base ml per kg according to age
        val mlPerKg = when {
            safeAge < 18 -> 35.0f
            safeAge <= 55 -> 35.0f
            safeAge <= 65 -> 32.0f
            else -> 30.0f
        }

        var totalMl = safeWeight * mlPerKg

        // 2. Gender adjustment
        when (gender.lowercase()) {
            "male", "مرد" -> totalMl += 250f
            "female", "زن" -> totalMl += 0f
            else -> totalMl += 100f
        }

        // 3. Height adjustment (body surface area proxy)
        if (safeHeight > 180f) {
            totalMl += 150f
        } else if (safeHeight < 155f) {
            totalMl -= 100f
        }

        // 4. Round to nearest 50 ml and clamp
        val finalGoalMl = ((totalMl / 50.0f).roundToInt() * 50).coerceIn(1200, 4500)
        val glasses = (finalGoalMl / 250.0f).roundToInt()

        // 5. Calculate ideal reminder interval based on awake hours
        val awakeHours = calculateAwakeHours(wakeUpTime, sleepTime)
        // Distribute water evenly across awake hours
        val calculatedInterval = ((awakeHours * 60f) / glasses).roundToInt().coerceIn(30, 120)

        val explanation = "بر اساس وزن ${safeWeight.toInt()} کیلوگرم، قد ${safeHeight.toInt()} سانتی‌متر و سن $safeAge سال، مصرف روزانه $finalGoalMl میلی‌لیتر ($glasses لیوان) برای هیدراتاسیون کامل بدن شما پیشنهاد می‌شود."

        return CalculationResult(
            dailyWaterGoalMl = finalGoalMl,
            recommendedGlasses = glasses,
            recommendedIntervalMinutes = calculatedInterval,
            explanation = explanation
        )
    }

    private fun calculateAwakeHours(wakeUpTime: String, sleepTime: String): Float {
        return try {
            val wakeParts = wakeUpTime.split(":").map { it.trim().toInt() }
            val sleepParts = sleepTime.split(":").map { it.trim().toInt() }
            val wakeMinutes = wakeParts[0] * 60 + wakeParts[1]
            var sleepMinutes = sleepParts[0] * 60 + sleepParts[1]
            if (sleepMinutes <= wakeMinutes) {
                sleepMinutes += 24 * 60
            }
            (sleepMinutes - wakeMinutes) / 60.0f
        } catch (e: Exception) {
            15.0f // default 15 hours awake (08:00 to 23:00)
        }
    }
}
