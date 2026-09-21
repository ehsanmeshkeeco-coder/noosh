package com.example.data.local.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.domain.model.UserProfile

@Entity(tableName = "user_profiles")
data class UserProfileEntity(
    @PrimaryKey val id: String = "default_user",
    val clerkUserId: String = com.clerk.android.Clerk.getUser()?.id ?: "",
    val name: String = "کاربر گرامی",
    val email: String = "user@example.com",
    val profileImageUrl: String? = null,
    val dailyWaterGoalMl: Int = 2000,
    val reminderIntervalMinutes: Int = 60,
    val reminderEnabled: Boolean = true,
    val wakeUpTime: String = "08:00",
    val sleepTime: String = "23:00",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val graceDayEnabled: Boolean = true,
    val soundEnabled: Boolean = true,
    val vibrateEnabled: Boolean = true,
    val inactivityThresholdMinutes: Int = 120
) {
    fun toDomain(): UserProfile = UserProfile(
        id = id,
        clerkUserId = clerkUserId,
        name = name,
        email = email,
        profileImageUrl = profileImageUrl,
        dailyWaterGoalMl = dailyWaterGoalMl,
        reminderIntervalMinutes = reminderIntervalMinutes,
        reminderEnabled = reminderEnabled,
        wakeUpTime = wakeUpTime,
        sleepTime = sleepTime,
        createdAt = createdAt,
        updatedAt = updatedAt,
        graceDayEnabled = graceDayEnabled,
        soundEnabled = soundEnabled,
        vibrateEnabled = vibrateEnabled,
        inactivityThresholdMinutes = inactivityThresholdMinutes
    )

    companion object {
        fun fromDomain(domain: UserProfile): UserProfileEntity = UserProfileEntity(
            id = domain.id,
            clerkUserId = domain.clerkUserId,
            name = domain.name,
            email = domain.email,
            profileImageUrl = domain.profileImageUrl,
            dailyWaterGoalMl = domain.dailyWaterGoalMl,
            reminderIntervalMinutes = domain.reminderIntervalMinutes,
            reminderEnabled = domain.reminderEnabled,
            wakeUpTime = domain.wakeUpTime,
            sleepTime = domain.sleepTime,
            createdAt = domain.createdAt,
            updatedAt = domain.updatedAt,
            graceDayEnabled = domain.graceDayEnabled,
            soundEnabled = domain.soundEnabled,
            vibrateEnabled = domain.vibrateEnabled,
            inactivityThresholdMinutes = domain.inactivityThresholdMinutes
        )
    }
}
