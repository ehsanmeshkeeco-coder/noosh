package com.example.data.remote.supabase

import android.util.Log
import com.example.data.remote.model.UserDevice
import com.example.domain.model.HealthAlertEvent
import com.example.domain.model.HealthCompanionConnection
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

open class SupabaseClient(
    val supabaseUrl: String = "",
    val supabaseKey: String = ""
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    open val isConfigured: Boolean
        get() = supabaseUrl.isNotBlank() && supabaseKey.isNotBlank() && !supabaseUrl.contains("YOUR_")

    open suspend fun insertWaterIntake(intake: SupabaseWaterIntake): Boolean {
        if (!isConfigured) {
            Log.d(TAG, "Supabase not configured, saving offline-first")
            return false
        }
        return try {
            val json = JSONObject().apply {
                put("id", intake.id)
                put("user_id", intake.userId)
                put("amount_ml", intake.amountMl)
                put("consumed_at", intake.consumedAt)
                put("source", intake.source)
                put("created_at", intake.createdAt)
            }
            val body = json.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("$supabaseUrl/rest/v1/water_intakes")
                .addHeader("apikey", supabaseKey)
                .addHeader("Authorization", "Bearer $supabaseKey")
                .addHeader("Prefer", "resolution=merge-duplicates")
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            Log.e(TAG, "Error inserting water intake to Supabase: ${e.message}")
            false
        }
    }

    open suspend fun syncHealthEvent(event: SupabaseHealthSyncEvent): Boolean {
        if (!isConfigured) return false
        return try {
            val json = JSONObject().apply {
                put("user_id", event.userId)
                put("daily_intake_ml", event.dailyIntakeMl)
                put("daily_goal_ml", event.dailyGoalMl)
                put("streak_days", event.streakDays)
                put("timestamp", event.timestamp)
            }
            val body = json.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("$supabaseUrl/rest/v1/health_sync_events")
                .addHeader("apikey", supabaseKey)
                .addHeader("Authorization", "Bearer $supabaseKey")
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing health event: ${e.message}")
            false
        }
    }

    // Section 51: user_devices management for FCM Tokens
    suspend fun registerOrUpdateDevice(device: UserDevice): Boolean {
        if (!isConfigured) return false
        return try {
            val json = JSONObject().apply {
                put("id", device.id)
                put("user_id", device.userId)
                put("fcm_token", device.fcmToken)
                put("platform", device.platform)
                put("device_name", device.deviceName)
                put("app_version", device.appVersion)
                put("last_seen_at", device.lastSeenAt)
                put("created_at", device.createdAt)
                put("updated_at", device.updatedAt)
                put("enabled", device.enabled)
            }
            val body = json.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("$supabaseUrl/rest/v1/user_devices")
                .addHeader("apikey", supabaseKey)
                .addHeader("Authorization", "Bearer $supabaseKey")
                .addHeader("Prefer", "resolution=merge-duplicates")
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            Log.e(TAG, "Error registering device token in Supabase: ${e.message}")
            false
        }
    }

    suspend fun disableDevice(fcmToken: String): Boolean {
        if (!isConfigured) return false
        return try {
            val json = JSONObject().apply {
                put("enabled", false)
                put("updated_at", System.currentTimeMillis())
            }
            val body = json.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("$supabaseUrl/rest/v1/user_devices?fcm_token=eq.$fcmToken")
                .addHeader("apikey", supabaseKey)
                .addHeader("Authorization", "Bearer $supabaseKey")
                .patch(body)
                .build()

            val response = client.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            Log.e(TAG, "Error disabling device token: ${e.message}")
            false
        }
    }

    // Section 53: health_alert_events management
    open suspend fun insertHealthAlertEvent(event: HealthAlertEvent): Boolean {
        if (!isConfigured) return false
        return try {
            val payloadJson = JSONObject().apply {
                put("eventType", event.eventType.name)
                put("userId", event.userId)
                put("timestamp", event.timestamp)
                put("date", event.date)
                put("currentWaterMl", event.currentWaterMl)
                put("dailyGoalMl", event.dailyGoalMl)
                put("goalPercentage", event.goalPercentage)
                put("lastWaterIntakeAt", event.lastWaterIntakeAt)
                put("missedReminderCount", event.missedReminderCount)
                put("streak", event.streak)
                put("severity", event.severity.name)
            }

            val json = JSONObject().apply {
                put("id", event.eventId)
                put("user_id", event.userId)
                put("event_type", event.eventType.name)
                put("severity", event.severity.name)
                put("payload", payloadJson)
                put("created_at", event.timestamp)
                put("sent_at", event.sentAt ?: System.currentTimeMillis())
                put("acknowledged_at", event.acknowledgedAt)
            }
            val body = json.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("$supabaseUrl/rest/v1/health_alert_events")
                .addHeader("apikey", supabaseKey)
                .addHeader("Authorization", "Bearer $supabaseKey")
                .addHeader("Prefer", "resolution=merge-duplicates")
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            Log.e(TAG, "Error inserting health alert event: ${e.message}")
            false
        }
    }

    open suspend fun acknowledgeHealthAlertEvent(eventId: String): Boolean {
        if (!isConfigured) return false
        return try {
            val json = JSONObject().apply {
                put("acknowledged_at", System.currentTimeMillis())
            }
            val body = json.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("$supabaseUrl/rest/v1/health_alert_events?id=eq.$eventId")
                .addHeader("apikey", supabaseKey)
                .addHeader("Authorization", "Bearer $supabaseKey")
                .patch(body)
                .build()

            val response = client.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            Log.e(TAG, "Error acknowledging health alert event: ${e.message}")
            false
        }
    }

    // Section 53 & 58: health_companion_connections management
    open suspend fun syncCompanionConnection(companion: HealthCompanionConnection): Boolean {
        if (!isConfigured) return false
        return try {
            val json = JSONObject().apply {
                put("id", companion.id)
                put("user_id", companion.userId)
                put("companion_user_id", companion.companionUserId)
                put("companion_name", companion.companionName)
                put("status", companion.status.name)
                put("alert_policy", companion.alertPolicy.name)
                put("last_active_at", companion.lastActiveAt)
                put("created_at", companion.createdAt)
                put("updated_at", companion.updatedAt)
            }
            val body = json.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("$supabaseUrl/rest/v1/health_companion_connections")
                .addHeader("apikey", supabaseKey)
                .addHeader("Authorization", "Bearer $supabaseKey")
                .addHeader("Prefer", "resolution=merge-duplicates")
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing companion connection: ${e.message}")
            false
        }
    }

    // Section 70: Health Companion Catch-Up - Fetch unacknowledged events
    open suspend fun fetchUnacknowledgedEvents(userId: String): List<HealthAlertEvent> {
        if (!isConfigured) return emptyList()
        return try {
            val request = Request.Builder()
                .url("$supabaseUrl/rest/v1/health_alert_events?user_id=eq.$userId&acknowledged_at=is.null&order=created_at.asc")
                .addHeader("apikey", supabaseKey)
                .addHeader("Authorization", "Bearer $supabaseKey")
                .get()
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return emptyList()
            val responseBody = response.body?.string() ?: return emptyList()
            val array = org.json.JSONArray(responseBody)
            val list = mutableListOf<HealthAlertEvent>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val payloadObj = obj.optJSONObject("payload")
                list.add(
                    HealthAlertEvent(
                        eventId = obj.getString("id"),
                        eventType = try {
                            com.example.domain.model.HealthEventType.valueOf(obj.getString("event_type"))
                        } catch (e: Exception) {
                            com.example.domain.model.HealthEventType.WATER_CONSUMED
                        },
                        userId = obj.getString("user_id"),
                        timestamp = obj.optLong("created_at", System.currentTimeMillis()),
                        date = payloadObj?.optString("date") ?: "",
                        currentWaterMl = payloadObj?.optInt("currentWaterMl") ?: 0,
                        dailyGoalMl = payloadObj?.optInt("dailyGoalMl") ?: 2000,
                        goalPercentage = payloadObj?.optInt("goalPercentage") ?: 0,
                        lastWaterIntakeAt = if (payloadObj?.has("lastWaterIntakeAt") == true) payloadObj.optLong("lastWaterIntakeAt") else null,
                        missedReminderCount = payloadObj?.optInt("missedReminderCount") ?: 0,
                        streak = payloadObj?.optInt("streak") ?: 1,
                        severity = try {
                            com.example.domain.model.AlertSeverity.valueOf(obj.optString("severity", "LOW"))
                        } catch (e: Exception) {
                            com.example.domain.model.AlertSeverity.LOW
                        },
                        deliveryStatus = com.example.domain.model.EventDeliveryStatus.SENT,
                        sentAt = if (obj.has("sent_at")) obj.optLong("sent_at") else null,
                        acknowledgedAt = null
                    )
                )
            }
            list
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching unacknowledged events: ${e.message}")
            emptyList()
        }
    }

    // Section 70: Fetch latest authoritative health state for user
    open suspend fun fetchLatestHealthState(userId: String): SupabaseHealthSyncEvent? {
        if (!isConfigured) return null
        return try {
            val request = Request.Builder()
                .url("$supabaseUrl/rest/v1/health_sync_events?user_id=eq.$userId&order=timestamp.desc&limit=1")
                .addHeader("apikey", supabaseKey)
                .addHeader("Authorization", "Bearer $supabaseKey")
                .get()
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return null
            val body = response.body?.string() ?: return null
            val array = org.json.JSONArray(body)
            if (array.length() == 0) return null
            val obj = array.getJSONObject(0)
            SupabaseHealthSyncEvent(
                userId = obj.getString("user_id"),
                dailyIntakeMl = obj.getInt("daily_intake_ml"),
                dailyGoalMl = obj.getInt("daily_goal_ml"),
                streakDays = obj.getInt("streak_days"),
                timestamp = obj.optLong("timestamp", System.currentTimeMillis())
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching latest health state: ${e.message}")
            null
        }
    }

    // Section 74: Server Reconciliation - Fetch water intakes from server
    open suspend fun fetchWaterIntakes(userId: String, sinceTimestamp: Long = 0): List<SupabaseWaterIntake> {
        if (!isConfigured) return emptyList()
        return try {
            val request = Request.Builder()
                .url("$supabaseUrl/rest/v1/water_intakes?user_id=eq.$userId&consumed_at=gte.$sinceTimestamp&order=consumed_at.asc")
                .addHeader("apikey", supabaseKey)
                .addHeader("Authorization", "Bearer $supabaseKey")
                .get()
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return emptyList()
            val body = response.body?.string() ?: return emptyList()
            val array = org.json.JSONArray(body)
            val list = mutableListOf<SupabaseWaterIntake>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    SupabaseWaterIntake(
                        id = obj.getString("id"),
                        userId = obj.getString("user_id"),
                        amountMl = obj.getInt("amount_ml"),
                        consumedAt = obj.getLong("consumed_at"),
                        source = obj.optString("source", "app_quick"),
                        createdAt = obj.optLong("created_at", System.currentTimeMillis())
                    )
                )
            }
            list
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching water intakes from Supabase: ${e.message}")
            emptyList()
        }
    }

    // Health Companion Room Creation & Invite Code via Supabase
    open suspend fun createCompanionRoom(userId: String, hostName: String, roomCode: String): Boolean {
        if (!isConfigured) return true
        return try {
            val json = JSONObject().apply {
                put("room_code", roomCode)
                put("host_user_id", userId)
                put("host_name", hostName)
                put("created_at", System.currentTimeMillis())
                put("status", "OPEN")
            }
            val body = json.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("$supabaseUrl/rest/v1/companion_rooms")
                .addHeader("apikey", supabaseKey)
                .addHeader("Authorization", "Bearer $supabaseKey")
                .addHeader("Prefer", "resolution=merge-duplicates")
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            Log.e(TAG, "Error creating companion room in Supabase: ${e.message}")
            false
        }
    }

    open suspend fun joinCompanionRoom(
        roomCode: String,
        companionUserId: String,
        companionName: String
    ): HealthCompanionConnection? {
        if (!isConfigured) {
            // When offline or unconfigured, return a local connection
            return HealthCompanionConnection(
                id = java.util.UUID.randomUUID().toString(),
                userId = companionUserId,
                companionUserId = "host_$roomCode",
                companionName = "همراه سلامت ($roomCode)",
                status = com.example.domain.model.CompanionConnectionStatus.CONNECTED,
                alertPolicy = com.example.domain.model.AlertFilterPolicy.ALL,
                lastActiveAt = System.currentTimeMillis(),
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        }
        return try {
            val request = Request.Builder()
                .url("$supabaseUrl/rest/v1/companion_rooms?room_code=eq.$roomCode&limit=1")
                .addHeader("apikey", supabaseKey)
                .addHeader("Authorization", "Bearer $supabaseKey")
                .get()
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return null
            val body = response.body?.string() ?: return null
            val array = org.json.JSONArray(body)
            if (array.length() == 0) return null
            val roomObj = array.getJSONObject(0)
            val hostUserId = roomObj.getString("host_user_id")
            val hostName = roomObj.optString("host_name", "همراه سلامت")

            val connection = HealthCompanionConnection(
                id = java.util.UUID.randomUUID().toString(),
                userId = hostUserId,
                companionUserId = companionUserId,
                companionName = companionName,
                status = com.example.domain.model.CompanionConnectionStatus.CONNECTED,
                alertPolicy = com.example.domain.model.AlertFilterPolicy.ALL,
                lastActiveAt = System.currentTimeMillis(),
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            syncCompanionConnection(connection)
            connection
        } catch (e: Exception) {
            Log.e(TAG, "Error joining companion room in Supabase: ${e.message}")
            null
        }
    }

    // Profile photo upload to Supabase Storage (avatars bucket)
    open suspend fun uploadAvatar(userId: String, imageBytes: ByteArray, mimeType: String = "image/jpeg"): String? {
        if (!isConfigured) return null
        return try {
            val body = imageBytes.toRequestBody(mimeType.toMediaType())
            val objectPath = "user_${userId}_avatar.jpg"
            val request = Request.Builder()
                .url("$supabaseUrl/storage/v1/object/avatars/$objectPath")
                .addHeader("apikey", supabaseKey)
                .addHeader("Authorization", "Bearer $supabaseKey")
                .addHeader("x-upsert", "true")
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                "$supabaseUrl/storage/v1/object/public/avatars/$objectPath"
            } else {
                Log.e(TAG, "Failed to upload avatar to Supabase Storage: ${response.code}")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading avatar to Supabase Storage: ${e.message}")
            null
        }
    }

    companion object {
        private const val TAG = "SupabaseClient"
    }
}
