package com.example.big

import android.graphics.Bitmap
import android.net.Uri
import java.util.Date

/**
 * 用户类，管理用户信息、状态和权限
 */
class User(// Getters
    // 基本用户信息
    val id: Int, // 用户ID
    // 用户名
    var username: String
) {
    // 用户状态枚举
    enum class UserStatus {
        LOGGED_OUT,  // 退出登录
        REGISTERED,  // 已注册但未登录
        LOGGED_IN // 已登录
    }

    // 权限类型
    enum class PermissionType {
        ALARM,  // 闹钟权限
        NOTIFICATION,  // 通知权限
        LOCATION,  // 位置权限
        STORAGE,  // 存储权限
        CALENDAR,  // 日历权限
        CONTACTS // 联系人权限
    }

    var nickname: String? = null // 昵称
        private set
    var email: String? = null // 邮箱
        private set
    var phoneNumber: String? = null // 手机号
        private set
    var avatar: Bitmap? = null // 头像（Bitmap格式）
        private set
    var avatarUri: Uri? = null // 头像URI
        private set
    var createdDate: Date? = null // 账户创建日期
        private set
    var lastLoginDate: Date? = null // 最后登录日期
        private set

    // 用户状态
    var status: UserStatus // 当前状态
        private set

    // 身份验证信息
    var token: String? = null // 登录令牌
        private set
    var tokenExpireDate: Date? = null // 令牌过期时间
        private set
    var refreshToken: String? = null // 刷新令牌
        private set

    // 用户偏好和设置
    private val permissions: MutableMap<PermissionType, Boolean> // 权限设置
    private val preferences: MutableMap<String, Any> // 其他用户偏好

    // 用户绑定的任务
    private val task_id: ArrayList<Int>? = null

    /**
     * 基本构造函数
     *
     * @param id       用户ID
     * @param username 用户名
     */
    init {
        this.username = username
        this.status = UserStatus.LOGGED_OUT
        this.permissions = HashMap()
        this.preferences = HashMap()
        initDefaultPermissions()
    }

    /**
     * 带有详细信息的构造函数
     *
     * @param id          用户ID
     * @param username    用户名
     * @param nickname    昵称
     * @param email       邮箱
     * @param phoneNumber 手机号
     */
    constructor(
        id: Int,
        username: String,
        nickname: String?,
        email: String?,
        phoneNumber: String?
    ) : this(id, username) {
        this.nickname = nickname
        this.email = email
        this.phoneNumber = phoneNumber
        this.createdDate = Date()
    }

    /**
     * 初始化默认权限设置
     */
    private fun initDefaultPermissions() {
        permissions[PermissionType.ALARM] = false
        permissions[PermissionType.NOTIFICATION] = false
        permissions[PermissionType.LOCATION] = false
        permissions[PermissionType.STORAGE] = false
        permissions[PermissionType.CALENDAR] = false
        permissions[PermissionType.CONTACTS] = false
    }

    /**
     * 更改用户状态
     *
     * @param newStatus 新状态
     * @return 是否成功更改状态
     */
    fun changeStatus(newStatus: UserStatus): Boolean {
        // 检查状态转换是否有效
        if (status == UserStatus.LOGGED_OUT && newStatus == UserStatus.LOGGED_IN) {
            // 需要先注册或有有效凭证
            if (token == null || isTokenExpired) {
                return false
            }
        }

        status = newStatus

        // 更新最后登录时间
        if (newStatus == UserStatus.LOGGED_IN) {
            lastLoginDate = Date()
        }

        return true
    }

    /**
     * 登录过程
     *
     * @param token        登录令牌
     * @param expireDate   令牌过期时间
     * @param refreshToken 刷新令牌
     * @return 是否登录成功
     */
    fun login(token: String?, expireDate: Date?, refreshToken: String?): Boolean {
        this.token = token
        this.tokenExpireDate = expireDate
        this.refreshToken = refreshToken
        return changeStatus(UserStatus.LOGGED_IN)
    }

    /**
     * 退出登录
     *
     * @return 是否成功退出
     */
    fun logout(): Boolean {
        this.token = null
        this.tokenExpireDate = null
        this.refreshToken = null
        return changeStatus(UserStatus.LOGGED_OUT)
    }

    val isTokenExpired: Boolean
        /**
         * 检查令牌是否过期
         *
         * @return 是否过期
         */
        get() {
            if (tokenExpireDate == null) {
                return true
            }
            return Date().after(tokenExpireDate)
        }

    /**
     * 刷新令牌
     *
     * @param newToken        新令牌
     * @param newExpireDate   新过期时间
     * @param newRefreshToken 新刷新令牌
     */
    fun refreshToken(newToken: String?, newExpireDate: Date?, newRefreshToken: String?) {
        this.token = newToken
        this.tokenExpireDate = newExpireDate
        this.refreshToken = newRefreshToken
    }

    /**
     * 更新用户昵称
     *
     * @param nickname 新昵称
     */
    fun updateNickname(nickname: String?) {
        this.nickname = nickname
    }

    /**
     * 更新用户头像 (Bitmap 格式)
     *
     * @param avatar 新头像
     */
    fun updateAvatar(avatar: Bitmap?) {
        this.avatar = avatar
    }

    /**
     * 更新用户头像 (URI 格式)
     *
     * @param avatarUri 新头像 URI
     */
    fun updateAvatar(avatarUri: Uri?) {
        this.avatarUri = avatarUri
    }

    /**
     * 更新用户邮箱
     *
     * @param email 新邮箱
     */
    fun updateEmail(email: String?) {
        this.email = email
    }

    /**
     * 更新用户手机号
     *
     * @param phoneNumber 新手机号
     */
    fun updatePhoneNumber(phoneNumber: String?) {
        this.phoneNumber = phoneNumber
    }

    /**
     * 设置权限
     *
     * @param type    权限类型
     * @param enabled 是否启用
     */
    fun setPermission(type: PermissionType, enabled: Boolean) {
        permissions[type] = enabled
    }

    /**
     * 检查是否拥有权限
     *
     * @param type 权限类型
     * @return 是否拥有权限
     */
    fun hasPermission(type: PermissionType): Boolean {
        val permission = permissions[type]
        return permission != null && permission
    }

    /**
     * 设置用户偏好
     *
     * @param key   偏好键
     * @param value 偏好值
     */
    fun setPreference(key: String, value: Any) {
        preferences[key] = value
    }

    /**
     * 获取用户偏好
     *
     * @param key 偏好键
     * @return 偏好值
     */
    fun getPreference(key: String): Any? {
        return preferences[key]
    }

    val userInfo: Map<String, Any?>
        /**
         * 获取用户完整信息的Map
         *
         * @return 用户信息Map
         */
        get() {
            val userInfo: MutableMap<String, Any?> =
                HashMap()
            userInfo["id"] = id
            userInfo["username"] = username
            userInfo["nickname"] = nickname
            userInfo["email"] = email
            userInfo["phoneNumber"] = phoneNumber
            userInfo["status"] = status
            userInfo["createdDate"] = createdDate
            userInfo["lastLoginDate"] = lastLoginDate
            userInfo["permissions"] = permissions
            userInfo["preferences"] = preferences
            return userInfo
        }

    val allPermissions: Map<PermissionType, Boolean>
        get() = permissions

    val allPreferences: Map<String, Any>
        get() = preferences
}