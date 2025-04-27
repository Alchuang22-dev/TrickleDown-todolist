package com.example.big.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.big.models.AuthResponse
import com.example.big.models.UserResponse
import com.example.big.repository.AuthRepository
import com.example.big.utils.Result
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {
    private val repository = AuthRepository()

    private val _loginResult = MutableLiveData<Result<AuthResponse>>()
    val loginResult: LiveData<Result<AuthResponse>> = _loginResult

    private val _registerResult = MutableLiveData<Result<AuthResponse>>()
    val registerResult: LiveData<Result<AuthResponse>> = _registerResult

    private val _currentUser = MutableLiveData<UserResponse?>()
    val currentUser: LiveData<UserResponse?> = _currentUser

    fun login(username: String, password: String) {
        viewModelScope.launch {
            _loginResult.value = Result.Loading
            val result = repository.loginUser(username, password)
            _loginResult.value = result

            if (result is Result.Success) {
                _currentUser.value = result.data.user
            }
        }
    }

    fun register(
        username: String,
        password: String,
        nickname: String? = null,
        email: String? = null,
        phoneNumber: String? = null
    ) {
        viewModelScope.launch {
            _registerResult.value = Result.Loading
            val result = repository.registerUser(username, password, nickname, email, phoneNumber)
            _registerResult.value = result

            if (result is Result.Success) {
                _currentUser.value = result.data.user
            }
        }
    }

    fun logout() {
        _currentUser.value = null
        // 实际应用中，这里应该调用API注销，并清除令牌
    }
}