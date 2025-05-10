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
import com.example.big.utils.HeatmapUtils

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

    // 缓存热力图数据，用于折线图显示
    private var dailyHeatmapData: Map<String, Int> = HashMap()
    private var monthlyHeatmapData: Map<String, Int> = HashMap()

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
        // btnPrevDate = findViewById(R.id.btn_prev_date)
        // btnNextDate = findViewById(R.id.btn_next_date)
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
                    // 获取月视图数据用于年度折线图
                    loadFocusDistribution("month", true)
                } else {
                    // 获取日视图数据用于月度折线图
                    loadFocusDistribution("day", true)
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

    private suspend fun loadFocusDistribution(type: String, isForChart: Boolean = false) {
        // 根据当前选择的日期范围构建参数
        val startDate = when (type) {
            "day" -> {
                // 对于日视图，使用当前月的第一天（用于获取整月数据）
                val tempCal = Calendar.getInstance()
                tempCal.time = calendar.time
                tempCal.set(Calendar.DAY_OF_MONTH, 1)
                dateFormatter.format(tempCal.time)
            }
            "week" -> {
                // 对于周视图，获取当前周的第一天
                val tempCal = Calendar.getInstance()
                tempCal.time = calendar.time
                tempCal.set(Calendar.DAY_OF_WEEK, tempCal.firstDayOfWeek)
                dateFormatter.format(tempCal.time)
            }
            "month" -> {
                // 对于月视图，获取当前年的第一天（用于获取整年数据）
                val tempCal = Calendar.getInstance()
                tempCal.time = calendar.time
                tempCal.set(Calendar.MONTH, 0)
                tempCal.set(Calendar.DAY_OF_MONTH, 1)
                dateFormatter.format(tempCal.time)
            }
            else -> dateFormatter.format(calendar.time)
        }

        Log.d(TAG, "Loading focus distribution for $type, start date: $startDate")

        // 调用API获取分布数据
        val result = TaskManager.getFocusDistribution(type, startDate)
        when (result) {
            is TaskManager.Result.Success -> {
                val distribution = result.data
                Log.d(TAG, "Received distribution data for $type: ${distribution.data}")

                // 缓存数据用于图表
                if (type == "day") {
                    dailyHeatmapData = distribution.data
                    // 如果这是为折线图获取的数据，更新月度折线图
                    if (isForChart && !isYearView) {
                        setupMonthlyChart(dailyHeatmapData)
                    }
                } else if (type == "month") {
                    monthlyHeatmapData = distribution.data
                    // 如果这是为折线图获取的数据，更新年度折线图
                    if (isForChart && isYearView) {
                        setupYearlyChart(monthlyHeatmapData)
                    }
                }

                // 只有当不是为图表获取数据时，才更新热力图
                if (!isForChart) {
                    runOnUiThread {
                        // 重要：确保正确设置period
                        distribution.period = type
                        generateHeatmap(distribution)
                    }
                }
            }
            is TaskManager.Result.Error -> {
                Log.e(TAG, "获取专注时长分布失败: ${result.message}")
                runOnUiThread {
                    Toast.makeText(applicationContext, "获取专注时长分布失败", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun generateHeatmap(distribution: FocusDistributionResponse) {
        // 清除现有内容
        dailyDistributionContainer!!.removeAllViews()

        Log.d(TAG, "Generating heatmap for period: ${distribution.period}")

        when (distribution.period) {
            "day" -> generateDailyHeatmap(distribution.data)
            "week" -> generateWeeklyHeatmap(distribution.data)
            "month" -> generateMonthlyHeatmap(distribution.data)
            else -> generateDailyHeatmap(distribution.data) // 默认日视图
        }

        // 添加图例
        addHeatmapLegend()
    }

    // 生成日视图热力图 - 显示一个月的日期数据
    private fun generateDailyHeatmap(data: Map<String, Int>) {
        Log.d(TAG, "Generating DAILY heatmap")
        // 获取当前年月
        val currentYearMonth = monthFormatter.format(calendar.time)

        // 创建日期标签行
        val labelRow = LinearLayout(this)
        labelRow.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        labelRow.orientation = LinearLayout.HORIZONTAL

        // 添加空白标签
        val emptyLabel = TextView(this)
        emptyLabel.layoutParams = LinearLayout.LayoutParams(
            resources.getDimensionPixelSize(R.dimen.heatmap_day_width),
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        emptyLabel.text = ""
        labelRow.addView(emptyLabel)

        // 获取当月的天数
        val cal = Calendar.getInstance()
        cal.time = calendar.time
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

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
            labelRow.addView(dayText)
        }
        dailyDistributionContainer!!.addView(labelRow)

        // 创建热力图行
        val heatmapRow = LinearLayout(this)
        heatmapRow.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        heatmapRow.orientation = LinearLayout.HORIZONTAL

        // 添加"专注"标签
        val focusLabel = TextView(this)
        focusLabel.layoutParams = LinearLayout.LayoutParams(
            resources.getDimensionPixelSize(R.dimen.heatmap_day_width),
            resources.getDimensionPixelSize(R.dimen.heatmap_cell_height)
        )
        focusLabel.text = "专注"
        focusLabel.textSize = 12f
        heatmapRow.addView(focusLabel)

        // 获取当前年月
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) + 1

        // 为每一天添加热力图单元格
        for (day in 1..daysInMonth) {
            val dateKey = String.format("%d-%02d-%02d", year, month, day)
            val duration = data[dateKey] ?: 0
            val cell = HeatmapUtils.createHeatmapCell(this, duration)
            heatmapRow.addView(cell)
        }

        dailyDistributionContainer!!.addView(heatmapRow)

        // 如果当前在月度视图，更新月度折线图
        if (!isYearView) {
            setupMonthlyChart(data)
        }
    }

    // 生成周视图热力图 - 显示一年的周数据
    private fun generateWeeklyHeatmap(data: Map<String, Int>) {
        Log.d(TAG, "Generating WEEKLY heatmap")
        // 获取当前年份
        val currentYear = yearFormatter.format(calendar.time)

        // 创建半年标签行（每年约52周，我们每行显示26周）
        val weekLabelsRow1 = LinearLayout(this)
        weekLabelsRow1.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        weekLabelsRow1.orientation = LinearLayout.HORIZONTAL

        val weekLabelsRow2 = LinearLayout(this)
        weekLabelsRow2.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        weekLabelsRow2.orientation = LinearLayout.HORIZONTAL

        // 添加空白标签
        val emptyLabel1 = TextView(this)
        emptyLabel1.layoutParams = LinearLayout.LayoutParams(
            resources.getDimensionPixelSize(R.dimen.heatmap_day_width),
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        emptyLabel1.text = "上半年"
        weekLabelsRow1.addView(emptyLabel1)

        val emptyLabel2 = TextView(this)
        emptyLabel2.layoutParams = LinearLayout.LayoutParams(
            resources.getDimensionPixelSize(R.dimen.heatmap_day_width),
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        emptyLabel2.text = "下半年"
        weekLabelsRow2.addView(emptyLabel2)

        // 添加周数标签
        for (week in 1..26) {
            val weekText = TextView(this)
            weekText.layoutParams = LinearLayout.LayoutParams(
                resources.getDimensionPixelSize(R.dimen.heatmap_cell_width),
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            weekText.text = week.toString()
            weekText.textAlignment = View.TEXT_ALIGNMENT_CENTER
            weekText.textSize = 10f
            weekLabelsRow1.addView(weekText)
        }

        for (week in 27..52) {
            val weekText = TextView(this)
            weekText.layoutParams = LinearLayout.LayoutParams(
                resources.getDimensionPixelSize(R.dimen.heatmap_cell_width),
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            weekText.text = week.toString()
            weekText.textAlignment = View.TEXT_ALIGNMENT_CENTER
            weekText.textSize = 10f
            weekLabelsRow2.addView(weekText)
        }

        dailyDistributionContainer!!.addView(weekLabelsRow1)

        // 创建上半年热力图行
        val heatmapRow1 = LinearLayout(this)
        heatmapRow1.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        heatmapRow1.orientation = LinearLayout.HORIZONTAL

        // 添加"专注"标签
        val focusLabel1 = TextView(this)
        focusLabel1.layoutParams = LinearLayout.LayoutParams(
            resources.getDimensionPixelSize(R.dimen.heatmap_day_width),
            resources.getDimensionPixelSize(R.dimen.heatmap_cell_height)
        )
        focusLabel1.text = "专注"
        focusLabel1.textSize = 12f
        heatmapRow1.addView(focusLabel1)

        // 为上半年每周添加热力图单元格
        for (week in 1..26) {
            val weekKey = String.format("%s-W%02d", currentYear, week)
            val duration = data[weekKey] ?: 0
            val cell = HeatmapUtils.createHeatmapCell(this, duration)
            heatmapRow1.addView(cell)
        }

        dailyDistributionContainer!!.addView(heatmapRow1)
        dailyDistributionContainer!!.addView(weekLabelsRow2)

        // 创建下半年热力图行
        val heatmapRow2 = LinearLayout(this)
        heatmapRow2.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        heatmapRow2.orientation = LinearLayout.HORIZONTAL

        // 添加"专注"标签
        val focusLabel2 = TextView(this)
        focusLabel2.layoutParams = LinearLayout.LayoutParams(
            resources.getDimensionPixelSize(R.dimen.heatmap_day_width),
            resources.getDimensionPixelSize(R.dimen.heatmap_cell_height)
        )
        focusLabel2.text = "专注"
        focusLabel2.textSize = 12f
        heatmapRow2.addView(focusLabel2)

        // 为下半年每周添加热力图单元格
        for (week in 27..52) {
            val weekKey = String.format("%s-W%02d", currentYear, week)
            val duration = data[weekKey] ?: 0
            val cell = HeatmapUtils.createHeatmapCell(this, duration)
            heatmapRow2.addView(cell)
        }

        dailyDistributionContainer!!.addView(heatmapRow2)
    }

    // 生成月视图热力图 - 显示一年的月数据
    private fun generateMonthlyHeatmap(data: Map<String, Int>) {
        Log.d(TAG, "Generating MONTHLY heatmap")
        // 获取当前年份
        val currentYear = yearFormatter.format(calendar.time)

        // 创建月份标签行
        val labelRow = LinearLayout(this)
        labelRow.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        labelRow.orientation = LinearLayout.HORIZONTAL

        // 添加空白标签
        val emptyLabel = TextView(this)
        emptyLabel.layoutParams = LinearLayout.LayoutParams(
            resources.getDimensionPixelSize(R.dimen.heatmap_day_width),
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        emptyLabel.text = ""
        labelRow.addView(emptyLabel)

        // 添加月份标签
        val months = arrayOf("1月", "2月", "3月", "4月", "5月", "6月",
            "7月", "8月", "9月", "10月", "11月", "12月")

        for (month in months) {
            val monthText = TextView(this)
            monthText.layoutParams = LinearLayout.LayoutParams(
                resources.getDimensionPixelSize(R.dimen.heatmap_cell_width),
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            monthText.text = month
            monthText.textAlignment = View.TEXT_ALIGNMENT_CENTER
            monthText.textSize = 10f
            labelRow.addView(monthText)
        }
        dailyDistributionContainer!!.addView(labelRow)

        // 创建热力图行
        val heatmapRow = LinearLayout(this)
        heatmapRow.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        heatmapRow.orientation = LinearLayout.HORIZONTAL

        // 添加"专注"标签
        val focusLabel = TextView(this)
        focusLabel.layoutParams = LinearLayout.LayoutParams(
            resources.getDimensionPixelSize(R.dimen.heatmap_day_width),
            resources.getDimensionPixelSize(R.dimen.heatmap_cell_height)
        )
        focusLabel.text = "专注"
        focusLabel.textSize = 12f
        heatmapRow.addView(focusLabel)

        // 为每个月添加热力图单元格
        for (month in 1..12) {
            val monthKey = String.format("%s-%02d", currentYear, month)
            val duration = data[monthKey] ?: 0
            val cell = HeatmapUtils.createHeatmapCell(this, duration)
            heatmapRow.addView(cell)
        }

        dailyDistributionContainer!!.addView(heatmapRow)

        // 如果当前在年度视图，更新年度折线图
        if (isYearView) {
            setupYearlyChart(data)
        }
    }

    private fun setupMonthlyChart(data: Map<String, Int>) {
        try {
            runOnUiThread {
                // 设置月度折线图
                val entries: MutableList<Entry> = ArrayList()

                // 获取当前年月
                val cal = Calendar.getInstance()
                cal.time = calendar.time
                val year = cal.get(Calendar.YEAR)
                val month = cal.get(Calendar.MONTH) + 1
                val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

                // 为每一天添加数据点
                for (day in 1..daysInMonth) {
                    val dateKey = String.format("%d-%02d-%02d", year, month, day)
                    val duration = data[dateKey] ?: 0
                    // 将分钟转换为小时
                    val hours = duration / 60f
                    entries.add(Entry(day.toFloat(), hours))
                }

                if (entries.isEmpty()) {
                    Log.e(TAG, "月度专注数据为空")
                    Toast.makeText(applicationContext, "本月没有专注数据", Toast.LENGTH_SHORT).show()
                    return@runOnUiThread
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
        } catch (e: Exception) {
            Log.e(TAG, "设置月度图表时出错", e)
            runOnUiThread {
                Toast.makeText(applicationContext, "设置月度图表时出错: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupYearlyChart(data: Map<String, Int>) {
        try {
            runOnUiThread {
                // 设置年度折线图
                val entries: MutableList<Entry> = ArrayList()

                // 获取当前年份
                val currentYear = yearFormatter.format(calendar.time)

                // 为每个月添加数据点
                for (month in 1..12) {
                    val monthKey = String.format("%s-%02d", currentYear, month)
                    val duration = data[monthKey] ?: 0
                    // 将分钟转换为小时
                    val hours = duration / 60f
                    entries.add(Entry(month.toFloat(), hours))
                }

                if (entries.isEmpty()) {
                    Log.e(TAG, "年度专注数据为空")
                    Toast.makeText(applicationContext, "本年没有专注数据", Toast.LENGTH_SHORT).show()
                    return@runOnUiThread
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
        } catch (e: Exception) {
            Log.e(TAG, "设置年度图表时出错", e)
            runOnUiThread {
                Toast.makeText(applicationContext, "设置年度图表时出错: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupDistributionButtons() {
        btnDaily!!.setOnClickListener {
            currentDistributionType = "day"
            Log.d(TAG, "Switching to DAY view")
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
            Log.d(TAG, "Switching to WEEK view")
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
            Log.d(TAG, "Switching to MONTH view")
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
        legendText.text = "专注时长："
        legendText.textSize = 12f
        legendRow.addView(legendText)

        // 添加图例项
        val legendItems = LinearLayout(this)
        legendItems.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        legendItems.orientation = LinearLayout.HORIZONTAL

        val legendLabels = arrayOf("0分钟", "<30分钟", "<60分钟", "<2小时", "≥2小时")
        val intensities = intArrayOf(0, 15, 45, 90, 150)

        for (i in legendLabels.indices) {
            val colorBox = View(this)
            val boxParams = LinearLayout.LayoutParams(
                resources.getDimensionPixelSize(R.dimen.heatmap_cell_width),
                resources.getDimensionPixelSize(R.dimen.heatmap_cell_height)
            )
            boxParams.setMargins(5, 0, 5, 0)
            colorBox.layoutParams = boxParams
            colorBox.setBackgroundColor(HeatmapUtils.getHeatmapColor(intensities[i]))

            val labelText = TextView(this)
            labelText.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            labelText.text = legendLabels[i]
            labelText.textSize = 10f
            labelText.setPadding(0, 0, 10, 0)

            legendItems.addView(colorBox)
            legendItems.addView(labelText)
        }

        legendRow.addView(legendItems)
        dailyDistributionContainer!!.addView(legendRow)
    }

    private fun setupMonthYearButtons() {
        btnMonthView!!.setOnClickListener {
            isYearView = false
            updateButtonSelection(btnMonthView!!, btnYearView!!)
            monthlyViewContainer!!.visibility = View.VISIBLE
            yearlyViewContainer!!.visibility = View.GONE
            updateDateTitles()

            // 确保日视图数据已加载，用于月度折线图
            if (currentDistributionType != "day") {
                lifecycleScope.launch {
                    loadFocusDistribution("day", true)
                }
            } else if (dailyHeatmapData.isNotEmpty()) {
                // 如果数据已存在，直接使用
                setupMonthlyChart(dailyHeatmapData)
            }
        }

        btnYearView!!.setOnClickListener {
            isYearView = true
            updateButtonSelection(btnYearView!!, btnMonthView!!)
            monthlyViewContainer!!.visibility = View.GONE
            yearlyViewContainer!!.visibility = View.VISIBLE
            updateDateTitles()

            // 确保月视图数据已加载，用于年度折线图
            if (currentDistributionType != "month") {
                lifecycleScope.launch {
                    loadFocusDistribution("month", true)
                }
            } else if (monthlyHeatmapData.isNotEmpty()) {
                // 如果数据已存在，直接使用
                setupYearlyChart(monthlyHeatmapData)
            }
        }

        // 默认选中月视图
        updateButtonSelection(btnMonthView!!, btnYearView!!)
        monthlyViewContainer!!.visibility = View.VISIBLE
        yearlyViewContainer!!.visibility = View.GONE
    }

    private fun setupDateNavigationButtons() {
        /*
        btnPrevDate!!.setOnClickListener {
            navigateDate(-1)
        }

        btnNextDate!!.setOnClickListener {
            navigateDate(1)
        }
        */

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

            // 如果需要，为折线图获取数据
            if (isYearView && currentDistributionType != "month") {
                loadFocusDistribution("month", true)
            } else if (!isYearView && currentDistributionType != "day") {
                loadFocusDistribution("day", true)
            }
        }
    }

    private fun navigateMonth(direction: Int) {
        // 调整月份或年份
        if (isYearView) {
            calendar.add(Calendar.YEAR, direction)
            lifecycleScope.launch {
                // 为年度视图获取月视图数据
                loadFocusDistribution("month", true)
            }
        } else {
            calendar.add(Calendar.MONTH, direction)
            lifecycleScope.launch {
                // 为月度视图获取日视图数据
                loadFocusDistribution("day", true)
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
            "day" -> "专注时长分布（日）"
            "week" -> "专注时长分布（周）"
            "month" -> "专注时长分布（月）"
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