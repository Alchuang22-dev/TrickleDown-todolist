package com.example.big;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    private List<Task> taskList;
    private Context context;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault());

    public TaskAdapter(List<Task> taskList, Context context) {
        this.taskList = taskList;
        this.context = context;
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.task, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task task = taskList.get(position);

        holder.titleText.setText(task.getTitle());
        holder.timeRangeText.setText(task.getTimeRange());

        if (task.getDate() != null) {
            holder.dateText.setText(dateFormat.format(task.getDate()));
        } else {
            holder.dateText.setText("未设定日期");
        }

        if (task.getDescription() != null && !task.getDescription().isEmpty()) {
            holder.descriptionText.setText(task.getDescription());
            holder.descriptionText.setVisibility(View.VISIBLE);
        } else {
            holder.descriptionText.setVisibility(View.GONE);
        }

        // 设置任务项点击事件
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, EditTaskActivity.class);
                intent.putExtra("task_id", task.getId());
                context.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return taskList.size();
    }

    public static class TaskViewHolder extends RecyclerView.ViewHolder {
        CardView taskCardView;
        TextView titleText;
        TextView timeRangeText;
        TextView dateText;
        TextView descriptionText;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            taskCardView = itemView.findViewById(R.id.task_card_view);
            titleText = itemView.findViewById(R.id.task_title);
            timeRangeText = itemView.findViewById(R.id.task_time_range);
            dateText = itemView.findViewById(R.id.task_date);
            descriptionText = itemView.findViewById(R.id.task_description);
        }
    }
}