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
    val avatarURL: String = "", // 头像URL
    val status: String,
    val createdDate: String,
    val lastLoginDate: String,
    val apiKey: String = "" // API密钥
)

// UpdateUserRequest.kt

data class UpdateUserRequest(
    val nickname: String? = null,
    val email: String? = null,
    val phoneNumber: String? = null,
    val avatarURL: String? = null
)

// 更新API密钥请求模型
data class UpdateApiKeyRequest(
    val api_key: String
)

// AI建议请求模型
data class AISuggestionRequest(
    val task_id: String,
    val detailed_prompts: String? = null
)

// AI建议响应模型
data class AISuggestionResponse(
    val suggestion: String
)