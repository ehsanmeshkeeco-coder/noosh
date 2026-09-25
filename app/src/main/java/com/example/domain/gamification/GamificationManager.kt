package com.example.domain.gamification

import java.util.Calendar

object GamificationManager {

    data class LevelThreshold(
        val level: Int,
        val titleFa: String,
        val iconEmoji: String,
        val minXp: Int,
        val maxXp: Int
    )

    val levels = listOf(
        LevelThreshold(1, "قطره تازه", "💧", 0, 199),
        LevelThreshold(2, "جویبار پویا", "🏞️", 200, 499),
        LevelThreshold(3, "رود پرآب", "🌊", 500, 999),
        LevelThreshold(4, "چشمه زلال", "⛲", 1000, 1799),
        LevelThreshold(5, "آبشار خروشان", "🏔️", 1800, 2999),
        LevelThreshold(6, "دریای آبی", "⛵", 3000, 4999),
        LevelThreshold(7, "اقیانوس حیات", "🐋", 5000, Int.MAX_VALUE)
    )

    val defaultBadges = listOf(
        GamificationBadge(
            id = "first_sip",
            title = "جرعه اول",
            description = "ثبت اولین لیوان آب در برنامه و آغاز مسیر سلامت",
            iconEmoji = "💧",
            xpReward = 20,
            category = "milestone"
        ),
        GamificationBadge(
            id = "morning_dew",
            title = "شبنم صبحگاهی",
            description = "نوشیدن آب تازه بین ساعت ۵ تا ۹ صبح برای بیداری شاداب بدن",
            iconEmoji = "🌅",
            xpReward = 30,
            category = "habit"
        ),
        GamificationBadge(
            id = "goal_crusher",
            title = "قهرمان هیدراتاسیون",
            description = "تکمیل ۱۰۰٪ هدف مصرف روزانه آب و رسیدن به سطح ایده‌آل",
            iconEmoji = "🏆",
            xpReward = 50,
            category = "achievement"
        ),
        GamificationBadge(
            id = "two_liters",
            title = "باشگاه ۲ لیتری‌ها",
            description = "ثبت حداقل ۲۰۰۰ میلی‌لیتر آب خالص در یک روز",
            iconEmoji = "🥛",
            xpReward = 40,
            category = "volume"
        ),
        GamificationBadge(
            id = "streak_3",
            title = "پایداری ۳ روزه",
            description = "۳ روز متوالی دستیابی به هدف مصرف آب روزانه",
            iconEmoji = "🔥",
            xpReward = 60,
            category = "streak"
        ),
        GamificationBadge(
            id = "streak_7",
            title = "استاد هفتگی",
            description = "یک هفته کامل همراهی منظم و نوشیدن کافی آب",
            iconEmoji = "👑",
            xpReward = 100,
            category = "streak"
        ),
        GamificationBadge(
            id = "level_3",
            title = "کاشف امواج",
            description = "ارتقای سطح کاربری به سطح ۳ (رود پرآب)",
            iconEmoji = "🌊",
            xpReward = 80,
            category = "level"
        ),
        GamificationBadge(
            id = "hydration_master",
            title = "استاد اقیانوس",
            description = "ارتقا به سطح ۵ و تبدیل شدن به قهرمان پایداری نوشیدن آب",
            iconEmoji = "🐋",
            xpReward = 150,
            category = "level"
        )
    )

    fun calculateXpForIntake(
        amountMl: Int,
        isGoalAchieved: Boolean,
        hourOfDay: Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    ): Int {
        var xp = (amountMl / 10).coerceAtLeast(10)
        // Morning hydration bonus (5:00 - 9:59)
        if (hourOfDay in 5..9) {
            xp += 15
        }
        // Daily goal achievement bonus
        if (isGoalAchieved) {
            xp += 50
        }
        return xp
    }

    fun getLevelInfo(totalXp: Int): UserLevelInfo {
        val safeXp = totalXp.coerceAtLeast(0)
        val currentLevel = levels.firstOrNull { safeXp in it.minXp..it.maxXp }
            ?: levels.last()

        val nextLevel = levels.getOrNull(currentLevel.level) // Next level (0-indexed)

        val minXp = currentLevel.minXp
        val targetXp = nextLevel?.minXp ?: currentLevel.minXp + 1000
        val currentProgress = (safeXp - minXp).coerceAtLeast(0)
        val neededXp = (targetXp - minXp).coerceAtLeast(1)
        val progressPercent = if (nextLevel != null) {
            (currentProgress.toFloat() / neededXp.toFloat()).coerceIn(0f, 1f)
        } else {
            1f // Max level reached
        }

        return UserLevelInfo(
            level = currentLevel.level,
            titleFa = currentLevel.titleFa,
            iconEmoji = currentLevel.iconEmoji,
            currentLevelMinXp = minXp,
            nextLevelTargetXp = targetXp,
            currentProgressXp = currentProgress,
            neededXpForNextLevel = neededXp,
            progressPercent = progressPercent,
            totalXp = safeXp
        )
    }
}
