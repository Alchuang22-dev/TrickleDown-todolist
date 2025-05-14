package com.example.big

import android.app.Activity
import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.big.api.ApiClient
import com.example.big.api.TaskApiClient
import com.example.big.models.TaskResponse
import com.example.big.ui.AuthScreen
import com.example.big.utils.TaskManager
import com.example.big.utils.TokenManager
import com.example.big.utils.UserManager
import com.example.big.viewmodel.AuthViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    // 在活动级别存储任务状态
    private var tasksState = mutableStateOf(listOf<Task>())

    // 添加新的状态变量用于存储各类任务的数量
    private var todayTaskCountState = mutableStateOf(0)
    private var allTaskCountState = mutableStateOf(0)
    private var importantTaskCountState = mutableStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 初始化工具
        initializeTools()

        // 检查登录状态
        checkAuthState()

        setContent {
            MaterialTheme {
                // 使用活动级别的状态
                val tasks by remember { tasksState }
                val todayTaskCount by remember { todayTaskCountState }
                val allTaskCount by remember { allTaskCountState }
                val importantTaskCount by remember { importantTaskCountState }

                App(
                    importantTasks = tasks,
                    todayTaskCount = todayTaskCount,
                    allTaskCount = allTaskCount,
                    importantTaskCount = importantTaskCount,
                    onNavigate = { activityClass ->
                        startActivity(Intent(this, activityClass))
                    },
                    onTaskClick = { task ->
                        val intent = Intent(this, EditTaskActivity::class.java).apply {
                            putExtra("task_id", task.id)
                        }
                        startActivity(intent)
                    },
                    onSettingsClick = {
                        startActivity(Intent(this, SettingsActivity::class.java))
                    }
                )
            }
        }

        // 在创建时获取任务
        refreshImportantTasks()

        // 获取各类任务数量
        fetchTaskCounts()
    }

    // 添加获取任务计数的方法
    private fun fetchTaskCounts() {
        val userId = UserManager.getUserId() ?: return

        // 获取今日任务数量
        lifecycleScope.launch {
            try {
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val todayDate = dateFormat.format(Date())

                val response = TaskApiClient.taskApiService.getTasksByDate(userId, todayDate)
                if (response.isSuccessful) {
                    val tasks = response.body() ?: emptyList()
                    todayTaskCountState.value = tasks.size
                    Log.d("MainActivity", "今日任务数: ${tasks.size}")
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "获取今日任务数量失败", e)
            }
        }

        // 获取所有任务数量
        lifecycleScope.launch {
            try {
                val response = TaskApiClient.taskApiService.getAllTasks(userId)
                if (response.isSuccessful) {
                    val taskList = response.body()
                    if (taskList != null) {
                        allTaskCountState.value = taskList.total

                        // 计算重要任务数量
                        val importantTasks = taskList.tasks.count { it.is_important }
                        importantTaskCountState.value = importantTasks

                        Log.d("MainActivity", "所有任务数: ${taskList.total}, 重要任务数: $importantTasks")
                    }
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "获取所有任务数量失败", e)
            }
        }
    }

    // 在每次恢复活动时刷新任务和计数
    override fun onResume() {
        super.onResume()
        refreshImportantTasks()
        fetchTaskCounts()
    }

    // 新方法：刷新重要任务
    private fun refreshImportantTasks() {
        lifecycleScope.launch {
            val tasks = fetchImportantTasks()
            tasksState.value = tasks
        }
    }

    private suspend fun fetchImportantTasks(): List<Task> {
        val importantTasks: MutableList<Task> = ArrayList()

        try {
            val userId = UserManager.getUserId() ?: return importantTasks
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val todayDate = dateFormat.format(Date())
            val response = TaskManager.getTasksByDate(userId, todayDate)

            if (response is TaskManager.Result.Success) {
                val importantTaskResponses = response.data.filter { it.is_important }
                for (taskResponse in importantTaskResponses) {
                    val task = Task(
                        id = taskResponse.id,
                        title = taskResponse.title,
                        timeRange = taskResponse.time_range,
                        date = taskResponse.date,
                        durationMinutes = taskResponse.duration_minutes,
                        important = taskResponse.is_important,
                        description = taskResponse.description,
                        place = taskResponse.place
                    )
                    task.isFinished = taskResponse.is_finished
                    importantTasks.add(task)
                }

                importantTasks.sortBy { task ->
                    try {
                        val timeRange = task.timeRange
                        val delimiter = if (timeRange.contains("--")) "--" else "-"
                        val startTimeStr = timeRange.split(delimiter)[0].trim()
                        val cleanTimeStr = startTimeStr.replace(" ", "")
                        val hourMinute = cleanTimeStr.split(":")
                        val hours = hourMinute[0].toInt()
                        val minutes = hourMinute[1].toInt()
                        hours * 60 + minutes
                    } catch (e: Exception) {
                        Log.e("MainActivity", "解析任务时间失败: ${e.message} for timeRange: ${task.timeRange}", e)
                        Int.MAX_VALUE
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "获取今日重要任务时出现异常", e)
        }

        return importantTasks
    }

    private fun initializeTools() {
        // 初始化 TokenManager
        TokenManager.init(applicationContext)

        // 初始化其他工具，如 UserManager
        UserManager.init(applicationContext)
        TaskManager.init(applicationContext)
    }

    private fun checkAuthState() {
        // 检查是否已经登录
        if (!TokenManager.isLoggedIn()) {
            // 可选：如果未登录且需要强制登录，直接跳转到登录页面
            // startActivity(Intent(this, LoginActivity::class.java))
            // finish()
            // return

            // 您当前的实现使用 AuthViewModel 和 AuthScreen 已经处理了这种情况，
            // 所以这里不需要额外动作
        }
    }
}

@Composable
fun App(
    importantTasks: List<Task>,
    todayTaskCount: Int,
    allTaskCount: Int,
    importantTaskCount: Int,
    onNavigate: (Class<out Activity>) -> Unit,
    onTaskClick: (Task) -> Unit,
    onSettingsClick: () -> Unit
) {
    // 使用 AuthViewModel 来管理认证状态
    val authViewModel: AuthViewModel = viewModel()
    val currentUser by authViewModel.currentUser.observeAsState()

    // 如果用户未登录，显示登录/注册界面
    if (currentUser == null) {
        AuthScreen(
            onAuthSuccess = {
                // 登录成功后不需要额外操作，因为 AuthViewModel 会更新 currentUser
            }
        )
    } else {
        // 用户已登录，显示主界面
        MainScreen(
            importantTasks = importantTasks,
            todayTaskCount = todayTaskCount,
            allTaskCount = allTaskCount,
            importantTaskCount = importantTaskCount,
            onNavigate = onNavigate,
            onTaskClick = onTaskClick,
            onLogout = {
                authViewModel.logout()
            },
            onSettingsClick = onSettingsClick
        )
    }
}

@Composable
fun MainScreen(
    importantTasks: List<Task>,
    todayTaskCount: Int,
    allTaskCount: Int,
    importantTaskCount: Int,
    onNavigate: (Class<out Activity>) -> Unit,
    onTaskClick: (Task) -> Unit,
    onLogout: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // 更新顶部栏包含设置按钮
        HeaderSection(
            onProfileClick = { onNavigate(ProfileActivity::class.java) },
            onSettingsClick = onSettingsClick
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 更新网格视图以显示任务计数
        EntriesGrid(
            todayTaskCount = todayTaskCount,
            allTaskCount = allTaskCount,
            importantTaskCount = importantTaskCount,
            onNavigate = onNavigate
        )

        Spacer(modifier = Modifier.height(24.dp))

        AddTaskButton(onClick = { onNavigate(AddTaskActivity::class.java) })

        Spacer(modifier = Modifier.height(24.dp))

        ImportantTasksSection(
            tasks = importantTasks,
            onTaskClick = onTaskClick
        )

        Spacer(modifier = Modifier.height(24.dp))

        LogoutButton(onClick = onLogout)
    }
}

@Composable
fun HeaderSection(onProfileClick: () -> Unit, onSettingsClick: () -> Unit) {
    val context = LocalContext.current
    val refreshTrigger = remember { mutableStateOf(0) }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshTrigger.value += 1
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val user = remember(refreshTrigger.value) { UserManager.getUserInfo() }
    val avatarUrl = remember(refreshTrigger.value, user) {
        if (user?.avatarURL?.isNotEmpty() == true) {
            if (user.avatarURL.startsWith("http")) {
                user.avatarURL
            } else {
                ApiClient.BASE_URL.removeSuffix("/") + user.avatarURL
            }
        } else {
            null
        }
    }
    Log.e(TAG,"服务器返回用户头像信息: ${avatarUrl?.toString()}")

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "我的待办",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF333333)
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 添加设置按钮
            IconButton(onClick = onSettingsClick) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "设置",
                    tint = Color(0xFF2196F3),
                    modifier = Modifier.size(28.dp)
                )
            }

            // 用户头像 - 完全保留原有实现
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(avatarUrl)
                    .diskCacheKey("avatar_${refreshTrigger.value}")
                    .build(),
                contentDescription = "用户头像",
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .border(2.dp, Color.White, CircleShape)
                    .clickable { onProfileClick() },
                contentScale = ContentScale.Crop,
                error = painterResource(R.drawable.default_avatar),
                fallback = painterResource(R.drawable.default_avatar)
            )
        }
    }
}

@Composable
fun LogoutButton(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFEF5350)) // 使用红色主题
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "退出登录",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun EntriesGrid(
    todayTaskCount: Int,
    allTaskCount: Int,
    importantTaskCount: Int,
    onNavigate: (Class<out Activity>) -> Unit
) {
    // 每个入口使用一个单独的卡片来获得更好的视觉层次
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 今日任务
        EntryCardWithCount(
            title = "今天",
            count = todayTaskCount,
            iconColor = Color(0xFF4CAF50),
            backgroundColor = Color(0xFFE8F5E9),
            onClick = { onNavigate(TodayTasksActivity::class.java) }
        )

        // 计划任务
        EntryCardWithCount(
            title = "计划",
            count = importantTaskCount,
            iconColor = Color(0xFFFF9800),
            backgroundColor = Color(0xFFFFF3E0),
            onClick = { onNavigate(KanbanViewActivityCompose::class.java) }
        )

        // 全部任务
        EntryCardWithCount(
            title = "全部",
            count = allTaskCount,
            iconColor = Color(0xFF2196F3),
            backgroundColor = Color(0xFFE3F2FD),
            onClick = { onNavigate(ListViewActivity::class.java) }
        )

        // 统计
        EntryCardSimple(
            title = "统计",
            iconColor = Color(0xFF9C27B0),
            backgroundColor = Color(0xFFF3E5F5),
            onClick = { onNavigate(StatisticsActivity::class.java) }
        )
    }
}

@Composable
fun EntryCardWithCount(
    title: String,
    count: Int,
    iconColor: Color,
    backgroundColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 图标圆形背景
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(backgroundColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    // 这里可以替换为实际的图标
                    Text(
                        text = title.substring(0, 1),
                        color = iconColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }

                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF333333)
                )
            }

            // 任务数量
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(iconColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = count.toString(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
fun EntryCardSimple(
    title: String,
    iconColor: Color,
    backgroundColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 图标圆形背景
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(backgroundColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                // 这里可以替换为实际的图标
                Text(
                    text = title.substring(0, 1),
                    color = iconColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF333333)
            )
        }
    }
}

@Composable
fun AddTaskButton(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2196F3)) // 使用蓝色主题
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "添加新事项",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun ImportantTasksSection(
    tasks: List<Task>,
    onTaskClick: (Task) -> Unit
) {
    Column {
        Text(
            text = "今日重要事项",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF333333),
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            LazyColumn(
                modifier = Modifier.padding(8.dp)
            ) {
                items(tasks) { task ->
                    TaskItem(
                        task = task,
                        onClick = { onTaskClick(task) }
                    )
                    if (tasks.indexOf(task) < tasks.size - 1) {
                        Divider()
                    }
                }
            }
        }
    }
}

@Composable
fun TaskItem(
    task: Task,
    onClick: () -> Unit
) {
    // 为完成的任务设置浅绿色背景
    val backgroundColor = if (task.isFinished) Color(0xFFE8F5E9) else Color.Transparent
    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)  // 添加背景色
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 为已完成的任务添加勾选图标
        if (task.isFinished) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "已完成",
                tint = Color(0xFF4CAF50),  // 绿色图标
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = task.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                // 为已完成的任务添加删除线
                textDecoration = if (task.isFinished) TextDecoration.LineThrough else TextDecoration.None,
                color = if (task.isFinished) Color.Gray else Color.Black  // 已完成任务的文字颜色变灰
            )
            Text(
                text = task.timeRange,
                fontSize = 14.sp,
                color = Color.Gray
            )
            task.description?.let {
                Text(
                    text = it,
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }
        }

        // 添加 AI 聊天按钮
        IconButton(
            onClick = {
                val intent = Intent(context, AIChatActivity::class.java).apply {
                    putExtra("task_id", task.id)
                }
                context.startActivity(intent)
            }
        ) {
            Icon(
                imageVector = Icons.Default.Chat,  // 使用聊天图标
                contentDescription = "AI对话",
                tint = Color(0xFF2196F3)  // 蓝色
            )
        }

        Icon(
            imageVector = Icons.Default.KeyboardArrowRight,
            contentDescription = "查看详情",
            tint = Color.Gray
        )
    }
}