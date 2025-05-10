package com.example.big

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
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
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class TodayViewComposeActivity : ComponentActivity() {
    private var selectedDate: Date? = null
    private val TAG = "TodayViewComposeActivity"

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
                        coroutineScope = lifecycleScope
                    )
                }
            }
        }
    }
}

@Composable
fun TodayViewScreen(selectedDate: Date, coroutineScope: kotlinx.coroutines.CoroutineScope) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // 状态管理
    var tasks by remember { mutableStateOf<List<Task>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // 无时间任务区域高度
    var noTimeTaskAreaHeight by remember { mutableStateOf(0.dp) }

    // 加载任务数据
    LaunchedEffect(selectedDate) {
        loadTasksForDate(selectedDate, coroutineScope) { result ->
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

            // 错误状态
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

            // 如果没有任务
            if (tasks.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "今日没有任务",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                return@Box
            }

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
                                .padding(start = 40.dp, end = 8.dp)
                        )
                    }.map { it.measure(constraints) }
                } else {
                    emptyList()
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
                        modifier = Modifier.fillMaxWidth()
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
                    // 将TaskResponse转换为UI需要的Task对象
                    val tasks = convertTaskResponsesToTasks(response.data)
                    callback(TaskLoadResult.Success(tasks))
                }
                is TaskManager.Result.Error -> {
                    Log.e("TodayView", "获取任务失败: ${response.message}")
                    callback(TaskLoadResult.Error("获取任务失败: ${response.message}"))
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
    onTaskMoved: ((Task, Calendar, Int, Int) -> Unit)? = null
) {
    val context = LocalContext.current
    val minHeight = 50.dp

    // 当前高度
    val actualHeight = if (heightDp.value < minHeight.value) minHeight else heightDp
    val isMinHeight = actualHeight.value <= 60f

    // 拖拽状态
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }

    // 本地密度换算
    val density = LocalDensity.current

    // 用于记录任务的原始位置信息，拖拽取消时恢复位置
    var originalX by remember { mutableStateOf(0f) }
    var originalY by remember { mutableStateOf(0f) }

    // 时间参数
    val hourHeightDp = 72.dp
    val startHour = 0

    // 获取当前可见的日期
    val currentDate = Calendar.getInstance()
    val visibleDates = listOf(
        (currentDate.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, -1) },
        currentDate.clone() as Calendar,
        (currentDate.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, 1) }
    )

    Card(
        modifier = modifier
            .height(actualHeight)
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {
                        isDragging = true
                        originalX = offsetX
                        originalY = offsetY
                    },
                    onDragEnd = {
                        isDragging = false

                        // 防止误触，如果拖动距离太小，恢复原始位置
                        if (abs(offsetX - originalX) < 20 && abs(offsetY - originalY) < 20) {
                            offsetX = originalX
                            offsetY = originalY

                            // 点击事件处理
                            val intent = Intent(context, EditTaskActivity::class.java)
                            intent.putExtra("task_id", task.id)
                            context.startActivity(intent)
                            return@detectDragGestures
                        }

                        // 简化实现：直接根据当前偏移计算新位置
                        val screenWidth = context.resources.displayMetrics.widthPixels
                        val leftPadding = with(density) { 40.dp.toPx() }
                        val availableWidth = screenWidth - leftPadding
                        val columnWidth = availableWidth / 3

                        // 确定在哪一列（第几天）
                        val dateIndex = ((offsetX + leftPadding) / columnWidth).toInt().coerceIn(0, 2)

                        // 计算开始时间
                        val hourHeightPx = with(density) { hourHeightDp.toPx() }
                        val totalMinutesFromStart = ((offsetY / hourHeightPx) * 60).toInt()
                        val newStartHour = startHour + (totalMinutesFromStart / 60)
                        val newStartMinute = (totalMinutesFromStart % 60) / 5 * 5 // 向下取整到5分钟

                        // 保持任务原有持续时间
                        val taskDurationMinutes = task.durationMinutes

                        // 计算新的结束时间
                        val totalEndMinutes = totalMinutesFromStart + taskDurationMinutes
                        val newEndHour = startHour + (totalEndMinutes / 60)
                        val newEndMinute = (totalEndMinutes % 60) / 5 * 5

                        // 获取目标日期
                        val targetDate = visibleDates[dateIndex]

                        // 调用回调更新任务
                        onTaskMoved?.invoke(task, targetDate,
                            newStartHour * 60 + newStartMinute,
                            newEndHour * 60 + newEndMinute)
                    },
                    onDragCancel = {
                        // 取消拖拽，恢复原始位置
                        isDragging = false
                        offsetX = originalX
                        offsetY = originalY
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        offsetX += dragAmount.x
                        offsetY += dragAmount.y
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
fun NoTimeTasksArea(tasks: List<Task>, modifier: Modifier = Modifier) {
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
                    .padding(vertical = 2.dp)
            )
        }

        // 再显示普通任务
        normalTasks.forEach { task ->
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

@Composable
fun TasksTimeline(tasks: List<Task>, startHour: Int, endHour: Int, modifier: Modifier = Modifier) {
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

            // 使用TaskCard显示任务
            TaskCard(
                task = task,
                heightDp = taskHeight,
                modifier = Modifier
                    .padding(start = 40.dp + (column * 70).dp, top = topOffset)
                    .width(65.dp)
                    .zIndex(if (task.isImportant) 10f else 5f)
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
