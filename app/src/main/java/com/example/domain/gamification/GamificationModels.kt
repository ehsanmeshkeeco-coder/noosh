package com.example.domain.gamification

data class GamificationBadge(
    val id: String,
    val title: String,
    val description: String,
    val iconEmoji: String,
    val xpReward: Int,
    val isUnlocked: Boolean = false,
    val unlockedAt: Long? = null,
    val category: String = "general"
)

data class UserLevelInfo(
    val level: Int,
    val titleFa: String,
    val iconEmoji: String,
    val currentLevelMinXp: Int,
    val nextLevelTargetXp: Int,
    val currentProgressXp: Int,
    val neededXpForNextLevel: Int,
    val progressPercent: Float,
    val totalXp: Int
)

data class GamificationResult(
    val xpEarned: Int,
    val totalXp: Int,
    val level: Int,
    val levelInfo: UserLevelInfo,
    val didLevelUp: Boolean,
    val newlyUnlockedBadges: List<GamificationBadge>
)

data class GamificationStats(
    val totalXp: Int,
    val levelInfo: UserLevelInfo,
    val unlockedBadgesCount: Int,
    val totalBadgesCount: Int,
    val badges: List<GamificationBadge>
)
