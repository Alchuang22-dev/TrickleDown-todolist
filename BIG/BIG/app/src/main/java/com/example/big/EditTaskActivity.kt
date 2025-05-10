package com.example.big

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.DatePicker
import android.widget.EditText
import android.widget.NumberPicker
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.big.models.TaskResponse
import com.example.big.utils.TaskManager
import com.example.big.viewmodel.TaskViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class EditTaskActivity : AppCompatActivity() {
    private var titleEditText: EditText? = null
    private var descriptionEditText: EditText? = null
    private var datePicker: DatePicker? = null
    private var startHourPicker: NumberPicker? = null
    private var startMinutePicker: NumberPicker? = null
    private var endHourPicker: NumberPicker? = null
    private var endMinutePicker: NumberPicker? = null
    private var placeEditText: EditText? = null

    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private var importantSwitch: Switch? = null
    private var editButton: Button? = null
    private var deleteButton: Button? = null
    private var finishedCheckBox: CheckBox? = null
    private var delayedCheckBox: CheckBox? = null
    private var finishedStatusText: TextView? = null
    private var delayedStatusText: TextView? = null
    private var categoryEditText: EditText? = null
    private var studyButton: Button? = null
    private var workButton: Button? = null
    private var lifeButton: Button? = null
    private var otherButton: Button? = null

    private var currentTask: TaskResponse? = null
    private var taskId: String = ""
    private lateinit var taskViewModel: TaskViewModel
    private val TAG = "EditTaskActivity"

    // 标记是否需要响应完成状态更改
    private var shouldRespondToFinishedChange = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.enableEdgeToEdge()
        setContentView(R.layout.activity_edit_task)
        ViewCompat.setOnApplyWindowInsetsListener(
            findViewById(R.id.main)
        ) { v: View, insets: WindowInsetsCompat ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 初始化 ViewModel
        taskViewModel = ViewModelProvider(this).get(TaskViewModel::class.java)

        // 获取当前编辑的任务ID
        taskId = intent.getStringExtra("task_id") ?: ""
        if (taskId.isEmpty()) {
            Toast.makeText(this, "任务ID无效", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // 初始化控件
        initViews()
        setupTimePickers()
        setupButtons()
        setupStatusCheckBoxes()
        setupCategoryButtons()
        setupObservers()

        // 获取任务详情
        fetchTaskDetails(taskId)

        findViewById<View>(R.id.back_button).setOnClickListener { finish() }
    }

    private fun setupObservers() {
        // 观察任务操作结果
        taskViewModel.taskOperationResult.observe(this) { result ->
            when (result) {
                is TaskManager.Result.Success -> {
                    hideLoading()
                    Toast.makeText(this, "任务更新成功", Toast.LENGTH_SHORT).show()

                    // 如果是完成状态变更，更新UI而不是关闭页面
                    if (result.data != null) {
                        currentTask = result.data
                        // 暂时禁用响应，防止循环触发
                        shouldRespondToFinishedChange = false
                        updateFinishedUI(result.data.is_finished)
                        shouldRespondToFinishedChange = true
                    } else {
                        setResult(RESULT_OK)
                        finish()
                    }
                }
                is TaskManager.Result.Error -> {
                    hideLoading()
                    Toast.makeText(this, "任务更新失败: ${result.message}", Toast.LENGTH_LONG).show()
                    Log.e(TAG, "任务更新失败: ${result.message}")
                }
            }
        }

        // 观察删除任务结果
        taskViewModel.deleteTaskResult.observe(this) { result ->
            when (result) {
                is TaskManager.Result.Success -> {
                    hideLoading()
                    Toast.makeText(this, "任务删除成功", Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK)
                    finish()
                }
                is TaskManager.Result.Error -> {
                    hideLoading()
                    Toast.makeText(this, "任务删除失败: ${result.message}", Toast.LENGTH_LONG).show()
                    Log.e(TAG, "任务删除失败: ${result.message}")
                }
            }
        }

        // 观察错误信息
        taskViewModel.error.observe(this) { error ->
            if (!error.isNullOrEmpty()) {
                Toast.makeText(this, error, Toast.LENGTH_LONG).show()
                Log.e(TAG, "错误: $error")
                taskViewModel.clearError()
                hideLoading()
            }
        }
    }

    private fun updateFinishedUI(isFinished: Boolean) {
        finishedCheckBox?.isChecked = isFinished
        finishedStatusText?.text = if (isFinished) "已完成" else "未完成"
    }

    private fun showLoading() {
        // 在实际应用中应该显示进度指示器
        editButton?.isEnabled = false
        deleteButton?.isEnabled = false
    }

    private fun hideLoading() {
        editButton?.isEnabled = true
        deleteButton?.isEnabled = true
    }

    private fun fetchTaskDetails(taskId: String) {
        showLoading()
        lifecycleScope.launch {
            try {
                val response = TaskManager.getTaskById(taskId)
                when (response) {
                    is TaskManager.Result.Success -> {
                        currentTask = response.data
                        populateTaskData(response.data)
                    }
                    is TaskManager.Result.Error -> {
                        Toast.makeText(this@EditTaskActivity,
                            "获取任务详情失败: ${response.message}",
                            Toast.LENGTH_LONG).show()
                        Log.e(TAG, "获取任务详情失败: ${response.message}")
                    }
                }
                hideLoading()
            } catch (e: Exception) {
                Log.e(TAG, "获取任务详情时出现异常", e)
                Toast.makeText(
                    this@EditTaskActivity,
                    "获取任务详情失败: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
                hideLoading()
                finish()
            }
        }
    }

    private suspend fun getTaskById(taskId: String): TaskManager.Result<TaskResponse> {
        return try {
            val response = TaskManager.getTaskById(taskId)
            response
        } catch (e: Exception) {
            Log.e(TAG, "获取任务详情异常", e)
            TaskManager.Result.Error(e.message ?: "未知错误")
        }
    }

    private fun initViews() {
        titleEditText = findViewById(R.id.edit_title)
        descriptionEditText = findViewById(R.id.edit_description)
        datePicker = findViewById(R.id.date_picker)
        startHourPicker = findViewById(R.id.start_hour_picker)
        startMinutePicker = findViewById(R.id.start_minute_picker)
        endHourPicker = findViewById(R.id.end_hour_picker)
        endMinutePicker = findViewById(R.id.end_minute_picker)
        placeEditText = findViewById(R.id.edit_place)
        importantSwitch = findViewById(R.id.switch_important)
        editButton = findViewById(R.id.button_edit)
        deleteButton = findViewById(R.id.button_delete)
        finishedCheckBox = findViewById(R.id.checkbox_finished)
        delayedCheckBox = findViewById(R.id.checkbox_delayed)
        finishedStatusText = findViewById(R.id.text_finished_status)
        delayedStatusText = findViewById(R.id.text_delayed_status)
        categoryEditText = findViewById(R.id.edit_category)
        studyButton = findViewById(R.id.button_study)
        workButton = findViewById(R.id.button_work)
        lifeButton = findViewById(R.id.button_life)
        otherButton = findViewById(R.id.button_other)
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
            // Toast.makeText(this, "结束时间必须晚于开始时间", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupButtons() {
        editButton!!.setOnClickListener { updateTask() }
        deleteButton!!.setOnClickListener { confirmDelete() }
    }

    private fun setupStatusCheckBoxes() {
        // 修改：在状态文本上添加点击事件
        val finishedCardView = findViewById<View>(R.id.card_finished)
        finishedCardView.setOnClickListener {
            // 直接调用finishTask切换任务状态
            finishTask()
        }

        // 为复选框添加点击事件，也调用finishTask
        finishedCheckBox!!.setOnCheckedChangeListener { _, isChecked ->
            if (shouldRespondToFinishedChange) {
                finishedStatusText!!.text = if (isChecked) "已完成" else "未完成"
                finishTask()
            }
        }

        delayedCheckBox!!.setOnCheckedChangeListener { _, isChecked ->
            delayedStatusText!!.text =
                if (isChecked) "已延期" else "按时完成"
        }
    }

    private fun setupCategoryButtons() {
        studyButton!!.setOnClickListener { categoryEditText!!.setText("学习") }
        workButton!!.setOnClickListener { categoryEditText!!.setText("工作") }
        lifeButton!!.setOnClickListener { categoryEditText!!.setText("生活") }
        otherButton!!.setOnClickListener { categoryEditText!!.setText("其他") }
    }

    private fun populateTaskData(task: TaskResponse) {
        // 填充标题
        titleEditText!!.setText(task.title)

        // 填充描述
        if (task.description != null) {
            descriptionEditText!!.setText(task.description)
        }

        // 填充日期选择器
        val date = task.date
        if (date != null) {
            val cal = Calendar.getInstance()
            cal.time = date
            datePicker!!.updateDate(
                cal[Calendar.YEAR], cal[Calendar.MONTH],
                cal[Calendar.DAY_OF_MONTH]
            )
        }

        // 填充时间范围
        val timeRange = task.time_range
        if (timeRange != null && timeRange != "未设定时间") {
            try {
                // 检查分隔符，可能是 "--" 或 "-"
                val delimiter = if (timeRange.contains("--")) "--" else "-"

                // 分割时间范围
                val parts = timeRange.split(delimiter).map { it.trim() }

                // 提取小时和分钟，移除可能的空格
                val startTimeStr = parts[0].replace(" ", "")
                val endTimeStr = parts[1].replace(" ", "")

                val startParts = startTimeStr.split(":")
                val endParts = endTimeStr.split(":")

                val startHour = startParts[0].toInt()
                val startMinute = startParts[1].toInt()
                val startMinuteIndex = startMinute / 5 // 计算分钟索引

                val endHour = endParts[0].toInt()
                val endMinute = endParts[1].toInt()
                val endMinuteIndex = endMinute / 5 // 计算分钟索引

                startHourPicker!!.value = startHour
                startMinutePicker!!.value = startMinuteIndex
                endHourPicker!!.value = endHour
                endMinutePicker!!.value = endMinuteIndex
            } catch (e: Exception) {
                Log.e(TAG, "解析时间范围失败: ${e.message}", e)
                // 解析错误，使用默认值
                startHourPicker!!.value = 9
                startMinutePicker!!.value = 0
                endHourPicker!!.value = 10
                endMinutePicker!!.value = 0
            }
        }

        // 填充地点
        if (task.place != null) {
            placeEditText!!.setText(task.place)
        }

        // 设置重要性
        importantSwitch!!.isChecked = task.is_important

        // 设置完成状态
        shouldRespondToFinishedChange = false  // 暂时禁用响应，防止触发API调用
        finishedCheckBox!!.isChecked = task.is_finished
        finishedStatusText!!.text = if (task.is_finished) "已完成" else "未完成"
        shouldRespondToFinishedChange = true  // 恢复响应

        // 设置延期状态
        val delayed = task.is_delayed
        delayedCheckBox!!.isChecked = delayed
        delayedStatusText!!.text = if (delayed) "已延期" else "按时完成"

        // 设置分类标签
        if (task.category != null) {
            categoryEditText!!.setText(task.category)
        }
    }

    private fun updateTask() {
        val title = titleEditText!!.text.toString().trim()

        // 检查必填字段
        if (title.isEmpty()) {
            Toast.makeText(this, "请输入任务标题", Toast.LENGTH_SHORT).show()
            return
        }

        // 获取日期
        val calendar = Calendar.getInstance()
        calendar[datePicker!!.year, datePicker!!.month, datePicker!!.dayOfMonth, 0, 0] = 0
        val date = calendar.time

        // 获取描述
        var description = descriptionEditText!!.text.toString().trim()
        if (description.isEmpty()) {
            description = "" // 默认空描述
        }

        // 获取地点
        var place = placeEditText!!.text.toString().trim()
        if (place.isEmpty()) {
            place = "" // 默认空地点
        }

        // 获取重要性
        val important = importantSwitch!!.isChecked

        // 获取时间范围和持续时间
        val startHour = startHourPicker!!.value
        val endHour = endHourPicker!!.value
        val startMinuteIndex = startMinutePicker!!.value
        val endMinuteIndex = endMinutePicker!!.value
        val startMinute = startMinuteIndex * 5
        val endMinute = endMinuteIndex * 5

        // 格式化时间范围
        @SuppressLint("DefaultLocale") val startHourStr = String.format("%02d", startHour)
        @SuppressLint("DefaultLocale") val startMinuteStr = String.format("%02d", startMinute)
        @SuppressLint("DefaultLocale") val endHourStr = String.format("%02d", endHour)
        @SuppressLint("DefaultLocale") val endMinuteStr = String.format("%02d", endMinute)

        val timeRange =
            "$startHourStr:$startMinuteStr - $endHourStr:$endMinuteStr"

        // 计算持续时间（分钟）
        val durationMinutes = (endHour - startHour) * 60 + (endMinute - startMinute)

        // 如果时间范围有效，设置dueDate为日期加结束时间
        val dueCal = Calendar.getInstance()
        dueCal.time = date
        dueCal[Calendar.HOUR_OF_DAY] = endHour
        dueCal[Calendar.MINUTE] = endMinute
        val dueDate = dueCal.time

        // 获取完成状态
        val finished = finishedCheckBox!!.isChecked

        // 获取延期状态
        val delayed = delayedCheckBox!!.isChecked

        // 获取标签
        var category = categoryEditText!!.text.toString().trim()
        if (category.isEmpty()) {
            category = "其他" // 默认标签
        }

        // 准备请求对象
        val request = com.example.big.models.CreateTaskRequest(
            title = title,
            time_range = timeRange,
            date = date,
            duration_minutes = durationMinutes,
            is_important = important,
            description = description,
            place = place,
            due_date = dueDate,
            is_finished = finished,
            is_delayed = delayed,
            category = category
        )

        showLoading()
        lifecycleScope.launch {
            try {
                // 修改：直接使用字符串形式的 taskId，不尝试转换为整数
                taskViewModel.updateTask(taskId, request)
            } catch (e: Exception) {
                Log.e(TAG, "更新任务时出现异常", e)
                Toast.makeText(this@EditTaskActivity, "更新任务失败: ${e.message}", Toast.LENGTH_LONG).show()
                hideLoading()
            }
        }
    }

    private fun formatDateForApi(date: Date): String {
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return format.format(date)
    }

    private fun confirmDelete() {
        AlertDialog.Builder(this)
            .setTitle("删除事项")
            .setMessage("确定要删除这个事项吗？此操作不可撤销。")
            .setPositiveButton(
                "删除"
            ) { _, _ -> deleteTask() }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun deleteTask() {
        showLoading()
        lifecycleScope.launch {
            try {
                // 直接使用字符串形式的 taskId
                taskViewModel.deleteTask(taskId)
            } catch (e: Exception) {
                Log.e(TAG, "删除任务时出现异常", e)
                Toast.makeText(this@EditTaskActivity, "删除任务失败: ${e.message}", Toast.LENGTH_LONG).show()
                hideLoading()
            }
        }
    }

    private fun finishTask() {
        showLoading()
        lifecycleScope.launch {
            try {
                // 调用 ViewModel 的方法来切换任务完成状态
                taskViewModel.finishTask(taskId)
            } catch (e: Exception) {
                Log.e(TAG, "完成任务时出现异常", e)
                Toast.makeText(this@EditTaskActivity, "完成任务失败: ${e.message}", Toast.LENGTH_LONG).show()
                hideLoading()
            }
        }
    }
}