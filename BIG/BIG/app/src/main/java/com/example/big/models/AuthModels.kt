package com.example.big.models

// 注册请求模型
data class RegisterRequest(
    val username: String,
    val password: String,
    val nickname: String? = null,
    val email: String? = null,
    val phoneNumber: String? = null
)

// 登录请求模型
data class LoginRequest(
    val username: String,
    val password: String
)

// 令牌响应模型
data class TokenResponse(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long
)

// 认证响应模型
data class AuthResponse(
    val message: String,
    val user: UserResponse,
    val token: TokenResponse
)

data class UserResponse(
    val id: String,
    val username: String,
    val nickname: String,
    val email: String,
    val phoneNumber: String,
    val avatarURL: String, // 确保这个字段存在且类型为 String
    val status: String,
    val createdDate: String,
    val lastLoginDate: String
)

data class UpdateUserRequest(
    val nickname: String? = null,
    val email: String? = null,
    val phoneNumber: String? = null,
    val avatarURL: String? = null
)