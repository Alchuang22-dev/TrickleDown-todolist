package com.example.big;

import java.util.Date;

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
}