package com.example.big

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.CompoundButton
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
import java.util.Calendar
import java.util.Date

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

    private var currentTask: Task? = null
    private var taskId = 0

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

        // 获取当前编辑的任务ID
        taskId = intent.getIntExtra("task_id", -1)
        if (taskId == -1) {
            Toast.makeText(this, "任务ID无效", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // 从数据库或其他存储中获取任务对象
        // 这里示例使用，实际应用中应该从数据库获取
        currentTask = getTaskById(taskId)
        if (currentTask == null) {
            Toast.makeText(this, "无法找到任务", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // 初始化控件
        initViews()
        setupTimePickers()
        setupButtons()
        setupStatusCheckBoxes()
        setupCategoryButtons()

        // 填充现有任务数据
        populateTaskData()

        findViewById<View>(R.id.back_button).setOnClickListener { v: View? -> finish() }
    }

    // 示例方法，实际应用中应从数据库获取
    private fun getTaskById(id: Int): Task {
        // 模拟数据，实际应用中应该从数据库获取
        // 这里暂时返回一个假的Task对象用于演示
        val cal = Calendar.getInstance()
        val date = cal.time

        val task = Task(
            id,
            "示例任务",
            "09 : 00 -- 10 : 30",
            date,
            90,
            true,
            "这是一个示例任务描述",
            "示例地点",
            date
        )
        task.category = "学习"
        task.isFinished = false
        // 由于Task类中没有setDelayed方法，我们不能在这里设置延期状态
        // 需要在Task类中添加这个方法
        return task
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

    private fun setupButtons() {
        editButton!!.setOnClickListener { v: View? -> updateTask() }
        deleteButton!!.setOnClickListener { v: View? -> confirmDelete() }
    }

    private fun setupStatusCheckBoxes() {
        finishedCheckBox!!.setOnCheckedChangeListener { buttonView: CompoundButton?, isChecked: Boolean ->
            finishedStatusText!!.text =
                if (isChecked) "已完成" else "未完成"
        }

        delayedCheckBox!!.setOnCheckedChangeListener { buttonView: CompoundButton?, isChecked: Boolean ->
            delayedStatusText!!.text =
                if (isChecked) "已延期" else "按时完成"
        }
    }

    private fun setupCategoryButtons() {
        studyButton!!.setOnClickListener { v: View? -> categoryEditText!!.setText("学习") }
        workButton!!.setOnClickListener { v: View? -> categoryEditText!!.setText("工作") }
        lifeButton!!.setOnClickListener { v: View? -> categoryEditText!!.setText("生活") }
        otherButton!!.setOnClickListener { v: View? -> categoryEditText!!.setText("其他") }
    }

    private fun populateTaskData() {
        // 使用当前任务的数据填充界面
        val taskData = currentTask!!.all

        // 填充标题
        titleEditText!!.setText(taskData["title"] as String?)

        // 填充描述
        if (taskData["description"] != null) {
            descriptionEditText!!.setText(taskData["description"] as String?)
        }

        // 填充日期选择器
        val date = taskData["date"] as Date?
        if (date != null) {
            val cal = Calendar.getInstance()
            cal.time = date
            datePicker!!.updateDate(
                cal[Calendar.YEAR], cal[Calendar.MONTH],
                cal[Calendar.DAY_OF_MONTH]
            )
        }

        // 填充时间范围
        val timeRange = taskData["timeRange"] as String?
        if (timeRange != null && timeRange != "未设定时间") {
            try {
                // 解析时间范围，格式: "HH : MM -- HH : MM"
                val parts =
                    timeRange.split(" -- ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                val startParts =
                    parts[0].split(" : ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                val endParts =
                    parts[1].split(" : ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

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
                // 解析错误，使用默认值
                startHourPicker!!.value = 9
                startMinutePicker!!.value = 0
                endHourPicker!!.value = 10
                endMinutePicker!!.value = 0
            }
        }

        // 填充地点
        if (taskData["place"] != null) {
            placeEditText!!.setText(taskData["place"] as String?)
        }

        // 设置重要性
        importantSwitch!!.isChecked = (taskData["important"] as Boolean?)!!

        // 设置完成状态
        if (taskData["finished"] != null) {
            val finished = taskData["finished"] as Boolean
            finishedCheckBox!!.isChecked = finished
            finishedStatusText!!.text = if (finished) "已完成" else "未完成"
        }

        // 设置延期状态
        if (taskData["delayed"] != null) {
            val delayed = taskData["delayed"] as Boolean
            delayedCheckBox!!.isChecked = delayed
            delayedStatusText!!.text = if (delayed) "已延期" else "按时完成"
        }

        // 设置分类标签
        if (taskData["category"] != null) {
            val category = taskData["category"] as String?
            categoryEditText!!.setText(category)
        }
    }

    private fun updateTask() {
        val title = titleEditText!!.text.toString().trim { it <= ' ' }

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
        var description = descriptionEditText!!.text.toString().trim { it <= ' ' }
        if (description.isEmpty()) {
            description = "" // 默认空描述
        }

        // 获取地点
        var place = placeEditText!!.text.toString().trim { it <= ' ' }
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
            "$startHourStr : $startMinuteStr -- $endHourStr : $endMinuteStr"

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

        // 获取标签
        var category = categoryEditText!!.text.toString().trim { it <= ' ' }
        if (category.isEmpty()) {
            category = "其他" // 默认标签
        }

        // 更新任务
        currentTask!!.edit(
            title,
            timeRange,
            date,
            durationMinutes,
            important,
            description,
            place,
            dueDate
        )
        currentTask!!.isFinished = finished
        currentTask!!.place = place
        currentTask!!.category = category

        // 由于Task类中没有setDelayed方法，我们不能设置延期状态
        // 您需要在Task类中添加这个方法：
        // public void setDelayed(boolean delayed) {
        //     this.delayed = delayed;
        // }
        Toast.makeText(this, "任务已更新: $title", Toast.LENGTH_SHORT).show()

        // 返回上一个Activity
        val resultIntent = Intent()
        resultIntent.putExtra("task_id", taskId)
        resultIntent.putExtra("action", "edit")
        setResult(RESULT_OK, resultIntent)
        finish()
    }

    private fun confirmDelete() {
        AlertDialog.Builder(this)
            .setTitle("删除事项")
            .setMessage("确定要删除这个事项吗？此操作不可撤销。")
            .setPositiveButton(
                "删除"
            ) { dialog: DialogInterface?, which: Int -> deleteTask() }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun deleteTask() {
        val success = currentTask!!.delete()

        if (success) {
            Toast.makeText(this, "事项已删除", Toast.LENGTH_SHORT).show()

            // 返回上一个Activity并传递删除结果
            val resultIntent = Intent()
            resultIntent.putExtra("task_id", taskId)
            resultIntent.putExtra("action", "delete")
            setResult(RESULT_OK, resultIntent)
            finish()
        } else {
            Toast.makeText(this, "删除失败，请稍后重试", Toast.LENGTH_SHORT).show()
        }
    }
}