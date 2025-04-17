package com.example.big

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.big.ProfileActivity
import java.util.Calendar

class MainActivity : AppCompatActivity() {
    private var profileImageView: ImageView? = null
    private var importantTasksRecyclerView: RecyclerView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 初始化视图
        initViews()

        // 设置重要事项列表
        setupImportantTasksList()

        // 设置点击事件
        setupClickListeners()
    }

    private fun initViews() {
        profileImageView = findViewById(R.id.profile_image)
        importantTasksRecyclerView = findViewById(R.id.important_tasks_recyclerview)
    }

    private fun setupImportantTasksList() {
        // 创建一个假的任务列表
        val importantTasks: MutableList<Task> = ArrayList()
        val cal = Calendar.getInstance()

        // Reset time to beginning of day
        cal[Calendar.HOUR_OF_DAY] = 0
        cal[Calendar.MINUTE] = 0
        cal[Calendar.SECOND] = 0
        cal[Calendar.MILLISECOND] = 0

        // 使用明确的十进制数字作为ID
        val taskId = 12345678 // 8位数ID

        // 使用正确的时间范围格式: "HH : MM -- HH : MM"
        importantTasks.add(
            Task(
                taskId,
                "上课",
                "19 : 00 -- 21 : 00",
                cal.time,
                120,
                false,
                "这是一个任务的简介"
            )
        )

        // 设置RecyclerView
        val taskAdapter = TaskAdapter(importantTasks, this)
        importantTasksRecyclerView!!.layoutManager = LinearLayoutManager(this)
        importantTasksRecyclerView!!.adapter = taskAdapter
    }

    private fun setupClickListeners() {
        // 头像点击事件 - 跳转到个人信息页面
        profileImageView!!.setOnClickListener { v: View? ->
            val intent = Intent(
                this@MainActivity,
                ProfileActivity::class.java
            )
            startActivity(intent)
        }

        // 今日入口点击事件
        val todayEntryCard = findViewById<CardView>(R.id.today_entry)
        todayEntryCard.setOnClickListener { v: View? ->
            val intent = Intent(
                this@MainActivity,
                TodayTasksActivity::class.java
            )
            startActivity(intent)
        }

        // 计划入口点击事件
        val planEntryCard = findViewById<CardView>(R.id.plan_entry)
        planEntryCard.setOnClickListener { v: View? ->
            val intent = Intent(
                this@MainActivity,
                KanbanViewActivity::class.java
            )
            startActivity(intent)
        }

        // 全部入口点击事件
        val allEntryCard = findViewById<CardView>(R.id.all_entry)
        allEntryCard.setOnClickListener { v: View? ->
            val intent = Intent(
                this@MainActivity,
                ListViewActivity::class.java
            )
            startActivity(intent)
        }

        // 统计入口点击事件
        val statsEntryCard = findViewById<CardView>(R.id.stats_entry)
        statsEntryCard.setOnClickListener { v: View? ->
            val intent = Intent(
                this@MainActivity,
                StatisticsActivity::class.java
            )
            startActivity(intent)
        }

        // 添加新事项按钮点击事件
        val addNewTaskButton = findViewById<CardView>(R.id.add_new_task_button)
        addNewTaskButton.setOnClickListener { v: View? ->
            val intent = Intent(
                this@MainActivity,
                AddTaskActivity::class.java
            )
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        // 如果从ProfileActivity返回，可能需要刷新头像
        // 这里可以添加从SharedPreferences或其他存储加载用户头像的代码
    }
}