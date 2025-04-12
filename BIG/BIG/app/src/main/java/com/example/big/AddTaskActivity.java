package com.example.big;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.Switch;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.Calendar;
import java.util.Date;
import java.util.Random;

public class AddTaskActivity extends AppCompatActivity {
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
    private Button addButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_task);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 初始化控件
        initViews();
        setupTimePickers();
        setupAddButton();
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
        addButton = findViewById(R.id.button_add);
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

    private void setupAddButton() {
        addButton.setOnClickListener(v -> createNewTask());
    }

    private void createNewTask() {
        String title = titleEditText.getText().toString().trim();

        // 检查必填字段
        if (title.isEmpty()) {
            Toast.makeText(this, "请输入任务标题", Toast.LENGTH_SHORT).show();
            return;
        }

        // 生成8位随机ID
        int id = generateRandomId();

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
        String timeRange;
        int durationMinutes;
        Date dueDate;

        // 检查是否设置了时间范围
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

        timeRange = startHourStr + " : " + startMinuteStr + " -- " + endHourStr + " : " + endMinuteStr;

        // 计算持续时间（分钟）
        durationMinutes = (endHour - startHour) * 60 + (endMinute - startMinute);

        // 如果时间范围有效，设置dueDate为日期加结束时间
        Calendar dueCal = Calendar.getInstance();
        dueCal.setTime(date);
        dueCal.set(Calendar.HOUR_OF_DAY, endHour);
        dueCal.set(Calendar.MINUTE, endMinute);
        dueDate = dueCal.getTime();

        // 创建任务
        Task newTask = new Task(id, title, timeRange, date, durationMinutes, important, description, place, dueDate);

        // 这里可以添加保存任务到数据库或传递回上一个Activity的代码

        Toast.makeText(this, "任务已添加: " + title, Toast.LENGTH_SHORT).show();
        finish(); // 返回上一个Activity
    }

    private int generateRandomId() {
        Random random = new Random();
        // 生成8位随机数（10000000-99999999）
        return 10000000 + random.nextInt(90000000);
    }
}