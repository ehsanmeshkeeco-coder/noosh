package com.example.data.repository

import com.example.data.local.room.dao.UserProfileDao
import com.example.data.local.room.entity.UserProfileEntity
import com.example.domain.model.UserProfile
import com.example.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class UserRepositoryImpl(
    private val userProfileDao: UserProfileDao
) : UserRepository {

    override fun getUserProfileFlow(userId: String): Flow<UserProfile?> {
        return userProfileDao.getUserProfileFlow(userId).map { it?.toDomain() }
    }

    override suspend fun getUserProfile(userId: String): UserProfile {
        var profile = userProfileDao.getUserProfile(userId)
        if (profile == null) {
            profile = UserProfileEntity(
                id = userId,
                name = "کاربر گرامی",
                email = "user@noosh.app",
                dailyWaterGoalMl = 2000,
                reminderIntervalMinutes = 60,
                reminderEnabled = true,
                wakeUpTime = "08:00",
                sleepTime = "23:00"
            )
            userProfileDao.insertOrUpdateProfile(profile)
        }
        return profile.toDomain()
    }

    override suspend fun updateProfile(profile: UserProfile) {
        val entity = UserProfileEntity.fromDomain(profile.copy(updatedAt = System.currentTimeMillis()))
        userProfileDao.insertOrUpdateProfile(entity)
    }

    override suspend fun updateGoal(goalMl: Int, userId: String) {
        val current = getUserProfile(userId)
        updateProfile(current.copy(dailyWaterGoalMl = goalMl))
    }

    override suspend fun setReminderEnabled(enabled: Boolean, userId: String) {
        val current = getUserProfile(userId)
        updateProfile(current.copy(reminderEnabled = enabled))
    }

    override suspend fun updateReminderSettings(
        enabled: Boolean,
        intervalMinutes: Int,
        startTime: String,
        endTime: String,
        userId: String
    ) {
        val current = getUserProfile(userId)
        updateProfile(
            current.copy(
                reminderEnabled = enabled,
                reminderIntervalMinutes = intervalMinutes,
                wakeUpTime = startTime,
                sleepTime = endTime
            )
        )
    }
}
