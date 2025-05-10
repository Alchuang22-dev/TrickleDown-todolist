package com.example.big

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.min

class TodayTimelineAdapter(
    private val context: Context,
    private val tasks: List<Task>?,
    private val selectedDate: Date
) : RecyclerView.Adapter<TodayTimelineAdapter.TimelineViewHolder>() {

    private var listener: OnTaskClickListener? = null

    // Hours to display (from 7 AM to 23 PM)
    companion object {
        private const val START_HOUR = 0
        private const val END_HOUR = 23

        // Category column titles
        private val CATEGORIES = arrayOf("重要事项", "学习", "工作", "生活", "其他")
    }

    interface OnTaskClickListener {
        fun onTaskClick(task: Task)
    }

    fun setOnTaskClickListener(listener: OnTaskClickListener) {
        this.listener = listener
    }

    // Lambda版本的监听器设置，方便Kotlin调用
    fun setOnTaskClickListener(listener: (Task) -> Unit) {
        this.listener = object : OnTaskClickListener {
            override fun onTaskClick(task: Task) {
                listener(task)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TimelineViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_today_timeline_hour, parent, false)
        return TimelineViewHolder(view)
    }

    override fun onBindViewHolder(holder: TimelineViewHolder, position: Int) {
        val hour = position + START_HOUR
        holder.hourText.text = String.format(Locale.getDefault(), "%02d", hour)

        // Clear existing task views
        holder.clearTaskViews()

        val taskList = tasks ?: return

        // Find tasks for this hour
        val cal = Calendar.getInstance()
        cal.time = selectedDate
        cal.set(Calendar.HOUR_OF_DAY, hour)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        val hourStart = cal.time
        cal.add(Calendar.HOUR_OF_DAY, 1)
        val hourEnd = cal.time

        for (task in tasks) {
            // Parse task start and end time
            val timeParts = task.timeRange.split(" - ")
            val startTimeParts = timeParts[0].split(":")

            val taskStartCal = Calendar.getInstance()
            taskStartCal.time = task.date
            taskStartCal.set(Calendar.HOUR_OF_DAY, startTimeParts[0].toInt())
            taskStartCal.set(Calendar.MINUTE, startTimeParts[1].toInt())

            // Determine if task occurs during this hour
            val taskInThisHour = isSameDay(taskStartCal.time, selectedDate) &&
                    (taskStartCal.get(Calendar.HOUR_OF_DAY) == hour ||
                            (task.durationMinutes > 60 &&
                                    taskStartCal.get(Calendar.HOUR_OF_DAY) < hour &&
                                    taskStartCal.get(Calendar.HOUR_OF_DAY) + (task.durationMinutes / 60) > hour))

            if (taskInThisHour) {
                // Create task view
                val taskView = LayoutInflater.from(context).inflate(R.layout.item_today_task, null)
                val taskTitle = taskView.findViewById<TextView>(R.id.task_title)
                val taskTime = taskView.findViewById<TextView>(R.id.task_time)

                taskTitle.text = task.title
                taskTime.text = task.timeRange

                // Calculate top position based on start time
                var topMargin = 0
                if (taskStartCal.get(Calendar.HOUR_OF_DAY) == hour) {
                    topMargin = (taskStartCal.get(Calendar.MINUTE) *
                            context.resources.getDimension(R.dimen.minute_height)).toInt()
                }

                // Calculate height based on duration
                val height = (min(60, getTaskDurationInThisHour(task, hour)) *
                        context.resources.getDimension(R.dimen.minute_height)).toInt()

                // Set background based on importance
                if (task.isImportant) {
                    taskView.setBackgroundResource(R.drawable.important_task_background)
                    holder.addTaskView(taskView, 0, topMargin, height)
                } else {
                    taskView.setBackgroundResource(R.drawable.normal_task_background)
                    // Find first available column
                    val column = holder.findAvailableColumn(topMargin, height)
                    holder.addTaskView(taskView, column, topMargin, height)
                }

                // Set click listener
                taskView.setOnClickListener {
                    listener?.onTaskClick(task)
                }
            }
        }
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

    private fun getTaskDurationInThisHour(task: Task, hour: Int): Int {
        // Parse task start time
        val timeParts = task.timeRange.split(" - ")
        val startTimeParts = timeParts[0].split(":")

        val taskStartCal = Calendar.getInstance()
        taskStartCal.time = task.date
        taskStartCal.set(Calendar.HOUR_OF_DAY, startTimeParts[0].toInt())
        taskStartCal.set(Calendar.MINUTE, startTimeParts[1].toInt())

        return when {
            taskStartCal.get(Calendar.HOUR_OF_DAY) == hour -> {
                // Task starts in this hour
                val minutesLeft = 60 - taskStartCal.get(Calendar.MINUTE)
                min(minutesLeft, task.durationMinutes)
            }
            taskStartCal.get(Calendar.HOUR_OF_DAY) < hour -> {
                // Task started in previous hour
                val hoursElapsed = hour - taskStartCal.get(Calendar.HOUR_OF_DAY)
                val minutesElapsed = hoursElapsed * 60 - taskStartCal.get(Calendar.MINUTE)
                val minutesLeft = task.durationMinutes - minutesElapsed
                min(60, minutesLeft)
            }
            else -> 0
        }
    }

    class TimelineViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val hourText: TextView = itemView.findViewById(R.id.hour_text)
        val taskColumns: Array<ViewGroup>
        val timeSlotOccupied: Array<BooleanArray> // [column][minute]

        init {
            // 初始化列数组
            taskColumns = arrayOf(
                itemView.findViewById(R.id.important_column),
                itemView.findViewById(R.id.category1_column),
                itemView.findViewById(R.id.category2_column),
                itemView.findViewById(R.id.category3_column),
                itemView.findViewById(R.id.category4_column)
            )

            // 初始化时间段跟踪（5列，每小时60分钟）
            timeSlotOccupied = Array(5) { BooleanArray(60) }
        }

        fun clearTaskViews() {
            for (column in taskColumns) {
                column.removeAllViews()
            }

            // Reset time slot tracking
            for (i in 0 until 5) {
                for (j in 0 until 60) {
                    timeSlotOccupied[i][j] = false
                }
            }
        }

        fun findAvailableColumn(startMinute: Int, height: Int): Int {
            val endMinute = min(59, startMinute + height / itemView.context.resources.getDimension(R.dimen.minute_height).toInt())

            // Skip column 0 (important items column)
            for (col in 1 until 5) {
                var available = true
                for (minute in startMinute..endMinute) {
                    if (minute < 60 && timeSlotOccupied[col][minute]) {
                        available = false
                        break
                    }
                }

                if (available) {
                    // Mark time slots as occupied
                    for (minute in startMinute..endMinute) {
                        if (minute < 60) {
                            timeSlotOccupied[col][minute] = true
                        }
                    }
                    return col
                }
            }

            // If no column is available, use the last one
            return 4
        }

        fun addTaskView(taskView: View, column: Int, topMargin: Int, height: Int) {
            val params = ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, height).apply {
                this.topMargin = topMargin
                leftMargin = 2
                rightMargin = 2
            }

            taskView.layoutParams = params
            taskColumns[column].addView(taskView)
        }
    }
}