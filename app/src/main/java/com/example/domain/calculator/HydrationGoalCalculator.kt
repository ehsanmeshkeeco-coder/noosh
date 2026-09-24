package com.example.domain.calculator

data class ActivityLevelOption(
    val id: String,
    val titleFa: String,
    val descriptionFa: String,
    val additionMl: Int,
    val iconEmoji: String
)

data class ClimateOption(
    val id: String,
    val titleFa: String,
    val descriptionFa: String,
    val additionMl: Int,
    val iconEmoji: String
)

data class HydrationCalculationResult(
    val recommendedGoalMl: Int,
    val recommendedGlasses: Int,
    val baselineMl: Int,
    val activityAdditionMl: Int,
    val climateAdditionMl: Int,
    val explanation: String
)

object HydrationGoalCalculator {

    val activityOptions = listOf(
        ActivityLevelOption(
            id = "sedentary",
            titleFa = "کم‌تحرک",
            descriptionFa = "کار پشت میز، فعالیت بدنی حداقل",
            additionMl = 0,
            iconEmoji = "🪑"
        ),
        ActivityLevelOption(
            id = "moderate",
            titleFa = "معتدل",
            descriptionFa = "پیاده‌روی روزانه یا ورزش سبک ۱ تا ۳ روز در هفته",
            additionMl = 350,
            iconEmoji = "🚶"
        ),
        ActivityLevelOption(
            id = "active",
            titleFa = "پرتحرک",
            descriptionFa = "ورزش منظم یا کار با تحرک بدنی بالا",
            additionMl = 700,
            iconEmoji = "🏃"
        ),
        ActivityLevelOption(
            id = "very_active",
            titleFa = "ورزشکار حرفه‌ای",
            descriptionFa = "تمرینات شدید روزانه یا کار فیزیکی سنگین",
            additionMl = 1050,
            iconEmoji = "🏋️"
        )
    )

    val climateOptions = listOf(
        ClimateOption(
            id = "cold",
            titleFa = "سرد / خنک",
            descriptionFa = "دمای پایین، تعریق ناچیز",
            additionMl = 0,
            iconEmoji = "❄️"
        ),
        ClimateOption(
            id = "temperate",
            titleFa = "معتدل و مطبوع",
            descriptionFa = "هوای نرمال بهاری یا تهویه مناسب",
            additionMl = 150,
            iconEmoji = "🌤️"
        ),
        ClimateOption(
            id = "warm_dry",
            titleFa = "گرم و خشک",
            descriptionFa = "تبخیر سطحی بالا و نیاز به آبرسانی بیشتر",
            additionMl = 400,
            iconEmoji = "🏜️"
        ),
        ClimateOption(
            id = "hot_humid",
            titleFa = "بسیار گرم / شرجی",
            descriptionFa = "تابستان داغ یا محیط مرطوب با تعریق شدید",
            additionMl = 650,
            iconEmoji = "🌴"
        )
    )

    fun calculate(
        weightKg: Float,
        activityLevelId: String,
        climateId: String
    ): HydrationCalculationResult {
        val safeWeight = weightKg.coerceIn(30f, 200f)

        // Baseline scientific hydration: 35 ml per kg of body mass
        val baseline = (safeWeight * 35f).toInt()

        val selectedActivity = activityOptions.find { it.id.equals(activityLevelId, ignoreCase = true) }
            ?: activityOptions[1]
        val selectedClimate = climateOptions.find { it.id.equals(climateId, ignoreCase = true) }
            ?: climateOptions[1]

        val rawTotal = baseline + selectedActivity.additionMl + selectedClimate.additionMl

        // Round to clean 100 ml interval
        val rounded = ((rawTotal + 50) / 100) * 100
        val clampedGoal = rounded.coerceIn(1200, 4500)
        val glasses = (clampedGoal + 125) / 250

        val explanation = buildString {
            append("بر اساس وزن ${safeWeight.toInt()} کیلوگرم، نیاز پایه‌ای بدن شما $baseline میلی‌لیتر است. ")
            if (selectedActivity.additionMl > 0) {
                append("برای سطح تحرک «${selectedActivity.titleFa}» (${selectedActivity.additionMl}+ میلی‌لیتر) ")
            }
            if (selectedClimate.additionMl > 0) {
                append("و اقلیم «${selectedClimate.titleFa}» (${selectedClimate.additionMl}+ میلی‌لیتر) به آن افزوده شد.")
            }
        }

        return HydrationCalculationResult(
            recommendedGoalMl = clampedGoal,
            recommendedGlasses = glasses,
            baselineMl = baseline,
            activityAdditionMl = selectedActivity.additionMl,
            climateAdditionMl = selectedClimate.additionMl,
            explanation = explanation
        )
    }
}
