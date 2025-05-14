package com.example.big.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.big.api.TaskApiClient
import com.example.big.models.CreateTaskRequest
import com.example.big.models.TaskResponse
import com.example.big.models.TodayFocusStatisticsResponse
import com.example.big.models.TotalFocusStatisticsResponse
import com.example.big.models.FocusDistributionResponse
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object TaskManager {
    private const val TAG = "TaskManager"
    private const val PREF_NAME = "TaskPrefs"
    private const val KEY_TASKS_CACHE = "tasks_cache"
    private const val KEY_LAST_SYNC = "last_sync_time"
    private const val SYNC_INTERVAL = 30 * 60 * 1000 // 30分钟同步一次（毫秒）

    // 当前登录用户ID
    private var currentUserId: String? = null

    private lateinit var prefs: SharedPreferences
    private val gson = Gson()

    // 初始化函数，需要在应用启动时调用
    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    // 在 TaskManager 类内部添加 Result 封装类
    sealed class Result<out T> {
        data class Success<T>(val data: T) : Result<T>()
        data class Error(val message: String) : Result<Nothing>()
    }

    // 设置当前用户ID
    fun setCurrentUserId(userId: String) {
        currentUserId = userId
    }

    // 获取当前用户ID，如果未设置则抛出异常
    private fun getCurrentUserId(): String {
        return currentUserId ?: throw IllegalStateException("用户ID未设置，请先登录")
    }

    // 缓存任务列表
    private fun cacheTaskList(tasks: List<TaskResponse>) {
        val tasksJson = gson.toJson(tasks)
        prefs.edit()
            .putString(KEY_TASKS_CACHE, tasksJson)
            .putLong(KEY_LAST_SYNC, System.currentTimeMillis())
            .apply()
        Log.d(TAG, "缓存了 ${tasks.size} 个任务")
    }

    // 获取缓存的任务列表
    fun getCachedTasks(): List<TaskResponse> {
        val tasksJson = prefs.getString(KEY_TASKS_CACHE, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<TaskResponse>>() {}.type
            gson.fromJson(tasksJson, type)
        } catch (e: Exception) {
            Log.e(TAG, "解析缓存任务失败: ${e.message}", e)
            emptyList()
        }
    }

    // 检查是否需要同步
    private fun needsSync(): Boolean {
        val lastSync = prefs.getLong(KEY_LAST_SYNC, 0)
        return System.currentTimeMillis() - lastSync > SYNC_INTERVAL
    }


    // 从服务器获取所有任务
    suspend fun getAllTasks(forceRefresh: Boolean = false): Result<List<TaskResponse>> {
        // 如果不强制刷新且缓存有效，返回缓存数据
//        if (!forceRefresh && !needsSync()) {
//            val cachedTasks = getCachedTasks()
//            if (cachedTasks.isNotEmpty()) {
//                Log.d(TAG, "返回缓存的任务列表 (${cachedTasks.size} 个任务)")
//                return Result.Success(cachedTasks)
//            }
//        }

        // 从服务器获取数据
        return withContext(Dispatchers.IO) {
            try {
                val userId = getCurrentUserId()
                val response = TaskApiClient.taskApiService.getAllTasks(userId)

                if (response.isSuccessful && response.body() != null) {
                    val responseBody = response.body()!!
                    val tasks = responseBody.tasks

                    // 打印详细信息以便调试
                    Log.d(TAG, "API响应: 总计${responseBody.total}个任务，当前页${responseBody.page}，每页${responseBody.limit}")
                    Log.d(TAG, "获取到${tasks.size}个任务")

                    cacheTaskList(tasks)
                    Result.Success(tasks)
                } else {
                    Log.e(TAG, "获取任务失败: ${response.code()} - ${response.errorBody()?.string()}")
                    Result.Error("获取任务失败: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "获取任务时发生错误: ${e.message}", e)
                Result.Error(e.message ?: "获取任务时发生未知错误")
            }
        }
    }

    // 添加到 TaskManager.kt 类中
    // 在 TaskManager.kt 中
    suspend fun getAllTasksWithPagination(userId: String? = null): Result<List<TaskResponse>> {
        return withContext(Dispatchers.IO) {
            try {
                val actualUserId = userId ?: getCurrentUserId()
                Log.d(TAG, "正在获取用户 $actualUserId 的所有任务")

                // 获取第一页
                val page1Response = TaskApiClient.taskApiService.getAllTasks(actualUserId, page = 1, limit = 10)

                if (!page1Response.isSuccessful || page1Response.body() == null) {
                    Log.e(TAG, "第一页请求失败: ${page1Response.code()}")
                    return@withContext Result.Error("获取任务失败: ${page1Response.code()}")
                }

                val page1Data = page1Response.body()!!
                val allTasks = page1Data.tasks.toMutableList()
                val totalTasks = page1Data.total

                Log.d(TAG, "第1页获取到 ${allTasks.size} 个任务 (总计: $totalTasks)")

                // 如果还有更多页，继续获取
                if (allTasks.size < totalTasks) {
                    // 计算需要的页数
                    val totalPages = (totalTasks + page1Data.limit - 1) / page1Data.limit

                    Log.d(TAG, "总共需要获取 $totalPages 页")

                    // 获取剩余页
                    for (page in 2..totalPages) {
                        Log.d(TAG, "获取第 $page 页...")

                        val nextPageResponse = TaskApiClient.taskApiService.getAllTasks(
                            actualUserId, page = page, limit = page1Data.limit
                        )

                        if (nextPageResponse.isSuccessful && nextPageResponse.body() != null) {
                            val nextPageTasks = nextPageResponse.body()!!.tasks
                            allTasks.addAll(nextPageTasks)
                            Log.d(TAG, "第 $page 页获取到 ${nextPageTasks.size} 个任务，当前总数: ${allTasks.size}")
                        } else {
                            Log.e(TAG, "获取第 $page 页失败: ${nextPageResponse.code()}")
                            // 继续尝试下一页，不返回错误
                        }
                    }
                }

                Log.d(TAG, "最终获取到 ${allTasks.size} 个任务 (总计应有: $totalTasks)")

                // 更新缓存
                cacheTaskList(allTasks)

                return@withContext Result.Success(allTasks)
            } catch (e: Exception) {
                Log.e(TAG, "获取任务列表出错", e)
                return@withContext Result.Error("获取任务列表出错: ${e.message}")
            }
        }
    }

    // 将这个方法添加到 TaskManager.kt 中

    /**
     * 获取专注时长分布数据，转换为 Map<String, Int> 格式
     * @param period 周期类型：day, week, month
     * @param startDate 开始日期，格式根据周期类型不同
     * @return 包含分布数据的 Map，键为日期/时间，值为专注分钟数
     */
// 在 TaskManager.kt 中修改
// 在 TaskManager.kt 中修改
    suspend fun getFocusDistribution(
        type: String,
        startDate: String? = null,
        endDate: String? = null
    ): Result<FocusDistributionResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val userId = getCurrentUserId()
                Log.d(TAG, "请求专注分布: type=$type, startDate=$startDate, endDate=$endDate")

                // 确保日期格式正确
                val validatedStartDate = if (startDate != null && !startDate.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) {
                    // 如果格式不是yyyy-MM-dd，则转换或使用当前日期
                    val calendar = Calendar.getInstance()
                    val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    formatter.format(calendar.time)
                } else {
                    startDate
                }

                Log.d(TAG, "使用验证后的日期: $validatedStartDate")

                val response = TaskApiClient.taskApiService.getFocusDistribution(
                    userId, type, validatedStartDate, endDate
                )

                if (response.isSuccessful && response.body() != null) {
                    val distribution = response.body()!!
                    Log.d(TAG, "获取专注时长分布成功，类型: $type, 数据: ${distribution.data}")
                    Result.Success(distribution)
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Unknown error"
                    Log.e(TAG, "获取专注时长分布失败: ${response.code()} - $errorBody")
                    Result.Error("获取专注时长分布失败: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "获取专注时长分布时发生错误: ${e.message}", e)
                Result.Error(e.message ?: "获取专注时长分布时发生未知错误")
            }
        }
    }

    // 创建新任务
    suspend fun createTask(request: CreateTaskRequest): Result<String> {  // 返回类型改为任务ID字符串
        return withContext(Dispatchers.IO) {
            try {
                val userId = getCurrentUserId()
                val response = TaskApiClient.taskApiService.createTask(userId, request)
                if (response.isSuccessful && response.body() != null) {
                    val createResponse = response.body()!!
                    Log.d(TAG, "成功创建任务: ${createResponse.message}")
                    // 在 TaskManager.createTask 中

                    // 刷新缓存以获取新创建的任务
                    refreshCache()

                    Result.Success(createResponse.task_id)  // 返回任务ID
                } else {
                    Log.e(TAG, "创建任务失败: ${response.code()} - ${response.errorBody()?.string()}")
                    Result.Error("创建任务失败: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "创建任务时发生错误: ${e.message}", e)
                Result.Error(e.message ?: "创建任务时发生未知错误")
            }
        }
    }
    // 修改参数类型从 Int 到 String
    suspend fun updateTask(taskId: String, request: CreateTaskRequest): Result<TaskResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = TaskApiClient.taskApiService.updateTask(taskId, request)
                if (response.isSuccessful && response.body() != null) {
                    val updatedTask = response.body()!!
                    // 更新缓存
                    val cachedTasks = getCachedTasks().toMutableList()
                    val index = cachedTasks.indexOfFirst { it.id == taskId }
                    if (index != -1) {
                        cachedTasks[index] = updatedTask
                        cacheTaskList(cachedTasks)
                    }
                    Log.d(TAG, "成功更新任务: ${updatedTask.title}")
                    Result.Success(updatedTask)
                } else {
                    Log.e(TAG, "更新任务失败: ${response.code()} - ${response.errorBody()?.string()}")
                    Result.Error("更新任务失败: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "更新任务时发生错误: ${e.message}", e)
                Result.Error(e.message ?: "更新任务时发生未知错误")
            }
        }
    }

    // 同样修改 deleteTask 方法
    suspend fun deleteTask(taskId: String): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val response = TaskApiClient.taskApiService.deleteTask(taskId)
                if (response.isSuccessful) {
                    // 更新缓存
                    val cachedTasks = getCachedTasks().toMutableList()
                    cachedTasks.removeIf { it.id == taskId }
                    cacheTaskList(cachedTasks)
                    Log.d(TAG, "成功删除任务 ID: $taskId")
                    Result.Success(true)
                } else {
                    Log.e(TAG, "删除任务失败: ${response.code()} - ${response.errorBody()?.string()}")
                    Result.Error("删除任务失败: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "删除任务时发生错误: ${e.message}", e)
                Result.Error(e.message ?: "删除任务时发生未知错误")
            }
        }
    }

    // 完成任务（修改为使用toggle接口）
    suspend fun finishTask(taskId: String): Result<TaskResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = TaskApiClient.taskApiService.toggleTaskFinished(taskId.toString())
                if (response.isSuccessful && response.body() != null) {
                    val finishedTask = response.body()!!
                    // 更新缓存
                    val cachedTasks = getCachedTasks().toMutableList()
                    val index = cachedTasks.indexOfFirst { it.id == taskId  }
                    if (index != -1) {
                        cachedTasks[index] = finishedTask
                        cacheTaskList(cachedTasks)
                    }
                    Log.d(TAG, "成功切换任务状态: ${finishedTask.title}")
                    Result.Success(finishedTask)
                } else {
                    Log.e(TAG, "切换任务状态失败: ${response.code()} - ${response.errorBody()?.string()}")
                    Result.Error("切换任务状态失败: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "切换任务状态时发生错误: ${e.message}", e)
                Result.Error(e.message ?: "切换任务状态时发生未知错误")
            }
        }
    }

    // 获取特定日期的任务
    suspend fun getTasksByDate(date: Date): Result<List<TaskResponse>> {
        return withContext(Dispatchers.IO) {
            try {
                val userId = getCurrentUserId()
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val dateString = dateFormat.format(date)
                val response = TaskApiClient.taskApiService.getTasksByDate(userId, dateString)

                if (response.isSuccessful && response.body() != null) {
                    val tasks = response.body()!!
                    Log.d(TAG, "成功获取日期 $dateString 的 ${tasks.size} 个任务")
                    Result.Success(tasks)
                } else {
                    Log.e(TAG, "获取日期任务失败: ${response.code()} - ${response.errorBody()?.string()}")
                    Result.Error("获取日期任务失败: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "获取日期任务时发生错误: ${e.message}", e)
                Result.Error(e.message ?: "获取日期任务时发生未知错误")
            }
        }
    }

    // 添加到 TaskManager 类中的新方法
    suspend fun getTaskById(taskId: String): Result<TaskResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = TaskApiClient.taskApiService.getTask(taskId)
                if (response.isSuccessful && response.body() != null) {
                    val task = response.body()!!
                    Log.d(TAG, "成功获取任务详情: ${task.title}")
                    Result.Success(task)
                } else {
                    Log.e(TAG, "获取任务详情失败: ${response.code()} - ${response.errorBody()?.string()}")
                    Result.Error("获取任务详情失败: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "获取任务详情时发生错误: ${e.message}", e)
                Result.Error(e.message ?: "获取任务详情时发生未知错误")
            }
        }
    }

    // 删除任务
    suspend fun deleteTask(taskId: Int): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val response = TaskApiClient.taskApiService.deleteTask(taskId.toString())
                if (response.isSuccessful) {
                    // 更新缓存
                    val cachedTasks = getCachedTasks().toMutableList()
                    cachedTasks.removeIf { it.id == taskId.toString()  }
                    cacheTaskList(cachedTasks)
                    Log.d(TAG, "成功删除任务 ID: $taskId")
                    Result.Success(true)
                } else {
                    Log.e(TAG, "删除任务失败: ${response.code()} - ${response.errorBody()?.string()}")
                    Result.Error("删除任务失败: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "删除任务时发生错误: ${e.message}", e)
                Result.Error(e.message ?: "删除任务时发生未知错误")
            }
        }
    }

    // 获取某个分类的所有任务
    suspend fun getTasksByCategory(category: String): Result<List<TaskResponse>> {
        return withContext(Dispatchers.IO) {
            try {
                val userId = getCurrentUserId()
                val response = TaskApiClient.taskApiService.getTasksByCategory(userId, category)

                if (response.isSuccessful && response.body() != null) {
                    val responseBody = response.body()!!
                    val tasks = responseBody.tasks

                    Log.d(TAG, "成功获取分类 $category 的 ${tasks.size} 个任务")
                    Result.Success(tasks)
                } else {
                    Log.e(TAG, "获取分类任务失败: ${response.code()} - ${response.errorBody()?.string()}")
                    Result.Error("获取分类任务失败: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "获取分类任务时发生错误: ${e.message}", e)
                Result.Error(e.message ?: "获取分类任务时发生未知错误")
            }
        }
    }

    // 按日期获取任务
    suspend fun getTasksByDate(userId: String, date: String): Result<List<TaskResponse>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = TaskApiClient.taskApiService.getTasksByDate(userId, date)
                if (response.isSuccessful && response.body() != null) {
                    val tasks = response.body()!!
                    Log.d(TAG, "成功获取 ${date} 的 ${tasks.size} 个任务")
                    Result.Success(tasks)
                } else {
                    Log.e(TAG, "获取日期任务失败: ${response.code()} - ${response.errorBody()?.string()}")
                    Result.Error("获取日期任务失败: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "获取日期任务时发生错误: ${e.message}", e)
                Result.Error(e.message ?: "获取日期任务时发生未知错误")
            }
        }
    }

    // 获取今日专注统计
    suspend fun getTodayFocusStatistics(): Result<TodayFocusStatisticsResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val userId = getCurrentUserId()
                val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                val todayDate = dateFormat.format(java.util.Date())
                val response = TaskApiClient.taskApiService.getTodayFocusStatistics(userId, todayDate)

                if (response.isSuccessful && response.body() != null) {
                    val stats = response.body()!!
                    Log.d(TAG, "获取今日专注统计成功")
                    Result.Success(stats)
                } else {
                    Log.e(TAG, "获取今日专注统计失败: ${response.code()} - ${response.errorBody()?.string()}")
                    Result.Error("获取今日专注统计失败: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "获取今日专注统计时发生错误: ${e.message}", e)
                Result.Error(e.message ?: "获取今日专注统计时发生未知错误")
            }
        }
    }

    // 获取累计专注统计
    suspend fun getTotalFocusStatistics(): Result<TotalFocusStatisticsResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val userId = getCurrentUserId()
                val response = TaskApiClient.taskApiService.getTotalFocusStatistics(userId)

                if (response.isSuccessful && response.body() != null) {
                    val stats = response.body()!!
                    Log.d(TAG, "获取累计专注统计成功")
                    Result.Success(stats)
                } else {
                    Log.e(TAG, "获取累计专注统计失败: ${response.code()} - ${response.errorBody()?.string()}")
                    Result.Error("获取累计专注统计失败: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "获取累计专注统计时发生错误: ${e.message}", e)
                Result.Error(e.message ?: "获取累计专注统计时发生未知错误")
            }
        }
    }

    // 刷新缓存
    // 刷新缓存
// 刷新缓存
    suspend fun refreshCache() {
        try {
            val userId = getCurrentUserId()
            // 使用分页方法获取所有任务
            val result = getAllTasksWithPagination(userId)

            if (result is Result.Success) {
                val tasks = result.data
                cacheTaskList(tasks)
                Log.d(TAG, "任务缓存已刷新 (共 ${tasks.size} 个任务)")
            } else {
                Log.e(TAG, "刷新缓存失败: ${(result as Result.Error).message}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "刷新缓存失败: ${e.message}", e)
        }
    }
    // 清除缓存
    fun clearCache() {
        prefs.edit()
            .remove(KEY_TASKS_CACHE)
            .remove(KEY_LAST_SYNC)
            .apply()
        Log.d(TAG, "任务缓存已清除")
    }
}