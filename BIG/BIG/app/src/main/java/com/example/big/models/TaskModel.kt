package com.example.big.models

import java.util.Date

// 用于创建和更新任务的请求模型
data class CreateTaskRequest(
    val title: String,
    val time_range: String,  // 使用下划线命名以匹配后端
    val date: Date,
    val duration_minutes: Int,  // 使用下划线命名以匹配后端
    val is_important: Boolean,  // 使用下划线命名以匹配后端
    val description: String? = null,
    val place: String? = null,
    val due_date: Date? = null,  // 使用下划线命名以匹配后端
    val category: String? = null,
    val is_finished: Boolean = false  // 使用下划线命名以匹配后端
)

// 服务器响应的任务模型
data class TaskResponse(
    val id: String,  // 使用String类型匹配MongoDB的ObjectID
    val user_id: String,  // 使用下划线命名以匹配后端
    val title: String,
    val time_range: String,  // 使用下划线命名以匹配后端
    val date: Date,
    val duration_minutes: Int,  // 使用下划线命名以匹配后端
    val is_important: Boolean,  // 使用下划线命名以匹配后端
    val description: String? = null,
    val place: String? = null,
    val due_date: Date? = null,  // 使用下划线命名以匹配后端
    val category: String? = null,
    val is_finished: Boolean = false,  // 使用下划线命名以匹配后端
    val is_delayed: Boolean = false,  // 使用下划线命名以匹配后端
    val created_at: Date,  // 使用下划线命名以匹配后端
    val updated_at: Date  // 使用下划线命名以匹配后端
)