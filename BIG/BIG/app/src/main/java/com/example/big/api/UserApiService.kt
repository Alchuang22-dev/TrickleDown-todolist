// UserApiService.kt
package com.example.big.api

import com.example.big.models.AISuggestionRequest
import com.example.big.models.AISuggestionResponse
import com.example.big.models.UpdateApiKeyRequest
import com.example.big.models.UpdateUserRequest
import com.example.big.models.UserResponse
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

interface UserApiService {
    @GET("api/users/{id}")
    suspend fun getUser(@Path("id") userId: String): Response<UserResponse>

    @PUT("api/users/{id}")
    suspend fun updateUser(
        @Path("id") userId: String,
        @Body updateRequest: UpdateUserRequest
    ): Response<UserResponse>

    @Multipart
    @POST("api/users/{id}/avatar")
    suspend fun uploadAvatar(
        @Path("id") userId: String,
        @Part avatar: MultipartBody.Part
    ): Response<Map<String, String>>

    @POST("api/logout")
    suspend fun logout(): Response<Map<String, String>>

    @PUT("api/users/{id}/apikey")
    suspend fun updateApiKey(
        @Path("id") userId: String,
        @Body updateRequest: UpdateApiKeyRequest
    ): Response<Map<String, String>>

    @POST("api/ai/suggestion")
    suspend fun getAISuggestion(
        @Body request: AISuggestionRequest
    ): Response<AISuggestionResponse>
}