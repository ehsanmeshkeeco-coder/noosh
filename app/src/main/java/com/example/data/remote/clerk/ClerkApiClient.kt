package com.example.data.remote.clerk

import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

sealed class ClerkAuthResult {
    data class Success(val user: ClerkUser, val sessionToken: String?) : ClerkAuthResult()
    data class NeedsVerification(val signUpId: String, val email: String, val clientToken: String) : ClerkAuthResult()
    data class Error(val message: String) : ClerkAuthResult()
}

class ClerkApiClient(
    private val publishableKey: String
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    val frontendApiHost: String = extractFrontendApi(publishableKey)

    val isConfigured: Boolean
        get() = publishableKey.isNotBlank() && frontendApiHost.isNotBlank()

    /**
     * Native sign-up request to Clerk Frontend API:
     * POST /v1/client/sign_ups?_is_native=1
     */
    suspend fun signUpWithEmail(
        email: String,
        firstName: String,
        password: String? = null
    ): ClerkAuthResult = withContext(Dispatchers.IO) {
        if (!isConfigured) {
            return@withContext ClerkAuthResult.Error("کلید Clerk تنظیم نشده است.")
        }

        try {
            val username = "u_${email.substringBefore("@").replace(Regex("[^a-zA-Z0-9_]"), "")}_${System.currentTimeMillis() % 10000}"
            val pwd = if (password.isNullOrBlank()) {
                "Noosh_${email.hashCode().toUInt()}_A!${System.currentTimeMillis() % 1000}"
            } else {
                password
            }

            val formBuilder = FormBody.Builder()
                .add("email_address", email.trim())
                .add("username", username)
                .add("password", pwd)
                .add("first_name", firstName.trim())

            val request = Request.Builder()
                .url("https://$frontendApiHost/v1/client/sign_ups?_is_native=1")
                .addHeader("Authorization", "Bearer $publishableKey")
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .post(formBuilder.build())
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            val clientAuthToken = response.header("Authorization") ?: ""

            Log.d(TAG, "Clerk SignUp Response: code=${response.code} body=$responseBody")

            if (response.isSuccessful) {
                val json = JSONObject(responseBody)
                val responseObj = json.optJSONObject("response") ?: JSONObject()
                val signUpId = responseObj.optString("id")
                val createdUserId = responseObj.optString("created_user_id", "")
                val clientObj = json.optJSONObject("client")

                // If user is already created without verification needed
                if (createdUserId.isNotBlank()) {
                    val user = ClerkUser(
                        id = createdUserId,
                        firstName = firstName,
                        email = email,
                        avatarUrl = null,
                        isGuest = false
                    )
                    ClerkAuthResult.Success(user, clientAuthToken)
                } else if (signUpId.isNotBlank()) {
                    // Send verification code (email_code)
                    prepareVerification(signUpId, clientAuthToken)
                    val dummyUserId = "clerk_pending_${signUpId.takeLast(8)}"
                    val user = ClerkUser(
                        id = dummyUserId,
                        firstName = firstName,
                        email = email,
                        avatarUrl = null,
                        isGuest = false
                    )
                    ClerkAuthResult.Success(user, clientAuthToken)
                } else {
                    ClerkAuthResult.Error("پاسخ ثبت‌نام از سرور دریافت نشد")
                }
            } else {
                val json = try { JSONObject(responseBody) } catch (e: Exception) { null }
                val errors = json?.optJSONArray("errors")
                val firstError = errors?.optJSONObject(0)
                val errMsg = firstError?.optString("message") ?: "خطا در ثبت‌نام با کد ${response.code}"
                Log.w(TAG, "Clerk SignUp Error: $errMsg")
                ClerkAuthResult.Error(errMsg)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in Clerk signUp: ${e.message}", e)
            ClerkAuthResult.Error("خطا در اتصال به سرور Clerk: ${e.localizedMessage}")
        }
    }

    private fun prepareVerification(signUpId: String, clientAuthToken: String) {
        try {
            val body = FormBody.Builder()
                .add("strategy", "email_code")
                .build()

            val request = Request.Builder()
                .url("https://$frontendApiHost/v1/client/sign_ups/$signUpId/prepare_verification?_is_native=1")
                .addHeader("Authorization", clientAuthToken)
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            Log.d(TAG, "prepareVerification: code=${response.code}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to prepare verification: ${e.message}")
        }
    }

    companion object {
        private const val TAG = "ClerkApiClient"

        fun extractFrontendApi(publishableKey: String): String {
            return try {
                if (!publishableKey.startsWith("pk_")) return ""
                val base64Part = publishableKey.substringAfter("pk_test_").substringAfter("pk_live_")
                val decoded = String(Base64.decode(base64Part, Base64.DEFAULT)).trim()
                decoded.removeSuffix("$")
            } catch (e: Exception) {
                ""
            }
        }
    }
}
