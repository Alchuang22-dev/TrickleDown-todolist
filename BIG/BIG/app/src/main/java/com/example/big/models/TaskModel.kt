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
    val is_finished: Boolean = false,  // 使用下划线命名以匹配后端
    val is_delayed: Boolean = false
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

// 今日专注统计响应
data class TodayFocusStatisticsResponse(
    val focus_count: Int,
    val tasks_completed: Int,
    val focus_duration_minutes: String // 格式化的时间，如 "2小时30分钟"
)

// 累计专注统计响应
data class TotalFocusStatisticsResponse(
    val focus_count: Int,
    val tasks_completed: Int,
    val focus_duration_minutes: String, // 格式化的时间，如 "21小时40分钟"
    val avg_daily_duration: String // 格式化的时间，如 "1小时58分钟"
)

// 专注时长分布响应
data class FocusDistributionResponse(
    val type: String, // 'day', 'week', 'month', 'custom'
    val data: List<FocusDistributionData>,
    val monthly_data: List<MonthlyDistributionData>? = null,
    val yearly_data: List<YearlyDistributionData>? = null
)

data class FocusDistributionData(
    val date: String, // 日期，格式根据type不同而不同
    val level: Int, // 0-4的活动级别
    val duration_minutes: Int // 专注时长(分钟)
)

data class MonthlyDistributionData(
    val day: Int, // 日期（1-31）
    val duration_minutes: Int // 当天专注时长(分钟)
)

data class YearlyDistributionData(
    val month: Int, // 月份（1-12）
    val duration_minutes: Int // 当月专注时长(分钟)
)

