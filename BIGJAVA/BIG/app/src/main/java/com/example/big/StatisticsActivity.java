package com.example.big;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class StatisticsActivity extends AppCompatActivity {

    private LinearLayout dailyDistributionContainer;
    private LinearLayout monthlyViewContainer;
    private LinearLayout yearlyViewContainer;
    private LineChart monthlyChart;
    private LineChart yearlyChart;
    private Button btnDaily;
    private Button btnWeekly;
    private Button btnMonthly;
    private Button btnCustom;
    private Button btnMonthView;
    private Button btnYearView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);

        // 初始化视图
        initViews();

        // 设置返回按钮
        findViewById(R.id.back_button).setOnClickListener(v -> finish());

        // 加载统计数据
        loadStatistics();

        // 设置任务分布切换按钮
        setupDistributionButtons();

        // 设置月度/年度视图切换按钮
        setupMonthYearButtons();
    }

    private void initViews() {
        // 今日任务数据视图
        TextView todayTasksCount = findViewById(R.id.today_tasks_count);
        TextView todayCompletedCount = findViewById(R.id.today_completed_count);
        TextView todayDuration = findViewById(R.id.today_duration);

        // 累计任务数据视图
        TextView totalCount = findViewById(R.id.total_count);
        TextView totalDuration = findViewById(R.id.total_duration);
        TextView dailyAvgDuration = findViewById(R.id.daily_avg_duration);

        // 任务分布视图
        dailyDistributionContainer = findViewById(R.id.daily_distribution_container);
        btnDaily = findViewById(R.id.btn_daily);
        btnWeekly = findViewById(R.id.btn_weekly);
        btnMonthly = findViewById(R.id.btn_monthly);
        btnCustom = findViewById(R.id.btn_custom);

        // 月度/年度数据视图
        monthlyViewContainer = findViewById(R.id.monthly_view_container);
        yearlyViewContainer = findViewById(R.id.yearly_view_container);
        btnMonthView = findViewById(R.id.btn_month_view);
        btnYearView = findViewById(R.id.btn_year_view);
        monthlyChart = findViewById(R.id.monthly_chart);
        yearlyChart = findViewById(R.id.yearly_chart);
    }

    private void loadStatistics() {
        // 加载今日任务数据（硬编码示例数据）
        TextView todayTasksCount = findViewById(R.id.today_tasks_count);
        TextView todayCompletedCount = findViewById(R.id.today_completed_count);
        TextView todayDuration = findViewById(R.id.today_duration);

        todayTasksCount.setText("5");
        todayCompletedCount.setText("3");
        todayDuration.setText("2小时30分钟");

        // 加载累计任务数据（硬编码示例数据）
        TextView totalCount = findViewById(R.id.total_count);
        TextView totalDuration = findViewById(R.id.total_duration);
        TextView dailyAvgDuration = findViewById(R.id.daily_avg_duration);

        totalCount.setText("15");
        totalDuration.setText("21小时40分钟");
        dailyAvgDuration.setText("1小时58分钟");

        // 生成并显示任务分布热力图
        generateHeatmap();

        // 设置月度/年度图表
        setupMonthlyChart();
        setupYearlyChart();
    }

    private void generateHeatmap() {
        // 清除现有内容
        dailyDistributionContainer.removeAllViews();

        // 创建月份标题行
        LinearLayout monthRow = new LinearLayout(this);
        monthRow.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        monthRow.setOrientation(LinearLayout.HORIZONTAL);

        // 添加空白单元格，对应左侧星期标签
        View emptyCell = new View(this);
        emptyCell.setLayoutParams(new LinearLayout.LayoutParams(
                getResources().getDimensionPixelSize(R.dimen.heatmap_day_width),
                getResources().getDimensionPixelSize(R.dimen.heatmap_cell_height)));
        monthRow.addView(emptyCell);

        // 添加月份标签
        String[] months = {"一月", "二月", "三月", "四月", "五月", "六月",
                "七月", "八月", "九月", "十月", "十一月", "十二月"};
        for (String month : months) {
            TextView monthText = new TextView(this);
            monthText.setLayoutParams(new LinearLayout.LayoutParams(
                    getResources().getDimensionPixelSize(R.dimen.heatmap_month_width),
                    LinearLayout.LayoutParams.WRAP_CONTENT));
            monthText.setText(month);
            monthText.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            monthText.setTextSize(12);
            monthRow.addView(monthText);
        }
        dailyDistributionContainer.addView(monthRow);

        // 创建星期行
        String[] weekdays = {"周一", "周三", "周五"};
        for (int weekday = 0; weekday < weekdays.length; weekday++) {
            LinearLayout weekRow = new LinearLayout(this);
            weekRow.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));
            weekRow.setOrientation(LinearLayout.HORIZONTAL);

            // 添加星期标签
            TextView weekdayText = new TextView(this);
            weekdayText.setLayoutParams(new LinearLayout.LayoutParams(
                    getResources().getDimensionPixelSize(R.dimen.heatmap_day_width),
                    getResources().getDimensionPixelSize(R.dimen.heatmap_cell_height)));
            weekdayText.setText(weekdays[weekday]);
            weekdayText.setTextSize(12);
            weekRow.addView(weekdayText);

            // 为每一天生成热力图单元格
            Random random = new Random();
            for (int month = 0; month < 12; month++) {
                // 每个月添加4个单元格
                for (int day = 0; day < 4; day++) {
                    View cell = new View(this);
                    LinearLayout.LayoutParams cellParams = new LinearLayout.LayoutParams(
                            getResources().getDimensionPixelSize(R.dimen.heatmap_cell_width),
                            getResources().getDimensionPixelSize(R.dimen.heatmap_cell_height));
                    cellParams.setMargins(2, 2, 2, 2);
                    cell.setLayoutParams(cellParams);

                    // 随机生成活动级别（0-4）
                    int level = random.nextInt(5);
                    switch (level) {
                        case 0:
                            cell.setBackgroundColor(Color.parseColor("#ebedf0")); // 无活动
                            break;
                        case 1:
                            cell.setBackgroundColor(Color.parseColor("#c6e48b")); // 低活动
                            break;
                        case 2:
                            cell.setBackgroundColor(Color.parseColor("#7bc96f")); // 中等活动
                            break;
                        case 3:
                            cell.setBackgroundColor(Color.parseColor("#239a3b")); // 高活动
                            break;
                        case 4:
                            cell.setBackgroundColor(Color.parseColor("#196127")); // 非常高活动
                            break;
                    }

                    weekRow.addView(cell);
                }
            }
            dailyDistributionContainer.addView(weekRow);
        }

        // 添加图例行
        LinearLayout legendRow = new LinearLayout(this);
        legendRow.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        legendRow.setOrientation(LinearLayout.HORIZONTAL);
        legendRow.setPadding(0, 20, 0, 0);

        // 添加说明文本
        TextView legendText = new TextView(this);
        legendText.setLayoutParams(new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f));
        legendText.setText("任务完成数量：");
        legendText.setTextSize(12);
        legendRow.addView(legendText);

        // 添加图例项
        String[] legendLabels = {"少", "", "", "", "多"};
        int[] legendColors = {
                Color.parseColor("#ebedf0"),
                Color.parseColor("#c6e48b"),
                Color.parseColor("#7bc96f"),
                Color.parseColor("#239a3b"),
                Color.parseColor("#196127")
        };

        LinearLayout legendItems = new LinearLayout(this);
        legendItems.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        legendItems.setOrientation(LinearLayout.HORIZONTAL);

        for (int i = 0; i < legendLabels.length; i++) {
            TextView labelText = new TextView(this);
            labelText.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));
            labelText.setText(legendLabels[i]);
            labelText.setTextSize(12);

            View colorBox = new View(this);
            LinearLayout.LayoutParams boxParams = new LinearLayout.LayoutParams(
                    getResources().getDimensionPixelSize(R.dimen.heatmap_cell_width),
                    getResources().getDimensionPixelSize(R.dimen.heatmap_cell_height));
            boxParams.setMargins(5, 0, 5, 0);
            colorBox.setLayoutParams(boxParams);
            colorBox.setBackgroundColor(legendColors[i]);

            legendItems.addView(labelText);
            legendItems.addView(colorBox);
        }

        legendRow.addView(legendItems);
        dailyDistributionContainer.addView(legendRow);
    }

    private void setupMonthlyChart() {
        // 设置月度折线图
        List<Entry> entries = new ArrayList<>();

        // 生成月度数据（示例数据，每天的任务时长）
        for (int i = 1; i <= 30; i++) {
            float hours = (float) (Math.random() * 5);
            entries.add(new Entry(i, hours));
        }

        LineDataSet dataSet = new LineDataSet(entries, "每日任务时长 (小时)");
        dataSet.setColor(Color.BLUE);
        dataSet.setCircleColor(Color.BLUE);
        dataSet.setValueTextSize(10f);
        dataSet.setLineWidth(2f);

        LineData lineData = new LineData(dataSet);
        monthlyChart.setData(lineData);

        // 自定义X轴
        XAxis xAxis = monthlyChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);

        // 设置当前月份的天数标签
        final String[] days = new String[31];
        for (int i = 0; i < 31; i++) {
            days[i] = String.valueOf(i + 1);
        }
        xAxis.setValueFormatter(new IndexAxisValueFormatter(days));

        monthlyChart.getDescription().setEnabled(false);
        monthlyChart.animateX(1000);
        monthlyChart.invalidate();
    }

    private void setupYearlyChart() {
        // 设置年度折线图
        List<Entry> entries = new ArrayList<>();

        // 生成年度数据（示例数据，每月的平均任务时长）
        for (int i = 1; i <= 12; i++) {
            float hours = (float) (Math.random() * 3 + 1);
            entries.add(new Entry(i, hours));
        }

        LineDataSet dataSet = new LineDataSet(entries, "月平均任务时长 (小时)");
        dataSet.setColor(Color.GREEN);
        dataSet.setCircleColor(Color.GREEN);
        dataSet.setValueTextSize(10f);
        dataSet.setLineWidth(2f);

        LineData lineData = new LineData(dataSet);
        yearlyChart.setData(lineData);

        // 自定义X轴
        XAxis xAxis = yearlyChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);

        // 设置月份标签
        final String[] months = {"1月", "2月", "3月", "4月", "5月", "6月",
                "7月", "8月", "9月", "10月", "11月", "12月"};
        xAxis.setValueFormatter(new IndexAxisValueFormatter(months));

        yearlyChart.getDescription().setEnabled(false);
        yearlyChart.animateX(1000);
        yearlyChart.invalidate();
    }

    private void setupDistributionButtons() {
        btnDaily.setOnClickListener(v -> {
            updateButtonSelection(btnDaily, btnWeekly, btnMonthly, btnCustom);
            generateHeatmap(); // 可以根据需要传递不同的参数以显示不同的视图
        });

        btnWeekly.setOnClickListener(v -> {
            updateButtonSelection(btnWeekly, btnDaily, btnMonthly, btnCustom);
            generateHeatmap();
        });

        btnMonthly.setOnClickListener(v -> {
            updateButtonSelection(btnMonthly, btnDaily, btnWeekly, btnCustom);
            generateHeatmap();
        });

        btnCustom.setOnClickListener(v -> {
            updateButtonSelection(btnCustom, btnDaily, btnWeekly, btnMonthly);
            generateHeatmap();
        });

        // 默认选中日视图
        updateButtonSelection(btnDaily, btnWeekly, btnMonthly, btnCustom);
    }

    private void setupMonthYearButtons() {
        btnMonthView.setOnClickListener(v -> {
            updateButtonSelection(btnMonthView, btnYearView);
            monthlyViewContainer.setVisibility(View.VISIBLE);
            yearlyViewContainer.setVisibility(View.GONE);
        });

        btnYearView.setOnClickListener(v -> {
            updateButtonSelection(btnYearView, btnMonthView);
            monthlyViewContainer.setVisibility(View.GONE);
            yearlyViewContainer.setVisibility(View.VISIBLE);
        });

        // 默认选中月视图
        updateButtonSelection(btnMonthView, btnYearView);
        monthlyViewContainer.setVisibility(View.VISIBLE);
        yearlyViewContainer.setVisibility(View.GONE);
    }

    private void updateButtonSelection(Button selectedButton, Button... otherButtons) {
        selectedButton.setBackground(ContextCompat.getDrawable(this, R.drawable.selected_button_background));
        selectedButton.setTextColor(ContextCompat.getColor(this, android.R.color.white));

        for (Button button : otherButtons) {
            button.setBackground(ContextCompat.getDrawable(this, R.drawable.unselected_button_background));
            button.setTextColor(ContextCompat.getColor(this, android.R.color.black));
        }
    }
}