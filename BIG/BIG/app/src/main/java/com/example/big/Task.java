package com.example.big;

public class Task {
    private final String title;
    private final String description;
    private final boolean isImportant;
    private final String dueDate;

    public Task(String title, String description, boolean isImportant, String dueDate) {
        this.title = title;
        this.description = description;
        this.isImportant = isImportant;
        this.dueDate = dueDate;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public boolean isImportant() {
        return isImportant;
    }

    public String getDueDate() {
        return dueDate;
    }

}