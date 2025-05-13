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
import com.google.ai.client.generativeai.Chat

class AIChatActivity : ComponentActivity() {
    private var taskId: String? = null
    private var userId: String? = null
    private var taskInfo: TaskResponse? = null

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

        setContent {
            MaterialTheme {
                AIChatScreen(
                    taskId = taskId!!,
                    userId = userId!!,
                    taskInfo = taskInfo,
                    onBackPressed = { finish() },
                    loadTaskInfo = { loadTaskInfo() }
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

                    // 强制重新组合界面
                    setContent {
                        MaterialTheme {
                            AIChatScreen(
                                taskId = taskId!!,
                                userId = userId!!,
                                taskInfo = taskInfo,
                                onBackPressed = { finish() },
                                loadTaskInfo = { loadTaskInfo() }
                            )
                        }
                    }
                } else {
                    Log.e(TAG, "获取任务信息失败: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "加载任务信息出错: ${e.message}", e)
            }
        }
    }

    companion object {
        private const val TAG = "AIChatActivity"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIChatScreen(
    taskId: String,
    userId: String,
    taskInfo: TaskResponse?,
    onBackPressed: () -> Unit,
    loadTaskInfo: () -> Unit
) {
    val context = LocalContext.current

    var promptText by remember { mutableStateOf("") }
    var apiKey by remember { mutableStateOf(UserManager.getUserInfo()?.apiKey ?: "") }
    var aiResponse by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var showApiKeyPassword by remember { mutableStateOf(false) }

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
                                // 更新本地用户信息
                                val user = UserManager.getUserInfo()
                                user?.let {
                                    val updatedUser = it.copy(apiKey = apiKey)
                                    UserManager.saveUserInfo(updatedUser)
                                }

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

            Spacer(modifier = Modifier.height(16.dp))

            // AI 对话卡片
            AIChatCard(
                promptText = promptText,
                aiResponse = aiResponse,
                onPromptChanged = { promptText = it },
                onSendPrompt = {
                    if (apiKey.isBlank()) {
                        (context as? AIChatActivity)?.let { activity ->
                            activity.runOnUiThread {
                                Toast.makeText(context, "请先设置API密钥", Toast.LENGTH_SHORT).show()
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

                            val response = ApiClient.userApiService.getAISuggestion(request)

                            if (response.isSuccessful) {
                                val suggestionResponse = response.body()
                                if (suggestionResponse != null) {
                                    aiResponse = suggestionResponse.suggestion
                                } else {
                                    aiResponse = "未收到有效回复"
                                }
                            } else {
                                Log.e("AIChatScreen", "AI建议请求失败: ${response.errorBody()?.string()}")
                                aiResponse = "获取AI建议失败，请检查API密钥是否正确"
                            }
                        } catch (e: Exception) {
                            Log.e("AIChatScreen", "发送提示到AI出错: ${e.message}", e)
                            aiResponse = "网络错误，请稍后重试"
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
                text = "API 密钥设置",
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