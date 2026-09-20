package com.example.data.remote.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.util.UUID

@JsonClass(generateAdapter = true)
data class UserDevice(
    @Json(name = "id")
    val id: String = UUID.randomUUID().toString(),
    @Json(name = "user_id")
    val userId: String,
    @Json(name = "fcm_token")
    val fcmToken: String,
    @Json(name = "platform")
    val platform: String = "android",
    @Json(name = "device_name")
    val deviceName: String,
    @Json(name = "app_version")
    val appVersion: String = "1.0",
    @Json(name = "last_seen_at")
    val lastSeenAt: Long = System.currentTimeMillis(),
    @Json(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @Json(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis(),
    @Json(name = "enabled")
    val enabled: Boolean = true
)
