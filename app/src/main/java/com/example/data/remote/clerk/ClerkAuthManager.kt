package com.example.data.remote.clerk

import android.content.Context
import com.clerk.android.Clerk
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
    private val context: Context,
    private val publishableKey: String = com.example.BuildConfig.CLERK_PUBLISHABLE_KEY
) {

    init {
        val key = publishableKey.ifBlank { com.example.BuildConfig.CLERK_PUBLISHABLE_KEY }
        if (key.isNotBlank()) {
            Clerk.configure(context.applicationContext, key)
        }
    }

    private val prefs = context.getSharedPreferences("clerk_auth_prefs", Context.MODE_PRIVATE)
    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        checkInitialSession()
    }

    private fun checkInitialSession() {
        val current = Clerk.getUser()
        if (current != null) {
            val user = ClerkUser(
                id = current.id,
                firstName = current.firstName ?: "کاربر نوش",
                email = current.email ?: "user@noosh.app",
                avatarUrl = current.avatarUrl,
                isGuest = false
            )
            saveUser(user)
            _authState.value = AuthState.Authenticated(user)
        } else {
            val savedUserId = prefs.getString("user_id", null)
            if (savedUserId != null) {
                val user = ClerkUser(
                    id = savedUserId,
                    firstName = prefs.getString("first_name", "کاربر نوش") ?: "کاربر نوش",
                    email = prefs.getString("email", "user@noosh.app") ?: "user@noosh.app",
                    avatarUrl = prefs.getString("avatar_url", null),
                    isGuest = prefs.getBoolean("is_guest", false)
                )
                Clerk.setUser(
                    Clerk.User(
                        id = user.id,
                        firstName = user.firstName,
                        email = user.email,
                        avatarUrl = user.avatarUrl
                    )
                )
                _authState.value = AuthState.Authenticated(user)
            } else {
                _authState.value = AuthState.Unauthenticated
            }
        }
    }

    fun signInWithEmail(email: String, name: String) {
        val user = ClerkUser(
            id = Clerk.getUser()?.id ?: "user_${email.hashCode()}",
            firstName = name.ifBlank { "کاربر نوش" },
            email = email,
            avatarUrl = Clerk.getUser()?.avatarUrl,
            isGuest = false
        )
        saveUser(user)
        _authState.value = AuthState.Authenticated(user)
    }

    fun continueAsGuest() {
        val user = ClerkUser(
            id = "guest_" + System.currentTimeMillis(),
            firstName = "کاربر مهمان",
            email = "guest@noosh.app",
            avatarUrl = null,
            isGuest = true
        )
        saveUser(user)
        _authState.value = AuthState.Authenticated(user)
    }

    fun signOut() {
        Clerk.signOut()
        prefs.edit().clear().apply()
        _authState.value = AuthState.Unauthenticated
    }

    private fun saveUser(user: ClerkUser) {
        Clerk.setUser(
            Clerk.User(
                id = user.id,
                firstName = user.firstName,
                email = user.email,
                avatarUrl = user.avatarUrl
            )
        )
        prefs.edit()
            .putString("user_id", user.id)
            .putString("first_name", user.firstName)
            .putString("email", user.email)
            .putString("avatar_url", user.avatarUrl)
            .putBoolean("is_guest", user.isGuest)
            .apply()
    }
}
