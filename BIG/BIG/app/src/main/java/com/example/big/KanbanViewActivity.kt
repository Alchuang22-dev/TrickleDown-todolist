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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.times
import androidx.compose.ui.zIndex
import androidx.lifecycle.lifecycleScope
import com.example.big.models.TaskResponse
import com.example.big.utils.TaskManager
import com.example.big.utils.UserManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
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

@Composable
fun MultiDayTimelineView(dates: List<Calendar>, allTasks: List<Task>) {
    val startHour = 7  // 开始显示的小时
    val endHour = 23   // 结束显示的小时
    val scrollState = rememberScrollState()
    val fiveMinHeightDp = 6.dp  // 每5分钟的高度
    val hourHeightDp = fiveMinHeightDp * 12  // 每小时的总高度

    // 计算时间线总高度
    val timelineHeight = (endHour - startHour + 1) * hourHeightDp

    // 无时间任务（按日期分组）
    val noTimeTasksByDate = dates.associateWith { date ->
        allTasks.filter { task ->
            isSameDay(task.date, date.time) &&
                    (task.timeRange == "未设定时间" || task.durationMinutes == 0)
        }
    }

    // 有时间的任务（按日期分组）
    val timeTasksByDate = dates.associateWith { date ->
        allTasks.filter { task ->
            isSameDay(task.date, date.time) &&
                    task.timeRange != "未设定时间" &&
                    task.durationMinutes > 0
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        // 无时间任务区域
        if (noTimeTasksByDate.any { it.value.isNotEmpty() }) {
            NoTimeTasksSection(noTimeTasksByDate, dates)
        }

        // 时间线区域 - 使用绝对定位
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(timelineHeight)
        ) {
            // 绘制时间格线
            for (hour in startHour..endHour) {
                val hourOffset = (hour - startHour) * hourHeightDp

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
                val tasks = timeTasksByDate[date] ?: emptyList()

                tasks.forEach { task ->
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

                        // 使用绝对定位放置任务卡片
                        TaskCard(
                            task = task,
                            heightDp = taskHeight,
                            modifier = Modifier
                                .width(taskWidth)
                                .offset(x = leftPosition + 4.dp, y = topOffsetDp.dp)
                                .zIndex(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NoTimeTasksSection(tasksByDate: Map<Calendar, List<Task>>, dates: List<Calendar>) {
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
            dates.forEach { date ->
                val tasks = tasksByDate[date] ?: emptyList()

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        tasks.forEach { task ->
                            TaskCard(
                                task = task,
                                heightDp = 80.dp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        Divider(modifier = Modifier.padding(vertical = 8.dp))
    }
}

@Composable
fun TaskCard(task: Task, heightDp: Dp, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val minHeight = 50.dp

    // 修复：正确比较 Dp 类型值
    val actualHeight = if (heightDp.value < minHeight.value) minHeight else heightDp
    val isMinHeight = actualHeight.value <= 60f  // 使用 .value 获取 Float 值进行比较

    Card(
        modifier = modifier
            .height(actualHeight)
            .clickable {
                val intent = Intent(context, EditTaskActivity::class.java)
                intent.putExtra("task_id", task.id)
                context.startActivity(intent)
            },
        colors = CardDefaults.cardColors(
            containerColor = if (task.isImportant)
                Color(0xFFE8F5E9) else Color(0xFFE3F2FD)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        // Card 内容保持不变
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