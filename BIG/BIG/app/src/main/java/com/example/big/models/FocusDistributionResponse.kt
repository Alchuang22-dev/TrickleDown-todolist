// 修改 FocusDistributionResponse.kt
package com.example.big.models

// 旧的响应模型继续保留以兼容其他代码
data class FocusDistributionResponse(
    var period: String,
    val data: Map<String, Int>,
    val type: String? = null,
    val categories: Map<String, Int>? = null,
    val monthly_data: List<MonthlyDistributionData>? = null,
    val yearly_data: List<YearlyDistributionData>? = null
)

// 如果需要继续支持旧的数据结构，保留这些类
data class FocusDistributionData(
    val date: String,
    val level: Int
)

data class MonthlyDistributionData(
    val day: Int,
    val duration_minutes: Int
)

data class YearlyDistributionData(
    val month: Int,
    val duration_minutes: Int
)