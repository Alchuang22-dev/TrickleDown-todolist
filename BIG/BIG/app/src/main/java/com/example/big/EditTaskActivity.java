package com.example.big;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.Calendar;
import java.util.Date;
import java.util.Map;

public class EditTaskActivity extends AppCompatActivity {
    private EditText titleEditText;
    private EditText descriptionEditText;
    private DatePicker datePicker;
    private NumberPicker startHourPicker;
    private NumberPicker startMinutePicker;
    private NumberPicker endHourPicker;
    private NumberPicker endMinutePicker;
    private EditText placeEditText;
    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private Switch importantSwitch;
    private Button editButton;
    private Button deleteButton;
    private CheckBox finishedCheckBox;
    private CheckBox delayedCheckBox;
    private TextView finishedStatusText;
    private TextView delayedStatusText;
    private EditText categoryEditText;
    private Button studyButton;
    private Button workButton;
    private Button lifeButton;
    private Button otherButton;

    private Task currentTask;
    private int taskId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_edit_task);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 获取当前编辑的任务ID
        taskId = getIntent().getIntExtra("task_id", -1);
        if (taskId == -1) {
            Toast.makeText(this, "任务ID无效", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 从数据库或其他存储中获取任务对象
        // 这里示例使用，实际应用中应该从数据库获取
        currentTask = getTaskById(taskId);
        if (currentTask == null) {
            Toast.makeText(this, "无法找到任务", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 初始化控件
        initViews();
        setupTimePickers();
        setupButtons();
        setupStatusCheckBoxes();
        setupCategoryButtons();

        // 填充现有任务数据
        populateTaskData();

        findViewById(R.id.back_button).setOnClickListener(v -> finish());
    }

    // 示例方法，实际应用中应从数据库获取
    private Task getTaskById(int id) {
        // 模拟数据，实际应用中应该从数据库获取
        // 这里暂时返回一个假的Task对象用于演示
        Calendar cal = Calendar.getInstance();
        Date date = cal.getTime();

        Task task = new Task(id, "示例任务", "09 : 00 -- 10 : 30", date, 90, true, "这是一个示例任务描述", "示例地点", date);
        task.setCategory("学习");
        task.setFinished(false);
        // 由于Task类中没有setDelayed方法，我们不能在这里设置延期状态
        // 需要在Task类中添加这个方法
        return task;
    }

    private void initViews() {
        titleEditText = findViewById(R.id.edit_title);
        descriptionEditText = findViewById(R.id.edit_description);
        datePicker = findViewById(R.id.date_picker);
        startHourPicker = findViewById(R.id.start_hour_picker);
        startMinutePicker = findViewById(R.id.start_minute_picker);
        endHourPicker = findViewById(R.id.end_hour_picker);
        endMinutePicker = findViewById(R.id.end_minute_picker);
        placeEditText = findViewById(R.id.edit_place);
        importantSwitch = findViewById(R.id.switch_important);
        editButton = findViewById(R.id.button_edit);
        deleteButton = findViewById(R.id.button_delete);
        finishedCheckBox = findViewById(R.id.checkbox_finished);
        delayedCheckBox = findViewById(R.id.checkbox_delayed);
        finishedStatusText = findViewById(R.id.text_finished_status);
        delayedStatusText = findViewById(R.id.text_delayed_status);
        categoryEditText = findViewById(R.id.edit_category);
        studyButton = findViewById(R.id.button_study);
        workButton = findViewById(R.id.button_work);
        lifeButton = findViewById(R.id.button_life);
        otherButton = findViewById(R.id.button_other);
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

    private void setupButtons() {
        editButton.setOnClickListener(v -> updateTask());
        deleteButton.setOnClickListener(v -> confirmDelete());
    }

    private void setupStatusCheckBoxes() {
        finishedCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> finishedStatusText.setText(isChecked ? "已完成" : "未完成"));

        delayedCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> delayedStatusText.setText(isChecked ? "已延期" : "按时完成"));
    }

    private void setupCategoryButtons() {
        studyButton.setOnClickListener(v -> categoryEditText.setText("学习"));
        workButton.setOnClickListener(v -> categoryEditText.setText("工作"));
        lifeButton.setOnClickListener(v -> categoryEditText.setText("生活"));
        otherButton.setOnClickListener(v -> categoryEditText.setText("其他"));
    }

    private void populateTaskData() {
        // 使用当前任务的数据填充界面
        Map<String, Object> taskData = currentTask.getAll();

        // 填充标题
        titleEditText.setText((String) taskData.get("title"));

        // 填充描述
        if (taskData.get("description") != null) {
            descriptionEditText.setText((String) taskData.get("description"));
        }

        // 填充日期选择器
        Date date = (Date) taskData.get("date");
        if (date != null) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            datePicker.updateDate(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
        }

        // 填充时间范围
        String timeRange = (String) taskData.get("timeRange");
        if (timeRange != null && !timeRange.equals("未设定时间")) {
            try {
                // 解析时间范围，格式: "HH : MM -- HH : MM"
                String[] parts = timeRange.split(" -- ");
                String[] startParts = parts[0].split(" : ");
                String[] endParts = parts[1].split(" : ");

                int startHour = Integer.parseInt(startParts[0]);
                int startMinute = Integer.parseInt(startParts[1]);
                int startMinuteIndex = startMinute / 5; // 计算分钟索引

                int endHour = Integer.parseInt(endParts[0]);
                int endMinute = Integer.parseInt(endParts[1]);
                int endMinuteIndex = endMinute / 5; // 计算分钟索引

                startHourPicker.setValue(startHour);
                startMinutePicker.setValue(startMinuteIndex);
                endHourPicker.setValue(endHour);
                endMinutePicker.setValue(endMinuteIndex);
            } catch (Exception e) {
                // 解析错误，使用默认值
                startHourPicker.setValue(9);
                startMinutePicker.setValue(0);
                endHourPicker.setValue(10);
                endMinutePicker.setValue(0);
            }
        }

        // 填充地点
        if (taskData.get("place") != null) {
            placeEditText.setText((String) taskData.get("place"));
        }

        // 设置重要性
        importantSwitch.setChecked((Boolean) taskData.get("important"));

        // 设置完成状态
        if (taskData.get("finished") != null) {
            boolean finished = (Boolean) taskData.get("finished");
            finishedCheckBox.setChecked(finished);
            finishedStatusText.setText(finished ? "已完成" : "未完成");
        }

        // 设置延期状态
        if (taskData.get("delayed") != null) {
            boolean delayed = (Boolean) taskData.get("delayed");
            delayedCheckBox.setChecked(delayed);
            delayedStatusText.setText(delayed ? "已延期" : "按时完成");
        }

        // 设置分类标签
        if (taskData.get("category") != null) {
            String category = (String) taskData.get("category");
            categoryEditText.setText(category);
        }
    }

    private void updateTask() {
        String title = titleEditText.getText().toString().trim();

        // 检查必填字段
        if (title.isEmpty()) {
            Toast.makeText(this, "请输入任务标题", Toast.LENGTH_SHORT).show();
            return;
        }

        // 获取日期
        Calendar calendar = Calendar.getInstance();
        calendar.set(datePicker.getYear(), datePicker.getMonth(), datePicker.getDayOfMonth(), 0, 0, 0);
        Date date = calendar.getTime();

        // 获取描述
        String description = descriptionEditText.getText().toString().trim();
        if (description.isEmpty()) {
            description = ""; // 默认空描述
        }

        // 获取地点
        String place = placeEditText.getText().toString().trim();
        if (place.isEmpty()) {
            place = ""; // 默认空地点
        }

        // 获取重要性
        boolean important = importantSwitch.isChecked();

        // 获取时间范围和持续时间
        int startHour = startHourPicker.getValue();
        int endHour = endHourPicker.getValue();
        int startMinuteIndex = startMinutePicker.getValue();
        int endMinuteIndex = endMinutePicker.getValue();
        int startMinute = startMinuteIndex * 5;
        int endMinute = endMinuteIndex * 5;

        // 格式化时间范围
        @SuppressLint("DefaultLocale") String startHourStr = String.format("%02d", startHour);
        @SuppressLint("DefaultLocale") String startMinuteStr = String.format("%02d", startMinute);
        @SuppressLint("DefaultLocale") String endHourStr = String.format("%02d", endHour);
        @SuppressLint("DefaultLocale") String endMinuteStr = String.format("%02d", endMinute);

        String timeRange = startHourStr + " : " + startMinuteStr + " -- " + endHourStr + " : " + endMinuteStr;

        // 计算持续时间（分钟）
        int durationMinutes = (endHour - startHour) * 60 + (endMinute - startMinute);

        // 如果时间范围有效，设置dueDate为日期加结束时间
        Calendar dueCal = Calendar.getInstance();
        dueCal.setTime(date);
        dueCal.set(Calendar.HOUR_OF_DAY, endHour);
        dueCal.set(Calendar.MINUTE, endMinute);
        Date dueDate = dueCal.getTime();

        // 获取完成状态
        boolean finished = finishedCheckBox.isChecked();

        // 获取标签
        String category = categoryEditText.getText().toString().trim();
        if (category.isEmpty()) {
            category = "其他"; // 默认标签
        }

        // 更新任务
        currentTask.edit(title, timeRange, date, durationMinutes, important, description, place, dueDate);
        currentTask.setFinished(finished);
        currentTask.setPlace(place);
        currentTask.setCategory(category);

        // 由于Task类中没有setDelayed方法，我们不能设置延期状态
        // 您需要在Task类中添加这个方法：
        // public void setDelayed(boolean delayed) {
        //     this.delayed = delayed;
        // }

        Toast.makeText(this, "任务已更新: " + title, Toast.LENGTH_SHORT).show();

        // 返回上一个Activity
        Intent resultIntent = new Intent();
        resultIntent.putExtra("task_id", taskId);
        resultIntent.putExtra("action", "edit");
        setResult(RESULT_OK, resultIntent);
        finish();
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle("删除事项")
                .setMessage("确定要删除这个事项吗？此操作不可撤销。")
                .setPositiveButton("删除", (dialog, which) -> deleteTask())
                .setNegativeButton("取消", null)
                .show();
    }

    private void deleteTask() {
        boolean success = currentTask.delete();

        if (success) {
            Toast.makeText(this, "事项已删除", Toast.LENGTH_SHORT).show();

            // 返回上一个Activity并传递删除结果
            Intent resultIntent = new Intent();
            resultIntent.putExtra("task_id", taskId);
            resultIntent.putExtra("action", "delete");
            setResult(RESULT_OK, resultIntent);
            finish();
        } else {
            Toast.makeText(this, "删除失败，请稍后重试", Toast.LENGTH_SHORT).show();
        }
    }
}