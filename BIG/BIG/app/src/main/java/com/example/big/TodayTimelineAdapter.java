package com.example.big;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TodayTimelineAdapter extends RecyclerView.Adapter<TodayTimelineAdapter.TimelineViewHolder> {

    private Context context;
    private List<Task> tasks;
    private Date selectedDate;
    private TodayTimelineAdapter.OnTaskClickListener listener;

    // Hours to display (from 7 AM to 23 PM)
    private static final int START_HOUR = 7;
    private static final int END_HOUR = 23;

    // Category column titles
    private static final String[] CATEGORIES = {"重要事项", "学习", "工作", "生活", "其他"};

    public interface OnTaskClickListener {
        void onTaskClick(Task task);
    }

    public TodayTimelineAdapter(Context context, List<Task> tasks, Date selectedDate) {
        this.context = context;
        this.tasks = tasks;
        this.selectedDate = selectedDate;
    }

    public void setOnTaskClickListener(OnTaskClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public TimelineViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_today_timeline_hour, parent, false);
        return new TimelineViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TimelineViewHolder holder, int position) {
        int hour = position + START_HOUR;
        holder.hourText.setText(String.format(Locale.getDefault(), "%02d", hour));

        // Clear existing task views
        holder.clearTaskViews();

        // Find tasks for this hour
        Calendar cal = Calendar.getInstance();
        cal.setTime(selectedDate);
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        Date hourStart = cal.getTime();
        cal.add(Calendar.HOUR_OF_DAY, 1);
        Date hourEnd = cal.getTime();

        for (Task task : tasks) {
            // Parse task start and end time
            String[] timeParts = task.getTimeRange().split(" - ");
            String[] startTimeParts = timeParts[0].split(":");

            Calendar taskStartCal = Calendar.getInstance();
            taskStartCal.setTime(task.getDate());
            taskStartCal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(startTimeParts[0]));
            taskStartCal.set(Calendar.MINUTE, Integer.parseInt(startTimeParts[1]));

            // Determine if task occurs during this hour
            boolean taskInThisHour = isSameDay(taskStartCal.getTime(), selectedDate) &&
                    (taskStartCal.get(Calendar.HOUR_OF_DAY) == hour ||
                            (task.getDurationMinutes() > 60 &&
                                    taskStartCal.get(Calendar.HOUR_OF_DAY) < hour &&
                                    taskStartCal.get(Calendar.HOUR_OF_DAY) + (task.getDurationMinutes() / 60) > hour));

            if (taskInThisHour) {
                // Create task view
                View taskView = LayoutInflater.from(context).inflate(R.layout.item_today_task, null);
                TextView taskTitle = taskView.findViewById(R.id.task_title);
                TextView taskTime = taskView.findViewById(R.id.task_time);

                taskTitle.setText(task.getTitle());
                taskTime.setText(task.getTimeRange());

                // Calculate top position based on start time
                int topMargin = 0;
                if (taskStartCal.get(Calendar.HOUR_OF_DAY) == hour) {
                    topMargin = (int) (taskStartCal.get(Calendar.MINUTE) * context.getResources().getDimension(R.dimen.minute_height));
                }

                // Calculate height based on duration
                int height = (int) (Math.min(60, getTaskDurationInThisHour(task, hour)) *
                        context.getResources().getDimension(R.dimen.minute_height));

                // Set background based on importance
                if (task.isImportant()) {
                    taskView.setBackgroundResource(R.drawable.important_task_background);
                    holder.addTaskView(taskView, 0, topMargin, height);
                } else {
                    taskView.setBackgroundResource(R.drawable.normal_task_background);
                    // Find first available column
                    int column = holder.findAvailableColumn(topMargin, height);
                    holder.addTaskView(taskView, column, topMargin, height);
                }

                // Set click listener
                taskView.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onTaskClick(task);
                    }
                });
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

    private int getTaskDurationInThisHour(Task task, int hour) {
        // Parse task start time
        String[] timeParts = task.getTimeRange().split(" - ");
        String[] startTimeParts = timeParts[0].split(":");

        Calendar taskStartCal = Calendar.getInstance();
        taskStartCal.setTime(task.getDate());
        taskStartCal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(startTimeParts[0]));
        taskStartCal.set(Calendar.MINUTE, Integer.parseInt(startTimeParts[1]));

        if (taskStartCal.get(Calendar.HOUR_OF_DAY) == hour) {
            // Task starts in this hour
            int minutesLeft = 60 - taskStartCal.get(Calendar.MINUTE);
            return Math.min(minutesLeft, task.getDurationMinutes());
        } else if (taskStartCal.get(Calendar.HOUR_OF_DAY) < hour) {
            // Task started in previous hour
            int hoursElapsed = hour - taskStartCal.get(Calendar.HOUR_OF_DAY);
            int minutesElapsed = hoursElapsed * 60 - taskStartCal.get(Calendar.MINUTE);
            int minutesLeft = task.getDurationMinutes() - minutesElapsed;
            return Math.min(60, minutesLeft);
        }

        return 0;
    }

    static class TimelineViewHolder extends RecyclerView.ViewHolder {
        TextView hourText;
        ViewGroup[] taskColumns;
        boolean[][] timeSlotOccupied; // [column][minute]

        TimelineViewHolder(@NonNull View itemView) {
            super(itemView);
            hourText = itemView.findViewById(R.id.hour_text);

            // Initialize columns
            taskColumns = new ViewGroup[5];
            taskColumns[0] = itemView.findViewById(R.id.important_column);
            taskColumns[1] = itemView.findViewById(R.id.category1_column);
            taskColumns[2] = itemView.findViewById(R.id.category2_column);
            taskColumns[3] = itemView.findViewById(R.id.category3_column);
            taskColumns[4] = itemView.findViewById(R.id.category4_column);

            // Initialize time slot tracking (5 columns, 60 minutes)
            timeSlotOccupied = new boolean[5][60];
        }

        void clearTaskViews() {
            for (ViewGroup column : taskColumns) {
                column.removeAllViews();
            }

            // Reset time slot tracking
            for (int i = 0; i < 5; i++) {
                for (int j = 0; j < 60; j++) {
                    timeSlotOccupied[i][j] = false;
                }
            }
        }

        int findAvailableColumn(int startMinute, int height) {
            int endMinute = Math.min(59, startMinute + height / (int) itemView.getContext().getResources().getDimension(R.dimen.minute_height));

            // Skip column 0 (important items column)
            for (int col = 1; col < 5; col++) {
                boolean available = true;
                for (int minute = startMinute; minute <= endMinute; minute++) {
                    if (minute < 60 && timeSlotOccupied[col][minute]) {
                        available = false;
                        break;
                    }
                }

                if (available) {
                    // Mark time slots as occupied
                    for (int minute = startMinute; minute <= endMinute; minute++) {
                        if (minute < 60) {
                            timeSlotOccupied[col][minute] = true;
                        }
                    }
                    return col;
                }
            }

            // If no column is available, use the last one
            return 4;
        }

        void addTaskView(View taskView, int column, int topMargin, int height) {
            ViewGroup.MarginLayoutParams params = new ViewGroup.MarginLayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, height);
            params.topMargin = topMargin;
            params.leftMargin = 2;
            params.rightMargin = 2;

            taskView.setLayoutParams(params);
            taskColumns[column].addView(taskView);
        }
    }
}