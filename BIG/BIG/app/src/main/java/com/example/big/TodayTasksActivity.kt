package com.example.big

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.NumberPicker
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.big.models.TaskResponse
import com.example.big.utils.TaskManager
import com.example.big.utils.UserManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class TodayTasksActivity : AppCompatActivity() {
    private var importantTasksRecyclerView: RecyclerView? = null
    private var otherTasksRecyclerView: RecyclerView? = null
    private var completedTasksRecyclerView: RecyclerView? = null

    private var allTasks: MutableList<Task>? = null
    private var originalTasks: MutableList<Task>? = null // 存储原始任务列表，用于筛选恢复
    private var importantTasks: MutableList<Task>? = null
    private var otherTasks: MutableList<Task>? = null
    private var completedTasks: MutableList<Task>? = null

    private var importantTasksAdapter: TaskAdapter? = null
    private var otherTasksAdapter: TaskAdapter? = null
    private var completedTasksAdapter: TaskAdapter? = null

    private val TAG = "TodayTasksActivity"
    private var isLoading = false

    // 时间筛选对话框中的控件
    private var yearPicker: NumberPicker? = null
    private var monthPicker: NumberPicker? = null
    private var dayPicker: NumberPicker? = null
    private var startHourPicker: NumberPicker? = null
    private var startMinutePicker: NumberPicker? = null
    private var endHourPicker: NumberPicker? = null
    private var endMinutePicker: NumberPicker? = null

    // 日历辅助变量
    private val daysInMonth = intArrayOf(31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
    private var selectedYear = 0
    private var selectedMonth = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_today_tasks)

        // 初始化视图
        initViews()

        // 设置返回按钮
        val backButton = findViewById<ImageButton>(R.id.back_button)
        backButton.setOnClickListener { finish() }

        // 设置菜单按钮
        val menuButton = findViewById<ImageButton>(R.id.menu_button)
        menuButton.setOnClickListener { view: View ->
            showFilterMenu(view)
        }

        // 设置添加任务按钮
        val addTaskButton = findViewById<FloatingActionButton>(R.id.add_task_button)
        addTaskButton.setOnClickListener {
            val intent = Intent(
                this@TodayTasksActivity,
                AddTaskActivity::class.java
            )
            startActivity(intent)
        }

        // 初始化空的任务列表
        allTasks = ArrayList()
        originalTasks = ArrayList()
        importantTasks = ArrayList()
        otherTasks = ArrayList()
        completedTasks = ArrayList()

        // 设置任务列表适配器
        setupTaskLists()

        // 加载任务数据
        loadTaskData()
    }

    private fun initViews() {
        importantTasksRecyclerView = findViewById(R.id.important_tasks_recyclerview)
        otherTasksRecyclerView = findViewById(R.id.other_tasks_recyclerview)
        completedTasksRecyclerView = findViewById(R.id.completed_tasks_recyclerview)

        importantTasksRecyclerView?.layoutManager = LinearLayoutManager(this)
        otherTasksRecyclerView?.layoutManager = LinearLayoutManager(this)
        completedTasksRecyclerView?.layoutManager = LinearLayoutManager(this)
    }

    private fun showFilterMenu(view: View) {
        val popup = PopupMenu(this, view)
        popup.menuInflater.inflate(R.menu.filter_menu, popup.menu)

        popup.setOnMenuItemClickListener { item: MenuItem ->
            val itemId = item.itemId
            if (itemId == R.id.menu_unfinished) {
                filterUnfinishedTasks()
                return@setOnMenuItemClickListener true
            } else if (itemId == R.id.menu_important) {
                filterImportantTasks()
                return@setOnMenuItemClickListener true
            } else if (itemId == R.id.menu_by_time) {
                showTimeFilterDialog()
                return@setOnMenuItemClickListener true
            } else if (itemId == R.id.menu_by_category) {
                showCategoryFilterDialog()
                return@setOnMenuItemClickListener true
            } else if (itemId == R.id.menu_all) {
                filterNone()
                return@setOnMenuItemClickListener true
            }
            false
        }

        popup.show()
    }

    private fun loadTaskData() {
        showLoading()

        // 使用协程从API获取今日任务
        lifecycleScope.launch {
            try {
                val userId = UserManager.getUserId()
                if (userId.isNullOrEmpty()) {
                    showError("用户未登录")
                    hideLoading()
                    return@launch
                }

                // 获取今天的日期，格式化为API需要的格式 (yyyy-MM-dd)
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val todayDate = dateFormat.format(Date())

                // 调用API获取今日任务
                val response = TaskManager.getTasksByDate(userId, todayDate)

                when (response) {
                    is TaskManager.Result.Success -> {
                        val taskResponses = response.data
                        Log.d(TAG, "获取到 ${taskResponses.size} 个任务")

                        // 将TaskResponse转换为Task对象
                        allTasks = convertTaskResponsesToTasks(taskResponses)
                        originalTasks = ArrayList(allTasks!!)

                        // 分类并更新UI
                        categorizeTasksForDisplay()

                        runOnUiThread {
                            updateTaskLists()
                            hideLoading()
                        }
                    }
                    is TaskManager.Result.Error -> {
                        Log.e(TAG, "获取任务失败: ${response.message}")
                        showError("获取任务失败: ${response.message}")
                        hideLoading()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "获取任务时出现异常", e)
                showError("获取任务时出现异常: ${e.message}")
                hideLoading()
            }
        }
    }

    private fun convertTaskResponsesToTasks(taskResponses: List<TaskResponse>): MutableList<Task> {
        val tasks: MutableList<Task> = ArrayList()

        for (taskResponse in taskResponses) {
            val task = Task(
                id = taskResponse.id,
                title = taskResponse.title,
                timeRange = taskResponse.time_range ?: "未设定时间",
                date = taskResponse.date,
                durationMinutes = taskResponse.duration_minutes,
                important = taskResponse.is_important,
                description = taskResponse.description
            )

            // 设置额外属性
            task.isFinished = taskResponse.is_finished
            task.isDelayed = taskResponse.is_delayed
            task.place = taskResponse.place
            task.category = taskResponse.category

            tasks.add(task)
        }

        // 按时间排序
        sortTasksByTime(tasks)

        return tasks
    }

    private fun sortTasksByTime(tasks: MutableList<Task>) {
        tasks.sortWith { t1, t2 ->
            try {
                if (t1.timeRange == "未设定时间" && t2.timeRange == "未设定时间") {
                    return@sortWith 0
                } else if (t1.timeRange == "未设定时间") {
                    return@sortWith 1 // 未设定时间的放在后面
                } else if (t2.timeRange == "未设定时间") {
                    return@sortWith -1 // 未设定时间的放在后面
                }

                // 检查分隔符，可能是 "--" 或 "-"
                val delimiter1 = if (t1.timeRange.contains("--")) "--" else "-"
                val delimiter2 = if (t2.timeRange.contains("--")) "--" else "-"

                // 分割时间范围
                val startTimeStr1 = t1.timeRange.split(delimiter1)[0].trim()
                val startTimeStr2 = t2.timeRange.split(delimiter2)[0].trim()

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
                Log.e(TAG, "解析任务时间失败: ${e.message} for timeRange1: ${t1.timeRange}, timeRange2: ${t2.timeRange}", e)
                0  // 无法比较时返回0
            }
        }
    }

    private fun showLoading() {
        isLoading = true
        // 在实际应用中，这里可以显示进度条
    }

    private fun hideLoading() {
        isLoading = false
        // 在实际应用中，这里可以隐藏进度条
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun categorizeTasksForDisplay() {
        importantTasks?.clear()
        otherTasks?.clear()
        completedTasks?.clear()

        for (task in allTasks!!) {
            if (task.isFinished) {
                completedTasks?.add(task)
            } else if (task.isImportant) {
                importantTasks?.add(task)
            } else {
                otherTasks?.add(task)
            }
        }
    }

    private fun setupTaskLists() {
        importantTasksAdapter = TaskAdapter(importantTasks ?: emptyList(), this)
        otherTasksAdapter = TaskAdapter(otherTasks ?: emptyList(), this)
        completedTasksAdapter = TaskAdapter(completedTasks ?: emptyList(), this)

        importantTasksRecyclerView?.adapter = importantTasksAdapter
        otherTasksRecyclerView?.adapter = otherTasksAdapter
        completedTasksRecyclerView?.adapter = completedTasksAdapter
    }

    private fun updateTaskLists() {
        // 更新适配器数据
        importantTasksAdapter = TaskAdapter(importantTasks ?: emptyList(), this)
        otherTasksAdapter = TaskAdapter(otherTasks ?: emptyList(), this)
        completedTasksAdapter = TaskAdapter(completedTasks ?: emptyList(), this)

        // 设置到RecyclerView
        importantTasksRecyclerView?.adapter = importantTasksAdapter
        otherTasksRecyclerView?.adapter = otherTasksAdapter
        completedTasksRecyclerView?.adapter = completedTasksAdapter
    }

    // 筛选未完成的任务
    private fun filterUnfinishedTasks() {
        val unfinishedTasks: MutableList<Task> = ArrayList()
        for (task in originalTasks!!) {
            if (!task.isFinished) {
                unfinishedTasks.add(task)
            }
        }
        updateAllTaskLists(unfinishedTasks)
    }

    // 筛选重要任务
    private fun filterImportantTasks() {
        val filteredTasks: MutableList<Task> = ArrayList()
        for (task in originalTasks!!) {
            if (task.isImportant) {
                filteredTasks.add(task)
            }
        }
        updateAllTaskLists(filteredTasks)
    }

    // 按时间筛选
    private fun showTimeFilterDialog() {
        // 创建对话框
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_time_filter)

        // 初始化控件
        yearPicker = dialog.findViewById(R.id.year_picker)
        monthPicker = dialog.findViewById(R.id.month_picker)
        dayPicker = dialog.findViewById(R.id.day_picker)
        startHourPicker = dialog.findViewById(R.id.start_hour_picker)
        startMinutePicker = dialog.findViewById(R.id.start_minute_picker)
        endHourPicker = dialog.findViewById(R.id.end_hour_picker)
        endMinutePicker = dialog.findViewById(R.id.end_minute_picker)
        val resetButton = dialog.findViewById<Button>(R.id.reset_button)
        val confirmButton = dialog.findViewById<Button>(R.id.confirm_button)

        // 设置日期选择器
        setupDatePickers()

        // 设置时间选择器
        setupTimePickers()

        // 设置重置按钮
        resetButton.setOnClickListener { resetTimeFilters() }

        // 设置确认按钮
        confirmButton.setOnClickListener {
            filterTasksByTime()
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun setupDatePickers() {
        // 获取当前日期
        val calendar = Calendar.getInstance()
        selectedYear = calendar[Calendar.YEAR]
        selectedMonth = calendar[Calendar.MONTH]
        val day = calendar[Calendar.DAY_OF_MONTH]

        // 设置年份范围，从当前年份-10年到当前年份+10年
        yearPicker!!.minValue = selectedYear - 10
        yearPicker!!.maxValue = selectedYear + 10
        yearPicker!!.value = selectedYear

        // 设置月份范围，1-12月
        monthPicker!!.minValue = 1
        monthPicker!!.maxValue = 12
        monthPicker!!.value = selectedMonth + 1 // 月份从0开始，所以+1

        // 设置日期范围，根据年月确定
        updateDayPicker()
        dayPicker!!.value = day

        // 设置年月变化监听器，更新日期选择器
        yearPicker!!.setOnValueChangedListener { _, _, newVal ->
            selectedYear = newVal
            updateDayPicker()
        }

        monthPicker!!.setOnValueChangedListener { _, _, newVal ->
            selectedMonth = newVal - 1 // 月份从0开始，所以-1
            updateDayPicker()
        }
    }

    private fun updateDayPicker() {
        // 检查是否闰年
        val isLeapYear =
            (selectedYear % 4 == 0 && selectedYear % 100 != 0) || (selectedYear % 400 == 0)
        var daysInSelectedMonth = daysInMonth[selectedMonth]

        // 如果是2月且是闰年，则有29天
        if (selectedMonth == 1 && isLeapYear) {
            daysInSelectedMonth = 29
        }

        // 更新日期选择器
        dayPicker!!.minValue = 1
        dayPicker!!.maxValue = daysInSelectedMonth

        // 防止当前值超出范围
        if (dayPicker!!.value > daysInSelectedMonth) {
            dayPicker!!.value = daysInSelectedMonth
        }
    }

    private fun setupTimePickers() {
        // 设置小时选择器范围 0-23
        startHourPicker!!.minValue = 0
        startHourPicker!!.maxValue = 23
        endHourPicker!!.minValue = 0
        endHourPicker!!.maxValue = 23

        // 设置分钟选择器
        val minutes =
            arrayOf("00", "05", "10", "15", "20", "25", "30", "35", "40", "45", "50", "55")
        startMinutePicker!!.minValue = 0
        startMinutePicker!!.maxValue = minutes.size - 1
        startMinutePicker!!.displayedValues = minutes

        endMinutePicker!!.minValue = 0
        endMinutePicker!!.maxValue = minutes.size - 1
        endMinutePicker!!.displayedValues = minutes

        // 设置结束时间的监听器，确保结束时间晚于开始时间
        endHourPicker!!.setOnValueChangedListener { _, _, _ -> validateEndTime() }
        endMinutePicker!!.setOnValueChangedListener { _, _, _ -> validateEndTime() }
        startHourPicker!!.setOnValueChangedListener { _, _, _ -> validateEndTime() }
        startMinutePicker!!.setOnValueChangedListener { _, _, _ -> validateEndTime() }
    }

    private fun validateEndTime() {
        val startHour = startHourPicker!!.value
        val startMinuteIndex = startMinutePicker!!.value
        val startMinute = startMinuteIndex * 5 // 分钟间隔为5

        val endHour = endHourPicker!!.value
        val endMinuteIndex = endMinutePicker!!.value
        val endMinute = endMinuteIndex * 5 // 分钟间隔为5

        // 如果结束时间早于开始时间，调整结束时间
        if (endHour < startHour || (endHour == startHour && endMinute <= startMinute)) {
            if (startMinuteIndex < 11) { // 11是最后一个索引（55分钟）
                endHourPicker!!.value = startHour
                endMinutePicker!!.value = startMinuteIndex + 1
            } else {
                endHourPicker!!.value = startHour + 1
                endMinutePicker!!.value = 0
            }
            Toast.makeText(this, "结束时间必须晚于开始时间", Toast.LENGTH_SHORT).show()
        }
    }

    private fun resetTimeFilters() {
        // 重置日期为当前日期
        val calendar = Calendar.getInstance()
        yearPicker!!.value = calendar[Calendar.YEAR]
        monthPicker!!.value = calendar[Calendar.MONTH] + 1
        dayPicker!!.value = calendar[Calendar.DAY_OF_MONTH]

        // 重置时间
        startHourPicker!!.value = 9 // 默认早上9点
        startMinutePicker!!.value = 0 // 默认0分
        endHourPicker!!.value = 10 // 默认上午10点
        endMinutePicker!!.value = 0 // 默认0分
    }

    private fun filterTasksByTime() {
        // 获取筛选条件
        val year = yearPicker!!.value
        val month = monthPicker!!.value - 1 // 月份从0开始
        val day = dayPicker!!.value

        val startHour = startHourPicker!!.value
        val startMinuteIndex = startMinutePicker!!.value
        val startMinute = startMinuteIndex * 5

        val endHour = endHourPicker!!.value
        val endMinuteIndex = endMinutePicker!!.value
        val endMinute = endMinuteIndex * 5

        // 格式化时间范围
        @SuppressLint("DefaultLocale") val startTimeStr =
            String.format("%02d : %02d", startHour, startMinute)
        @SuppressLint("DefaultLocale") val endTimeStr =
            String.format("%02d : %02d", endHour, endMinute)
        val timeRange = "$startTimeStr -- $endTimeStr"

        // 创建日期对象
        val cal = Calendar.getInstance()
        cal[year, month, day, 0, 0] = 0
        cal[Calendar.MILLISECOND] = 0
        val filterDate = cal.time

        // 筛选任务
        val filteredTasks: MutableList<Task> = ArrayList()
        for (task in originalTasks!!) {
            // 检查日期匹配
            var dateMatches = true
            if (task.date != null) {
                val taskCal = Calendar.getInstance()
                taskCal.time = task.date

                val taskYear = taskCal[Calendar.YEAR]
                val taskMonth = taskCal[Calendar.MONTH]
                val taskDay = taskCal[Calendar.DAY_OF_MONTH]

                dateMatches = (taskYear == year && taskMonth == month && taskDay == day)
            }

            // 检查时间范围匹配
            var timeMatches = true
            if (task.timeRange != null && task.timeRange.isNotEmpty() && task.timeRange != "未设定时间") {
                try {
                    // 检查分隔符，可能是 "--" 或 "-"
                    val delimiter = if (task.timeRange.contains("--")) "--" else "-"

                    // 分割时间范围
                    val parts = task.timeRange.split(delimiter).map { it.trim() }

                    // 提取小时和分钟
                    val taskStartTimeStr = parts[0]
                    val taskEndTimeStr = parts[1]

                    // 时间重叠检查
                    val isOverlap =
                        !(taskEndTimeStr.compareTo(startTimeStr) < 0 || taskStartTimeStr.compareTo(endTimeStr) > 0)
                    timeMatches = isOverlap
                } catch (e: Exception) {
                    Log.e(TAG, "解析时间范围失败: ${e.message} for timeRange: ${task.timeRange}", e)
                    timeMatches = false
                }
            }

            // 只有两个条件都匹配才添加到结果中
            if (dateMatches && timeMatches) {
                filteredTasks.add(task)
            }
        }

        // 更新列表
        updateAllTaskLists(filteredTasks)

        // 反馈筛选结果
        if (filteredTasks.isEmpty()) {
            Toast.makeText(this, "没有找到符合条件的事项", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(
                this,
                "找到 " + filteredTasks.size + " 个符合条件的事项",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // 按类别筛选对话框
    private fun showCategoryFilterDialog() {
        // 创建对话框
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_category_filter)

        // 初始化控件
        val categoryEditText = dialog.findViewById<EditText>(R.id.edit_category)
        val studyButton = dialog.findViewById<Button>(R.id.button_study)
        val workButton = dialog.findViewById<Button>(R.id.button_work)
        val lifeButton = dialog.findViewById<Button>(R.id.button_life)
        val otherButton = dialog.findViewById<Button>(R.id.button_other)
        val resetButton = dialog.findViewById<Button>(R.id.reset_button)
        val confirmButton = dialog.findViewById<Button>(R.id.confirm_button)

        // 设置类别按钮点击事件
        studyButton.setOnClickListener { categoryEditText.setText("学习") }
        workButton.setOnClickListener { categoryEditText.setText("工作") }
        lifeButton.setOnClickListener { categoryEditText.setText("生活") }
        otherButton.setOnClickListener { categoryEditText.setText("其他") }

        // 设置重置按钮
        resetButton.setOnClickListener { categoryEditText.setText("") }

        // 设置确认按钮
        confirmButton.setOnClickListener {
            var category = categoryEditText.text.toString().trim { it <= ' ' }
            if (category.isEmpty()) {
                category = "其他"
            }
            filterTasksByCategory(category)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun filterTasksByCategory(category: String) {
        val filteredTasks: MutableList<Task> = ArrayList()
        for (task in originalTasks!!) {
            if (task.category != null && task.category == category) {
                filteredTasks.add(task)
            }
        }

        updateAllTaskLists(filteredTasks)

        // 反馈筛选结果
        if (filteredTasks.isEmpty()) {
            Toast.makeText(this, "没有找到类别为 $category 的事项", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(
                this,
                "找到 " + filteredTasks.size + " 个类别为 " + category + " 的事项",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun filterNone() {
        updateAllTaskLists(originalTasks!!)
    }

    private fun updateAllTaskLists(filteredTasks: List<Task>) {
        // 清空当前列表
        allTasks = ArrayList(filteredTasks)

        // 重新分类
        categorizeTasksForDisplay()

        // 更新UI
        updateTaskLists()
    }

    override fun onResume() {
        super.onResume()
        // 在恢复活动时重新加载任务，以获取最新数据
        loadTaskData()
    }
}