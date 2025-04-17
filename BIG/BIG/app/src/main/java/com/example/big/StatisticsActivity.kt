package com.example.big

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import java.util.Random

class StatisticsActivity : AppCompatActivity() {
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_statistics)

        // 初始化视图
        initViews()

        // 设置返回按钮
        findViewById<View>(R.id.back_button).setOnClickListener { v: View? -> finish() }

        // 加载统计数据
        loadStatistics()

        // 设置任务分布切换按钮
        setupDistributionButtons()

        // 设置月度/年度视图切换按钮
        setupMonthYearButtons()
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
        btnCustom = findViewById(R.id.btn_custom)

        // 月度/年度数据视图
        monthlyViewContainer = findViewById(R.id.monthly_view_container)
        yearlyViewContainer = findViewById(R.id.yearly_view_container)
        btnMonthView = findViewById(R.id.btn_month_view)
        btnYearView = findViewById(R.id.btn_year_view)
        monthlyChart = findViewById(R.id.monthly_chart)
        yearlyChart = findViewById(R.id.yearly_chart)
    }

    private fun loadStatistics() {
        // 加载今日任务数据（硬编码示例数据）
        val todayTasksCount = findViewById<TextView>(R.id.today_tasks_count)
        val todayCompletedCount = findViewById<TextView>(R.id.today_completed_count)
        val todayDuration = findViewById<TextView>(R.id.today_duration)

        todayTasksCount.text = "5"
        todayCompletedCount.text = "3"
        todayDuration.text = "2小时30分钟"

        // 加载累计任务数据（硬编码示例数据）
        val totalCount = findViewById<TextView>(R.id.total_count)
        val totalDuration = findViewById<TextView>(R.id.total_duration)
        val dailyAvgDuration = findViewById<TextView>(R.id.daily_avg_duration)

        totalCount.text = "15"
        totalDuration.text = "21小时40分钟"
        dailyAvgDuration.text = "1小时58分钟"

        // 生成并显示任务分布热力图
        generateHeatmap()

        // 设置月度/年度图表
        setupMonthlyChart()
        setupYearlyChart()
    }

    private fun generateHeatmap() {
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

        // 添加月份标签
        val months = arrayOf(
            "一月", "二月", "三月", "四月", "五月", "六月",
            "七月", "八月", "九月", "十月", "十一月", "十二月"
        )
        for (month in months) {
            val monthText = TextView(this)
            monthText.layoutParams = LinearLayout.LayoutParams(
                resources.getDimensionPixelSize(R.dimen.heatmap_month_width),
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            monthText.text = month
            monthText.textAlignment = View.TEXT_ALIGNMENT_CENTER
            monthText.textSize = 12f
            monthRow.addView(monthText)
        }
        dailyDistributionContainer!!.addView(monthRow)

        // 创建星期行
        val weekdays = arrayOf("周一", "周三", "周五")
        for (weekday in weekdays.indices) {
            val weekRow = LinearLayout(this)
            weekRow.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            weekRow.orientation = LinearLayout.HORIZONTAL

            // 添加星期标签
            val weekdayText = TextView(this)
            weekdayText.layoutParams = LinearLayout.LayoutParams(
                resources.getDimensionPixelSize(R.dimen.heatmap_day_width),
                resources.getDimensionPixelSize(R.dimen.heatmap_cell_height)
            )
            weekdayText.text = weekdays[weekday]
            weekdayText.textSize = 12f
            weekRow.addView(weekdayText)

            // 为每一天生成热力图单元格
            val random = Random()
            for (month in 0..11) {
                // 每个月添加4个单元格
                for (day in 0..3) {
                    val cell = View(this)
                    val cellParams = LinearLayout.LayoutParams(
                        resources.getDimensionPixelSize(R.dimen.heatmap_cell_width),
                        resources.getDimensionPixelSize(R.dimen.heatmap_cell_height)
                    )
                    cellParams.setMargins(2, 2, 2, 2)
                    cell.layoutParams = cellParams

                    // 随机生成活动级别（0-4）
                    val level = random.nextInt(5)
                    when (level) {
                        0 -> cell.setBackgroundColor(Color.parseColor("#ebedf0")) // 无活动
                        1 -> cell.setBackgroundColor(Color.parseColor("#c6e48b")) // 低活动
                        2 -> cell.setBackgroundColor(Color.parseColor("#7bc96f")) // 中等活动
                        3 -> cell.setBackgroundColor(Color.parseColor("#239a3b")) // 高活动
                        4 -> cell.setBackgroundColor(Color.parseColor("#196127")) // 非常高活动
                    }

                    weekRow.addView(cell)
                }
            }
            dailyDistributionContainer!!.addView(weekRow)
        }

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

    private fun setupMonthlyChart() {
        // 设置月度折线图
        val entries: MutableList<Entry> = ArrayList()

        // 生成月度数据（示例数据，每天的任务时长）
        for (i in 1..30) {
            val hours = (Math.random() * 5).toFloat()
            entries.add(Entry(i.toFloat(), hours))
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
        val days = arrayOfNulls<String>(31)
        for (i in 0..30) {
            days[i] = (i + 1).toString()
        }
        xAxis.valueFormatter = IndexAxisValueFormatter(days)

        monthlyChart!!.description.isEnabled = false
        monthlyChart!!.animateX(1000)
        monthlyChart!!.invalidate()
    }

    private fun setupYearlyChart() {
        // 设置年度折线图
        val entries: MutableList<Entry> = ArrayList()

        // 生成年度数据（示例数据，每月的平均任务时长）
        for (i in 1..12) {
            val hours = (Math.random() * 3 + 1).toFloat()
            entries.add(Entry(i.toFloat(), hours))
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
            "1月", "2月", "3月", "4月", "5月", "6月",
            "7月", "8月", "9月", "10月", "11月", "12月"
        )
        xAxis.valueFormatter = IndexAxisValueFormatter(months)

        yearlyChart!!.description.isEnabled = false
        yearlyChart!!.animateX(1000)
        yearlyChart!!.invalidate()
    }

    private fun setupDistributionButtons() {
        btnDaily!!.setOnClickListener { v: View? ->
            updateButtonSelection(
                btnDaily!!,
                btnWeekly!!, btnMonthly!!, btnCustom!!
            )
            generateHeatmap() // 可以根据需要传递不同的参数以显示不同的视图
        }

        btnWeekly!!.setOnClickListener { v: View? ->
            updateButtonSelection(
                btnWeekly!!,
                btnDaily!!, btnMonthly!!, btnCustom!!
            )
            generateHeatmap()
        }

        btnMonthly!!.setOnClickListener { v: View? ->
            updateButtonSelection(
                btnMonthly!!,
                btnDaily!!, btnWeekly!!, btnCustom!!
            )
            generateHeatmap()
        }

        btnCustom!!.setOnClickListener { v: View? ->
            updateButtonSelection(
                btnCustom!!,
                btnDaily!!, btnWeekly!!, btnMonthly!!
            )
            generateHeatmap()
        }

        // 默认选中日视图
        updateButtonSelection(btnDaily!!, btnWeekly!!, btnMonthly!!, btnCustom!!)
    }

    private fun setupMonthYearButtons() {
        btnMonthView!!.setOnClickListener { v: View? ->
            updateButtonSelection(btnMonthView!!, btnYearView!!)
            monthlyViewContainer!!.visibility = View.VISIBLE
            yearlyViewContainer!!.visibility = View.GONE
        }

        btnYearView!!.setOnClickListener { v: View? ->
            updateButtonSelection(btnYearView!!, btnMonthView!!)
            monthlyViewContainer!!.visibility = View.GONE
            yearlyViewContainer!!.visibility = View.VISIBLE
        }

        // 默认选中月视图
        updateButtonSelection(btnMonthView!!, btnYearView!!)
        monthlyViewContainer!!.visibility = View.VISIBLE
        yearlyViewContainer!!.visibility = View.GONE
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