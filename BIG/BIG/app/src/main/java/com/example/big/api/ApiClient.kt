package com.example.big.api

import android.util.Log
import com.example.big.utils.TokenManager
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

object ApiClient {
    const val BASE_URL = "https://todo.dechelper.com/"
    private const val TAG = "ApiClient"

    private val authInterceptor = Interceptor { chain ->
        val originalRequest = chain.request()
        val token = TokenManager.getAccessToken()

        // 记录请求信息用于调试
        Log.d(TAG, "Sending request to: ${originalRequest.url}")

        val newRequest = if (token != null) {
            originalRequest.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            Log.w(TAG, "No authentication token available")
            originalRequest
        }

        chain.proceed(newRequest)
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    // 添加重试拦截器，特别针对AI请求
    private val retryInterceptor = Interceptor { chain ->
        val request = chain.request()
        val isAiRequest = request.url.toString().contains("/ai/")

        // 最多重试次数，AI请求允许更多重试
        val maxRetries = if (isAiRequest) 3 else 1
        var retryCount = 0
        var lastException: IOException? = null

        while (retryCount < maxRetries) {
            try {
                // 尝试执行请求
                val response = chain.proceed(request)

                // 如果请求成功或者不是我们想要重试的错误，直接返回响应
                if (response.isSuccessful || (!isAiRequest && response.code != 502 && response.code != 503)) {
                    return@Interceptor response
                }

                // 如果是服务器错误并且我们想要重试
                Log.w(TAG, "Server error ${response.code} for ${request.url}, retry $retryCount of $maxRetries")
                response.close() // 关闭响应释放资源
            } catch (e: IOException) {
                // 记录异常并重试
                lastException = e
                Log.w(TAG, "Request to ${request.url} failed with ${e.javaClass.simpleName}: ${e.message}, retry $retryCount of $maxRetries")

                // 如果不是我们想要重试的异常类型，直接抛出
                if (!isAiRequest && e !is SocketTimeoutException) {
                    throw e
                }
            }

            // 增加重试计数
            retryCount++

            if (retryCount < maxRetries) {
                // 使用指数退避策略等待
                val backoffTime = (1000L * (1 shl retryCount)).coerceAtMost(10000L)
                Log.d(TAG, "Waiting for ${backoffTime}ms before retry")
                Thread.sleep(backoffTime)
            }
        }

        // 所有重试都失败，抛出最后一个异常
        throw lastException ?: IOException("Request failed after $maxRetries retries")
    }

    // 标准客户端配置
    private val standardClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        .addInterceptor(retryInterceptor)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    // AI专用客户端配置，更长的超时
    private val aiClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        .addInterceptor(retryInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)  // 2分钟读取超时
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    // 标准接口的Retrofit实例
    private val standardRetrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(standardClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    // AI接口的Retrofit实例
    private val aiRetrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(aiClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    // API服务实例
    val userApiService: UserApiService = standardRetrofit.create(UserApiService::class.java)
    val taskApiService: TaskApiService = standardRetrofit.create(TaskApiService::class.java)

    // 获取AI服务实例 - 使用共享方法确保单例模式
    fun getAIService(): UserApiService {
        return aiRetrofit.create(UserApiService::class.java)
    }
}