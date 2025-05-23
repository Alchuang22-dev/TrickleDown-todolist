package com.example.big

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.lifecycleScope
import com.example.big.models.TaskResponse
import com.example.big.utils.TaskManager
import com.example.big.utils.UserManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max
import kotlin.math.min

class TodayViewComposeActivity : ComponentActivity() {
    private var selectedDate: Date? = null
    private val TAG = "TodayViewComposeActivity"

    // 添加刷新触发器
    private var refreshTrigger = mutableStateOf(0)

    // 注册结果回调，当从编辑页面返回时触发刷新
    private val editTaskLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // 任务被修改或删除，触发刷新
            refreshTrigger.value++
            Log.d(TAG, "任务已更新，刷新页面: ${refreshTrigger.value}")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 获取传入的日期，如果没有则使用当前日期
        val selectedDateMillis = intent.getLongExtra("selected_date", System.currentTimeMillis())
        selectedDate = Date(selectedDateMillis)

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    TodayViewScreen(
                        selectedDate = selectedDate ?: Date(),
                        coroutineScope = lifecycleScope,
                        refreshTrigger = refreshTrigger.value,
                        onEditTask = { taskId ->
                            // 使用结果启动器启动编辑页面
                            val intent = Intent(this, EditTaskActivity::class.java)
                            intent.putExtra("task_id", taskId)
                            editTaskLauncher.launch(intent)
                        }
                    )
                }
            }
        }
    }

    // 添加 onResume 方法，每次页面恢复时触发刷新
    override fun onResume() {
        super.onResume()
        // 每次页面恢复时增加刷新触发器值，强制刷新
        refreshTrigger.value++
        Log.d(TAG, "页面恢复，触发刷新: ${refreshTrigger.value}")
    }
}

@Composable
fun TodayViewScreen(
    selectedDate: Date,
    coroutineScope: kotlinx.coroutines.CoroutineScope,
    refreshTrigger: Int,
    onEditTask: (String) -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // 状态管理
    var tasks by remember { mutableStateOf<List<Task>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // 无时间任务区域高度
    var noTimeTaskAreaHeight by remember { mutableStateOf(0.dp) }

    // 加载任务数据 - 使用refreshTrigger作为依赖，确保刷新时重新加载
    LaunchedEffect(selectedDate, refreshTrigger) {
        isLoading = true
        loadTasksForDate(selectedDate, coroutineScope) { result ->
            when (result) {
                is TaskLoadResult.Success -> {
                    tasks = result.tasks
                    isLoading = false
                }
                is TaskLoadResult.Error -> {
                    // 修改：即使加载失败也清空任务列表，确保显示空界面而不是错误界面
                    tasks = emptyList()
                    // 只有在真正出错时才显示错误信息，"无任务"不算错误
                    if (!result.message.contains("无任务")) {
                        errorMessage = result.message
                    }
                    isLoading = false
                }
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // 顶部日期栏
        DateHeaderBar(
            date = selectedDate,
            onBackClick = { (context as? ComponentActivity)?.finish() },
            onMoreClick = { /* 打开菜单 */ }
        )

        // 主要内容区域
        Box(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            // 加载状态
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
                return@Box
            }

            // 错误状态 - 只在真正出错时显示错误
            /*
            if (errorMessage != null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "加载失败",
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.Red
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = errorMessage ?: "未知错误",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                return@Box
            }
             */

            // 将任务划分为无时间任务和有时间任务
            val noTimeTasks = tasks.filter { it.timeRange == "未设定时间" || it.durationMinutes == 0 }
            val timeTasks = tasks.filter { it.timeRange != "未设定时间" && it.durationMinutes > 0 }

            // 使用子布局计算无时间任务区域
            SubcomposeLayout { constraints ->
                // 测量无时间任务区域
                val noTimeTaskPlaceables = if (noTimeTasks.isNotEmpty()) {
                    subcompose("noTimeTasks") {
                        NoTimeTasksArea(
                            tasks = noTimeTasks,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 40.dp, end = 8.dp),
                            onEditTask = onEditTask
                        )
                    }.map { it.measure(constraints) }
                } else {
                    // 即使没有无时间任务，也显示无时间任务区域
                    subcompose("emptyNoTimeTasks") {
                        EmptyNoTimeTasksArea(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 40.dp, end = 8.dp)
                        )
                    }.map { it.measure(constraints) }
                }

                // 计算无时间任务区域的高度
                val noTimeTaskHeight = noTimeTaskPlaceables.maxOfOrNull { it.height } ?: 0

                // 测量时间线背景
                val timelineBackgroundPlaceables = subcompose("timelineBackground") {
                    TimelineBackground(startHour = 0, endHour = 23)
                }.map { it.measure(constraints) }

                // 测量时间任务区域
                val timeTasksPlaceables = subcompose("timeTasks") {
                    TasksTimeline(
                        tasks = timeTasks,
                        startHour = 0,
                        endHour = 23,
                        modifier = Modifier.fillMaxWidth(),
                        onEditTask = onEditTask
                    )
                }.map { it.measure(constraints) }

                // 计算总高度
                val totalHeight = if (noTimeTaskHeight > 0) {
                    noTimeTaskHeight + 16 + (timelineBackgroundPlaceables.maxOfOrNull { it.height } ?: 0)
                } else {
                    timelineBackgroundPlaceables.maxOfOrNull { it.height } ?: 0
                }

                // 放置元素
                layout(constraints.maxWidth, totalHeight) {
                    // 放置无时间任务区域
                    noTimeTaskPlaceables.forEach { it.place(0, 0) }

                    // 放置时间线背景
                    timelineBackgroundPlaceables.forEach { it.place(0, noTimeTaskHeight + 16) }

                    // 放置时间任务区域
                    timeTasksPlaceables.forEach { it.place(0, noTimeTaskHeight + 16) }
                }
            }
        }
    }
}

// 新增：空无时间任务区域组件
@Composable
fun EmptyNoTimeTasksArea(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp))
            .padding(8.dp)
    ) {
        Text(
            text = "无时间安排",
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // 显示空状态提示
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "今日没有无时间任务",
                color = Color.Gray,
                fontSize = 14.sp
            )
        }
    }
}

// 从API加载特定日期的任务
private fun loadTasksForDate(
    date: Date,
    coroutineScope: kotlinx.coroutines.CoroutineScope,
    callback: (TaskLoadResult) -> Unit
) {
    coroutineScope.launch {
        try {
            // 获取用户ID
            val userId = UserManager.getUserId()
            if (userId.isNullOrEmpty()) {
                callback(TaskLoadResult.Error("用户未登录"))
                return@launch
            }

            // 获取日期格式化为API需要的格式 (yyyy-MM-dd)
            val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            val dateString = dateFormat.format(date)

            // 调用API获取指定日期的任务
            val response = TaskManager.getTasksByDate(userId, dateString)

            when (response) {
                is TaskManager.Result.Success -> {
                    // 修改：即使获取到空任务列表也返回成功，而不是错误
                    val tasks = if (response.data.isEmpty()) {
                        emptyList()
                    } else {
                        convertTaskResponsesToTasks(response.data)
                    }
                    callback(TaskLoadResult.Success(tasks))
                }
                is TaskManager.Result.Error -> {
                    Log.e("TodayView", "获取任务失败: ${response.message}")

                    // 如果错误信息表明是"无任务"，返回空列表作为成功结果
                    if (response.message.contains("无任务") ||
                        response.message.contains("未找到任务") ||
                        response.message.contains("empty")) {
                        callback(TaskLoadResult.Success(emptyList()))
                    } else {
                        callback(TaskLoadResult.Error("获取任务失败: ${response.message}"))
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("TodayView", "获取任务时出现异常", e)
            callback(TaskLoadResult.Error("获取任务时出现异常: ${e.message}"))
        }
    }
}

// 转换TaskResponse为Task对象
private fun convertTaskResponsesToTasks(taskResponses: List<TaskResponse>): List<Task> {
    return taskResponses.map { response ->
        val task = if (response.time_range.isNullOrEmpty()) {
            // 使用没有timeRange的构造函数
            Task(
                id = response.id,
                title = response.title,
                date = response.date ?: Date(),
                important = response.is_important,
                description = response.description
            )
        } else {
            // 使用包含timeRange的构造函数
            Task(
                id = response.id,
                title = response.title,
                timeRange = response.time_range,
                date = response.date ?: Date(),
                durationMinutes = response.duration_minutes ?: 0,
                important = response.is_important,
                description = response.description,
                place = response.place,
                response.due_date
            )
        }

        // 设置可变属性
        task.isFinished = response.is_finished
        task.isDelayed = response.is_delayed
        task.category = response.category

        task
    }
}

@Composable
fun DateHeaderBar(date: Date, onBackClick: () -> Unit, onMoreClick: () -> Unit) {
    val dateFormat = SimpleDateFormat("yyyy年MM月dd日 EEE", Locale.getDefault())

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primaryContainer,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "返回"
                )
            }

            Text(
                text = dateFormat.format(date),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            IconButton(onClick = onMoreClick) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "更多选项"
                )
            }
        }
    }
}

@Composable
fun TimelineBackground(startHour: Int, endHour: Int) {
    val hourHeight = 72.dp

    Column(modifier = Modifier.fillMaxWidth()) {
        // 每小时的背景和时间标记
        for (hour in startHour..endHour) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(hourHeight)
            ) {
                // 小时标签
                Text(
                    text = String.format("%02d", hour),
                    modifier = Modifier
                        .padding(start = 16.dp, top = 2.dp)
                        .width(24.dp),
                    fontSize = 14.sp,
                    color = Color.Gray
                )

                // 水平线
                Divider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopStart),
                    color = Color.LightGray
                )

                // 半小时水平线
                Divider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopStart)
                        .padding(top = hourHeight / 2),
                    color = Color.LightGray.copy(alpha = 0.5f)
                )

                // 垂直分隔线
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .fillMaxHeight()
                        .padding(start = 40.dp)
                        .background(Color.LightGray)
                        .align(Alignment.TopStart)
                )
            }
        }
    }
}

@Composable
fun TaskCard(
    task: Task,
    heightDp: Dp,
    modifier: Modifier = Modifier,
    onEditTask: (String) -> Unit
) {
    val minHeight = 50.dp

    // 当前高度
    val actualHeight = if (heightDp.value < minHeight.value) minHeight else heightDp
    val isMinHeight = actualHeight.value <= 60f

    Card(
        modifier = modifier
            .height(actualHeight)
            .clickable {
                // 点击进入编辑页面，使用回调
                onEditTask(task.id)
            },
        colors = CardDefaults.cardColors(
            containerColor = if (task.isImportant) Color(0xFFE8F5E9) else Color(0xFFE3F2FD)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        // Card 内容
        Box(modifier = Modifier.fillMaxSize()) {
            // 重要性指示条
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(
                        if (task.isImportant) Color(0xFF66BB6A) else Color(0xFF42A5F5)
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 8.dp, end = 4.dp, top = 4.dp, bottom = 4.dp)
            ) {
                // 任务标题
                Text(
                    text = task.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = if (isMinHeight) 2 else 3,
                    overflow = TextOverflow.Ellipsis
                )

                // 如果高度足够，显示时间
                if (!isMinHeight) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = task.timeRange,
                        fontSize = 12.sp,
                        color = Color.Gray
                    )

                    // 如果高度更足够，显示地点
                    if (actualHeight.value > 90f && task.place != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = task.place!!,
                            fontSize = 12.sp,
                            color = Color.Gray,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NoTimeTasksArea(
    tasks: List<Task>,
    modifier: Modifier = Modifier,
    onEditTask: (String) -> Unit
) {
    Column(
        modifier = modifier
            .background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp))
            .padding(8.dp)
    ) {
        Text(
            text = "无时间安排",
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // 将任务分为重要和不重要两组
        val importantTasks = tasks.filter { it.isImportant }
        val normalTasks = tasks.filter { !it.isImportant }

        // 先显示重要任务
        importantTasks.forEach { task ->
            TaskCard(
                task = task,
                heightDp = 80.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                onEditTask = onEditTask
            )
        }

        // 再显示普通任务
        normalTasks.forEach { task ->
            TaskCard(
                task = task,
                heightDp = 80.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                onEditTask = onEditTask
            )
        }
    }
}

@Composable
fun TasksTimeline(
    tasks: List<Task>,
    startHour: Int,
    endHour: Int,
    modifier: Modifier = Modifier,
    onEditTask: (String) -> Unit
) {
    val hourHeight = 72.dp // 每小时的高度
    val minuteHeight = hourHeight / 60 // 每分钟的高度
    val totalHeight = ((endHour - startHour + 1) * hourHeight.value).dp // 转换为 Dp

    // 跟踪已经占用的时间段
    val occupiedTimeSlots = remember { mutableMapOf<String, MutableSet<Int>>() }

    Box(
        modifier = modifier
            .height(totalHeight)
    ) {
        tasks.forEach { task ->
            // 解析任务时间
            val timeRange = task.timeRange
            if (timeRange == "未设定时间") return@forEach

            val timeParts = timeRange.split(" - ")
            if (timeParts.size != 2) return@forEach

            val startTimeParts = timeParts[0].split(":")
            val endTimeParts = timeParts[1].split(":")

            if (startTimeParts.size != 2 || endTimeParts.size != 2) return@forEach

            val taskStartHour = startTimeParts[0].toInt()
            val taskStartMinute = startTimeParts[1].toInt()
            val taskEndHour = endTimeParts[0].toInt()
            val taskEndMinute = endTimeParts[1].toInt()

            // 检查任务是否在显示范围内
            if (taskStartHour < startHour && taskEndHour < startHour) return@forEach
            if (taskStartHour > endHour) return@forEach

            // 计算任务的位置和大小
            val displayStartHour = max(taskStartHour, startHour)
            val displayStartMinute = if (taskStartHour < startHour) 0 else taskStartMinute
            val displayEndHour = min(taskEndHour, endHour)
            val displayEndMinute = if (taskEndHour > endHour) 59 else taskEndMinute

            val totalMinutesFromStart = (displayStartHour - startHour) * 60 + displayStartMinute
            val topOffset = (minuteHeight.value * totalMinutesFromStart).dp

            val taskDurationMinutes = (displayEndHour * 60 + displayEndMinute) - (displayStartHour * 60 + displayStartMinute)
            val taskHeight = max((minuteHeight.value * taskDurationMinutes).dp, 50.dp)

            // 确定任务放置的列
            val column = findAvailableColumn(task, occupiedTimeSlots, totalMinutesFromStart, taskDurationMinutes)

            // 更新占用的时间段
            val key = "col$column"
            if (!occupiedTimeSlots.containsKey(key)) {
                occupiedTimeSlots[key] = mutableSetOf()
            }
            for (minute in totalMinutesFromStart until (totalMinutesFromStart + taskDurationMinutes)) {
                occupiedTimeSlots[key]?.add(minute)
            }

            // 使用TaskCard显示任务，移除拖动功能
            TaskCard(
                task = task,
                heightDp = taskHeight,
                modifier = Modifier
                    .padding(start = 40.dp + (column * 70).dp, top = topOffset)
                    .width(65.dp)
                    .zIndex(if (task.isImportant) 10f else 5f),
                onEditTask = onEditTask
            )
        }
    }
}

// 寻找可用的任务列
fun findAvailableColumn(task: Task, occupiedTimeSlots: Map<String, Set<Int>>, startMinute: Int, durationMinutes: Int): Int {
    // 重要任务优先放在第一列
    if (task.isImportant) return 0

    // 任务分类顺序
    val categoryPriority = mapOf(
        "学习" to 1,
        "工作" to 2,
        "生活" to 3,
        "其他" to 4
    )

    // 根据任务分类确定起始列
    val startColumn = categoryPriority[task.category] ?: 4

    // 查找从startColumn开始的第一个可用列
    for (col in startColumn..10) { // 最多支持10列
        val key = "col$col"
        val occupied = occupiedTimeSlots[key] ?: emptySet()

        // 检查是否有重叠
        var hasOverlap = false
        for (minute in startMinute until (startMinute + durationMinutes)) {
            if (minute in occupied) {
                hasOverlap = true
                break
            }
        }

        if (!hasOverlap) {
            return col
        }
    }

    // 如果所有列都被占用，返回最后一列
    return 10
}