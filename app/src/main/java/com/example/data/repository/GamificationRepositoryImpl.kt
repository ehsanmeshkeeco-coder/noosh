package com.example.data.repository

import com.example.data.local.room.dao.GamificationDao
import com.example.data.local.room.dao.UserProfileDao
import com.example.data.local.room.entity.GamificationBadgeEntity
import com.example.domain.gamification.GamificationBadge
import com.example.domain.gamification.GamificationManager
import com.example.domain.gamification.GamificationResult
import com.example.domain.gamification.GamificationStats
import com.example.domain.repository.GamificationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar

class GamificationRepositoryImpl(
    private val gamificationDao: GamificationDao,
    private val userProfileDao: UserProfileDao
) : GamificationRepository {

    override fun getAllBadgesFlow(): Flow<List<GamificationBadge>> {
        return gamificationDao.getAllBadgesFlow().map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun getAllBadges(): List<GamificationBadge> {
        seedBadgesIfEmpty()
        return gamificationDao.getAllBadges().map { it.toDomain() }
    }

    override suspend fun getGamificationStats(): GamificationStats {
        seedBadgesIfEmpty()
        val profile = userProfileDao.getUserProfile("default_user")
        val totalXp = profile?.totalXp ?: 0
        val levelInfo = GamificationManager.getLevelInfo(totalXp)
        val badges = gamificationDao.getAllBadges().map { it.toDomain() }
        val unlockedCount = badges.count { it.isUnlocked }

        return GamificationStats(
            totalXp = totalXp,
            levelInfo = levelInfo,
            unlockedBadgesCount = unlockedCount,
            totalBadgesCount = badges.size,
            badges = badges
        )
    }

    override suspend fun addXpAndCheckAchievements(
        intakeAmountMl: Int,
        isGoalAchieved: Boolean,
        todayTotalMl: Int,
        streakDays: Int
    ): GamificationResult {
        seedBadgesIfEmpty()
        val currentProfile = userProfileDao.getUserProfile("default_user")
        val previousXp = currentProfile?.totalXp ?: 0
        val previousLevel = currentProfile?.level ?: 1

        val earnedXp = GamificationManager.calculateXpForIntake(
            amountMl = intakeAmountMl,
            isGoalAchieved = isGoalAchieved
        )
        val newTotalXp = previousXp + earnedXp
        val newLevelInfo = GamificationManager.getLevelInfo(newTotalXp)
        val didLevelUp = newLevelInfo.level > previousLevel

        // Update user profile with new XP and level
        currentProfile?.let { prof ->
            userProfileDao.insertOrUpdateProfile(
                prof.copy(
                    totalXp = newTotalXp,
                    level = newLevelInfo.level,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }

        // Check for badge unlocks
        val newlyUnlocked = mutableListOf<GamificationBadge>()
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

        // 1. First Sip
        if (unlockIfEligible("first_sip", true)) {
            gamificationDao.getBadgeById("first_sip")?.let { newlyUnlocked.add(it.toDomain()) }
        }

        // 2. Morning Dew (5:00 - 9:59)
        if (hour in 5..9) {
            if (unlockIfEligible("morning_dew", true)) {
                gamificationDao.getBadgeById("morning_dew")?.let { newlyUnlocked.add(it.toDomain()) }
            }
        }

        // 3. Goal Crusher
        if (isGoalAchieved || todayTotalMl >= (currentProfile?.dailyWaterGoalMl ?: 2000)) {
            if (unlockIfEligible("goal_crusher", true)) {
                gamificationDao.getBadgeById("goal_crusher")?.let { newlyUnlocked.add(it.toDomain()) }
            }
        }

        // 4. Two Liters Club
        if (todayTotalMl >= 2000) {
            if (unlockIfEligible("two_liters", true)) {
                gamificationDao.getBadgeById("two_liters")?.let { newlyUnlocked.add(it.toDomain()) }
            }
        }

        // 5. Streak 3
        if (streakDays >= 3) {
            if (unlockIfEligible("streak_3", true)) {
                gamificationDao.getBadgeById("streak_3")?.let { newlyUnlocked.add(it.toDomain()) }
            }
        }

        // 6. Streak 7
        if (streakDays >= 7) {
            if (unlockIfEligible("streak_7", true)) {
                gamificationDao.getBadgeById("streak_7")?.let { newlyUnlocked.add(it.toDomain()) }
            }
        }

        // 7. Level 3
        if (newLevelInfo.level >= 3) {
            if (unlockIfEligible("level_3", true)) {
                gamificationDao.getBadgeById("level_3")?.let { newlyUnlocked.add(it.toDomain()) }
            }
        }

        // 8. Hydration Master (Level 5)
        if (newLevelInfo.level >= 5) {
            if (unlockIfEligible("hydration_master", true)) {
                gamificationDao.getBadgeById("hydration_master")?.let { newlyUnlocked.add(it.toDomain()) }
            }
        }

        return GamificationResult(
            xpEarned = earnedXp,
            totalXp = newTotalXp,
            level = newLevelInfo.level,
            levelInfo = newLevelInfo,
            didLevelUp = didLevelUp,
            newlyUnlockedBadges = newlyUnlocked
        )
    }

    private suspend fun unlockIfEligible(badgeId: String, eligible: Boolean): Boolean {
        if (!eligible) return false
        val rows = gamificationDao.unlockBadge(badgeId, System.currentTimeMillis())
        return rows > 0
    }

    suspend fun seedBadgesIfEmpty() {
        val existing = gamificationDao.getAllBadges()
        if (existing.isEmpty()) {
            val entities = GamificationManager.defaultBadges.map {
                GamificationBadgeEntity.fromDomain(it)
            }
            gamificationDao.insertBadgesIfAbsent(entities)
        }
    }
}
