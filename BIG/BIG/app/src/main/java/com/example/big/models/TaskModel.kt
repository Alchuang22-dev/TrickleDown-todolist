package com.example.big.models

import java.util.Date

// 用于创建和更新任务的请求模型
data class CreateTaskRequest(
    val title: String,
    val timeRange: String,
    val date: Date,
    val durationMinutes: Int,
    val isImportant: Boolean, // 注意这里改为isImportant
    val description: String? = null,
    val place: String? = null,
    val dueDate: Date? = null, // 改为可空
    val category: String? = null,
    val isFinished: Boolean = false // 注意这里改为isFinished
)

// 服务器响应的任务模型
data class TaskResponse(
    val id: Int, // 或String，取决于服务器返回的格式
    val userId: String,
    val title: String,
    val timeRange: String,
    val date: Date,
    val durationMinutes: Int,
    val isImportant: Boolean, // 注意这里改为isImportant
    val description: String? = null,
    val place: String? = null,
    val dueDate: Date? = null,
    val category: String? = null,
    val isFinished: Boolean = false,
    val isDelayed: Boolean = false,
    val createdAt: Date,
    val updatedAt: Date
)