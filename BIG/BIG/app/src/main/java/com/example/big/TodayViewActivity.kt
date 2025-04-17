package com.example.big

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class TodayViewActivity : AppCompatActivity() {
    private var dateTitle: TextView? = null
    private var timelineRecyclerView: RecyclerView? = null
    private var selectedDate: Date? = null
    private var taskList: MutableList<Task>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_today_view)

        dateTitle = findViewById(R.id.date_title)
        timelineRecyclerView = findViewById(R.id.today_timeline_recycler_view)

        // Get selected date from intent
        val selectedDateMillis = intent.getLongExtra("selected_date", System.currentTimeMillis())
        selectedDate = Date(selectedDateMillis)

        // Initialize demo tasks
        initializeTaskList()

        // Set date title
        val dateFormat = SimpleDateFormat("yyyy年MM月dd日 EEE", Locale.getDefault())
        dateTitle?.setText(dateFormat.format(selectedDate))

        // Setup timeline view
        setupTimelineView()

        // Setup back button
        findViewById<View>(R.id.back_button).setOnClickListener { v: View? ->
            finish()
        }
    }

    private fun setupTimelineView() {
        val adapter = TodayTimelineAdapter(this, taskList ?: emptyList(), selectedDate ?: Date())
        timelineRecyclerView!!.adapter = adapter

        adapter.setOnTaskClickListener { task: Task ->
            val intent =
                Intent(
                    this@TodayViewActivity,
                    EditTaskActivity::class.java
                )
            intent.putExtra("task_id", task.id)
            startActivity(intent)
        }
    }

    private fun initializeTaskList() {
        taskList = ArrayList()

        // Add some demo tasks
        val cal = Calendar.getInstance()
        cal.time = selectedDate

        // Reset time to beginning of day
        cal[Calendar.HOUR_OF_DAY] = 0
        cal[Calendar.MINUTE] = 0
        cal[Calendar.SECOND] = 0
        cal[Calendar.MILLISECOND] = 0

        // Classes for today
        cal[Calendar.HOUR_OF_DAY] = 9
        val task1 = Task(1, "上课", "09:00 - 11:30", cal.time, 150, false)

        cal[Calendar.HOUR_OF_DAY] = 11
        cal[Calendar.MINUTE] = 30
        val task2 = Task(2, "午间休息", "11:30 - 12:30", cal.time, 60, false)

        cal[Calendar.HOUR_OF_DAY] = 12
        cal[Calendar.MINUTE] = 30
        val task3 = Task(3, "算法预习", "12:30 - 13:30", cal.time, 60, true)

        cal[Calendar.HOUR_OF_DAY] = 13
        cal[Calendar.MINUTE] = 30
        val task4 = Task(4, "上课", "13:30 - 17:00", cal.time, 210, false)

        cal[Calendar.HOUR_OF_DAY] = 15
        cal[Calendar.MINUTE] = 0
        val task4_overlap = Task(10, "课间休息", "15:00 - 15:30", cal.time, 30, false)

        cal[Calendar.HOUR_OF_DAY] = 17
        cal[Calendar.MINUTE] = 30
        val task5 = Task(5, "法律原理预习", "17:30 - 19:00", cal.time, 90, true)

        cal[Calendar.HOUR_OF_DAY] = 19
        cal[Calendar.MINUTE] = 0
        val task6 = Task(6, "上课", "19:00 - 21:00", cal.time, 120, false)

        cal[Calendar.HOUR_OF_DAY] = 21
        cal[Calendar.MINUTE] = 30
        val task9 = Task(9, "班团例会 (双周)", "21:30 - 23:00", cal.time, 90, true)

        taskList?.add(task1)
        taskList?.add(task2)
        taskList?.add(task3)
        taskList?.add(task4)
        taskList?.add(task4_overlap)
        taskList?.add(task5)
        taskList?.add(task6)
        taskList?.add(task9)
    }
}
