package com.example.big.utils

import android.content.Context
import android.content.SharedPreferences
import java.util.*

class TokenManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)

    fun saveTokens(accessToken: String, refreshToken: String, expiresIn: Long) {
        with(prefs.edit()) {
            putString("access_token", accessToken)
            putString("refresh_token", refreshToken)
            putLong("expires_at", System.currentTimeMillis() + expiresIn * 1000)
            apply()
        }
    }

    fun getAccessToken(): String? = prefs.getString("access_token", null)

    fun getRefreshToken(): String? = prefs.getString("refresh_token", null)

    fun isTokenExpired(): Boolean {
        val expiresAt = prefs.getLong("expires_at", 0)
        return System.currentTimeMillis() > expiresAt
    }

    fun clearTokens() {
        prefs.edit().clear().apply()
    }

    fun getAuthHeader(): String? {
        val token = getAccessToken()
        return if (token != null) "Bearer $token" else null
    }
}