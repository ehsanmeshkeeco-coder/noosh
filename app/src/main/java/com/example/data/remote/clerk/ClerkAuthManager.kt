package com.example.data.remote.clerk

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ClerkUser(
    val id: String,
    val firstName: String,
    val email: String,
    val avatarUrl: String?,
    val isGuest: Boolean = false
)

sealed class AuthState {
    object Loading : AuthState()
    data class Authenticated(val user: ClerkUser) : AuthState()
    object Unauthenticated : AuthState()
}

class ClerkAuthManager(
    context: Context,
    val publishableKey: String = ""
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("clerk_auth_prefs", Context.MODE_PRIVATE)

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        checkInitialSession()
    }

    private fun checkInitialSession() {
        val savedUserId = prefs.getString("user_id", null)
        if (savedUserId != null) {
            val user = ClerkUser(
                id = savedUserId,
                firstName = prefs.getString("first_name", "کاربر نوش") ?: "کاربر نوش",
                email = prefs.getString("email", "user@noosh.app") ?: "user@noosh.app",
                avatarUrl = prefs.getString("avatar_url", null),
                isGuest = prefs.getBoolean("is_guest", false)
            )
            _authState.value = AuthState.Authenticated(user)
        } else {
            // By default initialize with guest/offline user for immediate frictionless UX
            val defaultUser = ClerkUser(
                id = "default_user",
                firstName = "کاربر گرامی",
                email = "user@noosh.app",
                avatarUrl = null,
                isGuest = true
            )
            _authState.value = AuthState.Authenticated(defaultUser)
        }
    }

    fun signInWithEmail(email: String, name: String) {
        val user = ClerkUser(
            id = "user_${email.hashCode()}",
            firstName = name.ifBlank { "کاربر نوش" },
            email = email,
            avatarUrl = null,
            isGuest = false
        )
        saveUser(user)
        _authState.value = AuthState.Authenticated(user)
    }

    fun continueAsGuest() {
        val user = ClerkUser(
            id = "default_user",
            firstName = "کاربر مهمان",
            email = "guest@noosh.app",
            avatarUrl = null,
            isGuest = true
        )
        saveUser(user)
        _authState.value = AuthState.Authenticated(user)
    }

    fun signOut() {
        prefs.edit().clear().apply()
        _authState.value = AuthState.Unauthenticated
    }

    private fun saveUser(user: ClerkUser) {
        prefs.edit()
            .putString("user_id", user.id)
            .putString("first_name", user.firstName)
            .putString("email", user.email)
            .putString("avatar_url", user.avatarUrl)
            .putBoolean("is_guest", user.isGuest)
            .apply()
    }
}
