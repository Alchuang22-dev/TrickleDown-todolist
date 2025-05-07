package controllers

import (
    "net/http"
    //"strconv"
    "fmt"       // 导入 fmt 包，用于格式化输出
    //"log"       // 导入 log 包，用于日志记录
    "time"
    "strings"  // 添加这一行
    
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
    // 获取用户ID
    userID := ctx.Param("user_id")
    if userID == "" {
        userID = ctx.Query("userId")
        if userID == "" {
            ctx.JSON(http.StatusBadRequest, gin.H{"error": "用户ID不能为空"})
            return
        }
    }
    
    // 记录请求信息
    fmt.Printf("GetFocusStatsForDay - userID: %s\n", userID)
    
    dateStr := ctx.Query("date")
    if dateStr == "" {
        dateStr = time.Now().Format("2006-01-02")
    }
    
    fmt.Printf("GetFocusStatsForDay - date: %s\n", dateStr)
    
    date, err := time.Parse("2006-01-02", dateStr)
    if err != nil {
        fmt.Printf("GetFocusStatsForDay - date parse error: %v\n", err)
        ctx.JSON(http.StatusBadRequest, gin.H{"error": "日期格式无效"})
        return
    }
    
    stats, err := c.taskRepo.GetFocusStatsForDay(ctx, userID, date)
    if err != nil {
        fmt.Printf("GetFocusStatsForDay - repository error: %v\n", err)
        ctx.JSON(http.StatusInternalServerError, gin.H{"error": "获取专注统计失败"})
        return
    }
    
    // 确保返回一个标准格式的响应，即使某些字段为空
    // 注意：这里不再引用stats.Categories，而是直接创建一个空的分类映射
    response := gin.H{
        "focus_count":           stats.FocusCount,
        "focus_duration_minutes": stats.FocusDuration,
        "tasks_completed":       stats.TasksCompleted,
        "categories":            map[string]int{},
    }
    
    // 如果需要添加分类数据，需要确认FocusStats结构体中有此字段
    // 目前暂时不添加，使用空map
    
    fmt.Printf("GetFocusStatsForDay - response: %+v\n", response)
    
    ctx.JSON(http.StatusOK, response)
}

// GetTotalFocusStats 获取用户的累计专注统计
func (c *TaskController) GetTotalFocusStats(ctx *gin.Context) {
    userID := ctx.Param("user_id")
    if userID == "" {
        ctx.JSON(http.StatusBadRequest, gin.H{"error": "用户ID不能为空"})
        return
    }
    
    fmt.Printf("GetTotalFocusStats - userID: %s\n", userID)
    
    stats, err := c.taskRepo.GetTotalFocusStats(ctx, userID)
    if err != nil {
        fmt.Printf("GetTotalFocusStats - repository error: %v\n", err)
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
    
    // 构建响应数据
    response := gin.H{
        "focus_count":           stats.FocusCount,
        "focus_duration_minutes": stats.FocusDuration,
        "tasks_completed":       stats.TasksCompleted,
        "avg_daily_duration":    averageDailyDuration,
        "categories":            map[string]int{
            "工作": 0,
            "学习": 0,
            "生活": 0,
            "其他": 0,
        },
    }
    
    fmt.Printf("GetTotalFocusStats - response: %+v\n", response)
    
    ctx.JSON(http.StatusOK, response)
}

// GetFocusDistribution 获取用户的专注时长分布
func (c *TaskController) GetFocusDistribution(ctx *gin.Context) {
    userID := ctx.Param("user_id")
    if userID == "" {
        ctx.JSON(http.StatusBadRequest, gin.H{"error": "用户ID不能为空"})
        return
    }
    
    // 处理period参数，可能来自不同名称
    period := ctx.Query("period")
    if period == "" {
        period = ctx.Query("type") // 兼容使用type而不是period
        if period == "" {
            period = "day" // 默认为天
        }
    }
    
    fmt.Printf("GetFocusDistribution - userID: %s, period: %s\n", userID, period)
    
    if period != "day" && period != "week" && period != "month" {
        fmt.Printf("GetFocusDistribution - invalid period: %s\n", period)
        ctx.JSON(http.StatusBadRequest, gin.H{"error": "无效的时间范围"})
        return
    }
    
    // 处理日期参数，可能来自不同名称
    startDateStr := ctx.Query("start_date")
    if startDateStr == "" {
        startDateStr = ctx.Query("startDate") // 兼容使用startDate而不是start_date
    }
    
    fmt.Printf("GetFocusDistribution - raw startDate: %s\n", startDateStr)
    
    var startDate time.Time
    var err error
    
    if startDateStr == "" {
        startDate = time.Now()
    } else {
        // 根据不同格式解析日期
        switch len(startDateStr) {
        case 7: // 年月格式 "2025-05"
            // 对于月份视图，添加日期部分（月初）
            startDate, err = time.Parse("2006-01", startDateStr)
            if err != nil {
                fmt.Printf("GetFocusDistribution - year-month parse error: %v\n", err)
                ctx.JSON(http.StatusBadRequest, gin.H{"error": "日期格式无效"})
                return
            }
        case 10: // 年月日格式 "2025-05-06"
            startDate, err = time.Parse("2006-01-02", startDateStr)
            if err != nil {
                fmt.Printf("GetFocusDistribution - full date parse error: %v\n", err)
                ctx.JSON(http.StatusBadRequest, gin.H{"error": "日期格式无效"})
                return
            }
        default:
            // 尝试灵活解析
            formats := []string{"2006-01-02", "2006-01", "2006/01/02", "2006/01"}
            var parsed bool
            for _, format := range formats {
                startDate, err = time.Parse(format, startDateStr)
                if err == nil {
                    parsed = true
                    break
                }
            }
            
            if !parsed {
                fmt.Printf("GetFocusDistribution - flexible parse error for: %s\n", startDateStr)
                ctx.JSON(http.StatusBadRequest, gin.H{"error": "日期格式无效"})
                return
            }
        }
    }
    
    fmt.Printf("GetFocusDistribution - parsed startDate: %s\n", startDate.Format("2006-01-02"))
    
    // 从存储库获取数据
    distribution, err := c.taskRepo.GetFocusDistribution(ctx, userID, period, startDate)
    if err != nil {
        fmt.Printf("GetFocusDistribution - repository error: %v\n", err)
        ctx.JSON(http.StatusInternalServerError, gin.H{"error": "获取专注分布失败"})
        return
    }
    
    // 所有数据的原始转储
    fmt.Printf("GetFocusDistribution - initial raw data: %v\n", distribution.Data)
    
    // 先检查是否有空键的数据，如果有就移动到适当的周期
    year := startDate.Year()
    totalMissingData := 0
    
    // 根据不同周期定义有效键格式
    var isValidKey func(string) bool
    
    switch period {
    case "day":
        // 日期格式：YYYY-MM-DD (例如: 2025-05-07)
        isValidKey = func(key string) bool {
            if len(key) != 10 {
                return false
            }
            // 简单检查格式 XXXX-XX-XX
            return key[4] == '-' && key[7] == '-'
        }
    
    case "week":
        // 周格式：YYYY-WXX (例如: 2025-W19)
        isValidKey = func(key string) bool {
            return strings.Contains(key, "-W")
        }
    
    case "month":
        // 月格式：YYYY-MM (例如: 2025-05)
        isValidKey = func(key string) bool {
            if len(key) != 7 {
                return false
            }
            // 简单检查格式 XXXX-XX
            return key[4] == '-'
        }
    }
    
    for key, value := range distribution.Data {
        // 检测空键或无效键
        if key == "" || !isValidKey(key) {
            fmt.Printf("GetFocusDistribution - Found invalid key: '%s' with value: %d\n", key, value)
            totalMissingData += value
            delete(distribution.Data, key) // 移除这个条目
        }
    }
    
    // 如果有未分配的数据，尝试分配到当前周期
    if totalMissingData > 0 {
        var currentKey string
        
        switch period {
        case "day":
            // 分配到当天
            currentKey = startDate.Format("2006-01-02")
        
        case "week":
            // 分配到当前周
            _, currentWeek := startDate.ISOWeek()
            currentKey = fmt.Sprintf("%d-W%02d", year, currentWeek)
        
        case "month":
            // 分配到当前月
            currentKey = startDate.Format("2006-01")
        }
        
        fmt.Printf("GetFocusDistribution - Assigning missing data %d to current %s: %s\n", 
                 totalMissingData, period, currentKey)
        
        // 将未分配数据添加到当前周期
        if existing, ok := distribution.Data[currentKey]; ok {
            distribution.Data[currentKey] = existing + totalMissingData
        } else {
            distribution.Data[currentKey] = totalMissingData
        }
    }
    
    // 确保数据完整性 - 填充缺失的日期/周/月
    completedData := make(map[string]int)
    
    switch period {
    case "day":
        // 获取当月天数
        year, month, _ := startDate.Date()
        firstDay := time.Date(year, month, 1, 0, 0, 0, 0, startDate.Location())
        lastDay := firstDay.AddDate(0, 1, -1) // 月末
        daysInMonth := lastDay.Day()
        
        // 填充当月所有日期
        for d := 1; d <= daysInMonth; d++ {
            dateKey := fmt.Sprintf("%d-%02d-%02d", year, month, d)
            if value, exists := distribution.Data[dateKey]; exists {
                completedData[dateKey] = value
            } else {
                completedData[dateKey] = 0
            }
        }
        
    case "week":
        // 获取当前年份
        year := startDate.Year()
        
        // 根据 ISO 8601 标准获取周数
        _, maxWeek := time.Date(year, 12, 28, 0, 0, 0, 0, startDate.Location()).ISOWeek()
        
        fmt.Printf("GetFocusDistribution - Year %d has %d weeks\n", year, maxWeek)
        
        // 填充该年所有周
        for week := 1; week <= maxWeek; week++ {
            weekKey := fmt.Sprintf("%d-W%02d", year, week)
            if value, exists := distribution.Data[weekKey]; exists {
                completedData[weekKey] = value
            } else {
                completedData[weekKey] = 0
            }
        }
        
    case "month":
        // 当年所有月份
        year := startDate.Year()
        
        for m := 1; m <= 12; m++ {
            monthKey := fmt.Sprintf("%d-%02d", year, m)
            if value, exists := distribution.Data[monthKey]; exists {
                completedData[monthKey] = value
            } else {
                completedData[monthKey] = 0
            }
        }
    }
    
    // 记录完整数据
    fmt.Printf("GetFocusDistribution - raw distribution data after processing: %v\n", distribution.Data)
    fmt.Printf("GetFocusDistribution - completed data: %v\n", completedData)
    
    // 构建响应数据
    response := gin.H{
        "period":     distribution.Period,
        "data":       completedData,
        "categories": map[string]int{
            "工作": 0,
            "学习": 0,
            "生活": 0,
            "其他": 0,
        },
    }
    
    ctx.JSON(http.StatusOK, response)
}

// ensureCompleteFocusDistribution 确保分布数据的完整性，填充缺失的日期/周/月
func (c *TaskController) ensureCompleteFocusDistribution(distribution *models.FocusDistribution, period string, startDate time.Time) map[string]int {
    result := make(map[string]int)
    
    // 复制已有数据
    for k, v := range distribution.Data {
        result[k] = v
    }
    
    switch period {
    case "day":
        // 确保返回当月所有日期（28-31天）
        year, month, _ := startDate.Date()
        firstDay := time.Date(year, month, 1, 0, 0, 0, 0, startDate.Location())
        lastDay := firstDay.AddDate(0, 1, -1) // 月末
        
        // 填充每一天
        for d := 1; d <= lastDay.Day(); d++ {
            dateStr := fmt.Sprintf("%d-%02d-%02d", year, month, d)
            if _, exists := result[dateStr]; !exists {
                result[dateStr] = 0
            }
        }
        
    case "week":
        // 使用 ISO 8601 标准计算周数
        year := startDate.Year()
        
        // 计算本年第一天
        jan1 := time.Date(year, 1, 1, 0, 0, 0, 0, startDate.Location())
        
        // 计算本年第一个周四所在的周是第一周
        // 如果1月1日是周四，周五，周六或周日，那么这一周就是前一年的最后一周
        firstThursday := jan1
        weekday := jan1.Weekday()
        
        if weekday != time.Thursday {
            daysUntilThursday := (4 - int(weekday) + 7) % 7
            firstThursday = jan1.AddDate(0, 0, daysUntilThursday)
        }
        
        // 计算本年最后一天
        dec31 := time.Date(year, 12, 31, 0, 0, 0, 0, startDate.Location())
        
        // 计算从第一个周四到12月31日有多少周
        days := dec31.Sub(firstThursday).Hours() / 24
        weekCount := int(days/7) + 1
        
        // 遍历所有周
        for week := 1; week <= weekCount; week++ {
            weekKey := fmt.Sprintf("%d-W%02d", year, week)
            if _, exists := result[weekKey]; !exists {
                result[weekKey] = 0
            }
        }
        
    case "month":
        // 确保返回一年中所有月份（12个月）
        year := startDate.Year()
        
        for month := 1; month <= 12; month++ {
            monthKey := fmt.Sprintf("%d-%02d", year, month)
            if _, exists := result[monthKey]; !exists {
                result[monthKey] = 0
            }
        }
    }
    
    return result
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