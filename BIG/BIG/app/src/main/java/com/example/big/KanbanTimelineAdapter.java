package com.example.big;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class KanbanTimelineAdapter extends RecyclerView.Adapter<KanbanTimelineAdapter.TimelineViewHolder> {

    private Context context;
    private List<Task> tasks;
    private Date currentDate;
    private OnTaskClickListener listener;

    // Hours to display (from 7 AM to 23 PM)
    private static final int START_HOUR = 7;
    private static final int END_HOUR = 23;

    public interface OnTaskClickListener {
        void onTaskClick(Task task);
    }

    public KanbanTimelineAdapter(Context context, List<Task> tasks, Date currentDate) {
        this.context = context;
        this.tasks = tasks;
        this.currentDate = currentDate;
    }

    public void setOnTaskClickListener(OnTaskClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public TimelineViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_timeline_hour, parent, false);
        return new TimelineViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TimelineViewHolder holder, int position) {
        int hour = position + START_HOUR;
        holder.hourText.setText(String.format(Locale.getDefault(), "%02d", hour));

        // Clear existing task views
        holder.taskContainer.removeAllViews();

        // Find tasks for this hour
        Calendar cal = Calendar.getInstance();
        cal.setTime(currentDate);
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        Date hourStart = cal.getTime();
        cal.add(Calendar.HOUR_OF_DAY, 1);
        Date hourEnd = cal.getTime();

        for (Task task : tasks) {
            // Parse task start time
            String[] timeParts = task.getTimeRange().split(" - ")[0].split(":");
            Calendar taskCal = Calendar.getInstance();
            taskCal.setTime(task.getDate());
            taskCal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(timeParts[0]));
            taskCal.set(Calendar.MINUTE, Integer.parseInt(timeParts[1]));

            // Check if task starts in this hour
            if (isSameDay(taskCal.getTime(), currentDate) &&
                    taskCal.get(Calendar.HOUR_OF_DAY) == hour) {

                // Create task view
                View taskView = LayoutInflater.from(context).inflate(R.layout.item_task, holder.taskContainer, false);
                TextView taskTitle = taskView.findViewById(R.id.task_title);
                TextView taskTime = taskView.findViewById(R.id.task_time);

                taskTitle.setText(task.getTitle());
                taskTime.setText(task.getTimeRange());

                // Set height based on duration
                ViewGroup.LayoutParams params = taskView.getLayoutParams();
                params.height = (int) (task.getDurationMinutes() * context.getResources().getDimension(R.dimen.minute_height));
                taskView.setLayoutParams(params);

                // Set background based on importance
                if (task.isImportant()) {
                    taskView.setBackgroundResource(R.drawable.important_task_background);
                } else {
                    taskView.setBackgroundResource(R.drawable.normal_task_background);
                }

                // Set click listener
                taskView.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onTaskClick(task);
                    }
                });

                holder.taskContainer.addView(taskView);
            }
        }
    }

    @Override
    public int getItemCount() {
        return END_HOUR - START_HOUR + 1;
    }

    private boolean isSameDay(Date date1, Date date2) {
        Calendar cal1 = Calendar.getInstance();
        Calendar cal2 = Calendar.getInstance();
        cal1.setTime(date1);
        cal2.setTime(date2);
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }

    static class TimelineViewHolder extends RecyclerView.ViewHolder {
        TextView hourText;
        ConstraintLayout taskContainer;

        TimelineViewHolder(@NonNull View itemView) {
            super(itemView);
            hourText = itemView.findViewById(R.id.hour_text);
            taskContainer = itemView.findViewById(R.id.task_container);
        }
    }
}