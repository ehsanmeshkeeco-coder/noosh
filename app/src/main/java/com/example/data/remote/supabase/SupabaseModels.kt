package com.example.data.remote.supabase

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SupabaseProfile(
    @Json(name = "id") val id: String,
    @Json(name = "clerk_user_id") val clerkUserId: String?,
    @Json(name = "name") val name: String,
    @Json(name = "email") val email: String,
    @Json(name = "profile_image_url") val profileImageUrl: String?,
    @Json(name = "daily_water_goal_ml") val dailyWaterGoalMl: Int,
    @Json(name = "updated_at") val updatedAt: Long
)

@JsonClass(generateAdapter = true)
data class SupabaseWaterIntake(
    @Json(name = "id") val id: String,
    @Json(name = "user_id") val userId: String,
    @Json(name = "amount_ml") val amountMl: Int,
    @Json(name = "consumed_at") val consumedAt: Long,
    @Json(name = "source") val source: String,
    @Json(name = "created_at") val createdAt: Long
)

@JsonClass(generateAdapter = true)
data class SupabaseHealthSyncEvent(
    @Json(name = "user_id") val userId: String,
    @Json(name = "daily_intake_ml") val dailyIntakeMl: Int,
    @Json(name = "daily_goal_ml") val dailyGoalMl: Int,
    @Json(name = "streak_days") val streakDays: Int,
    @Json(name = "timestamp") val timestamp: Long = System.currentTimeMillis()
)
