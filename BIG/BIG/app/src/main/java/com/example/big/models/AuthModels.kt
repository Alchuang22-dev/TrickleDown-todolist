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

// 用户响应模型
data class UserResponse(
    val id: String,
    val username: String,
    val nickname: String?,
    val email: String?,
    val status: String,
    // 其他字段可以根据需要添加
)