package com.example.domain.model

enum class BadgeTier {
    BRONZE,
    SILVER,
    GOLD,
    DIAMOND
}

data class StreakBadge(
    val id: String,
    val title: String,
    val subtitle: String,
    val requiredDays: Int,
    val tier: BadgeTier,
    val iconEmoji: String,
    val isUnlocked: Boolean,
    val currentProgress: Int,
    val progressPercent: Float,
    val rewardTitle: String,
    val healthBenefit: String
)

object StreakBadgeManager {

    private val milestonesData = listOf(
        Triple(
            1,
            "قطره آغازین",
            "اولین گام موفقیت‌آمیز در ثبت روزانه آب"
        ) to (BadgeTier.BRONZE to Pair("💧", "آغاز هیدراتاسیون و ترشح هورمون‌های هوشیاری صبحگاهی")),

        Triple(
            3,
            "جریان ۳ روزه",
            "۳ روز پیاپی دستیابی به هدف کامل هیدراتاسیون"
        ) to (BadgeTier.BRONZE to Pair("🌱", "کاهش سردردهای تنشی و بهبود چشمگیر گوارش")),

        Triple(
            7,
            "قهرمان هفته",
            "یک هفته تمام پایبندی بدون حتی یک روز غفلت"
        ) to (BadgeTier.SILVER to Pair("⚡", "افزایش تمرکز کاری، رفع خستگی مزمن و شفافیت پوست")),

        Triple(
            14,
            "ثبات دو هفته‌ای",
            "۱۴ روز مستمر زندگی با سلول‌های شاداب"
        ) to (BadgeTier.SILVER to Pair("🛡️", "تنظیم متابولیسم پایه و سم‌زدایی مؤثر کلیه‌ها")),

        Triple(
            30,
            "استاد هیدراتاسیون",
            "یک ماه کامل تثبیت سبک زندگی سالم و پایدار"
        ) to (BadgeTier.GOLD to Pair("👑", "تثبیت عادت پایدار در نورون‌های مغزی و تقویت سیستم ایمنی")),

        Triple(
            60,
            "چشمه ابدی",
            "۶۰ روز رکورد طلایی و الهام‌بخش تندرستی"
        ) to (BadgeTier.GOLD to Pair("💎", "بهبود ضربان و فشار خون، افزایش حداکثری انرژی روزانه")),

        Triple(
            100,
            "افسانه اقیانوس",
            "۱۰۰ روز زنجیره تسخیرناپذیر نوشیدن آب"
        ) to (BadgeTier.DIAMOND to Pair("🌊", "اوج سلامتی سلولی، شادابی جوانی و سبک زندگی ایده‌آل"))
    )

    fun getBadges(currentStreak: Int, longestStreak: Int): List<StreakBadge> {
        val effectiveDays = maxOf(currentStreak, longestStreak)
        return milestonesData.map { (meta, details) ->
            val req = meta.first
            val title = meta.second
            val subtitle = meta.third
            val tier = details.first
            val emoji = details.second.first
            val benefit = details.second.second

            val isUnlocked = effectiveDays >= req
            val progress = (effectiveDays.toFloat() / req.toFloat()).coerceIn(0f, 1f)

            StreakBadge(
                id = "badge_${req}_days",
                title = title,
                subtitle = subtitle,
                requiredDays = req,
                tier = tier,
                iconEmoji = emoji,
                isUnlocked = isUnlocked,
                currentProgress = minOf(effectiveDays, req),
                progressPercent = progress,
                rewardTitle = when (tier) {
                    BadgeTier.BRONZE -> "مدال برنزی"
                    BadgeTier.SILVER -> "مدال نقره‌ای"
                    BadgeTier.GOLD -> "مدال طلایی"
                    BadgeTier.DIAMOND -> "مدال الماس"
                },
                healthBenefit = benefit
            )
        }
    }

    fun getNextMilestone(currentStreak: Int, longestStreak: Int): StreakBadge? {
        val effectiveDays = maxOf(currentStreak, longestStreak)
        val badges = getBadges(currentStreak, longestStreak)
        return badges.firstOrNull { !it.isUnlocked }
    }

    fun getStreakTierTitle(currentStreak: Int): String {
        return when {
            currentStreak >= 100 -> "افسانه اقیانوس 🌊"
            currentStreak >= 60 -> "استاد طلایی 💎"
            currentStreak >= 30 -> "قهرمان بزرگ 👑"
            currentStreak >= 14 -> "پایدار و منظم 🛡️"
            currentStreak >= 7 -> "ورزشکار هفته ⚡"
            currentStreak >= 3 -> "جوینده پرانرژی 🌱"
            else -> "آغازگر مصمم 💧"
        }
    }
}
