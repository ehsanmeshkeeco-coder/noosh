package com.example.data.local.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.room.entity.GamificationBadgeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GamificationDao {

    @Query("SELECT * FROM gamification_badges ORDER BY isUnlocked DESC, id ASC")
    fun getAllBadgesFlow(): Flow<List<GamificationBadgeEntity>>

    @Query("SELECT * FROM gamification_badges ORDER BY isUnlocked DESC, id ASC")
    suspend fun getAllBadges(): List<GamificationBadgeEntity>

    @Query("SELECT * FROM gamification_badges WHERE isUnlocked = 1")
    suspend fun getUnlockedBadges(): List<GamificationBadgeEntity>

    @Query("SELECT * FROM gamification_badges WHERE id = :id LIMIT 1")
    suspend fun getBadgeById(id: String): GamificationBadgeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateBadge(badge: GamificationBadgeEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertBadgesIfAbsent(badges: List<GamificationBadgeEntity>)

    @Query("UPDATE gamification_badges SET isUnlocked = 1, unlockedAt = :unlockedAt WHERE id = :badgeId AND isUnlocked = 0")
    suspend fun unlockBadge(badgeId: String, unlockedAt: Long = System.currentTimeMillis()): Int
}
