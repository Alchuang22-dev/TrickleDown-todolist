package com.example.big;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TodayViewActivity extends AppCompatActivity {

    private TextView dateTitle;
    private RecyclerView timelineRecyclerView;
    private Date selectedDate;
    private List<Task> taskList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_today_view);

        dateTitle = findViewById(R.id.date_title);
        timelineRecyclerView = findViewById(R.id.today_timeline_recycler_view);

        // Get selected date from intent
        long selectedDateMillis = getIntent().getLongExtra("selected_date", System.currentTimeMillis());
        selectedDate = new Date(selectedDateMillis);

        // Initialize demo tasks
        initializeTaskList();

        // Set date title
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy年MM月dd日 EEE", Locale.getDefault());
        dateTitle.setText(dateFormat.format(selectedDate));

        // Setup timeline view
        setupTimelineView();

        // Setup back button
        findViewById(R.id.back_button).setOnClickListener(v -> {
            finish();
        });
    }

    private void setupTimelineView() {
        TodayTimelineAdapter adapter = new TodayTimelineAdapter(this, taskList, selectedDate);
        timelineRecyclerView.setAdapter(adapter);

        adapter.setOnTaskClickListener(task -> {
            Intent intent = new Intent(TodayViewActivity.this, EditTaskActivity.class);
            intent.putExtra("task_id", task.getId());
            startActivity(intent);
        });
    }

    private void initializeTaskList() {
        taskList = new ArrayList<>();

        // Add some demo tasks
        Calendar cal = Calendar.getInstance();
        cal.setTime(selectedDate);

        // Reset time to beginning of day
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        // Classes for today
        cal.set(Calendar.HOUR_OF_DAY, 9);
        Task task1 = new Task(1, "上课", "09:00 - 11:30", cal.getTime(), 150, false);

        cal.set(Calendar.HOUR_OF_DAY, 11);
        cal.set(Calendar.MINUTE, 30);
        Task task2 = new Task(2, "午间休息", "11:30 - 12:30", cal.getTime(), 60, false);

        cal.set(Calendar.HOUR_OF_DAY, 12);
        cal.set(Calendar.MINUTE, 30);
        Task task3 = new Task(3, "算法预习", "12:30 - 13:30", cal.getTime(), 60, true);

        cal.set(Calendar.HOUR_OF_DAY, 13);
        cal.set(Calendar.MINUTE, 30);
        Task task4 = new Task(4, "上课", "13:30 - 17:00", cal.getTime(), 210, false);

        cal.set(Calendar.HOUR_OF_DAY, 15);
        cal.set(Calendar.MINUTE, 0);
        Task task4_overlap = new Task(10, "课间休息", "15:00 - 15:30", cal.getTime(), 30, false);

        cal.set(Calendar.HOUR_OF_DAY, 17);
        cal.set(Calendar.MINUTE, 30);
        Task task5 = new Task(5, "法律原理预习", "17:30 - 19:00", cal.getTime(), 90, true);

        cal.set(Calendar.HOUR_OF_DAY, 19);
        cal.set(Calendar.MINUTE, 0);
        Task task6 = new Task(6, "上课", "19:00 - 21:00", cal.getTime(), 120, false);

        cal.set(Calendar.HOUR_OF_DAY, 21);
        cal.set(Calendar.MINUTE, 30);
        Task task9 = new Task(9, "班团例会 (双周)", "21:30 - 23:00", cal.getTime(), 90, true);

        taskList.add(task1);
        taskList.add(task2);
        taskList.add(task3);
        taskList.add(task4);
        taskList.add(task4_overlap);
        taskList.add(task5);
        taskList.add(task6);
        taskList.add(task9);
    }
}
