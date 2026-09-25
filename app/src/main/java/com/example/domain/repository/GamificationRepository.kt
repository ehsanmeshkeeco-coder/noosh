package com.example.domain.repository

import com.example.domain.gamification.GamificationBadge
import com.example.domain.gamification.GamificationResult
import com.example.domain.gamification.GamificationStats
import kotlinx.coroutines.flow.Flow

interface GamificationRepository {
    fun getAllBadgesFlow(): Flow<List<GamificationBadge>>
    suspend fun getAllBadges(): List<GamificationBadge>
    suspend fun getGamificationStats(): GamificationStats
    suspend fun addXpAndCheckAchievements(
        intakeAmountMl: Int,
        isGoalAchieved: Boolean,
        todayTotalMl: Int,
        streakDays: Int
    ): GamificationResult
}
