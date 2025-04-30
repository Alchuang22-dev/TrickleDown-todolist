package com.example.big

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import java.util.Calendar
import java.util.Date
import java.util.HashMap
import kotlin.math.ceil
import kotlin.math.min
import kotlin.math.round

class KanbanTimelineAdapter(
    private val context: Context,
    private val tasks: List<Task>,
    private val currentDate: Date
) : RecyclerView.Adapter<KanbanTimelineAdapter.TimelineViewHolder>() {

    private var listener: OnTaskClickListener? = null
    private val today: Calendar = Calendar.getInstance().apply {
        time = Date()
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    private val numDays = 5 // 要显示的天数

    // 小时范围（7 AM 到 23 PM）
    private companion object {
        const val START_HOUR = 7
        const val END_HOUR = 23

        // 每5分钟的高度（dp）
        const val FIVE_MIN_HEIGHT_DP = 6.0f

        // 进一步增加最小任务高度以确保文字完全显示
        const val MIN_TASK_HEIGHT_DP = 50.0f // 修改1: 从40dp增加到50dp
    }

    // 存储处理过的任务ID，避免在同一个任务中重复显示内容
    private val processedTaskIds = HashMap<Int, Boolean>()

    interface OnTaskClickListener {
        fun onTaskClick(task: Task)
    }

    fun setOnTaskClickListener(listener: OnTaskClickListener) {
        this.listener = listener
    }

    // 为了兼容Java调用，提供lambda版本
    fun setOnTaskClickListener(listener: (Task) -> Unit) {
        this.listener = object : OnTaskClickListener {
            override fun onTaskClick(task: Task) {
                listener(task)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TimelineViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_timeline_hour_multi, parent, false)
        return TimelineViewHolder(view, numDays)
    }

    override fun onBindViewHolder(holder: TimelineViewHolder, position: Int) {
        val hour = position + START_HOUR
        holder.hourText.text = String.format("%02d", hour)

        // 清除现有任务视图
        holder.clearTaskViews()

        // 修改2: 每个小时重置处理过的任务ID
        processedTaskIds.clear()

        // 获取当前日期范围的起始日期
        val startDateCal = Calendar.getInstance().apply {
            time = currentDate
            add(Calendar.DAY_OF_MONTH, -2) // 从当前日期前2天开始
        }

        // 遍历日期列
        for (dayIndex in 0 until numDays) {
            val dateCal = startDateCal.clone() as Calendar
            dateCal.add(Calendar.DAY_OF_MONTH, dayIndex)

            // 设置为当天的特定小时
            dateCal.set(Calendar.HOUR_OF_DAY, hour)
            dateCal.set(Calendar.MINUTE, 0)
            dateCal.set(Calendar.SECOND, 0)
            dateCal.set(Calendar.MILLISECOND, 0)

            // 查找此日期和小时的任务
            for (task in tasks) {
                // 解析任务开始和结束时间
                if (!isSameDay(task.date, dateCal.time)) {
                    continue
                }

                // 解析任务时间范围
                val timeParts = task.timeRange.split(" - ")
                if (timeParts.size < 2) continue

                val startTimeParts = timeParts[0].split(":")
                if (startTimeParts.size < 2) continue

                val taskStartHour = startTimeParts[0].toInt()
                val taskStartMinute = startTimeParts[1].toInt()

                // 判断任务是否在此小时内发生
                if (taskStartHour == hour ||
                    (task.durationMinutes > 0 &&
                            taskStartHour < hour &&
                            taskStartHour + ((taskStartMinute + task.durationMinutes) / 60) > hour)) {

                    // 创建任务视图，处理跨小时任务
                    addTaskViewToHolder(holder, task, hour, dayIndex, taskStartHour)
                }
            }
        }
    }

    private fun addTaskViewToHolder(
        holder: TimelineViewHolder,
        task: Task,
        hour: Int,
        dayIndex: Int,
        taskStartHour: Int
    ) {
        // 修改3: 检查这个任务在这一天是否已经处理过，避免重复显示
        val processed = true

        // 如果不是任务开始的小时，且已经处理过这个任务，就跳过（避免重复）
        if (taskStartHour != hour && processed != null && processed) {
            return
        }

        // 标记这个任务ID已经处理过
        // processedTaskIds[task.id] = true

        val taskView = LayoutInflater.from(context).inflate(R.layout.item_task, null)
        val taskTitle: TextView = taskView.findViewById(R.id.task_title)
        val taskTime: TextView = taskView.findViewById(R.id.task_time)
        val taskCard: CardView = taskView.findViewById(R.id.task_card)

        taskTitle.text = task.title

        // 解析任务开始和结束时间
        val timeParts = task.timeRange.split(" - ")
        val startTime = timeParts[0]
        val endTime = if (timeParts.size > 1) timeParts[1] else ""

        // 解析具体时间
        val startTimeParts = startTime.split(":")
        val taskStartMinute = startTimeParts[1].toInt()

        val endTimeParts = endTime.split(":")
        val taskEndHour = endTimeParts[0].toInt()
        val taskEndMinute = endTimeParts[1].toInt()

        // 计算top margin（如果任务在此小时开始）
        var topMarginPx = 0
        if (taskStartHour == hour) {
            // 根据5分钟精度计算top margin
            val minuteBlocks = taskStartMinute / 5
            val fiveMinHeightPx = dpToPx(FIVE_MIN_HEIGHT_DP)
            topMarginPx = round(minuteBlocks * fiveMinHeightPx).toInt()
        }

        // 计算此小时内的任务高度
        val taskDurationInThisHour: Int = when {
            // 修改4: 特殊处理跨小时的边界情况
            // 如果任务在当前小时结束，使用结束分钟计算
            taskEndHour == hour -> {
                taskEndMinute
            }
            taskStartHour == hour -> {
                // 任务在此小时开始
                val minutesLeft = 60 - taskStartMinute
                min(minutesLeft, task.durationMinutes)
            }
            else -> {
                // 任务从前一个小时延续到整个当前小时
                60
            }
        }

        // 根据5分钟精度计算高度
        val heightBlocks = ceil(taskDurationInThisHour / 5.0).toInt()
        var heightPx = round(heightBlocks * dpToPx(FIVE_MIN_HEIGHT_DP)).toInt()

        // 确保任务视图至少有最小高度
        val minHeightPx = round(dpToPx(MIN_TASK_HEIGHT_DP)).toInt()
        if (heightPx < minHeightPx) {
            heightPx = minHeightPx
        }

        // 修改5: 根据任务高度决定是否显示时间
        if (heightPx < minHeightPx * 1.2) { // 如果高度接近最小高度，只显示标题
            taskTitle.maxLines = 2 // 允许标题最多两行
            taskTime.visibility = View.GONE // 隐藏时间文本
        } else {
            taskTime.text = task.timeRange
            taskTime.visibility = View.VISIBLE
        }

        // 设置更明显的背景颜色和样式
        if (task.isImportant) {
            taskCard.setCardBackgroundColor(ContextCompat.getColor(context, R.color.important_task_bg))
            val importanceIndicator = taskView.findViewById<View>(R.id.importance_indicator)
            importanceIndicator?.apply {
                setBackgroundColor(ContextCompat.getColor(context, R.color.important_task_indicator))
                visibility = View.VISIBLE
            }
        } else {
            taskCard.setCardBackgroundColor(ContextCompat.getColor(context, R.color.normal_task_bg))
            val importanceIndicator = taskView.findViewById<View>(R.id.importance_indicator)
            importanceIndicator?.apply {
                setBackgroundColor(ContextCompat.getColor(context, R.color.normal_task_indicator))
                visibility = View.VISIBLE
            }
        }

        // 增加卡片阴影以提高视觉区分度
        taskCard.cardElevation = 4f

        // 修改6: 调整任务结束时间，使其与小时网格对齐
        // 如果任务结束时间是整点或半点，调整高度确保视觉上对齐
        if (taskEndHour == hour + 1 && taskEndMinute == 0) {
            // 任务在下一个整点结束，确保视觉上对齐到小时线
            heightPx = 60 * round(dpToPx(FIVE_MIN_HEIGHT_DP) / 5).toInt()
        } else if (taskEndHour == hour && (taskEndMinute == 0 || taskEndMinute == 30)) {
            // 任务在当前小时的整点或半点结束，调整以对齐
            val exactHeightPx = round((taskEndMinute / 5) * dpToPx(FIVE_MIN_HEIGHT_DP)).toInt()
            if (exactHeightPx > minHeightPx) {
                heightPx = exactHeightPx
            }
        }

        // 添加任务视图到相应的日期列
        val layoutParams = ViewGroup.MarginLayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, heightPx
        ).apply {
            topMargin = topMarginPx
            leftMargin = 3
            rightMargin = 3
        }
        taskView.layoutParams = layoutParams

        // 设置点击监听器
        taskView.setOnClickListener {
            listener?.onTaskClick(task)
        }

        holder.addTaskView(taskView, dayIndex, topMarginPx, heightPx)
    }

    override fun getItemCount(): Int {
        return END_HOUR - START_HOUR + 1
    }

    private fun isSameDay(date1: Date, date2: Date): Boolean {
        val cal1 = Calendar.getInstance()
        val cal2 = Calendar.getInstance()
        cal1.time = date1
        cal2.time = date2
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    private fun dpToPx(dp: Float): Float {
        return dp * context.resources.displayMetrics.density
    }

    class TimelineViewHolder(itemView: View, numDays: Int) : RecyclerView.ViewHolder(itemView) {
        val hourText: TextView = itemView.findViewById(R.id.hour_text)
        val dayColumns: Array<FrameLayout>
        val timeSlotOccupied: Array<BooleanArray> // [day][minute/5]

        init {
            // 初始化日期列
            dayColumns = Array(numDays) { i ->
                val resId = itemView.resources.getIdentifier(
                    "day_column_$i", "id", itemView.context.packageName
                )
                itemView.findViewById(resId)
            }

            // 初始化时间段跟踪（numDays列，每小时12个5分钟块）
            timeSlotOccupied = Array(numDays) { BooleanArray(12) }
        }

        fun clearTaskViews() {
            for (column in dayColumns) {
                column.removeAllViews()
            }

            // 重置时间段跟踪
            for (i in dayColumns.indices) {
                for (j in 0 until 12) {
                    timeSlotOccupied[i][j] = false
                }
            }
        }

        fun addTaskView(taskView: View, dayIndex: Int, topMargin: Int, height: Int) {
            if (dayIndex >= 0 && dayIndex < dayColumns.size) {
                dayColumns[dayIndex].addView(taskView)
            }
        }
    }
}