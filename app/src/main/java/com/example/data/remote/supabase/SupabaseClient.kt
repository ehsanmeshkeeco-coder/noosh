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

class SupabaseClient(
    val supabaseUrl: String = "",
    val supabaseKey: String = ""
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    val isConfigured: Boolean
        get() = supabaseUrl.isNotBlank() && supabaseKey.isNotBlank() && !supabaseUrl.contains("YOUR_")

    suspend fun insertWaterIntake(intake: SupabaseWaterIntake): Boolean {
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

    suspend fun syncHealthEvent(event: SupabaseHealthSyncEvent): Boolean {
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
    suspend fun insertHealthAlertEvent(event: HealthAlertEvent): Boolean {
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

    suspend fun acknowledgeHealthAlertEvent(eventId: String): Boolean {
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
    suspend fun syncCompanionConnection(companion: HealthCompanionConnection): Boolean {
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

    companion object {
        private const val TAG = "SupabaseClient"
    }
}
