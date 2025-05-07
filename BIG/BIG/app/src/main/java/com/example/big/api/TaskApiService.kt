package com.example.big.api

import com.example.big.models.CreateTaskRequest
import com.example.big.models.TaskResponse
import com.example.big.models.TodayFocusStatisticsResponse
import com.example.big.models.TotalFocusStatisticsResponse
import com.example.big.models.FocusDistributionResponse
import retrofit2.Response
import retrofit2.http.*

data class CreateTaskResponse(
    val task_id: String,
    val message: String
)

// 任务列表响应
data class TaskListResponse(
    val tasks: List<TaskResponse>,
    val total: Int,
    val page: Int,
    val limit: Int
)


interface TaskApiService {
    // 获取用户的所有任务
    @GET("api/tasks/users/{userId}")
    suspend fun getAllTasks(
        @Path("userId") userId: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 10
    ): Response<TaskListResponse>

    // 获取任务详情
    @GET("api/tasks/{taskId}")
    suspend fun getTask(@Path("taskId") taskId: String): Response<TaskResponse>

    // 创建任务
    @POST("api/tasks/users/{userId}")
    suspend fun createTask(
        @Path("userId") userId: String,
        @Body request: CreateTaskRequest
    ): Response<CreateTaskResponse>

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
    ): Response<TaskListResponse>

    // 按日期获取任务
    @GET("api/tasks/users/{userId}/today")
    suspend fun getTasksByDate(
        @Path("userId") userId: String,
        @Query("date") date: String
    ): Response<List<TaskResponse>>

    // 今日专注统计
    @GET("/api/tasks/users/{userId}/focus/today")
    suspend fun getTodayFocusStatistics(
        @Path("userId") userId: String,
        @Query("date") date: String
    ): Response<TodayFocusStatisticsResponse>

    // 累计专注统计
    @GET("/api/tasks/users/{userId}/focus/total")
    suspend fun getTotalFocusStatistics(@Path("userId") userId: String): Response<TotalFocusStatisticsResponse>

    // 专注时长分布
    @GET("/api/tasks/users/{userId}/focus/distribution")
    suspend fun getFocusDistribution(
        @Path("userId") userId: String,
        @Query("period") period: String, // 'day', 'week', 'month'
        @Query("start_date") startDate: String? = null,
        @Query("endDate") endDate: String? = null
    ): Response<FocusDistributionResponse>
}