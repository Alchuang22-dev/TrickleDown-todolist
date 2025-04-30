package com.example.big.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.big.models.CreateTaskRequest
import com.example.big.models.TaskResponse
import com.example.big.utils.Result
import com.example.big.utils.TaskManager
import kotlinx.coroutines.launch
import java.util.Date

class TaskViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "TaskViewModel"

    private val _tasks = MutableLiveData<List<TaskResponse>>()
    val tasks: LiveData<List<TaskResponse>> = _tasks

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _taskOperationResult = MutableLiveData<Result<TaskResponse>>()
    val taskOperationResult: LiveData<Result<TaskResponse>> = _taskOperationResult

    private val _deleteTaskResult = MutableLiveData<Result<Boolean>>()
    val deleteTaskResult: LiveData<Result<Boolean>> = _deleteTaskResult

    init {
        loadTasks()
    }

    fun loadTasks(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null

            try {
                val result = TaskManager.getAllTasks(forceRefresh)
                when (result) {
                    is Result.Success -> {
                        _tasks.value = result.data
                    }
                    is Result.Error -> {
                        Log.e(TAG, "加载任务失败: ${result.message}")
                        _error.value = result.message
                    }
                    is Result.Loading -> {
                        // 已经设置为加载中
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "加载任务时发生错误: ${e.message}", e)
                _error.value = e.message ?: "加载任务时发生未知错误"
            } finally {
                _loading.value = false
            }
        }
    }

    fun createTask(request: CreateTaskRequest) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null

            try {
                val result = TaskManager.createTask(request)
                _taskOperationResult.value = result

                // 刷新任务列表
                if (result is Result.Success) {
                    loadTasks(true)
                } else if (result is Result.Error) {
                    _error.value = result.message
                }
            } catch (e: Exception) {
                Log.e(TAG, "创建任务时发生错误: ${e.message}", e)
                _error.value = e.message ?: "创建任务时发生未知错误"
                _taskOperationResult.value = Result.Error(e.toString())
            } finally {
                _loading.value = false
            }
        }
    }

    fun updateTask(taskId: Int, request: CreateTaskRequest) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null

            try {
                val result = TaskManager.updateTask(taskId, request)
                _taskOperationResult.value = result

                // 刷新任务列表
                if (result is Result.Success) {
                    loadTasks(true)
                } else if (result is Result.Error) {
                    _error.value = result.message
                }
            } catch (e: Exception) {
                Log.e(TAG, "更新任务时发生错误: ${e.message}", e)
                _error.value = e.message ?: "更新任务时发生未知错误"
                _taskOperationResult.value = Result.Error(e.toString())
            } finally {
                _loading.value = false
            }
        }
    }

    fun finishTask(taskId: Int) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null

            try {
                val result = TaskManager.finishTask(taskId)
                _taskOperationResult.value = result

                // 刷新任务列表
                if (result is Result.Success) {
                    loadTasks(true)
                } else if (result is Result.Error) {
                    _error.value = result.message
                }
            } catch (e: Exception) {
                Log.e(TAG, "完成任务时发生错误: ${e.message}", e)
                _error.value = e.message ?: "完成任务时发生未知错误"
                _taskOperationResult.value = Result.Error(e.toString())
            } finally {
                _loading.value = false
            }
        }
    }

    fun deleteTask(taskId: Int) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null

            try {
                val result = TaskManager.deleteTask(taskId)
                _deleteTaskResult.value = result

                if (result is Result.Success) {
                    // 刷新任务列表
                    loadTasks(true)
                } else if (result is Result.Error) {
                    _error.value = result.message
                }
            } catch (e: Exception) {
                Log.e(TAG, "删除任务时发生错误: ${e.message}", e)
                _error.value = e.message ?: "删除任务时发生未知错误"
                _deleteTaskResult.value = Result.Error(e.toString())
            } finally {
                _loading.value = false
            }
        }
    }

    fun getTasksByDate(date: Date) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null

            try {
                val result = TaskManager.getTasksByDate(date)
                when (result) {
                    is Result.Success -> {
                        _tasks.value = result.data
                    }
                    is Result.Error -> {
                        Log.e(TAG, "获取日期任务失败: ${result.message}")
                        _error.value = result.message
                    }
                    is Result.Loading -> {
                        // 已经设置为加载中
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "获取日期任务时发生错误: ${e.message}", e)
                _error.value = e.message ?: "获取日期任务时发生未知错误"
            } finally {
                _loading.value = false
            }
        }
    }

    fun getTasksByCategory(category: String) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null

            try {
                val result = TaskManager.getTasksByCategory(category)
                when (result) {
                    is Result.Success -> {
                        _tasks.value = result.data
                    }
                    is Result.Error -> {
                        Log.e(TAG, "获取分类任务失败: ${result.message}")
                        _error.value = result.message
                    }
                    is Result.Loading -> {
                        // 已经设置为加载中
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "获取分类任务时发生错误: ${e.message}", e)
                _error.value = e.message ?: "获取分类任务时发生未知错误"
            } finally {
                _loading.value = false
            }
        }
    }

    fun refreshCache() {
        viewModelScope.launch {
            try {
                TaskManager.refreshCache()
                loadTasks(true)
            } catch (e: Exception) {
                Log.e(TAG, "刷新缓存时发生错误: ${e.message}", e)
            }
        }
    }

    fun clearCache() {
        viewModelScope.launch {
            TaskManager.clearCache()
            loadTasks(true)
        }
    }

    fun clearError() {
        _error.value = null
    }
}