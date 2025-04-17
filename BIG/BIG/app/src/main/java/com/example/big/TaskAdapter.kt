package com.example.big

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Locale

class TaskAdapter(
    private val taskList: List<Task>,
    private val context: Context
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    private val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.task, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = taskList[position]

        holder.titleText.text = task.title
        holder.timeRangeText.text = task.timeRange

        if (task.date != null) {
            holder.dateText.text = dateFormat.format(task.date)
        } else {
            holder.dateText.text = "未设定日期"
        }

        if (!task.description.isNullOrEmpty()) {
            holder.descriptionText.text = task.description
            holder.descriptionText.visibility = View.VISIBLE
        } else {
            holder.descriptionText.visibility = View.GONE
        }

        // 设置任务项点击事件
        holder.itemView.setOnClickListener {
            val intent = Intent(context, EditTaskActivity::class.java).apply {
                putExtra("task_id", task.id)
            }
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = taskList.size


    class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val taskCardView: CardView = itemView.findViewById(R.id.task_card_view)
        val titleText: TextView = itemView.findViewById(R.id.task_title)
        val timeRangeText: TextView = itemView.findViewById(R.id.task_time_range)
        val dateText: TextView = itemView.findViewById(R.id.task_date)
        val descriptionText: TextView = itemView.findViewById(R.id.task_description)
    }
}