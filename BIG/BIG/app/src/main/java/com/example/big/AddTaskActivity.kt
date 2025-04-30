package com.example.big

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.DatePicker
import android.widget.EditText
import android.widget.NumberPicker
import android.widget.ProgressBar
import android.widget.Switch
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import com.example.big.models.CreateTaskRequest
import com.example.big.utils.TaskManager
import com.example.big.viewmodel.TaskViewModel
import java.util.Calendar
import java.util.Random

class AddTaskActivity : AppCompatActivity() {
    private val TAG = "AddTaskActivity"
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
    private var addButton: Button? = null

    // 添加进度指示器
    private var progressBar: ProgressBar? = null

    // 声明新增的事项标签相关控件
    private var categoryEditText: EditText? = null
    private var studyButton: Button? = null
    private var workButton: Button? = null
    private var lifeButton: Button? = null
    private var otherButton: Button? = null

    // 使用 ViewModel
    private val taskViewModel: TaskViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.enableEdgeToEdge()
        setContentView(R.layout.activity_add_task)
        ViewCompat.setOnApplyWindowInsetsListener(
            findViewById(R.id.main)
        ) { v: View, insets: WindowInsetsCompat ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 初始化 TaskManager
        TaskManager.init(applicationContext)

        // 初始化控件
        initViews()
        setupTimePickers()
        setupAddButton()
        setupCategoryButtons()
        setupViewModelObservers()

        findViewById<View>(R.id.back_button).setOnClickListener { v: View? -> finish() }
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
        addButton = findViewById(R.id.button_add)
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

    private fun setupAddButton() {
        addButton!!.setOnClickListener { v: View? -> createNewTask() }
    }

    private fun setupCategoryButtons() {
        studyButton!!.setOnClickListener { v: View? -> categoryEditText!!.setText("学习") }
        workButton!!.setOnClickListener { v: View? -> categoryEditText!!.setText("工作") }
        lifeButton!!.setOnClickListener { v: View? -> categoryEditText!!.setText("生活") }
        otherButton!!.setOnClickListener { v: View? -> categoryEditText!!.setText("其他") }
    }

    private fun setupViewModelObservers() {
        // 观察任务操作结果
        taskViewModel.taskOperationResult.observe(this) { result ->
            when (result) {
                is TaskManager.Result.Success -> {
                    showLoading(false)
                    Toast.makeText(this, "任务已成功创建: ${result.data.title}", Toast.LENGTH_SHORT).show()
                    finish() // 返回上一个Activity
                }
                is TaskManager.Result.Error -> {
                    Log.v("result","$result")
                    showLoading(false)
                    Toast.makeText(this, "创建任务失败: ${result.message}", Toast.LENGTH_LONG).show()
                    Log.e(TAG, "创建任务失败: ${result.message}")
                }
                // 移除 Result.Loading 分支，因为 TaskManager.Result 中没有定义该类型
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        progressBar?.visibility = if (isLoading) View.VISIBLE else View.GONE
        addButton?.isEnabled = !isLoading
    }

    private fun createNewTask() {
        val title = titleEditText?.text.toString().trim()

        // 检查必填字段
        if (title.isEmpty()) {
            Toast.makeText(this, "请输入任务标题", Toast.LENGTH_SHORT).show()
            return
        }

        // 获取日期
        val calendar = Calendar.getInstance()
        datePicker?.let { calendar.set(it.year, datePicker!!.month, datePicker!!.dayOfMonth, 0, 0, 0) }
        val date = calendar.time

        // 获取描述
        val description = descriptionEditText?.text.toString().trim()

        // 获取地点
        val place = placeEditText?.text.toString().trim()

        // 获取重要性
        val important = importantSwitch?.isChecked

        // 获取标签
        val category = categoryEditText?.text.toString().trim().ifEmpty { "其他" }

        // 获取时间范围和持续时间
        val startHour = startHourPicker?.value
        val endHour = endHourPicker?.value
        val startMinuteIndex = startMinutePicker?.value
        val endMinuteIndex = endMinutePicker?.value
        val startMinute = startMinuteIndex?.times(5)
        val endMinute = endMinuteIndex?.times(5)

        // 格式化时间范围
        val startHourStr = String.format("%02d", startHour)
        val startMinuteStr = String.format("%02d", startMinute)
        val endHourStr = String.format("%02d", endHour)
        val endMinuteStr = String.format("%02d", endMinute)

        val timeRange = "$startHourStr:$startMinuteStr - $endHourStr:$endMinuteStr"

        // 计算持续时间（分钟）- 修复语法问题
        val durationMinutes = if (startHour != null && endHour != null && startMinute != null && endMinute != null) {
            (endHour - startHour) * 60 + (endMinute - startMinute)
        } else {
            0 // 默认值
        }

        // 设置截止日期为日期加结束时间
        val dueCal = Calendar.getInstance()
        dueCal.time = date
        endHour?.let { dueCal.set(Calendar.HOUR_OF_DAY, it) }
        endMinute?.let { dueCal.set(Calendar.MINUTE, it) }
        val dueDate = dueCal.time

        // 创建请求对象 - 注意字段名与后端匹配
        val createTaskRequest = important?.let {
            CreateTaskRequest(
                title = title,
                time_range = timeRange,  // 使用下划线命名
                date = date,
                duration_minutes = durationMinutes,  // 使用下划线命名
                is_important = it,  // 使用下划线命名
                description = description,
                place = place,
                due_date = dueDate,  // 使用下划线命名
                category = category,
                is_finished = false  // 使用下划线命名
            )
        }

        // 使用ViewModel创建任务
        val viewModel = ViewModelProvider(this)[TaskViewModel::class.java]
        createTaskRequest?.let { viewModel.createTask(it) }

        // 观察创建成功状态
        viewModel.taskCreationSuccess.observe(this) { success ->
            if (success) {
                Toast.makeText(this, "任务已成功创建", Toast.LENGTH_SHORT).show()
                finish()  // 返回上一个Activity
            }
        }

        // 观察错误信息
        viewModel.error.observe(this) { errorMsg ->
            if (errorMsg != null && errorMsg.isNotEmpty()) {
                Toast.makeText(this, "创建任务失败: $errorMsg", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun generateRandomId(): Int {
        val random = Random()
        // 生成8位随机数（10000000-99999999）
        return 10000000 + random.nextInt(90000000)
    }
}