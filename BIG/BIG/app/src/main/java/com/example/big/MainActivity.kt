package com.example.big

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Calendar
import java.util.Date

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val importantTasks = createSampleTasks()

        setContent {
            MaterialTheme {
                MainScreen(
                    importantTasks = importantTasks,
                    onNavigate = { activityClass ->
                        startActivity(Intent(this, activityClass))
                    },
                    onTaskClick = { task ->
                        val intent = Intent(this, EditTaskActivity::class.java).apply {
                            putExtra("task_id", task.id)

                        }
                        // 弹出包含 ID 的 Toast
                        Toast.makeText(
                            /* context = */ this@MainActivity,   // 或 holder.itemView.context 等
                            /* text     = */ "任务 ID: ${task.id}",
                            /* duration = */ Toast.LENGTH_SHORT
                        ).show()
                        startActivity(intent)
                    }
                )
            }
        }
    }

    private fun createSampleTasks(): List<Task> {
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

        return importantTasks
    }
}

// 用于模拟原来的Task类

@Composable
fun HeaderSection(onProfileClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "我的待办",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF333333)
        )

        Image(
            painter = painterResource(id = R.drawable.default_avatar),
            contentDescription = "个人头像",
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .border(2.dp, Color.White, CircleShape)
                .clickable(onClick = onProfileClick)
        )
    }
}


@Composable
fun MainScreen(
    importantTasks: List<Task>,
    onNavigate: (Class<out Activity>) -> Unit,
    onTaskClick: (Task) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        HeaderSection(onProfileClick = { onNavigate(ProfileActivity::class.java) })

        Spacer(modifier = Modifier.height(24.dp))

        EntriesGrid(onNavigate = onNavigate)

        Spacer(modifier = Modifier.height(24.dp))

        AddTaskButton(onClick = { onNavigate(AddTaskActivity::class.java) })

        Spacer(modifier = Modifier.height(24.dp))

        ImportantTasksSection(
            tasks = importantTasks,
            onTaskClick = onTaskClick
        )
    }
}

@Composable
fun EntriesGrid(onNavigate: (Class<out Activity>) -> Unit) {
    val entries = listOf(
        Triple("今日", Color(0xFF4CAF50), TodayTasksActivity::class.java),
        Triple("计划", Color(0xFF2196F3), KanbanViewActivity::class.java),
        Triple("全部", Color(0xFFFF9800), ListViewActivity::class.java),
        Triple("统计", Color(0xFF9C27B0), StatisticsActivity::class.java)
    )

    // 使用固定高度而不是自适应高度
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        // 修改为200dp高度，与原XML更匹配
        modifier = Modifier.height(200.dp),
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(entries) { (label, color, destination) ->
            EntryCard(
                title = label,
                backgroundColor = color,
                onClick = { onNavigate(destination) }
            )
        }
    }
}

@Composable
fun EntryCard(
    title: String,
    backgroundColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            // 确保卡片是正方形
            .height(80.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun AddTaskButton(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE91E63))
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "添加新事项",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun ImportantTasksSection(
    tasks: List<Task>,
    onTaskClick: (Task) -> Unit // 添加任务点击回调
) {
    Column {
        Text(
            text = "今日重要事项",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF333333),
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            LazyColumn(
                modifier = Modifier.padding(8.dp)
            ) {
                items(tasks) { task ->
                    TaskItem(
                        task = task,
                        onClick = { onTaskClick(task) } // 传递点击事件
                    )
                    if (tasks.indexOf(task) < tasks.size - 1) {
                        Divider()
                    }
                }
            }
        }
    }
}

@Composable
fun TaskItem(
    task: Task,
    onClick: () -> Unit // 添加点击回调
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick) // 添加点击事件
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = task.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = task.timeRange,
                fontSize = 14.sp,
                color = Color.Gray
            )
            task.description?.let {
                Text(
                    text = it,
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }
        }

        Icon(
            imageVector = Icons.Default.KeyboardArrowRight,
            contentDescription = "查看详情",
            tint = Color.Gray
        )
    }
}