// UserManager.kt
package com.example.big.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.big.models.UserResponse
import com.google.gson.Gson

object UserManager {
    private const val TAG = "UserManager"
    private const val PREF_NAME = "UserPrefs"
    private const val KEY_USER_INFO = "user_info"

    private lateinit var prefs: SharedPreferences
    private val gson = Gson()

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

        // 在初始化时，将当前用户ID设置到任务管理器
        getUserId()?.let { userId ->
            TaskManager.setCurrentUserId(userId)
            Log.d(TAG, "已设置当前用户ID: $userId")
        }
    }

    fun saveUserInfo(user: UserResponse) {
        val userJson = gson.toJson(user)
        prefs.edit().putString(KEY_USER_INFO, userJson).apply()

        // 当保存用户信息时，更新任务管理器中的用户ID
        user.id?.let { userId ->
            TaskManager.setCurrentUserId(userId)
            Log.d(TAG, "用户信息已保存，并更新任务管理器: $userId")
        }
    }

    fun getUserInfo(): UserResponse? {
        val userJson = prefs.getString(KEY_USER_INFO, null) ?: return null
        return try {
            gson.fromJson(userJson, UserResponse::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "解析用户信息失败", e)
            null
        }
    }

    // 新增方法：获取用户ID
    fun getUserId(): String? {
        return getUserInfo()?.id
    }

    // 新增方法：获取用户名
    fun getUserName(): String? {
        return getUserInfo()?.username
    }

    // 新增方法：检查用户是否已登录
    fun isLoggedIn(): Boolean {
        return getUserInfo() != null
    }

    fun clearUserInfo() {
        prefs.edit().remove(KEY_USER_INFO).apply()
        Log.d(TAG, "用户信息已清除")
    }
}