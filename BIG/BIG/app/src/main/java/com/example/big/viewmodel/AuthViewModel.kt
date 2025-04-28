package com.example.big.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.big.api.ApiClient
import com.example.big.models.AuthResponse
import com.example.big.models.UserResponse
import com.example.big.repository.AuthRepository
import com.example.big.utils.Result
import com.example.big.utils.TokenManager
import com.example.big.utils.UserManager
import kotlinx.coroutines.launch

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "AuthViewModel"
    private val repository = AuthRepository()

    private val _loginResult = MutableLiveData<Result<AuthResponse>>()
    val loginResult: LiveData<Result<AuthResponse>> = _loginResult

    private val _registerResult = MutableLiveData<Result<AuthResponse>>()
    val registerResult: LiveData<Result<AuthResponse>> = _registerResult

    private val _currentUser = MutableLiveData<UserResponse?>()
    val currentUser: LiveData<UserResponse?> = _currentUser

    private val _logoutResult = MutableLiveData<Result<Boolean>>()
    val logoutResult: LiveData<Result<Boolean>> = _logoutResult

    init {
        // 初始化时检查已有令牌，恢复会话
        checkPreviousSession()
    }

    /**
     * 检查已有会话并恢复用户状态
     */
    private fun checkPreviousSession() {
        viewModelScope.launch {
            try {
                // 检查是否有有效令牌
                if (TokenManager.isLoggedIn()) {
                    Log.d(TAG, "发现有效令牌，正在恢复会话")

                    // 1. 首先尝试从本地获取用户信息
                    val cachedUser = UserManager.getUserInfo()
                    if (cachedUser != null) {
                        Log.d(TAG, "从本地缓存恢复用户信息")
                        _currentUser.value = cachedUser
                    }

                    // 2. 然后尝试从服务器获取最新用户信息
                    fetchCurrentUser()
                } else {
                    Log.d(TAG, "无有效令牌或令牌已过期")
                    _currentUser.value = null
                }
            } catch (e: Exception) {
                Log.e(TAG, "恢复会话失败: ${e.message}", e)
                _currentUser.value = null
            }
        }
    }

    /**
     * 从服务器获取当前用户信息
     */
    private suspend fun fetchCurrentUser() {
        try {
            val userId = TokenManager.getUserId()
            if (userId != null) {
                Log.d(TAG, "正在获取用户ID: $userId 的信息")
                val response = ApiClient.userApiService.getUser(userId)

                if (response.isSuccessful && response.body() != null) {
                    val user = response.body()!!
                    _currentUser.value = user
                    UserManager.saveUserInfo(user)
                    Log.d(TAG, "成功获取并更新用户信息")
                } else {
                    Log.e(TAG, "获取用户信息失败: ${response.errorBody()?.string()}")
                    // 如果API调用失败但有令牌，可能是令牌失效
                    if (response.code() == 401) {
                        // 令牌无效，清除并重新登录
                        TokenManager.clearTokens()
                        UserManager.clearUserInfo()
                        _currentUser.value = null
                    }
                }
            } else {
                Log.e(TAG, "无法获取用户ID，令牌可能无效")
                TokenManager.clearTokens()
                UserManager.clearUserInfo()
                _currentUser.value = null
            }
        } catch (e: Exception) {
            Log.e(TAG, "获取用户信息时发生错误: ${e.message}", e)
        }
    }

    /**
     * 用户登录
     */
    fun login(username: String, password: String) {
        viewModelScope.launch {
            _loginResult.value = Result.Loading

            try {
                val result = repository.loginUser(username, password)
                _loginResult.value = result

                if (result is Result.Success) {
                    val authResponse = result.data

                    // 保存令牌
                    TokenManager.saveTokens(
                        authResponse.token.accessToken,
                        authResponse.token.refreshToken,
                        authResponse.token.expiresIn
                    )

                    // 保存用户ID
                    authResponse.user.id?.let { userId ->
                        Log.d(TAG, "保存用户ID: $userId")
                        TokenManager.saveUserId(userId)
                    }

                    // 保存用户信息
                    UserManager.saveUserInfo(authResponse.user)

                    // 更新当前用户
                    _currentUser.value = authResponse.user

                    Log.d(TAG, "登录成功: ${authResponse.user.username}")
                } else if (result is Result.Error) {
                    Log.e(TAG, "登录失败: ${result.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "登录过程发生未处理的错误", e)
                _loginResult.value = Result.Error(e.toString())
            }
        }
    }

    /**
     * 用户注册
     */
    fun register(
        username: String,
        password: String,
        nickname: String? = null,
        email: String? = null,
        phoneNumber: String? = null
    ) {
        viewModelScope.launch {
            _registerResult.value = Result.Loading

            try {
                val result = repository.registerUser(username, password, nickname, email, phoneNumber)
                _registerResult.value = result

                if (result is Result.Success) {
                    val authResponse = result.data

                    // 保存令牌
                    TokenManager.saveTokens(
                        authResponse.token.accessToken,
                        authResponse.token.refreshToken,
                        authResponse.token.expiresIn
                    )

                    // 保存用户ID
                    authResponse.user.id?.let { userId ->
                        Log.d(TAG, "保存用户ID: $userId")
                        TokenManager.saveUserId(userId)
                    }

                    // 保存用户信息
                    UserManager.saveUserInfo(authResponse.user)

                    // 更新当前用户
                    _currentUser.value = authResponse.user

                    Log.d(TAG, "注册成功: ${authResponse.user.username}")
                } else if (result is Result.Error) {
                    Log.e(TAG, "注册失败: ${result.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "注册过程发生未处理的错误", e)
                _registerResult.value = Result.Error(e.toString())
            }
        }
    }

    /**
     * 用户注销
     */
    fun logout() {
        viewModelScope.launch {
            _logoutResult.value = Result.Loading

            try {
                // 调用后端注销接口
                val userId = TokenManager.getUserId()
                if (userId != null) {
                    try {
                        val response = ApiClient.userApiService.logout()
                        Log.d(TAG, "注销API调用结果: ${response.isSuccessful}")
                        // 即使API失败也继续清除本地数据
                    } catch (e: Exception) {
                        Log.e(TAG, "调用注销API失败: ${e.message}")
                        // 继续清除本地数据
                    }
                }

                // 清除令牌
                TokenManager.clearTokens()

                // 清除用户信息
                UserManager.clearUserInfo()

                // 更新当前用户状态
                _currentUser.value = null

                Log.d(TAG, "注销成功")
                _logoutResult.value = Result.Success(true)
            } catch (e: Exception) {
                Log.e(TAG, "注销过程发生错误", e)
                // 即使发生错误，也尝试清理状态
                _currentUser.value = null
                _logoutResult.value = Result.Error(e.toString())
            }
        }
    }

    /**
     * 刷新用户信息
     */
    fun refreshUserInfo() {
        viewModelScope.launch {
            fetchCurrentUser()
        }
    }
}