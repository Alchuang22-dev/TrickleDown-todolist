package com.example.big;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private ImageView profileImageView;
    private RecyclerView importantTasksRecyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 初始化视图
        initViews();

        // 设置重要事项列表
        setupImportantTasksList();

        // 设置点击事件
        setupClickListeners();
    }

    private void initViews() {
        profileImageView = findViewById(R.id.profile_image);
        importantTasksRecyclerView = findViewById(R.id.important_tasks_recyclerview);
    }

    private void setupImportantTasksList() {
        // 创建一个假的任务列表
        List<Task> importantTasks = new ArrayList<>();
        Calendar cal = Calendar.getInstance();

        // Reset time to beginning of day
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        // 使用明确的十进制数字作为ID
        int taskId = 12345678; // 8位数ID

        // 使用正确的时间范围格式: "HH : MM -- HH : MM"
        importantTasks.add(new Task(taskId, "上课", "19 : 00 -- 21 : 00", cal.getTime(), 120, false, "这是一个任务的简介"));

        // 设置RecyclerView
        TaskAdapter taskAdapter = new TaskAdapter(importantTasks, this);
        importantTasksRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        importantTasksRecyclerView.setAdapter(taskAdapter);
    }

    private void setupClickListeners() {
        // 头像点击事件 - 跳转到个人信息页面
        profileImageView.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
            startActivity(intent);
        });

        // 今日入口点击事件
        CardView todayEntryCard = findViewById(R.id.today_entry);
        todayEntryCard.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, TodayTasksActivity.class);
            startActivity(intent);
        });

        // 计划入口点击事件
        CardView planEntryCard = findViewById(R.id.plan_entry);
        planEntryCard.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, KanbanViewActivity.class);
            startActivity(intent);
        });

        // 全部入口点击事件
        CardView allEntryCard = findViewById(R.id.all_entry);
        allEntryCard.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ListViewActivity.class);
            startActivity(intent);
        });

        // 统计入口点击事件
        CardView statsEntryCard = findViewById(R.id.stats_entry);
        statsEntryCard.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, StatisticsActivity.class);
            startActivity(intent);
        });

        // 添加新事项按钮点击事件
        CardView addNewTaskButton = findViewById(R.id.add_new_task_button);
        addNewTaskButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddTaskActivity.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 如果从ProfileActivity返回，可能需要刷新头像
        // 这里可以添加从SharedPreferences或其他存储加载用户头像的代码
    }
}