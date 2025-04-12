package com.example.big;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class KanbanViewActivity extends AppCompatActivity {

    private LinearLayout dateContainer;
    private RecyclerView timelineRecyclerView;
    private List<Task> taskList;
    private Calendar currentDate;
    private GestureDetector gestureDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_kanban_view);

        dateContainer = findViewById(R.id.date_container);
        timelineRecyclerView = findViewById(R.id.timeline_recycler_view);

        currentDate = Calendar.getInstance();

        // Initialize demo tasks
        initializeTaskList();

        // Setup dates
        setupDateView();

        // Setup timeline
        setupTimelineView();

        // Setup gesture detection for horizontal swiping
        setupGestureDetection();

        // Setup add task button
        FloatingActionButton addTaskButton = findViewById(R.id.add_task_button);
        addTaskButton.setOnClickListener(v -> {
            Intent intent = new Intent(KanbanViewActivity.this, AddTaskActivity.class);
            startActivity(intent);
        });

        // Setup back button
        findViewById(R.id.back_button).setOnClickListener(v -> {
            finish();
        });
    }

    private void setupDateView() {
        dateContainer.removeAllViews();

        // Add 5 days (-2, -1, today, +1, +2)
        Calendar temp = (Calendar) currentDate.clone();
        temp.add(Calendar.DAY_OF_MONTH, -2);

        SimpleDateFormat dayFormat = new SimpleDateFormat("E", Locale.getDefault());
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd", Locale.getDefault());

        for (int i = 0; i < 5; i++) {
            View dateView = getLayoutInflater().inflate(R.layout.date_item, dateContainer, false);
            TextView dayText = dateView.findViewById(R.id.day_text);
            TextView dateText = dateView.findViewById(R.id.date_text);

            dayText.setText(dayFormat.format(temp.getTime()));
            dateText.setText(dateFormat.format(temp.getTime()));

            // Highlight today
            if (i == 2) {
                dateView.setBackgroundResource(R.drawable.current_date_background);
                dayText.setTextColor(getResources().getColor(R.color.white));
                dateText.setTextColor(getResources().getColor(R.color.white));
            }

            // Set click listener to open TodayViewActivity
            final Date clickedDate = temp.getTime();
            dateView.setOnClickListener(v -> {
                Intent intent = new Intent(KanbanViewActivity.this, TodayViewActivity.class);
                intent.putExtra("selected_date", clickedDate.getTime());
                startActivity(intent);
            });

            dateContainer.addView(dateView);
            temp.add(Calendar.DAY_OF_MONTH, 1);
        }
    }

    private void setupTimelineView() {
        KanbanTimelineAdapter adapter = new KanbanTimelineAdapter(this, taskList, currentDate.getTime());
        timelineRecyclerView.setAdapter(adapter);

        adapter.setOnTaskClickListener(task -> {
            Intent intent = new Intent(KanbanViewActivity.this, EditTaskActivity.class);
            intent.putExtra("task_id", task.getId());
            startActivity(intent);
        });
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupGestureDetection() {
        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            private static final int SWIPE_THRESHOLD = 100;
            private static final int SWIPE_VELOCITY_THRESHOLD = 100;

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                float diffX = e2.getX() - e1.getX();
                if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                    if (diffX > 0) {
                        // Swipe right, go to previous days
                        currentDate.add(Calendar.DAY_OF_MONTH, -5);
                    } else {
                        // Swipe left, go to next days
                        currentDate.add(Calendar.DAY_OF_MONTH, 5);
                    }
                    setupDateView();
                    setupTimelineView();
                    return true;
                }
                return false;
            }
        });

        timelineRecyclerView.setOnTouchListener((v, event) -> {
            gestureDetector.onTouchEvent(event);
            return false;
        });
    }

    private void initializeTaskList() {
        taskList = new ArrayList<>();

        // Add some demo tasks
        Calendar cal = Calendar.getInstance();

        // Classes for today
        Task task1 = new Task(1, "上课", "09:00 - 11:30", cal.getTime(), 150, false);
        cal.add(Calendar.HOUR_OF_DAY, 2);
        Task task2 = new Task(2, "午间休息", "11:30 - 12:30", cal.getTime(), 60, false);
        cal.add(Calendar.HOUR_OF_DAY, 1);
        Task task3 = new Task(3, "算法预习", "12:30 - 13:30", cal.getTime(), 60, true);
        cal.add(Calendar.HOUR_OF_DAY, 1);
        Task task4 = new Task(4, "上课", "13:30 - 17:00", cal.getTime(), 210, false);
        cal.add(Calendar.HOUR_OF_DAY, 1);
        Task task5 = new Task(5, "法律原理预习", "17:30 - 19:00", cal.getTime(), 90, true);
        cal.add(Calendar.HOUR_OF_DAY, 2);
        Task task6 = new Task(6, "上课", "19:00 - 21:00", cal.getTime(), 120, false);

        // Classes for tomorrow
        Calendar tomorrow = Calendar.getInstance();
        tomorrow.add(Calendar.DAY_OF_MONTH, 1);
        Task task7 = new Task(7, "上课", "15:10 - 17:45", tomorrow.getTime(), 155, false);

        // Meetings for next two days
        Calendar dayAfterTomorrow = Calendar.getInstance();
        dayAfterTomorrow.add(Calendar.DAY_OF_MONTH, 2);

        Task task8 = new Task(8, "赞协商会 (双周)", "22:00 - 23:00", tomorrow.getTime(), 60, true);
        Task task9 = new Task(9, "班团例会 (双周)", "21:30 - 23:00", dayAfterTomorrow.getTime(), 90, true);

        taskList.add(task1);
        taskList.add(task2);
        taskList.add(task3);
        taskList.add(task4);
        taskList.add(task5);
        taskList.add(task6);
        taskList.add(task7);
        taskList.add(task8);
        taskList.add(task9);
    }

    // Simple Task class for demonstration
}