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
            return@withContext ClerkAuthResult.Error("تنظیمات احراز هویت سرور در دسترس نیست.")
        }

        try {
            val cleanEmail = email.trim()
            val emailPrefix = cleanEmail.substringBefore("@").replace(Regex("[^a-zA-Z0-9_]"), "")
            val username = "u_${emailPrefix}_${System.currentTimeMillis() % 100000}"
            val pwd = if (password.isNullOrBlank()) {
                "Noosh#App_${cleanEmail.hashCode().toUInt()}!X${System.currentTimeMillis() % 1000}"
            } else {
                password.trim()
            }

            val formBuilder = FormBody.Builder()
                .add("email_address", cleanEmail)
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
                val status = responseObj.optString("status")

                if (createdUserId.isNotBlank()) {
                    val user = ClerkUser(
                        id = createdUserId,
                        firstName = firstName.trim(),
                        email = cleanEmail,
                        avatarUrl = null,
                        isGuest = false
                    )
                    ClerkAuthResult.Success(user, clientAuthToken)
                } else if (signUpId.isNotBlank()) {
                    prepareVerification(signUpId, clientAuthToken)
                    val user = ClerkUser(
                        id = signUpId,
                        firstName = firstName.trim(),
                        email = cleanEmail,
                        avatarUrl = null,
                        isGuest = false
                    )
                    ClerkAuthResult.Success(user, clientAuthToken)
                } else {
                    ClerkAuthResult.Error("پاسخی از سرور دریافت نشد.")
                }
            } else {
                val json = try { JSONObject(responseBody) } catch (e: Exception) { null }
                val errors = json?.optJSONArray("errors")
                val firstError = errors?.optJSONObject(0)
                val errorCode = firstError?.optString("code") ?: ""

                // If identifier already exists, automatically attempt sign-in
                if (errorCode == "form_identifier_exists") {
                    return@withContext signInWithEmail(cleanEmail, pwd, firstName)
                }

                val errorMsg = when (errorCode) {
                    "form_password_pwned" -> "این رمز عبور به دلیل نقض امنیتی عمومی ناامن است. لطفاً رمز عبور دیگری انتخاب کنید."
                    "form_password_length_too_short" -> "رمز عبور باید حداقل ۸ کاراکتر باشد."
                    "form_param_format_invalid" -> "فرمت ایمیل نامعتبر است."
                    else -> firstError?.optString("message") ?: "خطا در ثبت‌نام با کد ${response.code}"
                }
                Log.w(TAG, "Clerk SignUp Error: $errorMsg (code: $errorCode)")
                ClerkAuthResult.Error(errorMsg)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in Clerk signUp: ${e.message}", e)
            ClerkAuthResult.Error("خطا در اتصال به سرور: ${e.localizedMessage}")
        }
    }

    /**
     * Native sign-in request to Clerk Frontend API:
     * POST /v1/client/sign_ins?_is_native=1
     */
    suspend fun signInWithEmail(
        email: String,
        password: String,
        firstName: String? = null
    ): ClerkAuthResult = withContext(Dispatchers.IO) {
        if (!isConfigured) {
            return@withContext ClerkAuthResult.Error("تنظیمات احراز هویت سرور در دسترس نیست.")
        }

        try {
            val cleanEmail = email.trim()
            val formBuilder = FormBody.Builder()
                .add("identifier", cleanEmail)
                .add("password", password.trim())

            val request = Request.Builder()
                .url("https://$frontendApiHost/v1/client/sign_ins?_is_native=1")
                .addHeader("Authorization", "Bearer $publishableKey")
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .post(formBuilder.build())
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            val clientAuthToken = response.header("Authorization") ?: ""

            Log.d(TAG, "Clerk SignIn Response: code=${response.code} body=$responseBody")

            if (response.isSuccessful) {
                val json = JSONObject(responseBody)
                val responseObj = json.optJSONObject("response") ?: JSONObject()
                val userData = responseObj.optJSONObject("user_data")
                val resolvedUserId = responseObj.optString("created_user_id").ifBlank {
                    userData?.optString("id") ?: responseObj.optString("id")
                }
                val resolvedName = firstName?.ifBlank { null }
                    ?: userData?.optString("first_name")
                    ?: cleanEmail.substringBefore("@")

                val user = ClerkUser(
                    id = resolvedUserId,
                    firstName = resolvedName,
                    email = cleanEmail,
                    avatarUrl = userData?.optString("image_url"),
                    isGuest = false
                )
                ClerkAuthResult.Success(user, clientAuthToken)
            } else {
                val json = try { JSONObject(responseBody) } catch (e: Exception) { null }
                val errors = json?.optJSONArray("errors")
                val firstError = errors?.optJSONObject(0)
                val errorCode = firstError?.optString("code") ?: ""

                val errorMsg = when (errorCode) {
                    "form_password_incorrect" -> "رمز عبور وارد شده اشتباه است."
                    "form_identifier_not_found" -> "حسابی با این ایمیل یافت نشد. برای ثبت‌نام مشخصات خود را تکمیل کنید."
                    "form_password_pwned" -> "این رمز عبور به دلیل نقض امنیتی عمومی ناامن است. لطفاً رمز قوی‌تری انتخاب کنید."
                    else -> firstError?.optString("message") ?: "خطا در ورود به حساب کاربری (کد ${response.code})"
                }
                Log.w(TAG, "Clerk SignIn Error: $errorMsg (code: $errorCode)")
                ClerkAuthResult.Error(errorMsg)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in Clerk signIn: ${e.message}", e)
            ClerkAuthResult.Error("خطا در ورود به حساب: ${e.localizedMessage}")
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
                val base64Part = publishableKey
                    .removePrefix("pk_test_")
                    .removePrefix("pk_live_")
                    .trim()
                    .removeSuffix("$")
                val padded = base64Part + "=".repeat((-base64Part.length).mod(4))
                val decoded = String(Base64.decode(padded, Base64.DEFAULT)).trim()
                decoded.removeSuffix("$")
            } catch (e: Exception) {
                ""
            }
        }
    }
}
