// UserManager.kt
package com.example.big.utils

import android.content.Context
import android.content.SharedPreferences
import com.example.big.models.UserResponse
import com.google.gson.Gson

object UserManager {
    private const val PREF_NAME = "UserPrefs"
    private const val KEY_USER_INFO = "user_info"

    private lateinit var prefs: SharedPreferences
    private val gson = Gson()

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun saveUserInfo(user: UserResponse) {
        val userJson = gson.toJson(user)
        prefs.edit().putString(KEY_USER_INFO, userJson).apply()
    }

    fun getUserInfo(): UserResponse? {
        val userJson = prefs.getString(KEY_USER_INFO, null) ?: return null
        return try {
            gson.fromJson(userJson, UserResponse::class.java)
        } catch (e: Exception) {
            null
        }
    }

    fun clearUserInfo() {
        prefs.edit().remove(KEY_USER_INFO).apply()
    }
}