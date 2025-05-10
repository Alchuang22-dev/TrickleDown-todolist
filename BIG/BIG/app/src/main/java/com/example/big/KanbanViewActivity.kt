package com.example.big

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.lifecycleScope
import com.example.big.models.TaskResponse
import com.example.big.utils.TaskManager
import com.example.big.utils.UserManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

class KanbanViewActivityCompose : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    KanbanScreen(lifecycleScope)
                }
            }
        }
    }
}

// 调试标签
private const val DEBUG_TAG = "KanbanDebug"

fun debugLog(message: String) {
    Log.d(DEBUG_TAG, message)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun KanbanScreen(coroutineScope: kotlinx.coroutines.CoroutineScope) {
    // 定义状态
    val context = LocalContext.current

    // 添加任务数据状态
    var tasks by remember { mutableStateOf<List<Task>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var currentDate by remember { mutableStateOf(Calendar.getInstance()) }
    val scope = rememberCoroutineScope()

    // 从API加载任务数据
    LaunchedEffect(key1 = true) {
        loadTasksFromApi(coroutineScope) { result ->
            when (result) {
                is TaskLoadResult.Success -> {
                    tasks = result.tasks
                    isLoading = false
                }
                is TaskLoadResult.Error -> {
                    errorMessage = result.message
                    isLoading = false
                }
            }
        }
    }

    // 保持当前主要展示的三天
    val visibleDates = remember(currentDate.timeInMillis) {
        val dates = mutableListOf<Calendar>()
        val startDate = currentDate.clone() as Calendar
        startDate.add(Calendar.DAY_OF_MONTH, -1)  // 从前一天开始

        for (i in 0 until 3) {
            val date = startDate.clone() as Calendar
            date.add(Calendar.DAY_OF_MONTH, i)
            dates.add(date)
        }
        dates
    }

    // 处理左右翻页
    val onPreviousDays = {
        val newDate = currentDate.clone() as Calendar
        newDate.add(Calendar.DAY_OF_MONTH, -3)
        currentDate = newDate
    }

    val onNextDays = {
        val newDate = currentDate.clone() as Calendar
        newDate.add(Calendar.DAY_OF_MONTH, 3)
        currentDate = newDate
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 顶部标题栏 - 显示当前月份
            TopAppBar(
                month = SimpleDateFormat("M月", Locale.getDefault()).format(currentDate.time),
                onBack = { (context as? ComponentActivity)?.finish() }
            )

            // 日期选择栏 - 显示三天
            DateSelectionBar(
                dates = visibleDates,
                onDateClick = { date ->
                    val intent = Intent(context, TodayViewComposeActivity::class.java)
                    intent.putExtra("selected_date", date.timeInMillis)
                    context.startActivity(intent)
                },
                onSwipeLeft = onNextDays,
                onSwipeRight = onPreviousDays
            )

            // 显示加载状态或错误信息
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                errorMessage != null -> {
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
                }
                else -> {
                    // 修改：使用多日视图时间线
                    MultiDayTimelineView(visibleDates, tasks)
                }
            }
        }

        // 错误提示信息
        if (errorMessage != null) {
            Snackbar(
                modifier = Modifier.padding(16.dp),
                action = {
                    Text(
                        text = "重试",
                        modifier = Modifier.clickable {
                            errorMessage = null
                            isLoading = true
                            loadTasksFromApi(coroutineScope) { result ->
                                when (result) {
                                    is TaskLoadResult.Success -> {
                                        tasks = result.tasks
                                        isLoading = false
                                    }
                                    is TaskLoadResult.Error -> {
                                        errorMessage = result.message
                                        isLoading = false
                                    }
                                }
                            }
                        }
                    )
                }
            ) {
                Text(errorMessage ?: "未知错误")
            }
        }

        // 添加任务的FAB
        FloatingActionButton(
            onClick = {
                val intent = Intent(context, AddTaskActivity::class.java)
                context.startActivity(intent)
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .size(56.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White,
            shape = CircleShape
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add Task"
            )
        }
    }
}

// 任务加载结果封装类
sealed class TaskLoadResult {
    data class Success(val tasks: List<Task>) : TaskLoadResult()
    data class Error(val message: String) : TaskLoadResult()
}

// 从API加载任务数据
private fun loadTasksFromApi(
    coroutineScope: kotlinx.coroutines.CoroutineScope,
    callback: (TaskLoadResult) -> Unit
) {
    coroutineScope.launch {
        try {
            val userId = UserManager.getUserId()
            Log.d(DEBUG_TAG, "获取到用户ID: $userId")

            if (userId.isNullOrEmpty()) {
                Log.e(DEBUG_TAG, "用户未登录，无法获取任务")
                callback(TaskLoadResult.Error("用户未登录"))
                return@launch
            }

            Log.d(DEBUG_TAG, "开始请求任务列表 userId=$userId")

            // 使用修改后的方法
            val response = TaskManager.getAllTasksWithPagination(userId)

            Log.d(DEBUG_TAG, "任务列表请求完成，检查结果")

            when (response) {
                is TaskManager.Result.Success -> {
                    val taskResponses = response.data
                    Log.d(DEBUG_TAG, "获取到 ${taskResponses.size} 个任务")

                    // 检查是否有未来日期的任务
                    val now = Date()
                    val futureTasks = taskResponses.filter { it.date != null && it.date.after(now) }
                    Log.d(DEBUG_TAG, "未来日期的任务数量: ${futureTasks.size}, 具体有: ${futureTasks.map { it.title }}")

                    // 将 TaskResponse 转换为 Task 对象
                    val tasks = convertTaskResponsesToTasks(taskResponses)
                    Log.d(DEBUG_TAG, "转换后共有 ${tasks.size} 个任务")

                    // 过滤出重要的任务
                    val importantTasks = tasks.filter { it.isImportant }
                    Log.d(DEBUG_TAG, "过滤出 ${importantTasks.size} 个重要任务")

                    // 按时间排序
                    val sortedTasks = sortTasksByTime(importantTasks)
                    Log.d(DEBUG_TAG, "排序后准备显示 ${sortedTasks.size} 个任务")

                    callback(TaskLoadResult.Success(sortedTasks))
                }
                is TaskManager.Result.Error -> {
                    Log.e(DEBUG_TAG, "获取任务失败: ${response.message}")
                    callback(TaskLoadResult.Error("获取任务失败: ${response.message}"))
                }
            }
        } catch (e: Exception) {
            Log.e(DEBUG_TAG, "获取任务时出现异常", e)
            e.printStackTrace()
            callback(TaskLoadResult.Error("获取任务时出现异常: ${e.message}"))
        }
    }
}

// 转换任务数据
private fun convertTaskResponsesToTasks(taskResponses: List<TaskResponse>): List<Task> {
    return taskResponses.map { response ->
        val task = if (response.time_range.isNullOrEmpty()) {
            // 使用没有timeRange的构造函数 (2)
            Task(
                id = response.id,
                title = response.title,
                date = response.date ?: Date(),
                important = response.is_important,
                description = response.description
            )
        } else {
            // 使用包含place的构造函数 (3)
            Task(
                id = response.id,
                title = response.title,
                timeRange = response.time_range,
                date = response.date ?: Date(),
                durationMinutes = response.duration_minutes ?: 0,
                important = response.is_important,
                description = response.description,
                place = response.place
            )
        }

        // 设置可变属性
        task.isFinished = response.is_finished
        task.isDelayed = response.is_delayed
        task.category = response.category
        task.dueDate = response.due_date

        task
    }
}


// 按时间排序任务
private fun sortTasksByTime(tasks: List<Task>): List<Task> {
    return tasks.sortedWith { t1, t2 ->
        try {
            // 检查分隔符，可能是 "--" 或 "-"
            val timeRange1 = t1.timeRange
            val timeRange2 = t2.timeRange

            // 如果是"未设定时间"，放到最后
            if (timeRange1 == "未设定时间" && timeRange2 != "未设定时间") {
                return@sortedWith 1
            }
            if (timeRange1 != "未设定时间" && timeRange2 == "未设定时间") {
                return@sortedWith -1
            }
            if (timeRange1 == "未设定时间" && timeRange2 == "未设定时间") {
                return@sortedWith 0
            }

            val delimiter1 = if (timeRange1.contains("--")) "--" else "-"
            val delimiter2 = if (timeRange2.contains("--")) "--" else "-"

            // 分割时间范围
            val startTimeStr1 = timeRange1.split(delimiter1)[0].trim()
            val startTimeStr2 = timeRange2.split(delimiter2)[0].trim()

            // 提取小时和分钟，移除可能的空格
            val cleanTimeStr1 = startTimeStr1.replace(" ", "")
            val cleanTimeStr2 = startTimeStr2.replace(" ", "")

            val hourMinute1 = cleanTimeStr1.split(":")
            val hourMinute2 = cleanTimeStr2.split(":")

            // 转换为分钟数
            val hours1 = hourMinute1[0].toInt()
            val minutes1 = hourMinute1[1].toInt()
            val hours2 = hourMinute2[0].toInt()
            val minutes2 = hourMinute2[1].toInt()

            (hours1 * 60 + minutes1) - (hours2 * 60 + minutes2)  // 比较分钟数
        } catch (e: Exception) {
            Log.e(DEBUG_TAG, "解析任务时间失败: ${e.message} for timeRange1: ${t1.timeRange}, timeRange2: ${t2.timeRange}", e)
            0  // 无法比较时返回0
        }
    }
}

@Composable
fun TopAppBar(month: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Back"
            )
        }

        Text(
            text = month,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )

        IconButton(onClick = { /* 打开菜单 */ }) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "Menu"
            )
        }
    }
}

@Composable
fun DateSelectionBar(
    dates: List<Calendar>,
    onDateClick: (Calendar) -> Unit,
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit
) {
    val today = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    // 改进滑动检测
    var initialX by remember { mutableStateOf(0f) }
    val MIN_SWIPE_DISTANCE = 50f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        initialX = offset.x
                    },
                    onDragEnd = {
                        // 不做任何操作
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val currentX = change.position.x
                        val distance = currentX - initialX

                        if (distance > MIN_SWIPE_DISTANCE) {
                            // 向右滑动
                            initialX = currentX
                            onSwipeRight()
                        } else if (distance < -MIN_SWIPE_DISTANCE) {
                            // 向左滑动
                            initialX = currentX
                            onSwipeLeft()
                        }
                    }
                )
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            dates.forEach { date ->
                val isToday = date.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                        date.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)

                val dayFormat = SimpleDateFormat("E", Locale.getDefault())
                val dateFormat = SimpleDateFormat("dd", Locale.getDefault())

                Box(
                    modifier = Modifier
                        .size(width = 64.dp, height = 80.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isToday) MaterialTheme.colorScheme.primary else Color.Transparent)
                        .clickable { onDateClick(date) }
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = dayFormat.format(date.time),
                            fontSize = 14.sp,
                            color = if (isToday) Color.White else Color.Black
                        )
                        Text(
                            text = dateFormat.format(date.time),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isToday) Color.White else Color.Black
                        )

                        // 特殊日期
                        val specialDay = getSpecialDay(date)
                        if (specialDay != null) {
                            Text(
                                text = specialDay,
                                fontSize = 12.sp,
                                color = if (isToday) Color.White else Color.Gray
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun updateTaskTime(
    task: Task,
    newDate: Calendar,
    newStartTotalMinutes: Int,
    newEndTotalMinutes: Int,
    timeTasksByDate: Map<Calendar, List<Task>>,
    coroutineScope: CoroutineScope,
    originalTasksList: List<Task>,
    onTasksUpdated: (List<Task>) -> Unit,
    onUpdateMessage: (String?) -> Unit,
    onIsUpdating: (Boolean) -> Unit
) = coroutineScope.launch {
    val logTag = "UpdateTaskTime"

    try {
        onIsUpdating(true)
        Log.d(logTag, "开始处理任务移动: 任务=${task.id}, 目标日期=${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(newDate.time)}")
        Log.d(logTag, "新时间段: ${newStartTotalMinutes/60}:${newStartTotalMinutes%60} - ${newEndTotalMinutes/60}:${newEndTotalMinutes%60}")

        // 计算新的开始和结束时间
        val newStartHour = newStartTotalMinutes / 60
        val newStartMinute = newStartTotalMinutes % 60
        val newEndHour = newEndTotalMinutes / 60
        val newEndMinute = newEndTotalMinutes % 60

        // 格式化时间字符串
        val startTimeStr = String.format("%02d:%02d", newStartHour, newStartMinute)
        val endTimeStr = String.format("%02d:%02d", newEndHour, newEndMinute)
        val newTimeRange = "$startTimeStr - $endTimeStr"
        val newDurationMinutes = newEndTotalMinutes - newStartTotalMinutes

        // 创建一个临时更新后的任务对象用于冲突检测和UI预览
        val updatedTask = Task(
            id = task.id,
            title = task.title,
            timeRange = newTimeRange,
            date = newDate.time,
            durationMinutes = newDurationMinutes,
            important = task.isImportant,
            description = task.description,
            place = task.place
        ).apply {
            isFinished = task.isFinished
            isDelayed = task.isDelayed
            category = task.category
            dueDate = task.dueDate
        }

        // 重要：获取最新任务列表再进行冲突检测
        val latestTasks = getLatestTasksFromServer()
        if (latestTasks == null) {
            onUpdateMessage("无法获取最新任务状态")
            onIsUpdating(false)
            return@launch
        }

        // 准备进行冲突检测
        val tasksOnTargetDate = latestTasks.filter {
            isSameDay(it.date, newDate.time) && it.id != task.id
        }

        Log.d(logTag, "目标日期有 ${tasksOnTargetDate.size} 个其他任务")

        // 检查时间冲突
        val conflicts = tasksOnTargetDate.filter { otherTask ->
            try {
                // 跳过无时间任务
                if (otherTask.timeRange == "未设定时间" || otherTask.durationMinutes <= 0) {
                    return@filter false
                }

                // 解析时间
                val otherTimeResult = parseTimeRange(otherTask.timeRange)
                if (!otherTimeResult.isValid) {
                    Log.w(logTag, "无法解析任务时间: ${otherTask.timeRange}")
                    return@filter false
                }

                // 计算分钟数
                val otherStartMin = otherTimeResult.startHour * 60 + otherTimeResult.startMinute
                val otherEndMin = otherTimeResult.endHour * 60 + otherTimeResult.endMinute

                // 检查重叠
                val hasOverlap = newStartTotalMinutes < otherEndMin &&
                        newEndTotalMinutes > otherStartMin

                if (hasOverlap) {
                    Log.d(logTag, "发现冲突: ${otherTask.title} (${otherTask.timeRange})")
                }

                hasOverlap
            } catch (e: Exception) {
                Log.e(logTag, "检查冲突时出错", e)
                false
            }
        }

        // 处理冲突
        if (conflicts.isNotEmpty()) {
            val conflictNames = conflicts.joinToString(", ") { it.title }
            val conflictTimes = conflicts.joinToString(", ") { it.timeRange }

            Log.d(logTag, "发现冲突，取消更新: $conflictNames")
            onUpdateMessage("与已有任务冲突: $conflictNames ($conflictTimes)")
            delay(3000)
            onUpdateMessage(null)
            onIsUpdating(false)
            return@launch
        }

        // 先更新UI
        Log.d(logTag, "无冲突，更新UI")
        val updatedTasks = originalTasksList.map {
            if (it.id == task.id) updatedTask else it
        }
        onTasksUpdated(updatedTasks)
        onUpdateMessage("正在更新任务...")

        // 然后更新服务器
        try {
            // 准备结束时间
            val newDueDate = Calendar.getInstance().apply {
                time = newDate.time
                set(Calendar.HOUR_OF_DAY, newEndHour)
                set(Calendar.MINUTE, newEndMinute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.time

            // 准备请求对象
            val request = com.example.big.models.CreateTaskRequest(
                title = task.title,
                time_range = newTimeRange,
                date = newDate.time,
                duration_minutes = newDurationMinutes,
                is_important = task.isImportant,
                description = task.description ?: "",
                place = task.place ?: "",
                due_date = newDueDate,
                is_finished = task.isFinished,
                is_delayed = task.isDelayed,
                category = task.category ?: "其他"
            )

            // 提交更新
            Log.d(logTag, "提交到服务器")
            val result = TaskManager.updateTask(task.id, request)

            when (result) {
                is TaskManager.Result.Success -> {
                    Log.d(logTag, "服务器更新成功")
                    onUpdateMessage("任务已更新")

                    // 刷新任务列表
                    val refreshedTasks = getLatestTasksFromServer()
                    if (refreshedTasks != null) {
                        onTasksUpdated(refreshedTasks)
                    }
                }
                is TaskManager.Result.Error -> {
                    Log.e(logTag, "服务器更新失败: ${result.message}")
                    onUpdateMessage("更新失败: ${result.message}")
                    onTasksUpdated(originalTasksList)  // 恢复原始状态
                }
            }
        } catch (e: Exception) {
            Log.e(logTag, "更新过程中出错", e)
            onUpdateMessage("更新出错: ${e.message}")
            onTasksUpdated(originalTasksList)  // 恢复原始状态
        } finally {
            delay(2000)
            onUpdateMessage(null)
            onIsUpdating(false)
        }
    } catch (e: Exception) {
        Log.e(logTag, "处理更新请求时出错", e)
        onUpdateMessage("处理失败: ${e.message}")
        onIsUpdating(false)
        delay(2000)
        onUpdateMessage(null)
    }
}

// 辅助函数：从服务器获取最新任务列表
private suspend fun getLatestTasksFromServer(): List<Task>? {
    try {
        val userId = UserManager.getUserId() ?: return null

        val response = TaskManager.getAllTasksWithPagination(userId)
        return when (response) {
            is TaskManager.Result.Success -> {
                val taskResponses = response.data
                val tasks = convertTaskResponsesToTasks(taskResponses)
                val importantTasks = tasks.filter { it.isImportant }
                sortTasksByTime(importantTasks)
            }
            is TaskManager.Result.Error -> null
        }
    } catch (e: Exception) {
        Log.e("TaskFetch", "获取最新任务失败", e)
        return null
    }
}

// 辅助函数：按日期分组任务
private fun groupTasksByDate(tasks: List<Task>, targetDate: Calendar): List<Task> {
    return tasks.filter { task ->
        isSameDay(task.date, targetDate.time)
    }
}

// 辅助类：时间解析结果
data class TimeParseResult(
    val startHour: Int,
    val startMinute: Int,
    val endHour: Int,
    val endMinute: Int,
    val isValid: Boolean
)

// 辅助函数：解析时间范围
private fun parseTimeRange(timeRange: String): TimeParseResult {
    try {
        val parts = timeRange.split(" - ")
        if (parts.size != 2) {
            return TimeParseResult(0, 0, 0, 0, false)
        }

        val startParts = parts[0].trim().split(":")
        val endParts = parts[1].trim().split(":")

        if (startParts.size != 2 || endParts.size != 2) {
            return TimeParseResult(0, 0, 0, 0, false)
        }

        val startHour = startParts[0].toInt()
        val startMinute = startParts[1].toInt()
        val endHour = endParts[0].toInt()
        val endMinute = endParts[1].toInt()

        return TimeParseResult(startHour, startMinute, endHour, endMinute, true)
    } catch (e: Exception) {
        return TimeParseResult(0, 0, 0, 0, false)
    }
}

@Composable
fun MultiDayTimelineView(dates: List<Calendar>, allTasks: List<Task>) {
    val startHour = 0
    val endHour = 23
    val scrollState = rememberScrollState()
    val fiveMinHeightDp = 6.dp
    val hourHeightDp = fiveMinHeightDp * 12
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val logTag = "MultiDayTimeline"

    // 当allTasks更新时，重置tasks状态 - 确保视图始终使用最新数据
    var tasks by remember { mutableStateOf(allTasks) }
    var updateMessage by remember { mutableStateOf<String?>(null) }
    var isUpdating by remember { mutableStateOf(false) }

    // 当外部任务列表变化时更新本地状态
    LaunchedEffect(allTasks) {
        if (allTasks != tasks) {
            Log.d(logTag, "检测到任务列表变化，更新本地状态")
            tasks = allTasks
        }
    }

    // 计算时间线总高度
    val timelineHeight = ((endHour - startHour + 1) * hourHeightDp.value).dp

    // 每次任务列表变化时，重新计算分组
    val noTimeTasksByDate = remember(tasks) {
        dates.associateWith { date ->
            tasks.filter { task ->
                isSameDay(task.date, date.time) &&
                        (task.timeRange == "未设定时间" || task.durationMinutes == 0)
            }
        }
    }

    val timeTasksByDate = remember(tasks) {
        dates.associateWith { date ->
            tasks.filter { task ->
                isSameDay(task.date, date.time) &&
                        task.timeRange != "未设定时间" &&
                        task.durationMinutes > 0
            }
        }
    }

    // 修改为使用新的封装函数处理任务更新
    val updateTaskTimeWrapper: (Task, Calendar, Int, Int) -> Unit = { task, newDate, newStartMinutes, newEndMinutes ->
        updateTaskTime(
            task = task,
            newDate = newDate,
            newStartTotalMinutes = newStartMinutes,
            newEndTotalMinutes = newEndMinutes,
            timeTasksByDate = timeTasksByDate,
            coroutineScope = coroutineScope,
            originalTasksList = tasks,
            onTasksUpdated = { updatedTasks ->
                Log.d(logTag, "任务更新成功，更新本地任务列表")
                // 确保更新后的任务是全新的列表实例
                tasks = updatedTasks.toList()
            },
            onUpdateMessage = { message ->
                Log.d(logTag, "显示更新消息: $message")
                updateMessage = message
            },
            onIsUpdating = { updating ->
                Log.d(logTag, "更新状态: $updating")
                isUpdating = updating
            }
        )
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            // 无时间任务区域 - 确保始终显示，哪怕没有任务也显示标题
            NoTimeTasksSection(
                tasksByDate = noTimeTasksByDate,
                dates = dates,
                onTaskMoved = updateTaskTimeWrapper
            )

            // 时间线区域 - 使用绝对定位
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(timelineHeight)
            ) {
                // 绘制时间格线
                for (hour in startHour..endHour) {
                    val hourOffset = ((hour - startHour) * hourHeightDp.value).dp

                    // 小时线
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .offset(y = hourOffset)
                            .background(Color.LightGray)
                    ) {}

                    // 小时标签
                    Text(
                        text = String.format("%02d", hour),
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .offset(x = 16.dp, y = hourOffset)
                            .width(40.dp),
                        fontSize = 14.sp,
                        color = Color.Gray
                    )

                    // 半小时线
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .offset(y = hourOffset + hourHeightDp / 2)
                            .background(Color.LightGray.copy(alpha = 0.5f))
                    ) {}
                }

                // 垂直分隔线（日期列分隔）
                for (i in 0..dates.size) {
                    val columnWidth = (LocalConfiguration.current.screenWidthDp - 40) / dates.size
                    val xPosition = if (i == 0) 40.dp else 40.dp + (i * columnWidth).dp

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(timelineHeight)
                            .offset(x = xPosition, y = 0.dp)
                            .background(Color.LightGray)
                    )
                }

                // 在一个Box中放置所有任务，启用绝对定位
                dates.forEachIndexed { dateIndex, date ->
                    val dateTasks = timeTasksByDate[date] ?: emptyList()

                    dateTasks.forEach { task ->
                        // 解析任务时间
                        val timeParts = task.timeRange.split(" - ")
                        if (timeParts.size != 2) return@forEach

                        val startTimeParts = timeParts[0].split(":")
                        val endTimeParts = timeParts[1].split(":")
                        if (startTimeParts.size != 2 || endTimeParts.size != 2) return@forEach

                        val taskStartHour = startTimeParts[0].toInt()
                        val taskStartMinute = startTimeParts[1].toInt()
                        val taskEndHour = endTimeParts[0].toInt()
                        val taskEndMinute = endTimeParts[1].toInt()

                        // 检查任务是否在时间线范围内
                        if (taskStartHour >= startHour && taskStartHour <= endHour) {
                            // 计算绝对定位坐标
                            val topPositionInMinutes = (taskStartHour - startHour) * 60 + taskStartMinute
                            val topOffsetDp = (topPositionInMinutes / 5f) * fiveMinHeightDp.value

                            // 计算任务持续时间（分钟）
                            val taskDurationMinutes = (taskEndHour * 60 + taskEndMinute) - (taskStartHour * 60 + taskStartMinute)

                            // 计算高度
                            val heightInDp = (taskDurationMinutes / 5f) * fiveMinHeightDp.value
                            val taskHeight = max(heightInDp, 50f).dp

                            // 计算水平位置
                            val columnWidth = (LocalConfiguration.current.screenWidthDp - 40) / dates.size
                            val leftPosition = 40.dp + (dateIndex * columnWidth).dp
                            val taskWidth = columnWidth.dp - 8.dp

                            // 使用绝对定位放置任务卡片，现在传入任务更新回调和日期信息
                            DraggableTaskCard(
                                task = task,
                                heightDp = taskHeight,
                                modifier = Modifier
                                    .width(taskWidth)
                                    .offset(x = leftPosition + 4.dp, y = topOffsetDp.dp)
                                    .zIndex(1f),
                                onTaskMoved = updateTaskTimeWrapper,
                                visibleDates = dates,
                                dateIndex = dateIndex,
                                startHour = startHour,
                                hourHeightDp = hourHeightDp
                            )
                        }
                    }
                }
            }
        }

        // 显示任务更新状态
        if (isUpdating) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 80.dp)
                    .background(MaterialTheme.colorScheme.surface, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 2.dp
                )
            }
        }

        // 显示更新结果消息
        AnimatedVisibility(
            visible = updateMessage != null,
            enter = fadeIn(animationSpec = tween(300)),
            exit = fadeOut(animationSpec = tween(300)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        ) {
            Card(
                modifier = Modifier.padding(16.dp),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Text(
                    text = updateMessage ?: "",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun NoTimeTasksSection(
    tasksByDate: Map<Calendar, List<Task>>,
    dates: List<Calendar>,
    onTaskMoved: ((Task, Calendar, Int, Int) -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Text(
            text = "无时间安排",
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(8.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
        ) {
            // 为每一天创建一个列
            dates.forEachIndexed { index, date ->
                val tasks = tasksByDate[date] ?: emptyList()

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // 日期标题
                        val dateFormat = SimpleDateFormat("MM/dd", Locale.getDefault())
                        Text(
                            text = dateFormat.format(date.time),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )

                        // 任务列表
                        if (tasks.isEmpty()) {
                            // 显示空状态
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(40.dp)
                                    .background(Color(0xFFEEEEEE), RoundedCornerShape(4.dp))
                                    .padding(4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "无任务",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }
                        } else {
                            // 显示任务
                            tasks.forEach { task ->
                                SimpleTaskCard(
                                    task = task,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp),
                                    onMoveToTimeline = { task, date ->
                                        // 将无时间任务移动到时间线上的回调
                                        // 默认移动到当天的8:00-9:00
                                        onTaskMoved?.invoke(task, date, 8 * 60, 9 * 60)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        Divider(modifier = Modifier.padding(vertical = 8.dp))
    }
}

// 简化版任务卡片 - 用于无时间任务
@Composable
fun SimpleTaskCard(
    task: Task,
    modifier: Modifier = Modifier,
    onMoveToTimeline: ((Task, Calendar) -> Unit)? = null
) {
    var showMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Card(
        modifier = modifier
            .clickable {
                val intent = Intent(context, EditTaskActivity::class.java)
                intent.putExtra("task_id", task.id)
                context.startActivity(intent)
            },
        colors = CardDefaults.cardColors(
            containerColor = if (task.isImportant) Color(0xFFE8F5E9) else Color(0xFFE3F2FD)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            // 重要性指示条
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(24.dp)
                    .background(
                        if (task.isImportant) Color(0xFF66BB6A) else Color(0xFF42A5F5)
                    )
                    .align(Alignment.CenterStart)
            )

            // 任务标题
            Text(
                text = task.title,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 24.dp)
                    .align(Alignment.Center)
            )

            // 菜单按钮
            IconButton(
                onClick = { showMenu = true },
                modifier = Modifier
                    .size(24.dp)
                    .align(Alignment.CenterEnd)
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "更多选项",
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}


// 简化的KanbanDragState，只跟踪当前拖动的卡片ID
object KanbanDragState {
    var currentDraggingId: String? = null
}

@Composable
fun DraggableTaskCard(
    task: Task,
    heightDp: Dp,
    modifier: Modifier = Modifier,
    onTaskMoved: ((Task, Calendar, Int, Int) -> Unit)? = null,
    visibleDates: List<Calendar>,
    dateIndex: Int,
    startHour: Int,
    hourHeightDp: Dp
) {
    val context = LocalContext.current
    val minHeight = 50.dp
    val logTag = "DraggableTask"

    // 任务卡片高度
    val actualHeight = if (heightDp.value < minHeight.value) minHeight else heightDp
    val isMinHeight = actualHeight.value <= 60f

    // 为每个卡片创建唯一键
    val uniqueCardKey = remember(task.id, task.timeRange, dateIndex) {
        "${task.id}_${task.timeRange}_${dateIndex}"
    }

    // 关键状态变量
    var isDragging by remember { mutableStateOf(false) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    var lastTapTime by remember { mutableStateOf(0L) }
    var hasMoved by remember { mutableStateOf(false) }
    var previewTimeRange by remember { mutableStateOf<String?>(null) }
    var previewDate by remember { mutableStateOf<Calendar?>(null) }
    var calculatedStartMinutes by remember { mutableStateOf(0) }
    var calculatedEndMinutes by remember { mutableStateOf(0) }

    // 新增：控制卡片更新和位置状态
    var isUpdating by remember { mutableStateOf(false) }
    var keepPosition by remember { mutableStateOf(false) }

    // 解析任务初始时间以便后续计算
    val timeInfo = parseTaskTime(task.timeRange, task.durationMinutes)
    val startHourValue = timeInfo.first
    val startMinuteValue = timeInfo.second
    val durationMinutes = timeInfo.fifth

    // 初始化或任务变化时重置状态
    LaunchedEffect(task.id, task.timeRange) {
        // 如果正在更新或需要保持位置，不重置位置偏移
        if (!isUpdating && !keepPosition) {
            offsetX = 0f
            offsetY = 0f
        }

        // 始终重置其他状态
        isDragging = false
        previewTimeRange = null
        previewDate = null
        hasMoved = false
    }

    // 密度和尺寸计算
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val leftPaddingPx = with(density) { 40.dp.toPx() }
    val columnWidth = (screenWidthPx - leftPaddingPx) / visibleDates.size
    val columnWidthPx = columnWidth
    val hourHeightPx = with(density) { hourHeightDp.toPx() }
    val timelineHeight = with(density) { ((24 - startHour) * hourHeightDp.value).dp.toPx() }

    // 计算允许的拖动范围
    val maxDragLeft = -(dateIndex * columnWidth)
    val maxDragRight = ((visibleDates.size - dateIndex - 1) * columnWidth)
    fun resetCard() {
        if (!keepPosition) {
            offsetX = 0f
            offsetY = 0f
        }
        previewTimeRange = null
        previewDate = null
        hasMoved = false
    }

    Card(
        modifier = modifier
            .height(actualHeight)
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .pointerInput(uniqueCardKey) {
                detectDragGestures(
                    onDragStart = {
                        if (isUpdating) return@detectDragGestures

                        lastTapTime = System.currentTimeMillis()
                        hasMoved = false
                        keepPosition = false

                        // 只允许一个卡片拖动
                        if (KanbanDragState.currentDraggingId == null ||
                            KanbanDragState.currentDraggingId == uniqueCardKey) {
                            isDragging = true
                            KanbanDragState.currentDraggingId = uniqueCardKey
                            Log.d(logTag, "开始拖动: $uniqueCardKey")
                        }
                    },
                    onDragEnd = {
                        if (!isDragging) return@detectDragGestures

                        Log.d(logTag, "拖动结束 - 偏移: x=$offsetX, y=$offsetY")

                        // 检查是否只是点击而非拖动
                        val isClickOperation = !hasMoved &&
                                (System.currentTimeMillis() - lastTapTime < 200)

                        // 清除全局拖动状态
                        if (KanbanDragState.currentDraggingId == uniqueCardKey) {
                            KanbanDragState.currentDraggingId = null
                        }

                        // 处理点击
                        if (isClickOperation) {
                            Log.d(logTag, "处理点击操作")
                            isDragging = false
                            resetCard()

                            // 启动编辑活动
                            val intent = Intent(context, EditTaskActivity::class.java)
                            intent.putExtra("task_id", task.id)
                            context.startActivity(intent)
                            return@detectDragGestures
                        }

                        // 处理有效拖动
                        if (hasMoved && previewDate != null) {
                            Log.d(logTag, "有效拖动 - 准备更新任务")
                            Log.d(logTag, "新日期: ${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(previewDate!!.time)}")
                            Log.d(logTag, "新时间: $previewTimeRange (分钟: $calculatedStartMinutes-$calculatedEndMinutes)")

                            // 保存位置信息
                            keepPosition = true
                            isUpdating = true
                            isDragging = false

                            // 使用保存的坐标延续视觉效果
                            val finalDate = previewDate!!
                            val finalStartMinutes = calculatedStartMinutes
                            val finalEndMinutes = calculatedEndMinutes

                            // 部分状态重置
                            previewTimeRange = null
                            previewDate = null
                            hasMoved = false

                            // 调用更新回调
                            onTaskMoved?.invoke(
                                task,
                                finalDate,
                                finalStartMinutes,
                                finalEndMinutes
                            )

                            // 任务更新完成后的清理
                            isUpdating = false
                        } else {
                            // 无效拖动，重置卡片
                            Log.d(logTag, "无效拖动，重置卡片")
                            isDragging = false
                            resetCard()
                        }
                    },
                    onDragCancel = {
                        if (KanbanDragState.currentDraggingId == uniqueCardKey) {
                            KanbanDragState.currentDraggingId = null
                        }
                        isDragging = false
                        resetCard()
                    },
                    onDrag = { change, dragAmount ->
                        if (!isDragging) return@detectDragGestures

                        change.consume()

                        // 检测实质性移动
                        if (abs(dragAmount.x) > 3f || abs(dragAmount.y) > 3f) {
                            hasMoved = true
                        }

                        // 更新卡片偏移量
                        offsetX += dragAmount.x
                        offsetY += dragAmount.y

                        // 限制拖动范围
                        offsetX = offsetX.coerceIn(maxDragLeft, maxDragRight)
                        offsetY = offsetY.coerceIn(-hourHeightPx * 2, timelineHeight)

                        // 计算新的日期索引
                        val absoluteX = offsetX + (dateIndex * columnWidthPx) + leftPaddingPx
                        val relativeX = absoluteX - leftPaddingPx
                        val newDateIndex = (relativeX / columnWidthPx).toInt().coerceIn(0, visibleDates.size - 1)

                        // 计算新时间
                        val currentMinutesFromStart = (startHourValue - startHour) * 60 + startMinuteValue
                        val currentStartYPosition = (currentMinutesFromStart / 60f) * hourHeightPx
                        val newYPosition = currentStartYPosition + offsetY

                        val totalMinutesRaw = (newYPosition / hourHeightPx * 60).toInt()
                        // 对齐到5分钟
                        val snappedMinutes = max(0, (totalMinutesRaw / 5) * 5)

                        // 转换为小时和分钟
                        val newStartHour = startHour + (snappedMinutes / 60)
                        val newStartMinute = snappedMinutes % 60

                        if (newDateIndex in visibleDates.indices) {
                            val newDate = visibleDates[newDateIndex]
                            previewDate = newDate

                            // 计算结束时间（保持原始持续时间）
                            val newEndMinutes = snappedMinutes + durationMinutes
                            val endHour = startHour + (newEndMinutes / 60)
                            val endMinute = newEndMinutes % 60

                            // 保存分钟数，用于后续更新
                            calculatedStartMinutes = newStartHour * 60 + newStartMinute
                            calculatedEndMinutes = endHour * 60 + endMinute

                            // 格式化时间字符串
                            val startTimeStr = String.format("%02d:%02d", newStartHour, newStartMinute)
                            val endTimeStr = String.format("%02d:%02d", endHour, endMinute)
                            previewTimeRange = "$startTimeStr - $endTimeStr"
                        } else {
                            previewDate = null
                            previewTimeRange = null
                        }
                    }
                )
            },
        colors = CardDefaults.cardColors(
            containerColor = if (task.isImportant)
                if (isDragging) Color(0xFFBBE0BB) else Color(0xFFE8F5E9)
            else
                if (isDragging) Color(0xFFB3E0FF) else Color(0xFFE3F2FD)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isDragging) 8.dp else 2.dp
        )
    ) {
        // 卡片内容
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

            // 完成状态背景
            if (task.isFinished) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.LightGray.copy(alpha = 0.3f))
                )
            }

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
                    overflow = TextOverflow.Ellipsis,
                    color = if (task.isFinished) Color.Gray else Color.Black
                )

                // 时间显示
                if (!isMinHeight) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isDragging && previewTimeRange != null)
                            previewTimeRange!! // 拖动时显示预览时间
                        else
                            task.timeRange,
                        fontSize = 12.sp,
                        color = if (isDragging) Color.DarkGray else Color.Gray,
                        fontWeight = if (isDragging) FontWeight.Bold else FontWeight.Normal
                    )

                    // 地点显示（如果有）
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

                    // 拖动时显示日期预览
                    if (isDragging && previewDate != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = SimpleDateFormat("MM月dd日", Locale.getDefault()).format(previewDate!!.time),
                            fontSize = 11.sp,
                            color = Color.DarkGray,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // 拖动状态指示器
            if (isDragging) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(8.dp)
                        .background(Color.Red, CircleShape)
                )
            }
        }
    }

    // 辅助函数：重置卡片状态
}

// 简化计算新日期索引的逻辑
private fun calculateNewDateIndex(
    offsetX: Float,
    dateIndex: Int,  // 使用传入的参数 dateIndex 代替 currentDateIndex
    leftPaddingPx: Float,
    columnWidth: Float,
    maxColumns: Int
): Int {
    // 使用卡片的中心点来确定新的列
    val totalX = offsetX + leftPaddingPx + (dateIndex * columnWidth) + (columnWidth / 2)
    val relativeX = totalX - leftPaddingPx
    return (relativeX / columnWidth).toInt().coerceIn(0, maxColumns - 1)
}

// 简化计算新时间的逻辑
private fun calculateNewTime(
    offsetY: Float,
    currentStartHour: Int,
    currentStartMinute: Int,
    hourHeightPx: Float,
    startHour: Int
): Pair<Int, Int> {
    // 当前时间在时间线上的绝对位置
    val currentMinutesFromStart = (currentStartHour - startHour) * 60 + currentStartMinute
    val currentY = (currentMinutesFromStart / 60f) * hourHeightPx

    // 新位置 = 当前位置 + 偏移
    val newY = currentY + offsetY

    // 转换回时间
    val totalMinutesFromStart = (newY / hourHeightPx * 60).toInt()

    // 取整到最接近的5分钟
    val snappedMinutes = max(0, (totalMinutesFromStart / 5) * 5)

    // 计算小时和分钟
    val newHour = startHour + (snappedMinutes / 60)
    val newMinute = snappedMinutes % 60

    return Pair(newHour, newMinute)
}

// 提取计算新位置的逻辑到一个单独的函数
private fun calculateNewPosition(
    offsetX: Float,
    offsetY: Float,
    currentDateIndex: Int,
    currentStartHour: Int,
    currentStartMinute: Int,
    leftPaddingPx: Float,
    columnWidth: Float,
    hourHeightPx: Float,
    startHour: Int,
    maxColumns: Int
): Triple<Int, Int, Int> {
    // 1. 计算新的日期索引（水平位置）
    val cardCenterX = leftPaddingPx + (currentDateIndex * columnWidth) + (columnWidth / 2) + offsetX
    val relativeX = cardCenterX - leftPaddingPx
    val newDateIndex = (relativeX / columnWidth).toInt().coerceIn(0, maxColumns - 1)

    // 2. 计算新的起始时间（垂直位置）
    // 当前时间在时间线上的绝对Y位置
    val currentMinutesFromStart = (currentStartHour - startHour) * 60 + currentStartMinute
    val currentAbsoluteY = (currentMinutesFromStart / 60f) * hourHeightPx

    // 应用偏移后的位置
    val finalAbsoluteY = currentAbsoluteY + offsetY

    // 转换回时间
    val newMinutesFromStart = (finalAbsoluteY / hourHeightPx * 60).toInt()
    val snappedMinutes = (newMinutesFromStart / 5) * 5 // 对齐到5分钟

    val newStartHour = startHour + (snappedMinutes / 60)
    val newStartMinute = snappedMinutes % 60

    return Triple(newDateIndex, newStartHour, newStartMinute)
}

// 解析任务时间信息
private fun parseTaskTime(timeRange: String, fallbackDuration: Int): Quintuple<Int, Int, Int, Int, Int> {
    try {
        val parts = timeRange.split(" - ")
        if (parts.size != 2) {
            // 无法解析时间格式，返回默认值
            return Quintuple(0, 0, 1, 0, fallbackDuration)
        }

        val startParts = parts[0].split(":")
        val endParts = parts[1].split(":")

        if (startParts.size != 2 || endParts.size != 2) {
            // 无法解析小时和分钟，返回默认值
            return Quintuple(0, 0, 1, 0, fallbackDuration)
        }

        val startHour = startParts[0].toInt()
        val startMinute = startParts[1].toInt()
        val endHour = endParts[0].toInt()
        val endMinute = endParts[1].toInt()

        // 计算真实持续时间（分钟）
        val startTotalMinutes = startHour * 60 + startMinute
        val endTotalMinutes = endHour * 60 + endMinute
        val duration = endTotalMinutes - startTotalMinutes

        // 如果计算出的持续时间无效，使用提供的备用值
        val finalDuration = if (duration > 0) duration else fallbackDuration

        return Quintuple(startHour, startMinute, endHour, endMinute, finalDuration)
    } catch (e: Exception) {
        // 出现异常，返回默认值
        return Quintuple(0, 0, 1, 0, fallbackDuration)
    }
}

// 5元组数据类，用于返回5个值
data class Quintuple<A, B, C, D, E>(val first: A, val second: B, val third: C, val fourth: D, val fifth: E)

// 计算拖动后的日期索引
private fun calculateDateIndex(currentX: Float, columnWidth: Float, maxColumns: Int): Int {
    val relativeX = currentX - 40f // 减去左侧时间栏的宽度
    return (relativeX / columnWidth).toInt().coerceIn(0, maxColumns - 1)
}

// 辅助函数
// 辅助函数
fun isSameDay(date1: Date?, date2: Date?): Boolean {
    if (date1 == null || date2 == null) return false

    val cal1 = Calendar.getInstance()
    val cal2 = Calendar.getInstance()
    cal1.time = date1
    cal2.time = date2

    val same = cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH) &&
            cal1.get(Calendar.DAY_OF_MONTH) == cal2.get(Calendar.DAY_OF_MONTH)

    if (same) {
        Log.d(DEBUG_TAG, "日期相同: ${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date1)} 和 ${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date2)}")
    }

    return same
}

fun getSpecialDay(date: Calendar): String? {
    // 这里可以添加农历节日或特殊日期的判断逻辑
    // 示例代码
    return when {
        date.get(Calendar.MONTH) == Calendar.JANUARY && date.get(Calendar.DAY_OF_MONTH) == 1 -> "元旦"
        date.get(Calendar.MONTH) == Calendar.FEBRUARY && date.get(Calendar.DAY_OF_MONTH) == 14 -> "情人节"
        // 假设参考图中的日期
        date.get(Calendar.MONTH) == Calendar.JANUARY && date.get(Calendar.DAY_OF_MONTH) == 28 -> "除夕"
        date.get(Calendar.MONTH) == Calendar.JANUARY && date.get(Calendar.DAY_OF_MONTH) == 29 -> "春节"
        else -> null
    }
}