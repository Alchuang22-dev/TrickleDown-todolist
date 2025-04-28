package models

import (
    "time"
    
    "go.mongodb.org/mongo-driver/bson/primitive"
)

// Task 表示一个任务
type Task struct {
    ID             primitive.ObjectID `json:"id,omitempty" bson:"_id,omitempty"`
    UserID         string             `json:"user_id" bson:"user_id"`
    Title          string             `json:"title" bson:"title"`
    TimeRange      string             `json:"time_range" bson:"time_range"`
    Date           time.Time          `json:"date" bson:"date"`
    DurationMinutes int                `json:"duration_minutes" bson:"duration_minutes"`
    IsImportant    bool               `json:"is_important" bson:"is_important"`
    Description    string             `json:"description,omitempty" bson:"description,omitempty"`
    DueDate        *time.Time         `json:"due_date,omitempty" bson:"due_date,omitempty"`
    Place          string             `json:"place,omitempty" bson:"place,omitempty"`
    IsFinished     bool               `json:"is_finished" bson:"is_finished"`
    IsDelayed      bool               `json:"is_delayed" bson:"is_delayed"`
    Category       string             `json:"category,omitempty" bson:"category,omitempty"`
    CreatedAt      time.Time          `json:"created_at" bson:"created_at"`
    UpdatedAt      time.Time          `json:"updated_at" bson:"updated_at"`
}

// TaskInput 表示创建或更新任务的输入数据
type TaskInput struct {
    Title          string     `json:"title" binding:"required"`
    TimeRange      string     `json:"time_range"`
    Date           time.Time  `json:"date" binding:"required"`
    DurationMinutes int        `json:"duration_minutes"`
    IsImportant    bool       `json:"is_important"`
    Description    string     `json:"description"`
    DueDate        *time.Time `json:"due_date"`
    Place          string     `json:"place"`
    Category       string     `json:"category"`
}

// FocusStats 表示专注统计数据
type FocusStats struct {
    FocusCount    int `json:"focus_count"`
    FocusDuration int `json:"focus_duration_minutes"` // 以分钟为单位
    TasksCompleted int `json:"tasks_completed"`
}

// FocusDistribution 表示专注时长分布
type FocusDistribution struct {
    Period string         `json:"period"` // "day", "week", "month"
    Data   map[string]int `json:"data"`   // 键是日期或小时，值是专注时长（分钟）
}

// TaskFilter 表示筛选条件
type TaskFilter struct {
    ShowAll       bool   `json:"show_all"`
    Unfinished    bool   `json:"unfinished"`
    Important     bool   `json:"important"`
    SortByTime    bool   `json:"sort_by_time"`
    CategoryFilter string `json:"category"`
}

// SearchQuery 表示搜索查询
type SearchQuery struct {
    Query string `json:"query" binding:"required"`
}

// PaginationParams 表示分页参数
type PaginationParams struct {
    Page  int `form:"page" json:"page"`
    Limit int `form:"limit" json:"limit"`
}