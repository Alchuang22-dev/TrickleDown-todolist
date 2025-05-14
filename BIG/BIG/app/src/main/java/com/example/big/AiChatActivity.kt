package com.example.big

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.example.big.api.ApiClient
import com.example.big.models.TaskResponse
import com.example.big.models.AISuggestionRequest
import com.example.big.models.UpdateApiKeyRequest
import com.example.big.utils.UserManager
import kotlinx.coroutines.launch
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import android.content.Intent
import com.example.big.models.UserResponse
import com.google.ai.client.generativeai.Chat

class AIChatActivity : ComponentActivity() {
    private var taskId: String? = null
    private var userId: String? = null
    private var taskInfo: TaskResponse? = null
    private var userInfo: UserResponse? = null
    private var apiKeyLoaded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 获取任务ID
        taskId = intent.getStringExtra("task_id")
        if (taskId == null) {
            finish()
            return
        }

        // 获取用户ID
        userId = UserManager.getUserId()
        if (userId == null) {
            finish()
            return
        }

        // 加载任务信息
        loadTaskInfo()

        // 加载用户信息（包括API密钥）
        loadUserInfo()

        setContent {
            MaterialTheme {
                AIChatScreen(
                    taskId = taskId!!,
                    userId = userId!!,
                    taskInfo = taskInfo,
                    userInfo = userInfo,
                    apiKeyLoaded = apiKeyLoaded,
                    onBackPressed = { finish() },
                    loadTaskInfo = { loadTaskInfo() },
                    loadUserInfo = { loadUserInfo() }
                )
            }
        }
    }

    private fun loadTaskInfo() {
        lifecycleScope.launch {
            try {
                val response = ApiClient.taskApiService.getTask(taskId!!)
                if (response.isSuccessful) {
                    taskInfo = response.body()
                    updateUI()
                } else {
                    Log.e(TAG, "获取任务信息失败: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "加载任务信息出错: ${e.message}", e)
            }
        }
    }

    private fun loadUserInfo() {
        lifecycleScope.launch {
            try {
                val response = ApiClient.userApiService.getUser(userId!!)
                if (response.isSuccessful) {
                    userInfo = response.body()

                    // 添加详细日志输出
                    Log.d(TAG, "服务器返回用户API密钥信息: ${userInfo?.toString()}")
                    Log.d(TAG, "API密钥是否为空: ${userInfo?.apiKey.isNullOrEmpty()}")
                    Log.d(TAG, "API密钥长度: ${userInfo?.apiKey?.length ?: 0}")
                    Log.d(TAG, "API密钥值: ${userInfo?.apiKey ?: "无"}")

                    // 更新本地存储的用户信息
                    userInfo?.let { UserManager.saveUserInfo(it) }

                    apiKeyLoaded = true
                    updateUI()

                    Log.d(TAG, "成功从服务器获取用户信息和API密钥")
                } else {
                    Log.e(TAG, "获取用户信息失败: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "加载用户信息出错: ${e.message}", e)

                // 如果网络请求失败，尝试使用本地存储的信息
                userInfo = UserManager.getUserInfo()
                Log.d(TAG, "尝试使用本地存储的用户信息: ${userInfo?.toString()}")
                Log.d(TAG, "本地存储的API密钥是否为空: ${userInfo?.apiKey.isNullOrEmpty()}")

                apiKeyLoaded = true
                updateUI()
            }
        }
    }

    private fun updateUI() {
        // 只有在组件还在活动状态时才更新UI
        if (!isFinishing) {
            setContent {
                MaterialTheme {
                    AIChatScreen(
                        taskId = taskId!!,
                        userId = userId!!,
                        taskInfo = taskInfo,
                        userInfo = userInfo,
                        apiKeyLoaded = apiKeyLoaded,
                        onBackPressed = { finish() },
                        loadTaskInfo = { loadTaskInfo() },
                        loadUserInfo = { loadUserInfo() }
                    )
                }
            }
        }
    }

    companion object {
        const val TAG = "AIChatActivity"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIChatScreen(
    taskId: String,
    userId: String,
    taskInfo: TaskResponse?,
    userInfo: UserResponse?,
    apiKeyLoaded: Boolean,
    onBackPressed: () -> Unit,
    loadTaskInfo: () -> Unit,
    loadUserInfo: () -> Unit
) {
    val context = LocalContext.current

    // 从服务器获取的API密钥（如果有的话）
    val serverApiKey = userInfo?.apiKey ?: ""

    var promptText by remember { mutableStateOf("") }
    var apiKey by remember(serverApiKey) { mutableStateOf(serverApiKey) }
    var aiResponse by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var showApiKeyPassword by remember { mutableStateOf(false) }

    // 是否显示API密钥编辑部分
    var showApiKeyEdit by remember(serverApiKey) { mutableStateOf(serverApiKey.isEmpty()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI 助手") },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // 任务信息卡片
            if (taskInfo != null) {
                TaskInfoCard(taskInfo)
            } else {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // API 密钥状态卡片 - 显示加载状态或实际状态
            if (!apiKeyLoaded) {
                // 如果API密钥还在加载中，显示加载状态
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Text("正在加载API密钥...")
                    }
                }
            } else {
                // 正常显示API密钥状态
                ApiKeyStatusCard(
                    hasApiKey = serverApiKey.isNotEmpty(),
                    showEditForm = showApiKeyEdit,
                    onToggleEditForm = { showApiKeyEdit = !showApiKeyEdit }
                )
            }

            // 条件性显示API密钥编辑卡片
            if (showApiKeyEdit && apiKeyLoaded) {
                Spacer(modifier = Modifier.height(8.dp))

                // API 密钥设置卡片
                ApiKeyCard(
                    apiKey = apiKey,
                    showPassword = showApiKeyPassword,
                    onApiKeyChanged = { apiKey = it },
                    onTogglePasswordVisibility = { showApiKeyPassword = !showApiKeyPassword },
                    onSaveApiKey = {
                        isLoading = true
                        (context as AIChatActivity).lifecycleScope.launch {
                            try {
                                val response = ApiClient.userApiService.updateApiKey(
                                    userId,
                                    UpdateApiKeyRequest(apiKey)
                                )

                                if (response.isSuccessful) {
                                    // 保存成功后重新加载用户信息以获取最新的API密钥
                                    loadUserInfo()

                                    // 保存成功后隐藏编辑表单
                                    showApiKeyEdit = false

                                    // 显示成功消息
                                    (context as? AIChatActivity)?.let { activity ->
                                        activity.runOnUiThread {
                                            Toast.makeText(context, "API密钥保存成功", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                } else {
                                    // 显示错误消息
                                    (context as? AIChatActivity)?.let { activity ->
                                        activity.runOnUiThread {
                                            Toast.makeText(context, "保存API密钥失败", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e("AIChatScreen", "保存API密钥出错: ${e.message}", e)
                                // 显示错误消息
                                (context as? AIChatActivity)?.let { activity ->
                                    activity.runOnUiThread {
                                        Toast.makeText(context, "网络错误，请稍后重试", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    isLoading = isLoading
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // AI 对话卡片
            AIChatCard(
                promptText = promptText,
                aiResponse = aiResponse,
                onPromptChanged = { promptText = it },
                // 在 onSendPrompt 闭包中使用从服务器获取的API密钥
                onSendPrompt = {
                    // 首先检查API密钥是否已加载完成
                    if (!apiKeyLoaded) {
                        (context as? AIChatActivity)?.let { activity ->
                            activity.runOnUiThread {
                                Toast.makeText(context, "正在加载API密钥，请稍候", Toast.LENGTH_SHORT).show()
                            }
                        }
                        return@AIChatCard
                    }

                    // 获取当前最新的API密钥，优先使用服务器上的值
                    val currentApiKey = userInfo?.apiKey ?: ""

                    if (currentApiKey.isBlank()) {
                        (context as? AIChatActivity)?.let { activity ->
                            activity.runOnUiThread {
                                Toast.makeText(context, "请先设置API密钥", Toast.LENGTH_SHORT).show()
                                // 如果没有API密钥，显示编辑表单
                                showApiKeyEdit = true
                            }
                        }
                        return@AIChatCard
                    }

                    isLoading = true
                    aiResponse = "正在生成回复..."

                    (context as AIChatActivity).lifecycleScope.launch {
                        try {
                            val request = AISuggestionRequest(
                                task_id = taskId,
                                detailed_prompts = if (promptText.isNotEmpty()) promptText else null
                            )

                            // 调用API，使用存储的用户ID
                            val response = ApiClient.getAIService().getAISuggestion(userId, request)

                            if (response.isSuccessful) {
                                val suggestionResponse = response.body()
                                if (suggestionResponse != null) {
                                    aiResponse = suggestionResponse.suggestion
                                } else {
                                    aiResponse = "未收到有效回复"
                                }
                            } else {
                                val errorBody = response.errorBody()?.string() ?: "未知错误"
                                Log.e("AIChatScreen", "AI建议请求失败: $errorBody")

                                // 检查是否是API密钥问题
                                if (errorBody.contains("API") || errorBody.contains("密钥") || errorBody.contains("apikey")) {
                                    aiResponse = "API密钥无效或已过期，请更新密钥"
                                    showApiKeyEdit = true
                                } else {
                                    aiResponse = "获取AI建议失败: $errorBody"
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("AIChatScreen", "发送提示到AI出错: ${e.message}", e)

                            // 更友好的错误提示
                            val errorMessage = when (e) {
                                is java.net.SocketTimeoutException -> "服务器响应超时，请稍后再试"
                                is java.net.ConnectException -> "连接服务器失败，请检查网络"
                                is java.net.UnknownHostException -> "无法连接到服务器，请检查网络"
                                else -> "网络错误: ${e.message}"
                            }

                            aiResponse = errorMessage
                        } finally {
                            isLoading = false
                        }
                    }
                },
                isLoading = isLoading
            )
        }
    }
}

@Composable
fun ApiKeyStatusCard(
    hasApiKey: Boolean,
    showEditForm: Boolean,
    onToggleEditForm: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "API 密钥状态",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    if (hasApiKey) {
                        Text(
                            text = "已设置",
                            color = Color.Green
                        )
                    } else {
                        Text(
                            text = "未设置",
                            color = Color.Red
                        )
                    }
                }

                Button(
                    onClick = onToggleEditForm,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (showEditForm) Color.Gray else MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(if (showEditForm) "隐藏设置" else if (hasApiKey) "修改" else "设置")
                }
            }
        }
    }
}

@Composable
fun TaskInfoCard(task: TaskResponse) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "任务信息",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = task.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = task.description ?: "无描述",
                fontSize = 14.sp,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "日期: ${task.date}",
                fontSize = 14.sp,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "地点: ${task.place ?: "未设置"}",
                fontSize = 14.sp,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "类别: ${task.category ?: "未分类"}",
                fontSize = 14.sp,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "状态: ${if (task.is_finished) "已完成" else "未完成"}",
                fontSize = 14.sp,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun ApiKeyCard(
    apiKey: String,
    showPassword: Boolean,
    onApiKeyChanged: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onSaveApiKey: () -> Unit,
    isLoading: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "设置 API 密钥",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            OutlinedTextField(
                value = apiKey,
                onValueChange = onApiKeyChanged,
                label = { Text("API 密钥") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                trailingIcon = {
                    IconButton(onClick = onTogglePasswordVisibility) {
                        Icon(
                            imageVector = if (showPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (showPassword) "隐藏密钥" else "显示密钥"
                        )
                    }
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onSaveApiKey,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("保存 API 密钥")
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "提示：API密钥格式通常为 'sk-' 开头，请确保完整复制",
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun AIChatCard(
    promptText: String,
    aiResponse: String,
    onPromptChanged: (String) -> Unit,
    onSendPrompt: () -> Unit,
    isLoading: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "与 AI 助手对话",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            OutlinedTextField(
                value = promptText,
                onValueChange = onPromptChanged,
                label = { Text("详细信息或问题（可选）") },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 100.dp),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Done
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onSendPrompt,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("发送")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "AI 回复",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 150.dp)
                    .background(Color(0xFFF5F5F5), RoundedCornerShape(4.dp))
                    .border(1.dp, Color(0xFFCCCCCC), RoundedCornerShape(4.dp))
                    .padding(8.dp)
            ) {
                Text(text = aiResponse)
            }
        }
    }
}