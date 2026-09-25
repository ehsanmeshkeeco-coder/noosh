package com.example.data.local.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.domain.gamification.GamificationBadge

@Entity(tableName = "gamification_badges")
data class GamificationBadgeEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val iconEmoji: String,
    val xpReward: Int,
    val isUnlocked: Boolean = false,
    val unlockedAt: Long? = null,
    val category: String = "general"
) {
    fun toDomain(): GamificationBadge = GamificationBadge(
        id = id,
        title = title,
        description = description,
        iconEmoji = iconEmoji,
        xpReward = xpReward,
        isUnlocked = isUnlocked,
        unlockedAt = unlockedAt,
        category = category
    )

    companion object {
        fun fromDomain(domain: GamificationBadge): GamificationBadgeEntity = GamificationBadgeEntity(
            id = domain.id,
            title = domain.title,
            description = domain.description,
            iconEmoji = domain.iconEmoji,
            xpReward = domain.xpReward,
            isUnlocked = domain.isUnlocked,
            unlockedAt = domain.unlockedAt,
            category = domain.category
        )
    }
}
