package com.example.big

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.big.utils.TaskManager
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import com.example.big.models.FocusDistributionResponse
import com.example.big.models.FocusDistributionData
import com.example.big.models.MonthlyDistributionData
import com.example.big.models.YearlyDistributionData

class StatisticsActivity : AppCompatActivity() {
    private val TAG = "StatisticsActivity"

    private var dailyDistributionContainer: LinearLayout? = null
    private var monthlyViewContainer: LinearLayout? = null
    private var yearlyViewContainer: LinearLayout? = null
    private var monthlyChart: LineChart? = null
    private var yearlyChart: LineChart? = null
    private var btnDaily: Button? = null
    private var btnWeekly: Button? = null
    private var btnMonthly: Button? = null
    private var btnCustom: Button? = null
    private var btnMonthView: Button? = null
    private var btnYearView: Button? = null
    private var btnPrevDate: ImageButton? = null
    private var btnNextDate: ImageButton? = null
    private var btnPrevMonth: ImageButton? = null
    private var btnNextMonth: ImageButton? = null
    private var tvDistributionTitle: TextView? = null
    private var tvMonthlyTitle: TextView? = null

    // 当前选择的视图类型
    private var currentDistributionType = "day" // day, week, month, custom
    private var isYearView = false

    // 日期相关变量
    private val calendar = Calendar.getInstance()
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val monthFormatter = SimpleDateFormat("yyyy-MM", Locale.getDefault())
    private val yearFormatter = SimpleDateFormat("yyyy", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_statistics)

        // 初始化视图
        initViews()

        // 设置返回按钮
        findViewById<View>(R.id.back_button).setOnClickListener { finish() }

        // 加载统计数据
        loadStatistics()

        // 设置任务分布切换按钮
        setupDistributionButtons()

        // 设置月度/年度视图切换按钮
        setupMonthYearButtons()

        // 设置日期导航按钮
        setupDateNavigationButtons()
    }

    private fun initViews() {
        // 今日任务数据视图
        val todayTasksCount = findViewById<TextView>(R.id.today_tasks_count)
        val todayCompletedCount = findViewById<TextView>(R.id.today_completed_count)
        val todayDuration = findViewById<TextView>(R.id.today_duration)

        // 累计任务数据视图
        val totalCount = findViewById<TextView>(R.id.total_count)
        val totalDuration = findViewById<TextView>(R.id.total_duration)
        val dailyAvgDuration = findViewById<TextView>(R.id.daily_avg_duration)

        // 任务分布视图
        dailyDistributionContainer = findViewById(R.id.daily_distribution_container)
        btnDaily = findViewById(R.id.btn_daily)
        btnWeekly = findViewById(R.id.btn_weekly)
        btnMonthly = findViewById(R.id.btn_monthly)
        btnPrevDate = findViewById(R.id.btn_prev_date)
        btnNextDate = findViewById(R.id.btn_next_date)
        tvDistributionTitle = findViewById<TextView>(R.id.tv_distribution_title)

        // 月度/年度数据视图
        monthlyViewContainer = findViewById(R.id.monthly_view_container)
        yearlyViewContainer = findViewById(R.id.yearly_view_container)
        btnMonthView = findViewById(R.id.btn_month_view)
        btnYearView = findViewById(R.id.btn_year_view)
        btnPrevMonth = findViewById(R.id.btn_prev_month)
        btnNextMonth = findViewById(R.id.btn_next_month)
        tvMonthlyTitle = findViewById<TextView>(R.id.tv_monthly_title)
        monthlyChart = findViewById(R.id.monthly_chart)
        yearlyChart = findViewById(R.id.yearly_chart)

        // 更新标题显示当前日期
        updateDateTitles()
    }

    private fun loadStatistics() {
        // 使用协程加载数据
        lifecycleScope.launch {
            try {
                // 获取今日专注统计
                loadTodayFocusStatistics()

                // 获取累计专注统计
                loadTotalFocusStatistics()

                // 获取专注时长分布
                loadFocusDistribution(currentDistributionType)

                // 加载月度/年度视图数据
                if (isYearView) {
                    loadYearlyFocusData()
                } else {
                    loadMonthlyFocusData()
                }
            } catch (e: Exception) {
                Log.e(TAG, "加载统计数据失败", e)
                Toast.makeText(applicationContext, "加载数据失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private suspend fun loadTodayFocusStatistics() {
        val result = TaskManager.getTodayFocusStatistics()
        when (result) {
            is TaskManager.Result.Success -> {
                val stats = result.data
                runOnUiThread {
                    findViewById<TextView>(R.id.today_tasks_count).text = stats.focus_count.toString()
                    findViewById<TextView>(R.id.today_completed_count).text = stats.tasks_completed.toString()
                    findViewById<TextView>(R.id.today_duration).text = stats.focus_duration_minutes
                }
            }
            is TaskManager.Result.Error -> {
                Log.e(TAG, "获取今日专注统计失败: ${result.message}")
                runOnUiThread {
                    Toast.makeText(applicationContext, "获取今日专注数据失败", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private suspend fun loadTotalFocusStatistics() {
        val result = TaskManager.getTotalFocusStatistics()
        when (result) {
            is TaskManager.Result.Success -> {
                val stats = result.data
                runOnUiThread {
                    findViewById<TextView>(R.id.total_count).text = stats.tasks_completed.toString()
                    findViewById<TextView>(R.id.total_duration).text = stats.focus_duration_minutes
                    findViewById<TextView>(R.id.daily_avg_duration).text = stats.avg_daily_duration
                }
            }
            is TaskManager.Result.Error -> {
                Log.e(TAG, "获取累计专注统计失败: ${result.message}")
                runOnUiThread {
                    Toast.makeText(applicationContext, "获取累计专注数据失败", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private suspend fun loadFocusDistribution(type: String) {
        // 根据当前选择的日期范围构建参数
        val startDate = when (type) {
            "day" -> dateFormatter.format(calendar.time)
            "week" -> {
                // 获取本周的第一天
                val tempCal = Calendar.getInstance()
                tempCal.time = calendar.time
                tempCal.set(Calendar.DAY_OF_WEEK, tempCal.firstDayOfWeek)
                dateFormatter.format(tempCal.time)
            }
            "month" -> {
                // 获取本月的第一天
                val tempCal = Calendar.getInstance()
                tempCal.time = calendar.time
                tempCal.set(Calendar.DAY_OF_MONTH, 1)
                dateFormatter.format(tempCal.time)
            }
            else -> null
        }

        val result = TaskManager.getFocusDistribution(type, startDate)
        when (result) {
            is TaskManager.Result.Success -> {
                val distribution = result.data
                generateHeatmap(distribution)
            }
            is TaskManager.Result.Error -> {
                Log.e(TAG, "获取专注时长分布失败: ${result.message}")
                runOnUiThread {
                    Toast.makeText(applicationContext, "获取专注时长分布失败", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private suspend fun loadMonthlyFocusData() {
        // 获取当前月的年月格式
        val yearMonth = monthFormatter.format(calendar.time)

        // 调用API获取本月各天的专注数据
        val result = TaskManager.getFocusDistribution("month", yearMonth)
        when (result) {
            is TaskManager.Result.Success -> {
                val distribution = result.data

                // 确保月度数据不为空
                if (distribution.monthly_data != null) {
                    setupMonthlyChart(distribution.monthly_data)
                } else {
                    Log.e(TAG, "月度专注数据为空")
                    runOnUiThread {
                        Toast.makeText(applicationContext, "没有找到月度专注数据", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            is TaskManager.Result.Error -> {
                Log.e(TAG, "获取月度专注数据失败: ${result.message}")
                runOnUiThread {
                    Toast.makeText(applicationContext, "获取月度专注数据失败", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private suspend fun loadYearlyFocusData() {
        // 获取当前年份
        val year = yearFormatter.format(calendar.time)

        // 调用API获取本年各月的专注数据
        val result = TaskManager.getFocusDistribution("year", year)
        when (result) {
            is TaskManager.Result.Success -> {
                val distribution = result.data

                // 确保年度数据不为空
                if (distribution.yearly_data != null) {
                    setupYearlyChart(distribution.yearly_data)
                } else {
                    Log.e(TAG, "年度专注数据为空")
                    runOnUiThread {
                        Toast.makeText(applicationContext, "没有找到年度专注数据", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            is TaskManager.Result.Error -> {
                Log.e(TAG, "获取年度专注数据失败: ${result.message}")
                runOnUiThread {
                    Toast.makeText(applicationContext, "获取年度专注数据失败", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun generateHeatmap(distribution: FocusDistributionResponse) {
        runOnUiThread {
            // 清除现有内容
            dailyDistributionContainer!!.removeAllViews()

            // 创建月份标题行
            val monthRow = LinearLayout(this)
            monthRow.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            monthRow.orientation = LinearLayout.HORIZONTAL

            // 添加空白单元格，对应左侧星期标签
            val emptyCell = View(this)
            emptyCell.layoutParams = LinearLayout.LayoutParams(
                resources.getDimensionPixelSize(R.dimen.heatmap_day_width),
                resources.getDimensionPixelSize(R.dimen.heatmap_cell_height)
            )
            monthRow.addView(emptyCell)

            // 根据分布类型生成相应的热力图
            when (distribution.type) {
                "day" -> generateDailyHeatmap(distribution.data, monthRow)
                "week" -> generateWeeklyHeatmap(distribution.data, monthRow)
                "month" -> generateMonthlyHeatmap(distribution.data, monthRow)
                else -> generateCustomHeatmap(distribution.data, monthRow)
            }

            dailyDistributionContainer!!.addView(monthRow)

            // 添加图例行
            addHeatmapLegend()
        }
    }

    private fun generateDailyHeatmap(data: List<FocusDistributionData>, monthRow: LinearLayout) {
        // 日视图按小时显示
        val hours = Array(24) { "$it:00" }
        for (hour in hours) {
            val hourText = TextView(this)
            hourText.layoutParams = LinearLayout.LayoutParams(
                resources.getDimensionPixelSize(R.dimen.heatmap_cell_width),
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            hourText.text = hour
            hourText.textAlignment = View.TEXT_ALIGNMENT_CENTER
            hourText.textSize = 10f
            monthRow.addView(hourText)
        }

        // 创建活动行
        val activityRow = LinearLayout(this)
        activityRow.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        activityRow.orientation = LinearLayout.HORIZONTAL

        // 添加"活动"标签
        val activityLabel = TextView(this)
        activityLabel.layoutParams = LinearLayout.LayoutParams(
            resources.getDimensionPixelSize(R.dimen.heatmap_day_width),
            resources.getDimensionPixelSize(R.dimen.heatmap_cell_height)
        )
        activityLabel.text = "活动"
        activityLabel.textSize = 12f
        activityRow.addView(activityLabel)

        // 将API数据映射到24小时
        val hourlyLevels = IntArray(24) { 0 }
        for (item in data) {
            try {
                val time = item.date.split(":")[0].toInt()
                if (time in 0..23) {
                    hourlyLevels[time] = item.level
                }
            } catch (e: Exception) {
                Log.e(TAG, "解析日期失败: ${item.date}", e)
            }
        }

        // 为每个小时添加热力图单元格
        for (hour in 0..23) {
            val cell = createHeatmapCell(hourlyLevels[hour])
            activityRow.addView(cell)
        }

        dailyDistributionContainer!!.addView(activityRow)
    }

    private fun generateWeeklyHeatmap(data: List<FocusDistributionData>, monthRow: LinearLayout) {
        // 周视图按天显示
        val weekdays = arrayOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
        for (day in weekdays) {
            val dayText = TextView(this)
            dayText.layoutParams = LinearLayout.LayoutParams(
                resources.getDimensionPixelSize(R.dimen.heatmap_cell_width),
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            dayText.text = day
            dayText.textAlignment = View.TEXT_ALIGNMENT_CENTER
            dayText.textSize = 10f
            monthRow.addView(dayText)
        }

        // 创建时间段行
        val timeSlots = arrayOf("上午", "下午", "晚上")
        for (timeslot in timeSlots) {
            val timeRow = LinearLayout(this)
            timeRow.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            timeRow.orientation = LinearLayout.HORIZONTAL

            // 添加时间段标签
            val timeLabel = TextView(this)
            timeLabel.layoutParams = LinearLayout.LayoutParams(
                resources.getDimensionPixelSize(R.dimen.heatmap_day_width),
                resources.getDimensionPixelSize(R.dimen.heatmap_cell_height)
            )
            timeLabel.text = timeslot
            timeLabel.textSize = 12f
            timeRow.addView(timeLabel)

            // 将API数据映射到周视图
            val weekData = data.filter { it.date.contains(timeslot) }
            val dailyLevels = IntArray(7) { 0 }

            for (item in weekData) {
                try {
                    // 假设API返回的日期格式为 "周一-上午" 这样的格式
                    val dayPart = item.date.split("-")[0]
                    val dayIndex = weekdays.indexOf(dayPart)
                    if (dayIndex != -1) {
                        dailyLevels[dayIndex] = item.level
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "解析周数据失败: ${item.date}", e)
                }
            }

            // 为每一天添加热力图单元格
            for (day in 0..6) {
                val cell = createHeatmapCell(dailyLevels[day])
                timeRow.addView(cell)
            }

            dailyDistributionContainer!!.addView(timeRow)
        }
    }

    private fun generateMonthlyHeatmap(data: List<FocusDistributionData>, monthRow: LinearLayout) {
        // 月视图按周和天显示
        val daysInMonth = 31 // 假设当前月有31天

        // 添加日期标签（1-31）
        for (day in 1..daysInMonth) {
            val dayText = TextView(this)
            dayText.layoutParams = LinearLayout.LayoutParams(
                resources.getDimensionPixelSize(R.dimen.heatmap_cell_width),
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            dayText.text = day.toString()
            dayText.textAlignment = View.TEXT_ALIGNMENT_CENTER
            dayText.textSize = 10f
            monthRow.addView(dayText)
        }

        // 创建活动行
        val activityRow = LinearLayout(this)
        activityRow.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        activityRow.orientation = LinearLayout.HORIZONTAL

        // 添加"活动"标签
        val activityLabel = TextView(this)
        activityLabel.layoutParams = LinearLayout.LayoutParams(
            resources.getDimensionPixelSize(R.dimen.heatmap_day_width),
            resources.getDimensionPixelSize(R.dimen.heatmap_cell_height)
        )
        activityLabel.text = "活动"
        activityLabel.textSize = 12f
        activityRow.addView(activityLabel)

        // 将API数据映射到月视图
        val dailyLevels = IntArray(31) { 0 }

        for (item in data) {
            try {
                // 假设API返回的日期格式为 "2023-04-15" 这样的格式
                val day = item.date.split("-").last().toInt()
                if (day in 1..31) {
                    dailyLevels[day - 1] = item.level
                }
            } catch (e: Exception) {
                Log.e(TAG, "解析月数据失败: ${item.date}", e)
            }
        }

        // 为每一天添加热力图单元格
        for (day in 0..30) {
            val cell = createHeatmapCell(dailyLevels[day])
            activityRow.addView(cell)
        }

        dailyDistributionContainer!!.addView(activityRow)
    }

    private fun generateCustomHeatmap(data: List<FocusDistributionData>, monthRow: LinearLayout) {
        // 自定义视图可以根据需求定制，这里简单实现为一个基于日期的线性展示

        // 添加日期标签
        for (item in data) {
            val dateText = TextView(this)
            dateText.layoutParams = LinearLayout.LayoutParams(
                resources.getDimensionPixelSize(R.dimen.heatmap_cell_width),
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            dateText.text = item.date
            dateText.textAlignment = View.TEXT_ALIGNMENT_CENTER
            dateText.textSize = 10f
            monthRow.addView(dateText)
        }

        // 创建活动行
        val activityRow = LinearLayout(this)
        activityRow.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        activityRow.orientation = LinearLayout.HORIZONTAL

        // 添加"活动"标签
        val activityLabel = TextView(this)
        activityLabel.layoutParams = LinearLayout.LayoutParams(
            resources.getDimensionPixelSize(R.dimen.heatmap_day_width),
            resources.getDimensionPixelSize(R.dimen.heatmap_cell_height)
        )
        activityLabel.text = "活动"
        activityLabel.textSize = 12f
        activityRow.addView(activityLabel)

        // 为每个数据点添加热力图单元格
        for (item in data) {
            val cell = createHeatmapCell(item.level)
            activityRow.addView(cell)
        }

        dailyDistributionContainer!!.addView(activityRow)
    }

    private fun createHeatmapCell(level: Int): View {
        val cell = View(this)
        val cellParams = LinearLayout.LayoutParams(
            resources.getDimensionPixelSize(R.dimen.heatmap_cell_width),
            resources.getDimensionPixelSize(R.dimen.heatmap_cell_height)
        )
        cellParams.setMargins(2, 2, 2, 2)
        cell.layoutParams = cellParams

        // 根据活动级别设置颜色
        when (level) {
            0 -> cell.setBackgroundColor(Color.parseColor("#ebedf0")) // 无活动
            1 -> cell.setBackgroundColor(Color.parseColor("#c6e48b")) // 低活动
            2 -> cell.setBackgroundColor(Color.parseColor("#7bc96f")) // 中等活动
            3 -> cell.setBackgroundColor(Color.parseColor("#239a3b")) // 高活动
            4 -> cell.setBackgroundColor(Color.parseColor("#196127")) // 非常高活动
            else -> cell.setBackgroundColor(Color.parseColor("#ebedf0")) // 默认无活动
        }

        return cell
    }

    private fun addHeatmapLegend() {
        // 添加图例行
        val legendRow = LinearLayout(this)
        legendRow.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        legendRow.orientation = LinearLayout.HORIZONTAL
        legendRow.setPadding(0, 20, 0, 0)

        // 添加说明文本
        val legendText = TextView(this)
        legendText.layoutParams = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1f
        )
        legendText.text = "任务完成数量："
        legendText.textSize = 12f
        legendRow.addView(legendText)

        // 添加图例项
        val legendLabels = arrayOf("少", "", "", "", "多")
        val legendColors = intArrayOf(
            Color.parseColor("#ebedf0"),
            Color.parseColor("#c6e48b"),
            Color.parseColor("#7bc96f"),
            Color.parseColor("#239a3b"),
            Color.parseColor("#196127")
        )

        val legendItems = LinearLayout(this)
        legendItems.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        legendItems.orientation = LinearLayout.HORIZONTAL

        for (i in legendLabels.indices) {
            val labelText = TextView(this)
            labelText.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            labelText.text = legendLabels[i]
            labelText.textSize = 12f

            val colorBox = View(this)
            val boxParams = LinearLayout.LayoutParams(
                resources.getDimensionPixelSize(R.dimen.heatmap_cell_width),
                resources.getDimensionPixelSize(R.dimen.heatmap_cell_height)
            )
            boxParams.setMargins(5, 0, 5, 0)
            colorBox.layoutParams = boxParams
            colorBox.setBackgroundColor(legendColors[i])

            legendItems.addView(labelText)
            legendItems.addView(colorBox)
        }

        legendRow.addView(legendItems)
        dailyDistributionContainer!!.addView(legendRow)
    }

    private fun setupMonthlyChart(monthlyData: List<MonthlyDistributionData>) {
        runOnUiThread {
            // 设置月度折线图
            val entries: MutableList<Entry> = ArrayList()

            // 使用API获取的月度数据
            for (dataPoint in monthlyData) {
                // 将分钟转换为小时
                val hours = dataPoint.duration_minutes / 60f
                entries.add(Entry(dataPoint.day.toFloat(), hours))
            }

            val dataSet = LineDataSet(entries, "每日任务时长 (小时)")
            dataSet.color = Color.BLUE
            dataSet.setCircleColor(Color.BLUE)
            dataSet.valueTextSize = 10f
            dataSet.lineWidth = 2f

            val lineData = LineData(dataSet)
            monthlyChart!!.data = lineData

            // 自定义X轴
            val xAxis = monthlyChart!!.xAxis
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.granularity = 1f

            // 设置当前月份的天数标签
            val cal = Calendar.getInstance()
            cal.time = calendar.time
            val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
            val days = arrayOfNulls<String>(daysInMonth + 1)
            for (i in 1..daysInMonth) {
                days[i] = i.toString()
            }
            xAxis.valueFormatter = IndexAxisValueFormatter(days)
            xAxis.axisMinimum = 1f
            xAxis.axisMaximum = daysInMonth.toFloat()

            monthlyChart!!.description.isEnabled = false
            monthlyChart!!.animateX(1000)
            monthlyChart!!.invalidate()
        }
    }

    private fun setupYearlyChart(yearlyData: List<YearlyDistributionData>) {
        runOnUiThread {
            // 设置年度折线图
            val entries: MutableList<Entry> = ArrayList()

            // 使用API获取的年度数据
            for (dataPoint in yearlyData) {
                // 将分钟转换为小时
                val hours = dataPoint.duration_minutes / 60f
                entries.add(Entry(dataPoint.month.toFloat(), hours))
            }

            val dataSet = LineDataSet(entries, "月平均任务时长 (小时)")
            dataSet.color = Color.GREEN
            dataSet.setCircleColor(Color.GREEN)
            dataSet.valueTextSize = 10f
            dataSet.lineWidth = 2f

            val lineData = LineData(dataSet)
            yearlyChart!!.data = lineData

            // 自定义X轴
            val xAxis = yearlyChart!!.xAxis
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.granularity = 1f

            // 设置月份标签
            val months = arrayOf(
                "", "1月", "2月", "3月", "4月", "5月", "6月",
                "7月", "8月", "9月", "10月", "11月", "12月"
            )
            xAxis.valueFormatter = IndexAxisValueFormatter(months)
            xAxis.axisMinimum = 1f
            xAxis.axisMaximum = 12f

            yearlyChart!!.description.isEnabled = false
            yearlyChart!!.animateX(1000)
            yearlyChart!!.invalidate()
        }
    }

    private fun setupDistributionButtons() {
        btnDaily!!.setOnClickListener {
            currentDistributionType = "day"
            updateButtonSelection(
                btnDaily!!,
                btnWeekly!!, btnMonthly!!
            )
            updateDateTitles()
            lifecycleScope.launch {
                loadFocusDistribution(currentDistributionType)
            }
        }

        btnWeekly!!.setOnClickListener {
            currentDistributionType = "week"
            updateButtonSelection(
                btnWeekly!!,
                btnDaily!!, btnMonthly!!
            )
            updateDateTitles()
            lifecycleScope.launch {
                loadFocusDistribution(currentDistributionType)
            }
        }

        btnMonthly!!.setOnClickListener {
            currentDistributionType = "month"
            updateButtonSelection(
                btnMonthly!!,
                btnDaily!!, btnWeekly!!
            )
            updateDateTitles()
            lifecycleScope.launch {
                loadFocusDistribution(currentDistributionType)
            }
        }

        // 默认选中日视图
        updateButtonSelection(btnDaily!!, btnWeekly!!, btnMonthly!!)
    }

    private fun setupMonthYearButtons() {
        btnMonthView!!.setOnClickListener {
            isYearView = false
            updateButtonSelection(btnMonthView!!, btnYearView!!)
            monthlyViewContainer!!.visibility = View.VISIBLE
            yearlyViewContainer!!.visibility = View.GONE
            updateDateTitles()
            lifecycleScope.launch {
                loadMonthlyFocusData()
            }
        }

        btnYearView!!.setOnClickListener {
            isYearView = true
            updateButtonSelection(btnYearView!!, btnMonthView!!)
            monthlyViewContainer!!.visibility = View.GONE
            yearlyViewContainer!!.visibility = View.VISIBLE
            updateDateTitles()
            lifecycleScope.launch {
                loadYearlyFocusData()
            }
        }

        // 默认选中月视图
        updateButtonSelection(btnMonthView!!, btnYearView!!)
        monthlyViewContainer!!.visibility = View.VISIBLE
        yearlyViewContainer!!.visibility = View.GONE
    }

    private fun setupDateNavigationButtons() {
        btnPrevDate!!.setOnClickListener {
            navigateDate(-1)
        }

        btnNextDate!!.setOnClickListener {
            navigateDate(1)
        }

        btnPrevMonth!!.setOnClickListener {
            navigateMonth(-1)
        }

        btnNextMonth!!.setOnClickListener {
            navigateMonth(1)
        }
    }

    private fun navigateDate(direction: Int) {
        // 根据当前视图类型调整日期
        when (currentDistributionType) {
            "day" -> calendar.add(Calendar.DAY_OF_MONTH, direction)
            "week" -> calendar.add(Calendar.WEEK_OF_YEAR, direction)
            "month" -> calendar.add(Calendar.MONTH, direction)
            else -> calendar.add(Calendar.DAY_OF_MONTH, direction)
        }

        updateDateTitles()

        // 重新加载数据
        lifecycleScope.launch {
            loadFocusDistribution(currentDistributionType)
        }
    }

    private fun navigateMonth(direction: Int) {
        // 调整月份或年份
        if (isYearView) {
            calendar.add(Calendar.YEAR, direction)
            lifecycleScope.launch {
                loadYearlyFocusData()
            }
        } else {
            calendar.add(Calendar.MONTH, direction)
            lifecycleScope.launch {
                loadMonthlyFocusData()
            }
        }

        updateDateTitles()
    }

    private fun updateDateTitles() {
        // 更新专注时长分布标题
        val distributionDate = when (currentDistributionType) {
            "day" -> dateFormatter.format(calendar.time)
            "week" -> {
                val tempCal = Calendar.getInstance()
                tempCal.time = calendar.time
                tempCal.set(Calendar.DAY_OF_WEEK, tempCal.firstDayOfWeek)
                val startDate = dateFormatter.format(tempCal.time)
                tempCal.add(Calendar.DAY_OF_WEEK, 6)
                val endDate = dateFormatter.format(tempCal.time)
                "$startDate 至 $endDate"
            }
            "month" -> monthFormatter.format(calendar.time)
            else -> dateFormatter.format(calendar.time)
        }
        val distributionTitle = when (currentDistributionType) {
            "day" -> "专注时长分布（日）$distributionDate"
            "week" -> "专注时长分布（周）$distributionDate"
            "month" -> "专注时长分布（月）$distributionDate"
            else -> "专注时长分布（自定义）$distributionDate"
        }
        tvDistributionTitle?.text = distributionTitle

        // 更新月度/年度视图标题
        val monthlyTitle = if (isYearView) {
            "年度专注时段分布 ${yearFormatter.format(calendar.time)}"
        } else {
            "月度专注时段分布 ${monthFormatter.format(calendar.time)}"
        }
        tvMonthlyTitle?.text = monthlyTitle
    }

    private fun updateButtonSelection(selectedButton: Button, vararg otherButtons: Button) {
        selectedButton.background =
            ContextCompat.getDrawable(this, R.drawable.selected_button_background)
        selectedButton.setTextColor(ContextCompat.getColor(this, android.R.color.white))

        for (button in otherButtons) {
            button.background =
                ContextCompat.getDrawable(
                    this,
                    R.drawable.unselected_button_background
                )
            button.setTextColor(ContextCompat.getColor(this, android.R.color.black))
        }
    }
}