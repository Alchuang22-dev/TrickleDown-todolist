package controllers

import (
    "net/http"
    //"strconv"
    "time"
    
    "github.com/Alchuang22-dev/backend/models"
    "github.com/Alchuang22-dev/backend/repositories"
    "github.com/gin-gonic/gin"
    //"go.mongodb.org/mongo-driver/bson/primitive"
)

// TaskController 处理任务相关的请求
type TaskController struct {
    taskRepo repositories.TaskRepository
}

// NewTaskController 创建一个新的任务控制器
func NewTaskController(taskRepo repositories.TaskRepository) *TaskController {
    return &TaskController{
        taskRepo: taskRepo,
    }
}

// CreateTask 创建一个新任务
func (c *TaskController) CreateTask(ctx *gin.Context) {
    userID := ctx.Param("user_id")
    if userID == "" {
        ctx.JSON(http.StatusBadRequest, gin.H{"error": "用户ID不能为空"})
        return
    }
    
    var taskInput models.TaskInput
    if err := ctx.ShouldBindJSON(&taskInput); err != nil {
        ctx.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
        return
    }
    
    task := &models.Task{
        UserID:         userID,
        Title:          taskInput.Title,
        TimeRange:      taskInput.TimeRange,
        Date:           taskInput.Date,
        DurationMinutes: taskInput.DurationMinutes,
        IsImportant:    taskInput.IsImportant,
        Description:    taskInput.Description,
        DueDate:        taskInput.DueDate,
        Place:          taskInput.Place,
        IsFinished:     false,
        IsDelayed:      false,
        Category:       taskInput.Category,
    }
    
    taskID, err := c.taskRepo.Create(ctx, task)
    if err != nil {
        ctx.JSON(http.StatusInternalServerError, gin.H{"error": "创建任务失败"})
        return
    }
    
    ctx.JSON(http.StatusCreated, gin.H{
        "task_id": taskID,
        "message": "任务创建成功",
    })
}

// GetTaskByID 通过ID获取任务
func (c *TaskController) GetTaskByID(ctx *gin.Context) {
    taskID := ctx.Param("task_id")
    if taskID == "" {
        ctx.JSON(http.StatusBadRequest, gin.H{"error": "任务ID不能为空"})
        return
    }
    
    task, err := c.taskRepo.GetByID(ctx, taskID)
    if err != nil {
        ctx.JSON(http.StatusNotFound, gin.H{"error": "任务不存在"})
        return
    }
    
    ctx.JSON(http.StatusOK, task)
}

// UpdateTask 更新任务
func (c *TaskController) UpdateTask(ctx *gin.Context) {
    taskID := ctx.Param("task_id")
    if taskID == "" {
        ctx.JSON(http.StatusBadRequest, gin.H{"error": "任务ID不能为空"})
        return
    }
    
    // 先获取原有任务
    existingTask, err := c.taskRepo.GetByID(ctx, taskID)
    if err != nil {
        ctx.JSON(http.StatusNotFound, gin.H{"error": "任务不存在"})
        return
    }
    
    var taskInput models.TaskInput
    if err := ctx.ShouldBindJSON(&taskInput); err != nil {
        ctx.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
        return
    }
    
    // 更新任务字段
    existingTask.Title = taskInput.Title
    existingTask.TimeRange = taskInput.TimeRange
    existingTask.Date = taskInput.Date
    existingTask.DurationMinutes = taskInput.DurationMinutes
    existingTask.IsImportant = taskInput.IsImportant
    existingTask.Description = taskInput.Description
    existingTask.DueDate = taskInput.DueDate
    existingTask.Place = taskInput.Place
    existingTask.Category = taskInput.Category
    
    if err := c.taskRepo.Update(ctx, taskID, existingTask); err != nil {
        ctx.JSON(http.StatusInternalServerError, gin.H{"error": "更新任务失败"})
        return
    }
    
    ctx.JSON(http.StatusOK, gin.H{"message": "任务更新成功"})
}

// GetTasksForDay 获取用户某一天的所有任务
func (c *TaskController) GetTasksForDay(ctx *gin.Context) {
    userID := ctx.Param("user_id")
    if userID == "" {
        ctx.JSON(http.StatusBadRequest, gin.H{"error": "用户ID不能为空"})
        return
    }
    
    dateStr := ctx.Query("date")
    if dateStr == "" {
        dateStr = time.Now().Format("2006-01-02")
    }
    
    date, err := time.Parse("2006-01-02", dateStr)
    if err != nil {
        ctx.JSON(http.StatusBadRequest, gin.H{"error": "日期格式无效"})
        return
    }
    
    tasks, err := c.taskRepo.GetTasksForDay(ctx, userID, date)
    if err != nil {
        ctx.JSON(http.StatusInternalServerError, gin.H{"error": "获取任务失败"})
        return
    }
    
    ctx.JSON(http.StatusOK, tasks)
}

// GetAllTasks 获取用户的所有任务（带分页）
func (c *TaskController) GetAllTasks(ctx *gin.Context) {
    userID := ctx.Param("user_id")
    if userID == "" {
        ctx.JSON(http.StatusBadRequest, gin.H{"error": "用户ID不能为空"})
        return
    }
    
    var params models.PaginationParams
    if err := ctx.ShouldBindQuery(&params); err != nil {
        ctx.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
        return
    }
    
    if params.Page < 1 {
        params.Page = 1
    }
    if params.Limit < 1 || params.Limit > 100 {
        params.Limit = 10
    }
    
    tasks, total, err := c.taskRepo.GetAllTasks(ctx, userID, params.Page, params.Limit)
    if err != nil {
        ctx.JSON(http.StatusInternalServerError, gin.H{"error": "获取任务失败"})
        return
    }
    
    ctx.JSON(http.StatusOK, gin.H{
        "tasks": tasks,
        "total": total,
        "page":  params.Page,
        "limit": params.Limit,
    })
}

// FilterTasksForDay 筛选用户某一天的任务
func (c *TaskController) FilterTasksForDay(ctx *gin.Context) {
    userID := ctx.Param("user_id")
    if userID == "" {
        ctx.JSON(http.StatusBadRequest, gin.H{"error": "用户ID不能为空"})
        return
    }
    
    dateStr := ctx.Query("date")
    if dateStr == "" {
        dateStr = time.Now().Format("2006-01-02")
    }
    
    date, err := time.Parse("2006-01-02", dateStr)
    if err != nil {
        ctx.JSON(http.StatusBadRequest, gin.H{"error": "日期格式无效"})
        return
    }
    
    var filter models.TaskFilter
    if err := ctx.ShouldBindJSON(&filter); err != nil {
        ctx.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
        return
    }
    
    tasks, err := c.taskRepo.FilterTasksForDay(ctx, userID, date, filter)
    if err != nil {
        ctx.JSON(http.StatusInternalServerError, gin.H{"error": "筛选任务失败"})
        return
    }
    
    ctx.JSON(http.StatusOK, tasks)
}

// FilterAllTasks 筛选用户的所有任务（带分页）
func (c *TaskController) FilterAllTasks(ctx *gin.Context) {
    userID := ctx.Param("user_id")
    if userID == "" {
        ctx.JSON(http.StatusBadRequest, gin.H{"error": "用户ID不能为空"})
        return
    }
    
    var params models.PaginationParams
    if err := ctx.ShouldBindQuery(&params); err != nil {
        ctx.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
        return
    }
    
    if params.Page < 1 {
        params.Page = 1
    }
    if params.Limit < 1 || params.Limit > 100 {
        params.Limit = 10
    }
    
    var filter models.TaskFilter
    if err := ctx.ShouldBindJSON(&filter); err != nil {
        ctx.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
        return
    }
    
    tasks, total, err := c.taskRepo.FilterAllTasks(ctx, userID, filter, params.Page, params.Limit)
    if err != nil {
        ctx.JSON(http.StatusInternalServerError, gin.H{"error": "筛选任务失败"})
        return
    }
    
    ctx.JSON(http.StatusOK, gin.H{
        "tasks": tasks,
        "total": total,
        "page":  params.Page,
        "limit": params.Limit,
    })
}

// SearchTasks 搜索用户任务
func (c *TaskController) SearchTasks(ctx *gin.Context) {
    userID := ctx.Param("user_id")
    if userID == "" {
        ctx.JSON(http.StatusBadRequest, gin.H{"error": "用户ID不能为空"})
        return
    }
    
    var params models.PaginationParams
    if err := ctx.ShouldBindQuery(&params); err != nil {
        ctx.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
        return
    }
    
    if params.Page < 1 {
        params.Page = 1
    }
    if params.Limit < 1 || params.Limit > 100 {
        params.Limit = 10
    }
    
    var searchQuery models.SearchQuery
    if err := ctx.ShouldBindJSON(&searchQuery); err != nil {
        ctx.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
        return
    }
    
    tasks, total, err := c.taskRepo.SearchTasks(ctx, userID, searchQuery.Query, params.Page, params.Limit)
    if err != nil {
        ctx.JSON(http.StatusInternalServerError, gin.H{"error": "搜索任务失败"})
        return
    }
    
    ctx.JSON(http.StatusOK, gin.H{
        "tasks": tasks,
        "total": total,
        "page":  params.Page,
        "limit": params.Limit,
    })
}

// GetFocusStatsForDay 获取用户某一天的专注统计
func (c *TaskController) GetFocusStatsForDay(ctx *gin.Context) {
    userID := ctx.Param("user_id")
    if userID == "" {
        ctx.JSON(http.StatusBadRequest, gin.H{"error": "用户ID不能为空"})
        return
    }
    
    dateStr := ctx.Query("date")
    if dateStr == "" {
        dateStr = time.Now().Format("2006-01-02")
    }
    
    date, err := time.Parse("2006-01-02", dateStr)
    if err != nil {
        ctx.JSON(http.StatusBadRequest, gin.H{"error": "日期格式无效"})
        return
    }
    
    stats, err := c.taskRepo.GetFocusStatsForDay(ctx, userID, date)
    if err != nil {
        ctx.JSON(http.StatusInternalServerError, gin.H{"error": "获取专注统计失败"})
        return
    }
    
    ctx.JSON(http.StatusOK, stats)
}

// GetTotalFocusStats 获取用户的累计专注统计
func (c *TaskController) GetTotalFocusStats(ctx *gin.Context) {
    userID := ctx.Param("user_id")
    if userID == "" {
        ctx.JSON(http.StatusBadRequest, gin.H{"error": "用户ID不能为空"})
        return
    }
    
    stats, err := c.taskRepo.GetTotalFocusStats(ctx, userID)
    if err != nil {
        ctx.JSON(http.StatusInternalServerError, gin.H{"error": "获取累计专注统计失败"})
        return
    }
    
    // 计算日均专注时长（假设用户从第一个任务开始计算）
    averageDailyDuration := 0
    if stats.FocusDuration > 0 {
        // 这里简化处理，实际上可能需要计算从第一个任务到现在的天数
        daysActive := 1 // 假设至少活跃1天
        averageDailyDuration = stats.FocusDuration / daysActive
    }
    
    ctx.JSON(http.StatusOK, gin.H{
        "focus_count":           stats.FocusCount,
        "focus_duration_minutes": stats.FocusDuration,
        "tasks_completed":       stats.TasksCompleted,
        "avg_daily_duration":    averageDailyDuration,
    })
}

// GetFocusDistribution 获取用户的专注时长分布
func (c *TaskController) GetFocusDistribution(ctx *gin.Context) {
    userID := ctx.Param("user_id")
    if userID == "" {
        ctx.JSON(http.StatusBadRequest, gin.H{"error": "用户ID不能为空"})
        return
    }
    
    period := ctx.Query("period")
    if period == "" {
        period = "day" // 默认为天
    }
    
    if period != "day" && period != "week" && period != "month" {
        ctx.JSON(http.StatusBadRequest, gin.H{"error": "无效的时间范围"})
        return
    }
    
    startDateStr := ctx.Query("start_date")
    var startDate time.Time
    var err error
    
    if startDateStr == "" {
        startDate = time.Now()
    } else {
        startDate, err = time.Parse("2006-01-02", startDateStr)
        if err != nil {
            ctx.JSON(http.StatusBadRequest, gin.H{"error": "日期格式无效"})
            return
        }
    }
    
    distribution, err := c.taskRepo.GetFocusDistribution(ctx, userID, period, startDate)
    if err != nil {
        ctx.JSON(http.StatusInternalServerError, gin.H{"error": "获取专注分布失败"})
        return
    }
    
    ctx.JSON(http.StatusOK, distribution)
}

// DeleteTask 删除任务
func (c *TaskController) DeleteTask(ctx *gin.Context) {
    taskID := ctx.Param("task_id")
    if taskID == "" {
        ctx.JSON(http.StatusBadRequest, gin.H{"error": "任务ID不能为空"})
        return
    }
    
    err := c.taskRepo.DeleteTask(ctx, taskID)
    if err != nil {
        ctx.JSON(http.StatusInternalServerError, gin.H{"error": "删除任务失败"})
        return
    }
    
    ctx.JSON(http.StatusOK, gin.H{"message": "任务删除成功"})
}

// ToggleTaskFinished 切换任务完成状态
func (c *TaskController) ToggleTaskFinished(ctx *gin.Context) {
    taskID := ctx.Param("task_id")
    if taskID == "" {
        ctx.JSON(http.StatusBadRequest, gin.H{"error": "任务ID不能为空"})
        return
    }
    
    // 获取当前任务
    task, err := c.taskRepo.GetByID(ctx, taskID)
    if err != nil {
        ctx.JSON(http.StatusNotFound, gin.H{"error": "任务不存在"})
        return
    }
    
    // 切换完成状态
    task.IsFinished = !task.IsFinished
    
    // 更新任务
    if err := c.taskRepo.Update(ctx, taskID, task); err != nil {
        ctx.JSON(http.StatusInternalServerError, gin.H{"error": "更新任务状态失败"})
        return
    }
    
    ctx.JSON(http.StatusOK, gin.H{
        "message":    "任务状态已更新",
        "is_finished": task.IsFinished,
    })
}