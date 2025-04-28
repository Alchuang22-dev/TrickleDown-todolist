package com.example.big.api

import com.example.big.models.AuthResponse
import com.example.big.models.LoginRequest
import com.example.big.models.RegisterRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {
    @POST("api/register")
    suspend fun registerUser(@Body request: RegisterRequest): Response<AuthResponse>

    @POST("api/login")
    suspend fun loginUser(@Body request: LoginRequest): Response<AuthResponse>
}