package com.example.big.repository

import com.example.big.api.RetrofitClient
import com.example.big.models.AuthResponse
import com.example.big.models.LoginRequest
import com.example.big.models.RegisterRequest
import com.example.big.utils.Result

class AuthRepository {
    private val apiService = RetrofitClient.apiService

    suspend fun registerUser(
        username: String,
        password: String,
        nickname: String? = null,
        email: String? = null,
        phoneNumber: String? = null
    ): Result<AuthResponse> {
        return try {
            val request = RegisterRequest(username, password, nickname, email, phoneNumber)
            val response = apiService.registerUser(request)

            if (response.isSuccessful) {
                response.body()?.let {
                    Result.Success(it)
                } ?: Result.Error("Empty response body")
            } else {
                Result.Error(response.errorBody()?.string() ?: "Unknown error")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }

    suspend fun loginUser(username: String, password: String): Result<AuthResponse> {
        return try {
            val request = LoginRequest(username, password)
            val response = apiService.loginUser(request)

            if (response.isSuccessful) {
                response.body()?.let {
                    Result.Success(it)
                } ?: Result.Error("Empty response body")
            } else {
                Result.Error(response.errorBody()?.string() ?: "Unknown error")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }
}