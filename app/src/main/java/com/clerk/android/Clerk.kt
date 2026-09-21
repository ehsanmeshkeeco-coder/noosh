package com.clerk.android

import android.content.Context

/**
 * Lightweight native Clerk client providing authentication state,
 * session management, and current user identity.
 */
object Clerk {

    data class User(
        val id: String,
        val firstName: String? = null,
        val email: String? = null,
        val avatarUrl: String? = null
    )

    private var currentUser: User? = null
    private var isConfigured: Boolean = false
    private var publishableKey: String? = null

    fun configure(context: Context, publishableKey: String) {
        this.publishableKey = publishableKey
        this.isConfigured = publishableKey.isNotBlank()
    }

    fun getUser(): User? = currentUser

    fun setUser(user: User?) {
        this.currentUser = user
    }

    fun signOut() {
        this.currentUser = null
    }

    fun isConfigured(): Boolean = isConfigured

    fun getPublishableKey(): String? = publishableKey
}
