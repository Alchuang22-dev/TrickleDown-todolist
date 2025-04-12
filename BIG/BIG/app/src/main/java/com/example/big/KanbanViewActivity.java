package com.example.big;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.HorizontalScrollView;
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

        // 获取今天的日期（用于高亮判断）
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);

        // 添加5天 (-2, -1, today, +1, +2)
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

            // 高亮今天（如果今天在当前显示的日期范围内）
            if (isSameDay(temp.getTime(), today.getTime())) {
                dateView.setBackgroundResource(R.drawable.current_date_background);
                dayText.setTextColor(getResources().getColor(R.color.white));
                dateText.setTextColor(getResources().getColor(R.color.white));
            }

            // 设置点击监听器打开TodayViewActivity
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

    // 判断两个日期是否是同一天
    private boolean isSameDay(Date date1, Date date2) {
        Calendar cal1 = Calendar.getInstance();
        Calendar cal2 = Calendar.getInstance();
        cal1.setTime(date1);
        cal2.setTime(date2);
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
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
        // 创建手势检测器专门用于日期滚动
        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            private static final int SWIPE_THRESHOLD = 100;
            private static final int SWIPE_VELOCITY_THRESHOLD = 100;

            @Override
            public boolean onDown(MotionEvent e) {
                // 必须返回true以表示我们对此事件感兴趣
                return true;
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if (e1 == null || e2 == null) {
                    return false;
                }

                float diffX = e2.getX() - e1.getX();
                if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                    if (diffX > 0) {
                        // 向右滑动，显示更早的日期
                        currentDate.add(Calendar.DAY_OF_MONTH, -5);
                    } else {
                        // 向左滑动，显示更晚的日期
                        currentDate.add(Calendar.DAY_OF_MONTH, 5);
                    }
                    setupDateView();
                    setupTimelineView();
                    return true;
                }
                return false;
            }
        });

        // 获取日期滚动视图并添加触摸监听器
        HorizontalScrollView dateScrollView = findViewById(R.id.date_scroll_view);
        dateScrollView.setOnTouchListener((v, event) -> {
            // 将事件传递给手势检测器
            return gestureDetector.onTouchEvent(event);
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