package com.example.fcm

import android.content.Context
import android.os.Build
import android.util.Log
import com.example.data.remote.model.UserDevice
import com.example.data.remote.supabase.SupabaseClient
import com.example.domain.repository.UserRepository
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class FcmTokenManager(
    private val context: Context,
    private val supabaseClient: SupabaseClient,
    private val userRepository: UserRepository,
    private val scope: CoroutineScope
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getCurrentToken(): String? {
        return prefs.getString(KEY_FCM_TOKEN, null)
    }

    fun initTokenRegistration() {
        scope.launch(Dispatchers.IO) {
            try {
                val token = FirebaseMessaging.getInstance().token.await()
                if (!token.isNullOrBlank()) {
                    onNewToken(token)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to retrieve initial FCM token: ${e.message}")
            }
        }
    }

    fun onNewToken(token: String) {
        val current = prefs.getString(KEY_FCM_TOKEN, null)
        prefs.edit().putString(KEY_FCM_TOKEN, token).apply()

        scope.launch(Dispatchers.IO) {
            try {
                val profile = userRepository.getUserProfile()
                val deviceName = "${Build.MANUFACTURER} ${Build.MODEL}"
                val device = UserDevice(
                    userId = profile.id,
                    fcmToken = token,
                    platform = "android",
                    deviceName = deviceName,
                    appVersion = "1.0",
                    lastSeenAt = System.currentTimeMillis(),
                    enabled = true
                )
                supabaseClient.registerOrUpdateDevice(device)
                Log.d(TAG, "FCM token registered for user: ${profile.id}")
            } catch (e: Exception) {
                Log.e(TAG, "Error registering FCM token: ${e.message}")
            }
        }
    }

    fun onUserLogin(userId: String) {
        val token = getCurrentToken() ?: return
        scope.launch(Dispatchers.IO) {
            val device = UserDevice(
                userId = userId,
                fcmToken = token,
                platform = "android",
                deviceName = "${Build.MANUFACTURER} ${Build.MODEL}",
                appVersion = "1.0",
                lastSeenAt = System.currentTimeMillis(),
                enabled = true
            )
            supabaseClient.registerOrUpdateDevice(device)
        }
    }

    fun onUserLogout() {
        val token = getCurrentToken() ?: return
        scope.launch(Dispatchers.IO) {
            supabaseClient.disableDevice(token)
        }
    }

    companion object {
        private const val TAG = "FcmTokenManager"
        private const val PREFS_NAME = "noosh_fcm_prefs"
        private const val KEY_FCM_TOKEN = "fcm_registration_token"
    }
}
