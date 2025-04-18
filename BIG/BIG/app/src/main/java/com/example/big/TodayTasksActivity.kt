package com.example.big

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.NumberPicker
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.util.Calendar

class TodayTasksActivity : AppCompatActivity() {
    private var importantTasksRecyclerView: RecyclerView? = null
    private var otherTasksRecyclerView: RecyclerView? = null
    private var completedTasksRecyclerView: RecyclerView? = null

    private var allTasks: MutableList<Task>? = null
    private var importantTasks: MutableList<Task>? = null
    private var otherTasks: MutableList<Task>? = null
    private var completedTasks: MutableList<Task>? = null

    private var importantTasksAdapter: TaskAdapter? = null
    private var otherTasksAdapter: TaskAdapter? = null
    private var completedTasksAdapter: TaskAdapter? = null

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
        backButton.setOnClickListener { v: View? -> finish() }

        // 设置菜单按钮
        val menuButton = findViewById<ImageButton>(R.id.menu_button)
        menuButton.setOnClickListener { view: View ->
            this.showFilterMenu(
                view
            )
        }

        // 设置添加任务按钮
        val addTaskButton = findViewById<FloatingActionButton>(R.id.add_task_button)
        addTaskButton.setOnClickListener { v: View? ->
            val intent = Intent(
                this@TodayTasksActivity,
                AddTaskActivity::class.java
            )
            startActivity(intent)
        }

        // 初始化任务数据
        initTaskData()

        // 设置任务列表
        setupTaskLists()
    }

    private fun initViews() {
        importantTasksRecyclerView = findViewById(R.id.important_tasks_recyclerview)
        otherTasksRecyclerView = findViewById(R.id.other_tasks_recyclerview)
        completedTasksRecyclerView = findViewById(R.id.completed_tasks_recyclerview)

        importantTasksRecyclerView?.setLayoutManager(LinearLayoutManager(this))
        otherTasksRecyclerView?.setLayoutManager(LinearLayoutManager(this))
        completedTasksRecyclerView?.setLayoutManager(LinearLayoutManager(this))
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

    private fun initTaskData() {
        // 创建一些示例任务数据
        allTasks = ArrayList()

        val cal = Calendar.getInstance()
        cal[Calendar.HOUR_OF_DAY] = 0
        cal[Calendar.MINUTE] = 0
        cal[Calendar.SECOND] = 0
        cal[Calendar.MILLISECOND] = 0
        val today = cal.time

        // 使用8位数ID

        // 重要任务
        val task1 = Task(
            12345001,
            "完成项目报告",
            "09 : 00 -- 11 : 00",
            today,
            120,
            true,
            "需要提交给经理审核"
        )
        task1.place = "办公室"
        task1.category = "工作"

        val task2 =
            Task(12345002, "客户会议", "14 : 00 -- 15 : 30", today, 90, true, "讨论新产品方案")
        task2.place = "会议室A"
        task2.category = "工作"

        // 其他任务
        val task3 = Task(12345003, "午餐", "12 : 00 -- 13 : 00", today, 60, false, "与同事共进午餐")
        task3.category = "生活"

        val task4 =
            Task(12345004, "整理邮件", "16 : 00 -- 17 : 00", today, 60, false, "回复重要客户邮件")
        task4.category = "工作"

        // 已完成任务
        val task5 = Task(12345005, "晨会", "08 : 30 -- 09 : 00", today, 30, false, "每日工作安排")
        task5.isFinished = true
        task5.category = "工作"

        val task6 =
            Task(12345006, "回复客户邮件", "10 : 00 -- 10 : 30", today, 30, false, "处理紧急问题")
        task6.isFinished = true
        task6.category = "工作"

        // 添加到列表
        allTasks?.add(task1)
        allTasks?.add(task2)
        allTasks?.add(task3)
        allTasks?.add(task4)
        allTasks?.add(task5)
        allTasks?.add(task6)

        // 分类
        categorizeTasksForDisplay()
    }

    private fun categorizeTasksForDisplay() {
        importantTasks = ArrayList()
        otherTasks = ArrayList()
        completedTasks = ArrayList()

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

        importantTasksRecyclerView!!.adapter = importantTasksAdapter
        otherTasksRecyclerView!!.adapter = otherTasksAdapter
        completedTasksRecyclerView!!.adapter = completedTasksAdapter
    }

    // 筛选未完成的任务
    private fun filterUnfinishedTasks() {
        val unfinishedTasks: MutableList<Task> = ArrayList()
        for (task in allTasks!!) {
            if (!task.isFinished) {
                unfinishedTasks.add(task)
            }
        }
        updateAllTaskLists(unfinishedTasks)
    }

    // 筛选重要任务
    private fun filterImportantTasks() {
        val filteredTasks: MutableList<Task> = ArrayList()
        for (task in allTasks!!) {
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
        resetButton.setOnClickListener { v: View? -> resetTimeFilters() }

        // 设置确认按钮
        confirmButton.setOnClickListener { v: View? ->
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
        yearPicker!!.setOnValueChangedListener { picker: NumberPicker?, oldVal: Int, newVal: Int ->
            selectedYear = newVal
            updateDayPicker()
        }

        monthPicker!!.setOnValueChangedListener { picker: NumberPicker?, oldVal: Int, newVal: Int ->
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
        endHourPicker!!.setOnValueChangedListener { picker: NumberPicker?, oldVal: Int, newVal: Int -> validateEndTime() }
        endMinutePicker!!.setOnValueChangedListener { picker: NumberPicker?, oldVal: Int, newVal: Int -> validateEndTime() }
        startHourPicker!!.setOnValueChangedListener { picker: NumberPicker?, oldVal: Int, newVal: Int -> validateEndTime() }
        startMinutePicker!!.setOnValueChangedListener { picker: NumberPicker?, oldVal: Int, newVal: Int -> validateEndTime() }
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
        for (task in allTasks!!) {
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
            if (task.timeRange != null && !task.timeRange.isEmpty() && task.timeRange != "未设定时间") {
                // 简单字符串比较（实际应用中可能需要更复杂的时间解析）
                val taskStart = task.timeRange.split(" -- ".toRegex())
                    .dropLastWhile { it.isEmpty() }.toTypedArray()[0].trim { it <= ' ' }
                val taskEnd = task.timeRange.split(" -- ".toRegex()).dropLastWhile { it.isEmpty() }
                    .toTypedArray()[1].trim { it <= ' ' }

                // 时间重叠检查
                timeMatches =
                    !(taskEnd.compareTo(startTimeStr) < 0 || taskStart.compareTo(endTimeStr) > 0)
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
        studyButton.setOnClickListener { v: View? -> categoryEditText.setText("学习") }
        workButton.setOnClickListener { v: View? -> categoryEditText.setText("工作") }
        lifeButton.setOnClickListener { v: View? -> categoryEditText.setText("生活") }
        otherButton.setOnClickListener { v: View? -> categoryEditText.setText("其他") }

        // 设置重置按钮
        resetButton.setOnClickListener { v: View? -> categoryEditText.setText("") }

        // 设置确认按钮
        confirmButton.setOnClickListener { v: View? ->
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
        for (task in allTasks!!) {
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
        updateAllTaskLists(allTasks!!)
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun updateAllTaskLists(filteredTasks: List<Task>) {
        // 清空当前列表
        importantTasks!!.clear()
        otherTasks!!.clear()
        completedTasks!!.clear()

        // 重新分类
        for (task in filteredTasks) {
            if (task.isFinished) {
                completedTasks!!.add(task)
            } else if (task.isImportant) {
                importantTasks!!.add(task)
            } else {
                otherTasks!!.add(task)
            }
        }

        // 通知适配器数据已更改
        importantTasksAdapter!!.notifyDataSetChanged()
        otherTasksAdapter!!.notifyDataSetChanged()
        completedTasksAdapter!!.notifyDataSetChanged()
    }

    override fun onResume() {
        super.onResume()
        // 刷新数据（实际应用中，这里应该从数据库重新加载数据）
        initTaskData()
        setupTaskLists()
    }
}