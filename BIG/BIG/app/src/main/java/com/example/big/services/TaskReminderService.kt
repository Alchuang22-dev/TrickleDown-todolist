//package com.example.big.services
//
//import android.app.AlarmManager
//import android.app.NotificationChannel
//import android.app.NotificationManager
//import android.app.PendingIntent
//import android.content.BroadcastReceiver
//import android.content.Context
//import android.content.Intent
//import android.os.Build
//import android.util.Log
//import androidx.core.app.NotificationCompat
//import com.example.big.R
//import com.example.big.EditTaskActivity
//import com.example.big.models.TaskResponse
//import com.example.big.utils.UserManager
//import java.text.SimpleDateFormat
//import java.util.*
//
//class TaskReminderService {
//    companion object {
//        private const val CHANNEL_ID = "task_reminder_channel"
//        private const val REMINDER_REQUEST_CODE = 1001
//        private const val TAG = "TaskReminderService"
//
//        // 设置任务提醒
//        fun scheduleTaskReminder(context: Context, task: TaskResponse) {
//            // 检查用户权限设置
//            val user = UserManager.getUserInfo() ?: return
//            val permissions = user.permissions ?: return
//
//            // 如果没有启用任何提醒权限，不设置提醒
//            if (!(permissions["NOTIFICATION"] == true || permissions["ALARM"] == true)) {
//                Log.d(TAG, "用户未启用提醒权限，不设置提醒")
//                return
//            }
//
//            try {
//                // 解析任务时间范围，获取开始时间
//                val timeRange = task.time_range
//                val startTime = parseTimeRange(timeRange)?.first ?: return
//
//                // 结合任务日期和开始时间创建完整的提醒时间
//                val taskDate = task.date
//                val calendar = Calendar.getInstance()
//                calendar.time = taskDate
//
//                // 设置日历的小时和分钟
//                calendar.set(Calendar.HOUR_OF_DAY, startTime.first)
//                calendar.set(Calendar.MINUTE, startTime.second)
//                calendar.set(Calendar.SECOND, 0)
//
//                // 创建提醒Intent
//                val intent = Intent(context, TaskReminderReceiver::class.java).apply {
//                    putExtra("taskId", task.id)
//                    putExtra("taskTitle", task.title)
//                    putExtra("taskDescription", task.description ?: "")
//                }
//
//                val pendingIntent = PendingIntent.getBroadcast(
//                    context,
//                    task.id.hashCode(), // 使用任务ID的哈希码作为请求码
//                    intent,
//                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
//                )
//
//                // 获取系统闹钟服务
//                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
//
//                // 设置提醒
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//                    if (permissions["ALARM"] == true) {
//                        // 使用精确闹钟
//                        alarmManager.setExactAndAllowWhileIdle(
//                            AlarmManager.RTC_WAKEUP,
//                            calendar.timeInMillis,
//                            pendingIntent
//                        )
//                        Log.d(TAG, "已为任务 ${task.id} 设置精确闹钟提醒: ${calendar.time}")
//                    } else {
//                        // 使用常规提醒
//                        alarmManager.set(
//                            AlarmManager.RTC_WAKEUP,
//                            calendar.timeInMillis,
//                            pendingIntent
//                        )
//                        Log.d(TAG, "已为任务 ${task.id} 设置常规提醒: ${calendar.time}")
//                    }
//                } else {
//                    // 旧版Android使用常规提醒
//                    alarmManager.set(
//                        AlarmManager.RTC_WAKEUP,
//                        calendar.timeInMillis,
//                        pendingIntent
//                    )
//                    Log.d(TAG, "已为任务 ${task.id} 设置常规提醒: ${calendar.time}")
//                }
//            } catch (e: Exception) {
//                Log.e(TAG, "设置任务提醒出错: ${e.message}", e)
//            }
//        }
//
//        // 取消任务提醒
//        fun cancelTaskReminder(context: Context, taskId: String) {
//            val intent = Intent(context, TaskReminderReceiver::class.java)
//            val pendingIntent = PendingIntent.getBroadcast(
//                context,
//                taskId.hashCode(),
//                intent,
//                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
//            )
//
//            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
//            alarmManager.cancel(pendingIntent)
//
//            Log.d(TAG, "已取消任务 $taskId 的提醒")
//        }
//
//        // 解析时间范围字符串，返回(小时,分钟)对
//        private fun parseTimeRange(timeRange: String): Pair<Pair<Int, Int>, Pair<Int, Int>>? {
//            try {
//                // 处理不同格式的时间范围
//                val delimiter = if (timeRange.contains("--")) "--" else "-"
//                val parts = timeRange.split(delimiter)
//
//                if (parts.size != 2) {
//                    Log.e(TAG, "时间范围格式错误: $timeRange")
//                    return null
//                }
//
//                val startTimeStr = parts[0].trim().replace(" ", "")
//                val endTimeStr = parts[1].trim().replace(" ", "")
//
//                // 解析开始时间
//                val startTimeParts = startTimeStr.split(":")
//                val startHour = startTimeParts[0].toInt()
//                val startMinute = startTimeParts[1].toInt()
//
//                // 解析结束时间
//                val endTimeParts = endTimeStr.split(":")
//                val endHour = endTimeParts[0].toInt()
//                val endMinute = endTimeParts[1].toInt()
//
//                return Pair(Pair(startHour, startMinute), Pair(endHour, endMinute))
//            } catch (e: Exception) {
//                Log.e(TAG, "解析时间范围出错: ${e.message}", e)
//                return null
//            }
//        }
//
//        // 创建通知渠道
//        fun createNotificationChannel(context: Context) {
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                val name = "任务提醒"
//                val descriptionText = "显示任务开始和截止提醒"
//                val importance = NotificationManager.IMPORTANCE_HIGH
//                val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
//                    description = descriptionText
//                }
//
//                // 注册通知渠道
//                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
//                notificationManager.createNotificationChannel(channel)
//
//                Log.d(TAG, "已创建通知渠道")
//            }
//        }
//    }
//}
//
//// 广播接收器，接收并显示任务提醒
//class TaskReminderReceiver : BroadcastReceiver() {
//    override fun onReceive(context: Context, intent: Intent) {
//        // 检查用户是否启用了通知权限
//        val user = UserManager.getUserInfo()
//        if (user?.permissions?.get("NOTIFICATION") != true) {
//            Log.d("TaskReminderReceiver", "用户未启用通知权限，不显示通知")
//            return
//        }
//
//        val taskId = intent.getStringExtra("taskId") ?: return
//        val taskTitle = intent.getStringExtra("taskTitle") ?: "任务提醒"
//        val taskDescription = intent.getStringExtra("taskDescription") ?: ""
//
//        // 创建点击通知时打开任务详情的意图
//        val detailIntent = Intent(context, TaskDetailActivity::class.java).apply {
//            putExtra("task_id", taskId)
//            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
//        }
//
//        val pendingIntent = PendingIntent.getActivity(
//            context,
//            taskId.hashCode(),
//            detailIntent,
//            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
//        )
//
//        // 构建通知
//        val builder = NotificationCompat.Builder(context, TaskReminderService.CHANNEL_ID)
//            .setSmallIcon(R.drawable.ic_notification)
//            .setContentTitle(taskTitle)
//            .setContentText("任务即将开始: $taskDescription")
//            .setPriority(NotificationCompat.PRIORITY_HIGH)
//            .setContentIntent(pendingIntent)
//            .setAutoCancel(true)
//
//        // 显示通知
//        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
//        notificationManager.notify(taskId.hashCode(), builder.build())
//
//        Log.d("TaskReminderReceiver", "已显示任务提醒通知: $taskTitle")
//    }
//}