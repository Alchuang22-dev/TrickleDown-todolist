package com.example.big;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class Task {
    private int id;
    private String title;
    private String timeRange;
    private Date date;
    private int durationMinutes;
    private boolean important;
    private String description;
    //关联用户
    private int user_id;
    //倒计时
    private Date due_date;
    //地点
    private String place;
    //是否完成
    private boolean finished;
    //是否拖延
    private boolean delayed;
    //与别的任务存在时间重叠的个数
    private int layers;
    //从属的类别（如果有）
    private String category;

    public Task(int id, String title, String timeRange, Date date, int durationMinutes, boolean important) {
        this.id = id;
        this.title = title;
        this.timeRange = timeRange;
        this.date = date;
        this.durationMinutes = durationMinutes;
        this.important = important;
    }

    // （1）包含 description 的构造函数
    public Task(int id, String title, String timeRange, Date date, int durationMinutes, boolean important, String description) {
        this.id = id;
        this.title = title;
        this.timeRange = timeRange;
        this.date = date;
        this.durationMinutes = durationMinutes;
        this.important = important;
        this.description = description;
    }

    // （2）允许 timeRange 和 durationMinutes 使用默认值的构造函数
    public Task(int id, String title, Date date, boolean important, String description) {
        this.id = id;
        this.title = title;
        this.date = date;
        this.important = important;
        this.timeRange = "未设定时间"; // 或者你可以用 null / ""
        this.durationMinutes = 0;
        this.description = description;
    }

    // （3）添加包含 place 参数的构造函数
    public Task(int id, String title, String timeRange, Date date, int durationMinutes, boolean important, String description, String place) {
        this.id = id;
        this.title = title;
        this.timeRange = timeRange;
        this.date = date;
        this.durationMinutes = durationMinutes;
        this.important = important;
        this.description = description;
        this.place = place;
    }

    // （4）添加包含 place 和 due_date 参数的构造函数
    public Task(int id, String title, String timeRange, Date date, int durationMinutes, boolean important, String description, String place, Date due_date) {
        this.id = id;
        this.title = title;
        this.timeRange = timeRange;
        this.date = date;
        this.durationMinutes = durationMinutes;
        this.important = important;
        this.description = description;
        this.place = place;
        this.due_date = due_date;
    }

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getTimeRange() {
        return timeRange;
    }

    public Date getDate() {
        return date;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public boolean isImportant() {
        return important;
    }

    public String getDescription() {
        return description;
    }

    public Date getDueDate() {
        return due_date;
    }

    public String getPlace() {
        return place;
    }

    public void setDueDate(Date due_date) {
        this.due_date = due_date;
    }

    public void setCategory(String category){ this.category = category; }

    public boolean isFinished() { return finished; }

    public void setFinished(boolean finished) {this.finished = finished; }

    public void setPlace(String place) {this.place = place; }

    public void setDelayed(boolean delayed) {
        this.delayed = delayed;
    }

    public boolean isDelayed() {
        return delayed;
    }

    public String getCategory() { return category; }
    // 获取所有属性的方法
    public Map<String, Object> getAll() {
        Map<String, Object> taskData = new HashMap<>();
        taskData.put("id", id);
        taskData.put("title", title);
        taskData.put("timeRange", timeRange);
        taskData.put("date", date);
        taskData.put("durationMinutes", durationMinutes);
        taskData.put("important", important);
        taskData.put("description", description);
        taskData.put("user_id", user_id);
        taskData.put("due_date", due_date);
        taskData.put("place", place);
        taskData.put("finished", finished);
        taskData.put("delayed", delayed);
        taskData.put("layers", layers);
        taskData.put("category", category);
        return taskData;
    }

    // 编辑任务的方法
    public void edit(String title, String timeRange, Date date, int durationMinutes,
                     boolean important, String description, String place, Date due_date) {
        this.title = title;
        this.timeRange = timeRange;
        this.date = date;
        this.durationMinutes = durationMinutes;
        this.important = important;
        this.description = description;
        this.place = place;
        this.due_date = due_date;

        // 这里可以添加保存到数据库的代码
    }

    // 删除任务的方法
    public boolean delete() {
        // 这里添加从数据库中删除该任务的代码
        // 成功删除返回true，失败返回false
        return true;
    }
}