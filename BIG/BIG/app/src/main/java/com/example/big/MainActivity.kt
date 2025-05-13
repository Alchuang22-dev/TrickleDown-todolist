package com.example.big

import android.app.Activity
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.big.ui.AuthScreen
import com.example.big.utils.TaskManager
import com.example.big.viewmodel.AuthViewModel
import java.util.Calendar
import com.example.big.utils.TokenManager
import com.example.big.utils.UserManager
// import com.google.android.gms.common.api.Result
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material3.IconButton
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import com.example.big.api.ApiClient
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent   // success 时需要
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.compose.ui.platform.LocalLifecycleOwner
import coil.request.ImageRequest

class MainActivity : ComponentActivity() {
    // 在活动级别存储任务状态
    private var tasksState = mutableStateOf(listOf<Task>())

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

                App(
                    importantTasks = tasks,
                    onNavigate = { activityClass ->
                        startActivity(Intent(this, activityClass))
                    },
                    onTaskClick = { task ->
                        val intent = Intent(this, EditTaskActivity::class.java).apply {
                            putExtra("task_id", task.id)
                        }
                        // 弹出包含 ID 的 Toast
                        Toast.makeText(
                            this@MainActivity,
                            "任务 ID: ${task.id}",
                            Toast.LENGTH_SHORT
                        ).show()
                        startActivity(intent)
                    }
                )
            }
        }

        // 在创建时获取任务
        refreshImportantTasks()
    }


    private fun createSampleTasks(): List<Task> {
        val importantTasks: MutableList<Task> = ArrayList()
        val cal = Calendar.getInstance()

        // Reset time to beginning of day
        cal[Calendar.HOUR_OF_DAY] = 0
        cal[Calendar.MINUTE] = 0
        cal[Calendar.SECOND] = 0
        cal[Calendar.MILLISECOND] = 0

        // 使用明确的十进制数字作为ID
        val taskId = "12345678" // 8位数ID

        // 使用正确的时间范围格式: "HH : MM -- HH : MM"
        importantTasks.add(
            Task(
                taskId,
                "上课",
                "19 : 00 -- 21 : 00",
                cal.time,
                120,
                false,
                "这是一个任务的简介"
            )
        )

        return importantTasks
    }

    // 添加 onResume 生命周期方法，确保每次回到该活动时都刷新任务
    override fun onResume() {
        super.onResume()
        refreshImportantTasks()
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
            // 获取当前用户ID
            val userId = UserManager.getUserId() ?: return importantTasks

            // 获取今天的日期，格式化为API需要的格式 (yyyy-MM-dd)
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val todayDate = dateFormat.format(Date())

            // 调用API获取今日任务
            val response = TaskManager.getTasksByDate(userId, todayDate)

            if (response is TaskManager.Result.Success) {
                // 筛选出重要的任务
                val importantTaskResponses = response.data.filter { it.is_important }

                // 将TaskResponse转换为UI需要的Task对象
                for (taskResponse in importantTaskResponses) {
                    val task = Task(
                        id = taskResponse.id, // 尝试将字符串ID转为整数
                        title = taskResponse.title,
                        timeRange = taskResponse.time_range,
                        date = taskResponse.date, // 直接使用API返回的日期
                        durationMinutes = taskResponse.duration_minutes,
                        important = taskResponse.is_important,
                        description = taskResponse.description,
                        place = taskResponse.place
                    )
                    // 添加完成状态
                    task.isFinished = taskResponse.is_finished
                    importantTasks.add(task)
                }

                importantTasks.sortBy { task ->
                    try {
                        // 检查分隔符，可能是 "--" 或 "-"
                        val timeRange = task.timeRange
                        val delimiter = if (timeRange.contains("--")) "--" else "-"

                        // 分割时间范围
                        val startTimeStr = timeRange.split(delimiter)[0].trim()

                        // 提取小时和分钟，移除可能的空格
                        val cleanTimeStr = startTimeStr.replace(" ", "")
                        val hourMinute = cleanTimeStr.split(":")

                        // 转换为分钟数
                        val hours = hourMinute[0].toInt()
                        val minutes = hourMinute[1].toInt()

                        hours * 60 + minutes  // 转换为分钟数用于排序
                    } catch (e: Exception) {
                        // 错误处理
                        Log.e("MainActivity", "解析任务时间失败: ${e.message} for timeRange: ${task.timeRange}", e)
                        Int.MAX_VALUE  // 排在最后
                    }
                }
            } else if (response is TaskManager.Result.Error) {
                // 记录错误日志
                Log.e("MainActivity", "获取今日重要任务失败: ${response.message}")
            }
        } catch (e: Exception) {
            // 错误处理
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
    onNavigate: (Class<out Activity>) -> Unit,
    onTaskClick: (Task) -> Unit
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
            onNavigate = onNavigate,
            onTaskClick = onTaskClick,
            onLogout = {
                authViewModel.logout()
            }
        )
    }
}

@Composable
fun HeaderSection(onProfileClick: () -> Unit) {
    val context = LocalContext.current

    // 创建一个可观察的状态，用于触发头像重新加载
    val refreshTrigger = remember { mutableStateOf(0) }

    // 添加一个订阅，当onResume时更新refreshTrigger
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                // 增加计数器触发重新加载
                refreshTrigger.value += 1
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // 每次refreshTrigger变化时获取最新的用户信息
    val user = remember(refreshTrigger.value) { UserManager.getUserInfo() }

    // 处理头像URL - 添加refreshTrigger作为依赖，确保刷新
    val avatarUrl = remember(refreshTrigger.value, user) {
        if (user?.avatarURL?.isNotEmpty() == true) {
            // 如果是相对路径，添加基础URL前缀
            if (user.avatarURL.startsWith("http")) {
                user.avatarURL
            } else {
                ApiClient.BASE_URL.removeSuffix("/") + user.avatarURL
            }
        } else {
            null
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "我的待办",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF333333)
        )

        // 添加cacheKey参数强制刷新图片
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

@Composable
fun MainScreen(
    importantTasks: List<Task>,
    onNavigate: (Class<out Activity>) -> Unit,
    onTaskClick: (Task) -> Unit,
    onLogout: () -> Unit  // 添加登出功能
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        HeaderSection(onProfileClick = { onNavigate(ProfileActivity::class.java) })

        Spacer(modifier = Modifier.height(24.dp))

        EntriesGrid(onNavigate = onNavigate)

        Spacer(modifier = Modifier.height(24.dp))

        AddTaskButton(onClick = { onNavigate(AddTaskActivity::class.java) })

        Spacer(modifier = Modifier.height(24.dp))

        ImportantTasksSection(
            tasks = importantTasks,
            onTaskClick = onTaskClick
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 添加登出按钮
        LogoutButton(onClick = onLogout)
    }
}

@Composable
fun LogoutButton(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF757575)) // 灰色按钮
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
fun EntriesGrid(onNavigate: (Class<out Activity>) -> Unit) {
    val entries = listOf(
        Triple("今日", Color(0xFF4CAF50), TodayTasksActivity::class.java),
        Triple("计划", Color(0xFF2196F3), KanbanViewActivityCompose::class.java),
        Triple("全部", Color(0xFFFF9800), ListViewActivity::class.java),
        Triple("统计", Color(0xFF9C27B0), StatisticsActivity::class.java)
    )

    // 使用固定高度而不是自适应高度
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        // 修改为200dp高度，与原XML更匹配
        modifier = Modifier.height(200.dp),
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(entries) { (label, color, destination) ->
            EntryCard(
                title = label,
                backgroundColor = color,
                onClick = { onNavigate(destination) }
            )
        }
    }
}

@Composable
fun EntryCard(
    title: String,
    backgroundColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            // 确保卡片是正方形
            .height(80.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun AddTaskButton(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE91E63))
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