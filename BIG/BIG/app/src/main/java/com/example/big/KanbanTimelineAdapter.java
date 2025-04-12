package com.example.big;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
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
    private Calendar today;
    private int numDays = 5; // 要显示的天数

    // 小时范围（7 AM 到 23 PM）
    private static final int START_HOUR = 7;
    private static final int END_HOUR = 23;

    // 每5分钟的高度（dp）
    private static final float FIVE_MIN_HEIGHT_DP = 5.0f;

    public interface OnTaskClickListener {
        void onTaskClick(Task task);
    }

    public KanbanTimelineAdapter(Context context, List<Task> tasks, Date currentDate) {
        this.context = context;
        this.tasks = tasks;
        this.currentDate = currentDate;

        // 初始化今天的日期（仅保留年月日，清除时分秒）
        this.today = Calendar.getInstance();
        this.today.setTime(new Date());
        this.today.set(Calendar.HOUR_OF_DAY, 0);
        this.today.set(Calendar.MINUTE, 0);
        this.today.set(Calendar.SECOND, 0);
        this.today.set(Calendar.MILLISECOND, 0);
    }

    public void setOnTaskClickListener(OnTaskClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public TimelineViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_timeline_hour_multi, parent, false);
        return new TimelineViewHolder(view, numDays);
    }

    @Override
    public void onBindViewHolder(@NonNull TimelineViewHolder holder, int position) {
        int hour = position + START_HOUR;
        holder.hourText.setText(String.format(Locale.getDefault(), "%02d", hour));

        // 清除现有任务视图
        holder.clearTaskViews();

        // 获取当前日期范围的起始日期
        Calendar startDateCal = Calendar.getInstance();
        startDateCal.setTime(currentDate);
        startDateCal.add(Calendar.DAY_OF_MONTH, -2); // 从当前日期前2天开始

        // 遍历日期列
        for (int dayIndex = 0; dayIndex < numDays; dayIndex++) {
            Calendar dateCal = (Calendar) startDateCal.clone();
            dateCal.add(Calendar.DAY_OF_MONTH, dayIndex);

            // 设置为当天的特定小时
            dateCal.set(Calendar.HOUR_OF_DAY, hour);
            dateCal.set(Calendar.MINUTE, 0);
            dateCal.set(Calendar.SECOND, 0);
            dateCal.set(Calendar.MILLISECOND, 0);

            // 查找此日期和小时的任务
            for (Task task : tasks) {
                // 解析任务开始和结束时间
                if (!isSameDay(task.getDate(), dateCal.getTime())) {
                    continue;
                }

                // 解析任务时间范围
                String[] timeParts = task.getTimeRange().split(" - ");
                if (timeParts.length < 2) continue;

                String[] startTimeParts = timeParts[0].split(":");
                if (startTimeParts.length < 2) continue;

                int taskStartHour = Integer.parseInt(startTimeParts[0]);
                int taskStartMinute = Integer.parseInt(startTimeParts[1]);

                // 判断任务是否在此小时内发生
                if (taskStartHour == hour ||
                        (task.getDurationMinutes() > 0 &&
                                taskStartHour < hour &&
                                taskStartHour + ((taskStartMinute + task.getDurationMinutes()) / 60) > hour)) {

                    // 创建任务视图
                    addTaskViewToHolder(holder, task, hour, dayIndex);
                }
            }
        }
    }

    private void addTaskViewToHolder(TimelineViewHolder holder, Task task, int hour, int dayIndex) {
        View taskView = LayoutInflater.from(context).inflate(R.layout.item_task, null);
        TextView taskTitle = taskView.findViewById(R.id.task_title);
        TextView taskTime = taskView.findViewById(R.id.task_time);

        taskTitle.setText(task.getTitle());
        taskTime.setText(task.getTimeRange());

        // 解析任务开始时间
        String[] timeParts = task.getTimeRange().split(" - ")[0].split(":");
        int taskStartHour = Integer.parseInt(timeParts[0]);
        int taskStartMinute = Integer.parseInt(timeParts[1]);

        // 计算top margin（如果任务在此小时开始）
        int topMarginPx = 0;
        if (taskStartHour == hour) {
            // 根据5分钟精度计算top margin
            int minuteBlocks = taskStartMinute / 5;
            float fiveMinHeightPx = dpToPx(FIVE_MIN_HEIGHT_DP);
            topMarginPx = Math.round(minuteBlocks * fiveMinHeightPx);
        }

        // 计算此小时内的任务高度
        int taskDurationInThisHour;
        if (taskStartHour == hour) {
            // 任务在此小时开始
            int minutesLeft = 60 - taskStartMinute;
            taskDurationInThisHour = Math.min(minutesLeft, task.getDurationMinutes());
        } else {
            // 任务从前一个小时延续
            int hoursElapsed = hour - taskStartHour;
            int minutesElapsed = hoursElapsed * 60 - taskStartMinute;
            int minutesLeft = task.getDurationMinutes() - minutesElapsed;
            taskDurationInThisHour = Math.min(60, minutesLeft);
        }

        // 根据5分钟精度计算高度
        int heightBlocks = (int) Math.ceil(taskDurationInThisHour / 5.0);
        int heightPx = Math.round(heightBlocks * dpToPx(FIVE_MIN_HEIGHT_DP));

        // 设置背景颜色
        if (task.isImportant()) {
            taskView.setBackgroundResource(R.drawable.important_task_background);
        } else {
            taskView.setBackgroundResource(R.drawable.normal_task_background);
        }

        // 添加任务视图到相应的日期列
        ViewGroup.MarginLayoutParams layoutParams = new ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, heightPx);
        layoutParams.topMargin = topMarginPx;
        layoutParams.leftMargin = 2;
        layoutParams.rightMargin = 2;
        taskView.setLayoutParams(layoutParams);

        // 设置点击监听器
        taskView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onTaskClick(task);
            }
        });

        holder.addTaskView(taskView, dayIndex, topMarginPx, heightPx);
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

    private float dpToPx(float dp) {
        return dp * context.getResources().getDisplayMetrics().density;
    }

    static class TimelineViewHolder extends RecyclerView.ViewHolder {
        TextView hourText;
        FrameLayout[] dayColumns;
        boolean[][] timeSlotOccupied; // [day][minute/5]

        TimelineViewHolder(@NonNull View itemView, int numDays) {
            super(itemView);
            hourText = itemView.findViewById(R.id.hour_text);

            // 初始化日期列
            dayColumns = new FrameLayout[numDays];
            for (int i = 0; i < numDays; i++) {
                int resId = itemView.getResources().getIdentifier("day_column_" + i, "id", itemView.getContext().getPackageName());
                dayColumns[i] = itemView.findViewById(resId);
            }

            // 初始化时间段跟踪（numDays列，每小时12个5分钟块）
            timeSlotOccupied = new boolean[numDays][12];
        }

        void clearTaskViews() {
            for (FrameLayout column : dayColumns) {
                column.removeAllViews();
            }

            // 重置时间段跟踪
            for (int i = 0; i < dayColumns.length; i++) {
                for (int j = 0; j < 12; j++) {
                    timeSlotOccupied[i][j] = false;
                }
            }
        }

        void addTaskView(View taskView, int dayIndex, int topMargin, int height) {
            if (dayIndex >= 0 && dayIndex < dayColumns.length) {
                dayColumns[dayIndex].addView(taskView);
            }
        }
    }
}