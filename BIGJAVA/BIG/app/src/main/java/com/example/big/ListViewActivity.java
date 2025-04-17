package com.example.big;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class ListViewActivity extends AppCompatActivity {

    private RecyclerView allTasksRecyclerView;
    private List<Task> allTasks;
    private TaskAdapter taskAdapter;

    private LinearLayout searchBar;
    private EditText searchEditText;
    private TextView titleText;

    private boolean isSearchVisible = false;

    // 时间筛选对话框中的控件
    private NumberPicker yearPicker;
    private NumberPicker monthPicker;
    private NumberPicker dayPicker;
    private NumberPicker startHourPicker;
    private NumberPicker startMinutePicker;
    private NumberPicker endHourPicker;
    private NumberPicker endMinutePicker;

    // 日历辅助变量
    private int[] daysInMonth = {31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};
    private int selectedYear;
    private int selectedMonth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_view);

        // 初始化视图
        initViews();

        // 设置返回按钮
        ImageButton backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> finish());

        // 设置搜索按钮
        ImageButton searchButton = findViewById(R.id.search_button);
        searchButton.setOnClickListener(v -> toggleSearchBar());

        // 设置清除搜索按钮
        ImageButton clearSearchButton = findViewById(R.id.clear_search_button);
        clearSearchButton.setOnClickListener(v -> {
            searchEditText.setText("");
            refreshTaskList(allTasks);
        });

        // 设置菜单按钮
        ImageButton menuButton = findViewById(R.id.menu_button);
        menuButton.setOnClickListener(this::showFilterMenu);

        // 设置添加任务按钮
        FloatingActionButton addTaskButton = findViewById(R.id.add_task_button);
        addTaskButton.setOnClickListener(v -> {
            Intent intent = new Intent(ListViewActivity.this, AddTaskActivity.class);
            startActivity(intent);
        });

        // 初始化任务数据
        initTaskData();

        // 设置任务列表
        setupTaskList();

        // 设置搜索监听
        setupSearchListener();
    }

    private void initViews() {
        allTasksRecyclerView = findViewById(R.id.all_tasks_recyclerview);
        allTasksRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        searchBar = findViewById(R.id.search_bar);
        searchEditText = findViewById(R.id.search_edit_text);
        titleText = findViewById(R.id.title_text);
    }

    private void toggleSearchBar() {
        isSearchVisible = !isSearchVisible;
        searchBar.setVisibility(isSearchVisible ? View.VISIBLE : View.GONE);

        if (isSearchVisible) {
            searchEditText.requestFocus();
        } else {
            searchEditText.setText("");
            refreshTaskList(allTasks);
        }
    }

    private void setupSearchListener() {
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String searchText = s.toString().toLowerCase().trim();
                if (searchText.isEmpty()) {
                    refreshTaskList(allTasks);
                } else {
                    filterTasksByTitle(searchText);
                }
            }
        });
    }

    private void filterTasksByTitle(String searchText) {
        List<Task> filteredTasks = new ArrayList<>();
        for (Task task : allTasks) {
            if (task.getTitle().toLowerCase().contains(searchText)) {
                filteredTasks.add(task);
            }
        }
        refreshTaskList(filteredTasks);
    }

    private void showFilterMenu(View view) {
        PopupMenu popup = new PopupMenu(this, view);
        popup.getMenuInflater().inflate(R.menu.filter_menu, popup.getMenu());

        popup.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.menu_unfinished) {
                filterUnfinishedTasks();
                return true;
            } else if (itemId == R.id.menu_important) {
                filterImportantTasks();
                return true;
            } else if (itemId == R.id.menu_by_time) {
                showTimeFilterDialog();
                return true;
            } else if (itemId == R.id.menu_by_category) {
                showCategoryFilterDialog();
                return true;
            } else if (itemId == R.id.menu_all) {
                refreshTaskList(allTasks);
                return true;
            }
            return false;
        });

        popup.show();
    }

    private void initTaskData() {
        // 创建一些示例任务数据
        allTasks = new ArrayList<>();

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date today = cal.getTime();

        // 创建10个样例任务，按时间排序
        Task task1 = new Task(12345001, "晨会", "08 : 30 -- 09 : 00", today, 30, false, "每日工作安排");
        task1.setPlace("会议室B");
        task1.setCategory("工作");

        Task task2 = new Task(12345002, "完成项目报告", "09 : 00 -- 11 : 00", today, 120, true, "需要提交给经理审核");
        task2.setPlace("办公室");
        task2.setCategory("工作");

        Task task3 = new Task(12345003, "回复客户邮件", "10 : 00 -- 10 : 30", today, 30, false, "处理紧急问题");
        task3.setCategory("工作");
        task3.setFinished(true);

        Task task4 = new Task(12345004, "午餐", "12 : 00 -- 13 : 00", today, 60, false, "与同事共进午餐");
        task4.setPlace("公司餐厅");
        task4.setCategory("生活");

        Task task5 = new Task(12345005, "客户会议", "14 : 00 -- 15 : 30", today, 90, true, "讨论新产品方案");
        task5.setPlace("会议室A");
        task5.setCategory("工作");

        Task task6 = new Task(12345006, "整理邮件", "16 : 00 -- 17 : 00", today, 60, false, "回复重要客户邮件");
        task6.setCategory("工作");

        Task task7 = new Task(12345007, "健身", "18 : 00 -- 19 : 00", today, 60, false, "每周锻炼计划");
        task7.setPlace("健身房");
        task7.setCategory("生活");

        Task task8 = new Task(12345008, "阅读", "20 : 00 -- 21 : 00", today, 60, false, "学习新技能");
        task8.setCategory("学习");

        Task task9 = new Task(12345009, "准备明天的演讲", "21 : 00 -- 22 : 00", today, 60, true, "公司年度会议演讲");
        task9.setCategory("工作");

        Task task10 = new Task(12345010, "计划下周任务", "22 : 00 -- 23 : 00", today, 60, false, "安排下周工作计划");
        task10.setCategory("工作");

        // 添加到列表
        allTasks.add(task1);
        allTasks.add(task2);
        allTasks.add(task3);
        allTasks.add(task4);
        allTasks.add(task5);
        allTasks.add(task6);
        allTasks.add(task7);
        allTasks.add(task8);
        allTasks.add(task9);
        allTasks.add(task10);

        // 按时间排序
        sortTasksByTime();
    }

    private void sortTasksByTime() {
        allTasks.sort((t1, t2) -> {
            if (t1.getTimeRange() == null || t2.getTimeRange() == null) {
                return 0;
            }
            return t1.getTimeRange().compareTo(t2.getTimeRange());
        });
    }

    private void setupTaskList() {
        taskAdapter = new TaskAdapter(allTasks, this);
        allTasksRecyclerView.setAdapter(taskAdapter);
    }

    private void refreshTaskList(List<Task> tasks) {
        taskAdapter = new TaskAdapter(tasks, this);
        allTasksRecyclerView.setAdapter(taskAdapter);
    }

    // 筛选未完成的任务
    private void filterUnfinishedTasks() {
        List<Task> unfinishedTasks = new ArrayList<>();
        for (Task task : allTasks) {
            if (!task.isFinished()) {
                unfinishedTasks.add(task);
            }
        }
        refreshTaskList(unfinishedTasks);
    }

    // 筛选重要任务
    private void filterImportantTasks() {
        List<Task> filteredTasks = new ArrayList<>();
        for (Task task : allTasks) {
            if (task.isImportant()) {
                filteredTasks.add(task);
            }
        }
        refreshTaskList(filteredTasks);
    }

    // 显示时间筛选对话框
    private void showTimeFilterDialog() {
        // 创建对话框
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_time_filter);

        // 初始化控件
        yearPicker = dialog.findViewById(R.id.year_picker);
        monthPicker = dialog.findViewById(R.id.month_picker);
        dayPicker = dialog.findViewById(R.id.day_picker);
        startHourPicker = dialog.findViewById(R.id.start_hour_picker);
        startMinutePicker = dialog.findViewById(R.id.start_minute_picker);
        endHourPicker = dialog.findViewById(R.id.end_hour_picker);
        endMinutePicker = dialog.findViewById(R.id.end_minute_picker);
        Button resetButton = dialog.findViewById(R.id.reset_button);
        Button confirmButton = dialog.findViewById(R.id.confirm_button);

        // 设置日期选择器
        setupDatePickers();

        // 设置时间选择器
        setupTimePickers();

        // 设置重置按钮
        resetButton.setOnClickListener(v -> resetTimeFilters());

        // 设置确认按钮
        confirmButton.setOnClickListener(v -> {
            filterTasksByTime();
            dialog.dismiss();
        });

        dialog.show();
    }

    private void setupDatePickers() {
        // 获取当前日期
        Calendar calendar = Calendar.getInstance();
        selectedYear = calendar.get(Calendar.YEAR);
        selectedMonth = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        // 设置年份范围，从当前年份-10年到当前年份+10年
        yearPicker.setMinValue(selectedYear - 10);
        yearPicker.setMaxValue(selectedYear + 10);
        yearPicker.setValue(selectedYear);

        // 设置月份范围，1-12月
        monthPicker.setMinValue(1);
        monthPicker.setMaxValue(12);
        monthPicker.setValue(selectedMonth + 1); // 月份从0开始，所以+1

        // 设置日期范围，根据年月确定
        updateDayPicker();
        dayPicker.setValue(day);

        // 设置年月变化监听器，更新日期选择器
        yearPicker.setOnValueChangedListener((picker, oldVal, newVal) -> {
            selectedYear = newVal;
            updateDayPicker();
        });

        monthPicker.setOnValueChangedListener((picker, oldVal, newVal) -> {
            selectedMonth = newVal - 1; // 月份从0开始，所以-1
            updateDayPicker();
        });
    }

    private void updateDayPicker() {
        // 检查是否闰年
        boolean isLeapYear = (selectedYear % 4 == 0 && selectedYear % 100 != 0) || (selectedYear % 400 == 0);
        int daysInSelectedMonth = daysInMonth[selectedMonth];

        // 如果是2月且是闰年，则有29天
        if (selectedMonth == 1 && isLeapYear) {
            daysInSelectedMonth = 29;
        }

        // 更新日期选择器
        dayPicker.setMinValue(1);
        dayPicker.setMaxValue(daysInSelectedMonth);

        // 防止当前值超出范围
        if (dayPicker.getValue() > daysInSelectedMonth) {
            dayPicker.setValue(daysInSelectedMonth);
        }
    }

    private void setupTimePickers() {
        // 设置小时选择器范围 0-23
        startHourPicker.setMinValue(0);
        startHourPicker.setMaxValue(23);
        endHourPicker.setMinValue(0);
        endHourPicker.setMaxValue(23);

        // 设置分钟选择器
        String[] minutes = {"00", "05", "10", "15", "20", "25", "30", "35", "40", "45", "50", "55"};
        startMinutePicker.setMinValue(0);
        startMinutePicker.setMaxValue(minutes.length - 1);
        startMinutePicker.setDisplayedValues(minutes);

        endMinutePicker.setMinValue(0);
        endMinutePicker.setMaxValue(minutes.length - 1);
        endMinutePicker.setDisplayedValues(minutes);

        // 设置结束时间的监听器，确保结束时间晚于开始时间
        endHourPicker.setOnValueChangedListener((picker, oldVal, newVal) -> validateEndTime());
        endMinutePicker.setOnValueChangedListener((picker, oldVal, newVal) -> validateEndTime());
        startHourPicker.setOnValueChangedListener((picker, oldVal, newVal) -> validateEndTime());
        startMinutePicker.setOnValueChangedListener((picker, oldVal, newVal) -> validateEndTime());
    }

    private void validateEndTime() {
        int startHour = startHourPicker.getValue();
        int startMinuteIndex = startMinutePicker.getValue();
        int startMinute = startMinuteIndex * 5; // 分钟间隔为5

        int endHour = endHourPicker.getValue();
        int endMinuteIndex = endMinutePicker.getValue();
        int endMinute = endMinuteIndex * 5; // 分钟间隔为5

        // 如果结束时间早于开始时间，调整结束时间
        if (endHour < startHour || (endHour == startHour && endMinute <= startMinute)) {
            if (startMinuteIndex < 11) { // 11是最后一个索引（55分钟）
                endHourPicker.setValue(startHour);
                endMinutePicker.setValue(startMinuteIndex + 1);
            } else {
                endHourPicker.setValue(startHour + 1);
                endMinutePicker.setValue(0);
            }
            Toast.makeText(this, "结束时间必须晚于开始时间", Toast.LENGTH_SHORT).show();
        }
    }

    private void resetTimeFilters() {
        // 重置日期为当前日期
        Calendar calendar = Calendar.getInstance();
        yearPicker.setValue(calendar.get(Calendar.YEAR));
        monthPicker.setValue(calendar.get(Calendar.MONTH) + 1);
        dayPicker.setValue(calendar.get(Calendar.DAY_OF_MONTH));

        // 重置时间
        startHourPicker.setValue(9); // 默认早上9点
        startMinutePicker.setValue(0); // 默认0分
        endHourPicker.setValue(10); // 默认上午10点
        endMinutePicker.setValue(0); // 默认0分
    }

    private void filterTasksByTime() {
        // 获取筛选条件
        int year = yearPicker.getValue();
        int month = monthPicker.getValue() - 1; // 月份从0开始
        int day = dayPicker.getValue();

        int startHour = startHourPicker.getValue();
        int startMinuteIndex = startMinutePicker.getValue();
        int startMinute = startMinuteIndex * 5;

        int endHour = endHourPicker.getValue();
        int endMinuteIndex = endMinutePicker.getValue();
        int endMinute = endMinuteIndex * 5;

        // 格式化时间范围
        @SuppressLint("DefaultLocale") String startTimeStr = String.format("%02d : %02d", startHour, startMinute);
        @SuppressLint("DefaultLocale") String endTimeStr = String.format("%02d : %02d", endHour, endMinute);
        String timeRange = startTimeStr + " -- " + endTimeStr;

        // 创建日期对象
        Calendar cal = Calendar.getInstance();
        cal.set(year, month, day, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date filterDate = cal.getTime();

        // 筛选任务
        List<Task> filteredTasks = new ArrayList<>();
        for (Task task : allTasks) {
            // 检查日期匹配
            boolean dateMatches = true;
            if (task.getDate() != null) {
                Calendar taskCal = Calendar.getInstance();
                taskCal.setTime(task.getDate());

                int taskYear = taskCal.get(Calendar.YEAR);
                int taskMonth = taskCal.get(Calendar.MONTH);
                int taskDay = taskCal.get(Calendar.DAY_OF_MONTH);

                dateMatches = (taskYear == year && taskMonth == month && taskDay == day);
            }

            // 检查时间范围匹配
            boolean timeMatches = true;
            if (task.getTimeRange() != null && !task.getTimeRange().isEmpty() && !task.getTimeRange().equals("未设定时间")) {
                // 简单字符串比较（实际应用中可能需要更复杂的时间解析）
                String taskStart = task.getTimeRange().split(" -- ")[0].trim();
                String taskEnd = task.getTimeRange().split(" -- ")[1].trim();

                // 时间重叠检查
                boolean isOverlap = !(taskEnd.compareTo(startTimeStr) < 0 || taskStart.compareTo(endTimeStr) > 0);
                timeMatches = isOverlap;
            }

            // 只有两个条件都匹配才添加到结果中
            if (dateMatches && timeMatches) {
                filteredTasks.add(task);
            }
        }

        // 更新列表
        refreshTaskList(filteredTasks);

        // 反馈筛选结果
        if (filteredTasks.isEmpty()) {
            Toast.makeText(this, "没有找到符合条件的事项", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "找到 " + filteredTasks.size() + " 个符合条件的事项", Toast.LENGTH_SHORT).show();
        }
    }

    // 显示类别筛选对话框
    private void showCategoryFilterDialog() {
        // 创建对话框
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_category_filter);

        // 初始化控件
        EditText categoryEditText = dialog.findViewById(R.id.edit_category);
        Button studyButton = dialog.findViewById(R.id.button_study);
        Button workButton = dialog.findViewById(R.id.button_work);
        Button lifeButton = dialog.findViewById(R.id.button_life);
        Button otherButton = dialog.findViewById(R.id.button_other);
        Button resetButton = dialog.findViewById(R.id.reset_button);
        Button confirmButton = dialog.findViewById(R.id.confirm_button);

        // 设置类别按钮点击事件
        studyButton.setOnClickListener(v -> categoryEditText.setText("学习"));
        workButton.setOnClickListener(v -> categoryEditText.setText("工作"));
        lifeButton.setOnClickListener(v -> categoryEditText.setText("生活"));
        otherButton.setOnClickListener(v -> categoryEditText.setText("其他"));

        // 设置重置按钮
        resetButton.setOnClickListener(v -> categoryEditText.setText(""));

        // 设置确认按钮
        confirmButton.setOnClickListener(v -> {
            String category = categoryEditText.getText().toString().trim();
            if (category.isEmpty()) {
                category = "其他";
            }
            filterTasksByCategory(category);
            dialog.dismiss();
        });

        dialog.show();
    }

    private void filterTasksByCategory(String category) {
        List<Task> filteredTasks = new ArrayList<>();
        for (Task task : allTasks) {
            if (task.getCategory() != null && task.getCategory().equals(category)) {
                filteredTasks.add(task);
            }
        }

        refreshTaskList(filteredTasks);

        // 反馈筛选结果
        if (filteredTasks.isEmpty()) {
            Toast.makeText(this, "没有找到类别为 " + category + " 的事项", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "找到 " + filteredTasks.size() + " 个类别为 " + category + " 的事项", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 刷新数据（实际应用中，这里应该从数据库重新加载数据）
        initTaskData();
        setupTaskList();
    }
}