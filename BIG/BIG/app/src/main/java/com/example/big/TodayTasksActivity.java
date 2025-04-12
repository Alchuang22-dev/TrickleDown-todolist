package com.example.big;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.PopupMenu;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class TodayTasksActivity extends AppCompatActivity {

    private RecyclerView importantTasksRecyclerView;
    private RecyclerView otherTasksRecyclerView;
    private RecyclerView completedTasksRecyclerView;

    private List<Task> allTasks;
    private List<Task> importantTasks;
    private List<Task> otherTasks;
    private List<Task> completedTasks;

    private TaskAdapter importantTasksAdapter;
    private TaskAdapter otherTasksAdapter;
    private TaskAdapter completedTasksAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_today_tasks);

        // 初始化视图
        initViews();

        // 设置返回按钮
        ImageButton backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> finish());

        // 设置菜单按钮
        ImageButton menuButton = findViewById(R.id.menu_button);
        menuButton.setOnClickListener(this::showFilterMenu);

        // 设置添加任务按钮
        FloatingActionButton addTaskButton = findViewById(R.id.add_task_button);
        addTaskButton.setOnClickListener(v -> {
            Intent intent = new Intent(TodayTasksActivity.this, AddTaskActivity.class);
            startActivity(intent);
        });

        // 初始化任务数据
        initTaskData();

        // 设置任务列表
        setupTaskLists();
    }

    private void initViews() {
        importantTasksRecyclerView = findViewById(R.id.important_tasks_recyclerview);
        otherTasksRecyclerView = findViewById(R.id.other_tasks_recyclerview);
        completedTasksRecyclerView = findViewById(R.id.completed_tasks_recyclerview);

        importantTasksRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        otherTasksRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        completedTasksRecyclerView.setLayoutManager(new LinearLayoutManager(this));
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
                filterByTime();
                return true;
            } else if (itemId == R.id.menu_by_category) {
                filterByCategory();
                return true;
            } else if (itemId == R.id.menu_all) {
                filterNone();
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

        int taskId1 = 12345001;
        int taskId2 = 12345002;
        int taskId3 = 12345003;
        int taskId4 = 12345004;
        int taskId5 = 12345005;
        int taskId6 = 12345006;

        // 重要任务
        Task task1 = new Task(taskId1, "完成项目报告", "09:00 -- 11:00", today, 120, true, "需要提交给经理审核");
        task1.setPlace("办公室");
        Task task2 = new Task(taskId2, "客户会议", "14:00 -- 15:30", today, 90, true, "讨论新产品方案");
        task2.setPlace("会议室A");

        // 其他任务
        Task task3 = new Task(taskId3, "午餐", "12:00 -- 13:00", today, 60, false, "与同事共进午餐");
        Task task4 = new Task(taskId4, "整理邮件", "16:00 -- 17:00", today, 60, false);

        // 已完成任务
        Task task5 = new Task(taskId5, "晨会", "08:30 -- 09:00", today, 30, false, "每日工作安排");
        task5.setFinished(true);
        Task task6 = new Task(taskId6, "回复客户邮件", "10:00 -- 10:30", today, 30, false);
        task6.setFinished(true);

        // 添加到列表
        allTasks.add(task1);
        allTasks.add(task2);
        allTasks.add(task3);
        allTasks.add(task4);
        allTasks.add(task5);
        allTasks.add(task6);

        TaskAdapter taskAdapter = new TaskAdapter(importantTasks, this);
        importantTasksRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        importantTasksRecyclerView.setAdapter(taskAdapter);

        // 分类
        categorizeTasksForDisplay();
    }

    private void categorizeTasksForDisplay() {
        importantTasks = new ArrayList<>();
        otherTasks = new ArrayList<>();
        completedTasks = new ArrayList<>();

        for (Task task : allTasks) {
            if (task.isFinished()) {
                completedTasks.add(task);
            } else if (task.isImportant()) {
                importantTasks.add(task);
            } else {
                otherTasks.add(task);
            }
        }
    }

    private void setupTaskLists() {
        importantTasksAdapter = new TaskAdapter(importantTasks, this);
        otherTasksAdapter = new TaskAdapter(otherTasks, this);
        completedTasksAdapter = new TaskAdapter(completedTasks, this);

        importantTasksRecyclerView.setAdapter(importantTasksAdapter);
        otherTasksRecyclerView.setAdapter(otherTasksAdapter);
        completedTasksRecyclerView.setAdapter(completedTasksAdapter);
    }

    // 筛选未完成的任务
    private void filterUnfinishedTasks() {
        List<Task> unfinishedTasks = new ArrayList<>();
        for (Task task : allTasks) {
            if (!task.isFinished()) {
                unfinishedTasks.add(task);
            }
        }
        updateAllTaskLists(unfinishedTasks);
    }

    // 筛选重要任务
    private void filterImportantTasks() {
        List<Task> filteredTasks = new ArrayList<>();
        for (Task task : allTasks) {
            if (task.isImportant()) {
                filteredTasks.add(task);
            }
        }
        updateAllTaskLists(filteredTasks);
    }

    // 按时间筛选
    private void filterByTime() {
        // 这里可以实现时间筛选逻辑
        // 示例：按照时间顺序排序
        List<Task> sortedTasks = new ArrayList<>(allTasks);
        sortedTasks.sort((t1, t2) -> {
            if (t1.getTimeRange() == null || t2.getTimeRange() == null) {
                return 0;
            }
            return t1.getTimeRange().compareTo(t2.getTimeRange());
        });
        updateAllTaskLists(sortedTasks);
    }

    // 按类别筛选
    private void filterByCategory() {
        // 这里可以实现类别筛选逻辑
        // 示例：显示所有任务并按类别分组（这里我们没有实际的类别数据）
        updateAllTaskLists(allTasks);
    }

    private void filterNone() {
        updateAllTaskLists(allTasks);
    }

    private void updateAllTaskLists(List<Task> filteredTasks) {
        // 清空当前列表
        importantTasks.clear();
        otherTasks.clear();
        completedTasks.clear();

        // 重新分类
        for (Task task : filteredTasks) {
            if (task.isFinished()) {
                completedTasks.add(task);
            } else if (task.isImportant()) {
                importantTasks.add(task);
            } else {
                otherTasks.add(task);
            }
        }

        // 通知适配器数据已更改
        importantTasksAdapter.notifyDataSetChanged();
        otherTasksAdapter.notifyDataSetChanged();
        completedTasksAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 刷新数据（实际应用中，这里应该从数据库重新加载数据）
        initTaskData();
        setupTaskLists();
    }
}