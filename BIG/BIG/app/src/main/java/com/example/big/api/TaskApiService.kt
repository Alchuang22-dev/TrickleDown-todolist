package com.example.big.api

import com.example.big.models.CreateTaskRequest
import com.example.big.models.TaskResponse
import retrofit2.Response
import retrofit2.http.*

interface TaskApiService {
    // 获取用户的所有任务（修改为包含用户ID）
    @GET("api/tasks/users/{userId}")
    suspend fun getAllTasks(@Path("userId") userId: String): Response<List<TaskResponse>>

    // 获取任务详情
    @GET("api/tasks/{taskId}")
    suspend fun getTask(@Path("taskId") taskId: String): Response<TaskResponse>

    // 创建任务（修改为包含用户ID）
    @POST("api/tasks/users/{userId}")
    suspend fun createTask(
        @Path("userId") userId: String,
        @Body request: CreateTaskRequest
    ): Response<TaskResponse>

    // 更新任务
    @PUT("api/tasks/{taskId}")
    suspend fun updateTask(
        @Path("taskId") taskId: String,
        @Body request: CreateTaskRequest
    ): Response<TaskResponse>

    // 删除任务
    @DELETE("api/tasks/{taskId}")
    suspend fun deleteTask(@Path("taskId") taskId: String): Response<Map<String, String>>

    // 切换任务完成状态
    @PATCH("api/tasks/{taskId}/toggle")
    suspend fun toggleTaskFinished(@Path("taskId") taskId: String): Response<TaskResponse>

    // 按分类获取任务
    @GET("api/tasks/users/{userId}")
    suspend fun getTasksByCategory(
        @Path("userId") userId: String,
        @Query("category") category: String
    ): Response<List<TaskResponse>>

    // 按日期获取任务
    @GET("api/tasks/users/{userId}/today")
    suspend fun getTasksByDate(
        @Path("userId") userId: String,
        @Query("date") date: String
    ): Response<List<TaskResponse>>
}