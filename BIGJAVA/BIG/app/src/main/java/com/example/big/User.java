package com.example.big;

import android.graphics.Bitmap;
import android.net.Uri;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import java.util.ArrayList;

/**
 * 用户类，管理用户信息、状态和权限
 */
public class User {
    // 用户状态枚举
    public enum UserStatus {
        LOGGED_OUT, // 退出登录
        REGISTERED, // 已注册但未登录
        LOGGED_IN // 已登录
    }

    // 权限类型
    public enum PermissionType {
        ALARM, // 闹钟权限
        NOTIFICATION, // 通知权限
        LOCATION, // 位置权限
        STORAGE, // 存储权限
        CALENDAR, // 日历权限
        CONTACTS // 联系人权限
    }

    // 基本用户信息
    private final int id; // 用户ID
    private final String username; // 用户名
    private String nickname; // 昵称
    private String email; // 邮箱
    private String phoneNumber; // 手机号
    private Bitmap avatar; // 头像（Bitmap格式）
    private Uri avatarUri; // 头像URI
    private Date createdDate; // 账户创建日期
    private Date lastLoginDate; // 最后登录日期

    // 用户状态
    private UserStatus status; // 当前状态

    // 身份验证信息
    private String token; // 登录令牌
    private Date tokenExpireDate; // 令牌过期时间
    private String refreshToken; // 刷新令牌

    // 用户偏好和设置
    private final Map<PermissionType, Boolean> permissions; // 权限设置
    private final Map<String, Object> preferences; // 其他用户偏好

    // 用户绑定的任务
    private ArrayList<Integer> task_id;

    /**
     * 基本构造函数
     * 
     * @param id       用户ID
     * @param username 用户名
     */
    public User(int id, String username) {
        this.id = id;
        this.username = username;
        this.status = UserStatus.LOGGED_OUT;
        this.permissions = new HashMap<>();
        this.preferences = new HashMap<>();
        initDefaultPermissions();
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
    public User(int id, String username, String nickname, String email, String phoneNumber) {
        this(id, username);
        this.nickname = nickname;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.createdDate = new Date();
    }

    /**
     * 初始化默认权限设置
     */
    private void initDefaultPermissions() {
        permissions.put(PermissionType.ALARM, false);
        permissions.put(PermissionType.NOTIFICATION, false);
        permissions.put(PermissionType.LOCATION, false);
        permissions.put(PermissionType.STORAGE, false);
        permissions.put(PermissionType.CALENDAR, false);
        permissions.put(PermissionType.CONTACTS, false);
    }

    /**
     * 更改用户状态
     * 
     * @param newStatus 新状态
     * @return 是否成功更改状态
     */
    public boolean changeStatus(UserStatus newStatus) {
        // 检查状态转换是否有效
        if (status == UserStatus.LOGGED_OUT && newStatus == UserStatus.LOGGED_IN) {
            // 需要先注册或有有效凭证
            if (token == null || isTokenExpired()) {
                return false;
            }
        }

        status = newStatus;

        // 更新最后登录时间
        if (newStatus == UserStatus.LOGGED_IN) {
            lastLoginDate = new Date();
        }

        return true;
    }

    /**
     * 登录过程
     * 
     * @param token        登录令牌
     * @param expireDate   令牌过期时间
     * @param refreshToken 刷新令牌
     * @return 是否登录成功
     */
    public boolean login(String token, Date expireDate, String refreshToken) {
        this.token = token;
        this.tokenExpireDate = expireDate;
        this.refreshToken = refreshToken;
        return changeStatus(UserStatus.LOGGED_IN);
    }

    /**
     * 退出登录
     * 
     * @return 是否成功退出
     */
    public boolean logout() {
        this.token = null;
        this.tokenExpireDate = null;
        this.refreshToken = null;
        return changeStatus(UserStatus.LOGGED_OUT);
    }

    /**
     * 检查令牌是否过期
     * 
     * @return 是否过期
     */
    public boolean isTokenExpired() {
        if (tokenExpireDate == null) {
            return true;
        }
        return new Date().after(tokenExpireDate);
    }

    /**
     * 刷新令牌
     * 
     * @param newToken        新令牌
     * @param newExpireDate   新过期时间
     * @param newRefreshToken 新刷新令牌
     */
    public void refreshToken(String newToken, Date newExpireDate, String newRefreshToken) {
        this.token = newToken;
        this.tokenExpireDate = newExpireDate;
        this.refreshToken = newRefreshToken;
    }

    /**
     * 更新用户昵称
     * 
     * @param nickname 新昵称
     */
    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }

    /**
     * 更新用户头像 (Bitmap 格式)
     * 
     * @param avatar 新头像
     */
    public void updateAvatar(Bitmap avatar) {
        this.avatar = avatar;
    }

    /**
     * 更新用户头像 (URI 格式)
     * 
     * @param avatarUri 新头像 URI
     */
    public void updateAvatar(Uri avatarUri) {
        this.avatarUri = avatarUri;
    }

    /**
     * 更新用户邮箱
     * 
     * @param email 新邮箱
     */
    public void updateEmail(String email) {
        this.email = email;
    }

    /**
     * 更新用户手机号
     * 
     * @param phoneNumber 新手机号
     */
    public void updatePhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    /**
     * 设置权限
     * 
     * @param type    权限类型
     * @param enabled 是否启用
     */
    public void setPermission(PermissionType type, boolean enabled) {
        permissions.put(type, enabled);
    }

    /**
     * 检查是否拥有权限
     * 
     * @param type 权限类型
     * @return 是否拥有权限
     */
    public boolean hasPermission(PermissionType type) {
        Boolean permission = permissions.get(type);
        return permission != null && permission;
    }

    /**
     * 设置用户偏好
     * 
     * @param key   偏好键
     * @param value 偏好值
     */
    public void setPreference(String key, Object value) {
        preferences.put(key, value);
    }

    /**
     * 获取用户偏好
     * 
     * @param key 偏好键
     * @return 偏好值
     */
    public Object getPreference(String key) {
        return preferences.get(key);
    }

    /**
     * 获取用户完整信息的Map
     * 
     * @return 用户信息Map
     */
    public Map<String, Object> getUserInfo() {
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", id);
        userInfo.put("username", username);
        userInfo.put("nickname", nickname);
        userInfo.put("email", email);
        userInfo.put("phoneNumber", phoneNumber);
        userInfo.put("status", status);
        userInfo.put("createdDate", createdDate);
        userInfo.put("lastLoginDate", lastLoginDate);
        userInfo.put("permissions", permissions);
        userInfo.put("preferences", preferences);
        return userInfo;
    }

    // Getters
    public int getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getNickname() {
        return nickname;
    }

    public String getEmail() {
        return email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public Bitmap getAvatar() {
        return avatar;
    }

    public Uri getAvatarUri() {
        return avatarUri;
    }

    public UserStatus getStatus() {
        return status;
    }

    public String getToken() {
        return token;
    }

    public Date getTokenExpireDate() {
        return tokenExpireDate;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public Date getLastLoginDate() {
        return lastLoginDate;
    }

    public Map<PermissionType, Boolean> getAllPermissions() {
        return permissions;
    }

    public Map<String, Object> getAllPreferences() {
        return preferences;
    }
}