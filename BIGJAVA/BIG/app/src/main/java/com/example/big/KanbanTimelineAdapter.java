package com.example.big;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import androidx.cardview.widget.CardView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
    private static final float FIVE_MIN_HEIGHT_DP = 6.0f;

    // 进一步增加最小任务高度以确保文字完全显示
    private static final float MIN_TASK_HEIGHT_DP = 50.0f; // 修改1: 从40dp增加到50dp

    // 存储处理过的任务ID，避免在同一个任务中重复显示内容
    private Map<Integer, Boolean> processedTaskIds = new HashMap<>();

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

        // 修改2: 每个小时重置处理过的任务ID
        processedTaskIds.clear();

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

                    // 创建任务视图，处理跨小时任务
                    addTaskViewToHolder(holder, task, hour, dayIndex, taskStartHour);
                }
            }
        }
    }

    private void addTaskViewToHolder(TimelineViewHolder holder, Task task, int hour, int dayIndex, int taskStartHour) {
        // 修改3: 检查这个任务在这一天是否已经处理过，避免重复显示
        String taskKey = task.getId() + "_" + dayIndex;
        Boolean processed = processedTaskIds.get(task.getId());

        // 如果不是任务开始的小时，且已经处理过这个任务，就跳过（避免重复）
        if (taskStartHour != hour && processed != null && processed) {
            return;
        }

        // 标记这个任务ID已经处理过
        processedTaskIds.put(task.getId(), true);

        View taskView = LayoutInflater.from(context).inflate(R.layout.item_task, null);
        TextView taskTitle = taskView.findViewById(R.id.task_title);
        TextView taskTime = taskView.findViewById(R.id.task_time);
        CardView taskCard = taskView.findViewById(R.id.task_card);

        taskTitle.setText(task.getTitle());

        // 解析任务开始和结束时间
        String[] timeParts = task.getTimeRange().split(" - ");
        String startTime = timeParts[0];
        String endTime = timeParts.length > 1 ? timeParts[1] : "";

        // 解析具体时间
        String[] startTimeParts = startTime.split(":");
        int taskStartHourFromTime = Integer.parseInt(startTimeParts[0]);
        int taskStartMinute = Integer.parseInt(startTimeParts[1]);

        String[] endTimeParts = endTime.split(":");
        int taskEndHour = Integer.parseInt(endTimeParts[0]);
        int taskEndMinute = Integer.parseInt(endTimeParts[1]);

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

        // 修改4: 特殊处理跨小时的边界情况
        // 如果任务在当前小时结束，使用结束分钟计算
        if (taskEndHour == hour) {
            taskDurationInThisHour = taskEndMinute;
        } else if (taskStartHour == hour) {
            // 任务在此小时开始
            int minutesLeft = 60 - taskStartMinute;
            taskDurationInThisHour = Math.min(minutesLeft, task.getDurationMinutes());
        } else {
            // 任务从前一个小时延续到整个当前小时
            taskDurationInThisHour = 60;
        }

        // 根据5分钟精度计算高度
        int heightBlocks = (int) Math.ceil(taskDurationInThisHour / 5.0);
        int heightPx = Math.round(heightBlocks * dpToPx(FIVE_MIN_HEIGHT_DP));

        // 确保任务视图至少有最小高度
        int minHeightPx = Math.round(dpToPx(MIN_TASK_HEIGHT_DP));
        if (heightPx < minHeightPx) {
            heightPx = minHeightPx;
        }

        // 修改5: 根据任务高度决定是否显示时间
        if (heightPx < minHeightPx * 1.2) { // 如果高度接近最小高度，只显示标题
            taskTitle.setMaxLines(2); // 允许标题最多两行
            taskTime.setVisibility(View.GONE); // 隐藏时间文本
        } else {
            taskTime.setText(task.getTimeRange());
            taskTime.setVisibility(View.VISIBLE);
        }

        // 设置更明显的背景颜色和样式
        if (task.isImportant()) {
            taskCard.setCardBackgroundColor(ContextCompat.getColor(context, R.color.important_task_bg));
            View importanceIndicator = taskView.findViewById(R.id.importance_indicator);
            if (importanceIndicator != null) {
                importanceIndicator.setBackgroundColor(ContextCompat.getColor(context, R.color.important_task_indicator));
                importanceIndicator.setVisibility(View.VISIBLE);
            }
        } else {
            taskCard.setCardBackgroundColor(ContextCompat.getColor(context, R.color.normal_task_bg));
            View importanceIndicator = taskView.findViewById(R.id.importance_indicator);
            if (importanceIndicator != null) {
                importanceIndicator.setBackgroundColor(ContextCompat.getColor(context, R.color.normal_task_indicator));
                importanceIndicator.setVisibility(View.VISIBLE);
            }
        }

        // 增加卡片阴影以提高视觉区分度
        taskCard.setCardElevation(4f);

        // 修改6: 调整任务结束时间，使其与小时网格对齐
        // 如果任务结束时间是整点或半点，调整高度确保视觉上对齐
        if (taskEndHour == hour + 1 && taskEndMinute == 0) {
            // 任务在下一个整点结束，确保视觉上对齐到小时线
            heightPx = 60 * Math.round(dpToPx(FIVE_MIN_HEIGHT_DP) / 5);
        } else if (taskEndHour == hour && (taskEndMinute == 0 || taskEndMinute == 30)) {
            // 任务在当前小时的整点或半点结束，调整以对齐
            int exactHeightPx = Math.round((taskEndMinute / 5) * dpToPx(FIVE_MIN_HEIGHT_DP));
            if (exactHeightPx > minHeightPx) {
                heightPx = exactHeightPx;
            }
        }

        // 添加任务视图到相应的日期列
        ViewGroup.MarginLayoutParams layoutParams = new ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, heightPx);
        layoutParams.topMargin = topMarginPx;
        layoutParams.leftMargin = 3;
        layoutParams.rightMargin = 3;
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