package com.example.big

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.Calendar
import java.util.Date
import java.util.Locale

class KanbanViewActivity : AppCompatActivity() {

    private lateinit var dateContainer: LinearLayout
    private lateinit var timelineRecyclerView: RecyclerView
    private lateinit var taskList: List<Task>
    private lateinit var currentDate: Calendar
    private lateinit var gestureDetector: GestureDetector

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_kanban_view)

        dateContainer = findViewById(R.id.date_container)
        timelineRecyclerView = findViewById(R.id.timeline_recycler_view)

        currentDate = Calendar.getInstance()

        // Initialize demo tasks
        initializeTaskList()

        // Setup dates
        setupDateView()

        // Setup timeline
        setupTimelineView()

        // Setup gesture detection for horizontal swiping
        setupGestureDetection()

        // Setup add task button
        val addTaskButton: FloatingActionButton = findViewById(R.id.add_task_button)
        addTaskButton.setOnClickListener {
            val intent = Intent(this@KanbanViewActivity, AddTaskActivity::class.java)
            startActivity(intent)
        }

        // Setup back button
        findViewById<View>(R.id.back_button).setOnClickListener {
            finish()
        }
    }

    private fun setupDateView() {
        dateContainer.removeAllViews()

        // 获取今天的日期（用于高亮判断）
        val today = Calendar.getInstance()
        today.set(Calendar.HOUR_OF_DAY, 0)
        today.set(Calendar.MINUTE, 0)
        today.set(Calendar.SECOND, 0)
        today.set(Calendar.MILLISECOND, 0)

        // 添加5天 (-2, -1, today, +1, +2)
        val temp = currentDate.clone() as Calendar
        temp.add(Calendar.DAY_OF_MONTH, -2)

        val dayFormat = SimpleDateFormat("E", Locale.getDefault())
        val dateFormat = SimpleDateFormat("dd", Locale.getDefault())

        for (i in 0 until 5) {
            val dateView = layoutInflater.inflate(R.layout.date_item, dateContainer, false)
            val dayText: TextView = dateView.findViewById(R.id.day_text)
            val dateText: TextView = dateView.findViewById(R.id.date_text)

            dayText.text = dayFormat.format(temp.time)
            dateText.text = dateFormat.format(temp.time)

            // 高亮今天（如果今天在当前显示的日期范围内）
            if (isSameDay(temp.time, today.time)) {
                dateView.setBackgroundResource(R.drawable.current_date_background)
                dayText.setTextColor(resources.getColor(R.color.white))
                dateText.setTextColor(resources.getColor(R.color.white))
            }

            // 设置点击监听器打开TodayViewActivity
            val clickedDate = temp.time
            dateView.setOnClickListener {
                val intent = Intent(this@KanbanViewActivity, TodayViewActivity::class.java)
                intent.putExtra("selected_date", clickedDate.time)
                startActivity(intent)
            }

            dateContainer.addView(dateView)
            temp.add(Calendar.DAY_OF_MONTH, 1)
        }
    }

    // 判断两个日期是否是同一天
    private fun isSameDay(date1: Date, date2: Date): Boolean {
        val cal1 = Calendar.getInstance()
        val cal2 = Calendar.getInstance()
        cal1.time = date1
        cal2.time = date2
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    private fun setupTimelineView() {
        val adapter = KanbanTimelineAdapter(this, taskList, currentDate.time)
        timelineRecyclerView.adapter = adapter

        adapter.setOnTaskClickListener { task ->
            val intent = Intent(this@KanbanViewActivity, EditTaskActivity::class.java)
            intent.putExtra("task_id", task.id)
            startActivity(intent)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupGestureDetection() {
        // 创建手势检测器专门用于日期滚动
        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            private val SWIPE_THRESHOLD = 100
            private val SWIPE_VELOCITY_THRESHOLD = 100

            override fun onDown(e: MotionEvent): Boolean {
                // 必须返回true以表示我们对此事件感兴趣
                return true
            }

            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                if (e1 == null || e2 == null) {
                    return false
                }

                val diffX = e2.x - e1.x
                if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                    if (diffX > 0) {
                        // 向右滑动，显示更早的日期
                        currentDate.add(Calendar.DAY_OF_MONTH, -5)
                    } else {
                        // 向左滑动，显示更晚的日期
                        currentDate.add(Calendar.DAY_OF_MONTH, 5)
                    }
                    setupDateView()
                    setupTimelineView()
                    return true
                }
                return false
            }
        })

        // 获取日期滚动视图并添加触摸监听器
        val dateScrollView = findViewById<HorizontalScrollView>(R.id.date_scroll_view)
        dateScrollView.setOnTouchListener { _, event ->
            // 将事件传递给手势检测器
            gestureDetector.onTouchEvent(event)
        }
    }

    private fun initializeTaskList() {
        taskList = ArrayList()

        // Add some demo tasks
        val cal = Calendar.getInstance()

        // Classes for today
        val task1 = Task("1", "上课", "09:00 - 11:30", cal.time, 150, false)
        cal.add(Calendar.HOUR_OF_DAY, 2)
        val task2 = Task("2", "午间休息", "11:30 - 12:30", cal.time, 60, false)
        cal.add(Calendar.HOUR_OF_DAY, 1)
        val task3 = Task("3", "算法预习", "12:30 - 13:30", cal.time, 60, true)
        cal.add(Calendar.HOUR_OF_DAY, 1)
        val task4 = Task("4", "上课", "13:30 - 17:00", cal.time, 210, false)
        cal.add(Calendar.HOUR_OF_DAY, 1)
        val task5 = Task("5", "法律原理预习", "17:30 - 19:00", cal.time, 90, true)
        cal.add(Calendar.HOUR_OF_DAY, 2)
        val task6 = Task("6", "上课", "19:00 - 21:00", cal.time, 120, false)

        // Classes for tomorrow
        val tomorrow = Calendar.getInstance()
        tomorrow.add(Calendar.DAY_OF_MONTH, 1)
        val task7 = Task("7", "上课", "15:10 - 17:45", tomorrow.time, 155, false)

        // Meetings for next two days
        val dayAfterTomorrow = Calendar.getInstance()
        dayAfterTomorrow.add(Calendar.DAY_OF_MONTH, 2)

        val task8 = Task("8", "赞协商会 (双周)", "22:00 - 23:00", tomorrow.time, 60, true)
        val task9 = Task("9", "班团例会 (双周)", "21:30 - 23:00", dayAfterTomorrow.time, 90, true)

        (taskList as ArrayList<Task>).apply {
            add(task1)
            add(task2)
            add(task3)
            add(task4)
            add(task5)
            add(task6)
            add(task7)
            add(task8)
            add(task9)
        }
    }

    // Simple Task class for demonstration
}