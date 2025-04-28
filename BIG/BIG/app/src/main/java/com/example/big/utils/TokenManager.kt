package com.example.big.utils

import android.content.Context
import android.content.SharedPreferences

/**
 * TokenManager 负责管理身份验证令牌的存储、检索和验证
 */
object TokenManager {
    private const val PREF_NAME = "auth_prefs"
    private const val KEY_ACCESS_TOKEN = "access_token"
    private const val KEY_REFRESH_TOKEN = "refresh_token"
    private const val KEY_EXPIRES_AT = "expires_at"
    private const val KEY_USER_ID = "user_id"

    private lateinit var prefs: SharedPreferences

    /**
     * 初始化 TokenManager
     * 必须在使用其他方法前调用
     */
    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    /**
     * 保存身份验证令牌和过期时间
     * @param accessToken 访问令牌
     * @param refreshToken 刷新令牌
     * @param expiresIn 令牌有效期（秒）
     */
    fun saveTokens(accessToken: String, refreshToken: String, expiresIn: Long) {
        with(prefs.edit()) {
            putString(KEY_ACCESS_TOKEN, accessToken)
            putString(KEY_REFRESH_TOKEN, refreshToken)
            putLong(KEY_EXPIRES_AT, System.currentTimeMillis() + expiresIn * 1000)
            apply()
        }
    }

    /**
     * 获取访问令牌
     * @return 访问令牌，如果不存在则返回 null
     */
    fun getAccessToken(): String? = prefs.getString(KEY_ACCESS_TOKEN, null)

    /**
     * 获取刷新令牌
     * @return 刷新令牌，如果不存在则返回 null
     */
    fun getRefreshToken(): String? = prefs.getString(KEY_REFRESH_TOKEN, null)

    /**
     * 检查令牌是否过期
     * @return 如果令牌已过期返回 true，否则返回 false
     */
    fun isTokenExpired(): Boolean {
        val expiresAt = prefs.getLong(KEY_EXPIRES_AT, 0)
        return System.currentTimeMillis() > expiresAt
    }

    /**
     * 计算令牌还有多少秒到期
     * @return 剩余秒数，如果已过期则返回负数
     */
    fun getTimeToExpiration(): Long {
        val expiresAt = prefs.getLong(KEY_EXPIRES_AT, 0)
        return (expiresAt - System.currentTimeMillis()) / 1000
    }

    /**
     * 获取授权请求头
     * @return 格式为 "Bearer {token}" 的授权头，如果没有令牌则返回 null
     */
    fun getAuthHeader(): String? {
        val token = getAccessToken()
        return if (token != null) "Bearer $token" else null
    }

    /**
     * 保存用户ID
     * @param userId 用户ID
     */
    fun saveUserId(userId: String) {
        prefs.edit().putString(KEY_USER_ID, userId).apply()
    }

    /**
     * 获取用户ID
     * @return 用户ID，如果不存在则返回 null
     */
    fun getUserId(): String? = prefs.getString(KEY_USER_ID, null)

    /**
     * 清除所有存储的令牌和用户信息
     */
    fun clearTokens() {
        prefs.edit().apply {
            remove(KEY_ACCESS_TOKEN)
            remove(KEY_REFRESH_TOKEN)
            remove(KEY_EXPIRES_AT)
            remove(KEY_USER_ID)
            apply()
        }
    }

    /**
     * 检查用户是否已登录
     * @return 如果用户已登录返回 true，否则返回 false
     */
    fun isLoggedIn(): Boolean {
        val token = getAccessToken()
        return token != null && !isTokenExpired()
    }
}