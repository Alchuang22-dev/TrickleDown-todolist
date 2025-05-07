package repositories

import (
    "context"
    "time"
    "fmt"
    
    "github.com/Alchuang22-dev/backend/models"
    "go.mongodb.org/mongo-driver/bson"
    "go.mongodb.org/mongo-driver/bson/primitive"
    "go.mongodb.org/mongo-driver/mongo"
    "go.mongodb.org/mongo-driver/mongo/options"
)

// TaskRepository 接口定义了任务存储库的方法
type TaskRepository interface {
    Create(ctx context.Context, task *models.Task) (string, error)
    GetByID(ctx context.Context, id string) (*models.Task, error)
    Update(ctx context.Context, id string, task *models.Task) error
    GetTasksForDay(ctx context.Context, userID string, date time.Time) ([]*models.Task, error)
    GetAllTasks(ctx context.Context, userID string, page, limit int) ([]*models.Task, int, error)
    FilterTasksForDay(ctx context.Context, userID string, date time.Time, filter models.TaskFilter) ([]*models.Task, error)
    FilterAllTasks(ctx context.Context, userID string, filter models.TaskFilter, page, limit int) ([]*models.Task, int, error)
    SearchTasks(ctx context.Context, userID string, query string, page, limit int) ([]*models.Task, int, error)
    GetFocusStatsForDay(ctx context.Context, userID string, date time.Time) (*models.FocusStats, error)
    GetTotalFocusStats(ctx context.Context, userID string) (*models.FocusStats, error)
    GetFocusDistribution(ctx context.Context, userID string, period string, startDate time.Time) (*models.FocusDistribution, error)
    DeleteTask(ctx context.Context, id string) error
}

// MongoTaskRepository MongoDB实现的任务存储库
type MongoTaskRepository struct {
    db *mongo.Database
}

// NewTaskRepository 创建一个新的任务存储库
func NewTaskRepository(db *mongo.Database) TaskRepository {
    return &MongoTaskRepository{db: db}
}

// getCollection 获取任务集合
func (r *MongoTaskRepository) getCollection() *mongo.Collection {
    return r.db.Collection("tasks")
}

// Create 创建一个新任务
func (r *MongoTaskRepository) Create(ctx context.Context, task *models.Task) (string, error) {
    task.CreatedAt = time.Now()
    task.UpdatedAt = time.Now()
    
    result, err := r.getCollection().InsertOne(ctx, task)
    if err != nil {
        return "", err
    }
    
    if oid, ok := result.InsertedID.(primitive.ObjectID); ok {
        return oid.Hex(), nil
    }
    
    return "", nil
}

// GetByID 通过ID获取任务
func (r *MongoTaskRepository) GetByID(ctx context.Context, id string) (*models.Task, error) {
    objectID, err := primitive.ObjectIDFromHex(id)
    if err != nil {
        return nil, err
    }
    
    var task models.Task
    err = r.getCollection().FindOne(ctx, bson.M{"_id": objectID}).Decode(&task)
    if err != nil {
        return nil, err
    }
    
    return &task, nil
}

// Update 更新任务
func (r *MongoTaskRepository) Update(ctx context.Context, id string, task *models.Task) error {
    objectID, err := primitive.ObjectIDFromHex(id)
    if err != nil {
        return err
    }
    
    task.UpdatedAt = time.Now()
    
    _, err = r.getCollection().UpdateOne(
        ctx,
        bson.M{"_id": objectID},
        bson.M{"$set": task},
    )
    
    return err
}

// DeleteTask 删除任务
func (r *MongoTaskRepository) DeleteTask(ctx context.Context, id string) error {
    objectID, err := primitive.ObjectIDFromHex(id)
    if err != nil {
        return err
    }
    
    _, err = r.getCollection().DeleteOne(ctx, bson.M{"_id": objectID})
    return err
}

// GetTasksForDay 获取用户某一天的所有任务
func (r *MongoTaskRepository) GetTasksForDay(ctx context.Context, userID string, date time.Time) ([]*models.Task, error) {
    // 设置日期范围为当天的0点到23:59:59
    startDate := time.Date(date.Year(), date.Month(), date.Day(), 0, 0, 0, 0, date.Location())
    endDate := startDate.Add(24 * time.Hour).Add(-time.Second)
    
    filter := bson.M{
        "user_id": userID,
        "date": bson.M{
            "$gte": startDate,
            "$lte": endDate,
        },
    }
    
    return r.findTasks(ctx, filter, bson.M{"date": 1})
}

// GetAllTasks 获取用户的所有任务（带分页）
func (r *MongoTaskRepository) GetAllTasks(ctx context.Context, userID string, page, limit int) ([]*models.Task, int, error) {
    filter := bson.M{"user_id": userID}
    return r.findTasksWithPagination(ctx, filter, bson.M{"date": 1}, page, limit)
}

// FilterTasksForDay 根据筛选条件筛选用户某一天的任务
func (r *MongoTaskRepository) FilterTasksForDay(ctx context.Context, userID string, date time.Time, filter models.TaskFilter) ([]*models.Task, error) {
    // 设置日期范围为当天的0点到23:59:59
    startDate := time.Date(date.Year(), date.Month(), date.Day(), 0, 0, 0, 0, date.Location())
    endDate := startDate.Add(24 * time.Hour).Add(-time.Second)
    
    taskFilter := bson.M{
        "user_id": userID,
        "date": bson.M{
            "$gte": startDate,
            "$lte": endDate,
        },
    }
    
    // 应用筛选条件
    if filter.Unfinished {
        taskFilter["is_finished"] = false
    }
    
    if filter.Important {
        taskFilter["is_important"] = true
    }
    
    if filter.CategoryFilter != "" {
        taskFilter["category"] = filter.CategoryFilter
    }
    
    // 确定排序方式
    sort := bson.M{}
    if filter.SortByTime {
        sort["time_range"] = 1
    } else {
        sort["date"] = 1
    }
    
    return r.findTasks(ctx, taskFilter, sort)
}

// FilterAllTasks 根据筛选条件筛选用户的所有任务（带分页）
func (r *MongoTaskRepository) FilterAllTasks(ctx context.Context, userID string, filter models.TaskFilter, page, limit int) ([]*models.Task, int, error) {
    taskFilter := bson.M{"user_id": userID}
    
    // 应用筛选条件
    if filter.Unfinished {
        taskFilter["is_finished"] = false
    }
    
    if filter.Important {
        taskFilter["is_important"] = true
    }
    
    if filter.CategoryFilter != "" {
        taskFilter["category"] = filter.CategoryFilter
    }
    
    // 确定排序方式
    sort := bson.M{}
    if filter.SortByTime {
        sort["time_range"] = 1
    } else {
        sort["date"] = 1
    }
    
    return r.findTasksWithPagination(ctx, taskFilter, sort, page, limit)
}

// SearchTasks 搜索用户任务（带分页）
func (r *MongoTaskRepository) SearchTasks(ctx context.Context, userID string, query string, page, limit int) ([]*models.Task, int, error) {
    filter := bson.M{
        "user_id": userID,
        "$or": []bson.M{
            {"title": bson.M{"$regex": query, "$options": "i"}},
            {"description": bson.M{"$regex": query, "$options": "i"}},
            {"category": bson.M{"$regex": query, "$options": "i"}},
            {"place": bson.M{"$regex": query, "$options": "i"}},
        },
    }
    
    return r.findTasksWithPagination(ctx, filter, bson.M{"date": 1}, page, limit)
}

// GetFocusStatsForDay 获取用户某一天的专注统计
func (r *MongoTaskRepository) GetFocusStatsForDay(ctx context.Context, userID string, date time.Time) (*models.FocusStats, error) {
    // 设置日期范围为当天的0点到23:59:59
    startDate := time.Date(date.Year(), date.Month(), date.Day(), 0, 0, 0, 0, date.Location())
    endDate := startDate.Add(24 * time.Hour).Add(-time.Second)
    
    // 获取当天完成的任务数量
    completedCount, err := r.getCollection().CountDocuments(ctx, bson.M{
        "user_id": userID,
        "date": bson.M{
            "$gte": startDate,
            "$lte": endDate,
        },
        "is_finished": true,
    })
    if err != nil {
        return nil, err
    }
    
    // 获取当天的专注总时长
    pipeline := []bson.M{
        {
            "$match": bson.M{
                "user_id": userID,
                "date": bson.M{
                    "$gte": startDate,
                    "$lte": endDate,
                },
                "is_finished": true,
            },
        },
        {
            "$group": bson.M{
                "_id": nil,
                "total_duration": bson.M{"$sum": "$duration_minutes"},
                "count": bson.M{"$sum": 1},
            },
        },
    }
    
    cursor, err := r.getCollection().Aggregate(ctx, pipeline)
    if err != nil {
        return nil, err
    }
    defer cursor.Close(ctx)
    
    type result struct {
        TotalDuration int `bson:"total_duration"`
        Count         int `bson:"count"`
    }
    
    var results []result
    if err = cursor.All(ctx, &results); err != nil {
        return nil, err
    }
    
    stats := &models.FocusStats{
        TasksCompleted: int(completedCount),
    }
    
    if len(results) > 0 {
        stats.FocusCount = results[0].Count
        stats.FocusDuration = results[0].TotalDuration
    }
    
    return stats, nil
}

// GetTotalFocusStats 获取用户的累计专注统计
func (r *MongoTaskRepository) GetTotalFocusStats(ctx context.Context, userID string) (*models.FocusStats, error) {
    // 获取累计完成的任务数量
    completedCount, err := r.getCollection().CountDocuments(ctx, bson.M{
        "user_id":    userID,
        "is_finished": true,
    })
    if err != nil {
        return nil, err
    }
    
    // 获取累计专注时长和次数
    pipeline := []bson.M{
        {
            "$match": bson.M{
                "user_id":    userID,
                "is_finished": true,
            },
        },
        {
            "$group": bson.M{
                "_id": nil,
                "total_duration": bson.M{"$sum": "$duration_minutes"},
                "count": bson.M{"$sum": 1},
            },
        },
    }
    
    cursor, err := r.getCollection().Aggregate(ctx, pipeline)
    if err != nil {
        return nil, err
    }
    defer cursor.Close(ctx)
    
    type result struct {
        TotalDuration int `bson:"total_duration"`
        Count         int `bson:"count"`
    }
    
    var results []result
    if err = cursor.All(ctx, &results); err != nil {
        return nil, err
    }
    
    stats := &models.FocusStats{
        TasksCompleted: int(completedCount),
    }
    
    if len(results) > 0 {
        stats.FocusCount = results[0].Count
        stats.FocusDuration = results[0].TotalDuration
    }
    
    return stats, nil
}

// GetFocusDistribution 获取用户的专注时长分布
func (r *MongoTaskRepository) GetFocusDistribution(ctx context.Context, userID string, period string, startDate time.Time) (*models.FocusDistribution, error) {
    var endDate time.Time
    var groupFormat string
    
    year := startDate.Year()
    month := startDate.Month()
    
    switch period {
    case "day":
        // 一个月内专注分布（按天）
        firstDayOfMonth := time.Date(year, month, 1, 0, 0, 0, 0, startDate.Location())
        lastDayOfMonth := firstDayOfMonth.AddDate(0, 1, 0).Add(-time.Second)
        endDate = lastDayOfMonth
        groupFormat = "%Y-%m-%d"
        
    case "week":
        // 一年内专注分布（按周）
        firstDayOfYear := time.Date(year, 1, 1, 0, 0, 0, 0, startDate.Location())
        lastDayOfYear := time.Date(year, 12, 31, 23, 59, 59, 0, startDate.Location())
        startDate = firstDayOfYear
        endDate = lastDayOfYear
        // MongoDB 没有内置周数支持，需要自定义实现
        // 这里简化处理，使用年和周数组合
        
    case "month":
        // 一年内专注分布（按月）
        firstDayOfYear := time.Date(year, 1, 1, 0, 0, 0, 0, startDate.Location())
        lastDayOfYear := time.Date(year, 12, 31, 23, 59, 59, 0, startDate.Location())
        startDate = firstDayOfYear
        endDate = lastDayOfYear
        groupFormat = "%Y-%m"
    }
    
    // 获取用户在日期范围内已完成的任务
    matchStage := bson.M{
        "$match": bson.M{
            "user_id": userID,
            "is_finished": true,
            "date": bson.M{
                "$gte": startDate,
                "$lte": endDate,
            },
        },
    }
    
    var groupStage bson.M
    if period == "week" {
        // 按周分组需要特殊处理
        groupStage = bson.M{
            "$group": bson.M{
                "_id": bson.M{
                    "year": bson.M{"$year": "$date"},
                    "week": bson.M{"$week": "$date"}, // MongoDB 的 $week 操作符
                },
                "duration": bson.M{"$sum": "$duration_minutes"},
            },
        }
    } else {
        // 按天或按月分组
        groupStage = bson.M{
            "$group": bson.M{
                "_id": bson.M{
                    "$dateToString": bson.M{
                        "format": groupFormat,
                        "date": "$date",
                    },
                },
                "duration": bson.M{"$sum": "$duration_minutes"},
            },
        }
    }
    
    pipeline := []bson.M{matchStage, groupStage}
    
    cursor, err := r.getCollection().Aggregate(ctx, pipeline)
    if err != nil {
        return nil, err
    }
    defer cursor.Close(ctx)
    
    type result struct {
        ID       interface{} `bson:"_id"`
        Duration int         `bson:"duration"`
    }
    
    var results []result
    if err = cursor.All(ctx, &results); err != nil {
        return nil, err
    }
    
    distribution := &models.FocusDistribution{
        Period: period,
        Data:   make(map[string]int),
    }
    
    for _, res := range results {
        var key string
        
        if period == "week" {
            // 处理周格式
            if weekData, ok := res.ID.(bson.M); ok {
                year := weekData["year"]
                week := weekData["week"]
                key = fmt.Sprintf("%d-W%02d", year, week)
            }
        } else {
            // 处理天或月格式
            switch v := res.ID.(type) {
            case string:
                key = v
            default:
                key = fmt.Sprintf("%v", v)
            }
        }
        
        distribution.Data[key] = res.Duration
    }
    
    return distribution, nil
}

// findTasks 根据筛选条件查找任务
func (r *MongoTaskRepository) findTasks(ctx context.Context, filter bson.M, sort bson.M) ([]*models.Task, error) {
    opts := options.Find().SetSort(sort)
    
    cursor, err := r.getCollection().Find(ctx, filter, opts)
    if err != nil {
        return nil, err
    }
    defer cursor.Close(ctx)
    
    var tasks []*models.Task
    if err = cursor.All(ctx, &tasks); err != nil {
        return nil, err
    }
    
    return tasks, nil
}

// findTasksWithPagination 根据筛选条件查找任务（带分页）
func (r *MongoTaskRepository) findTasksWithPagination(ctx context.Context, filter bson.M, sort bson.M, page, limit int) ([]*models.Task, int, error) {
    // 设置默认分页参数
    if page < 1 {
        page = 1
    }
    if limit < 1 {
        limit = 10
    }
    
    // 计算偏移量
    skip := (page - 1) * limit
    
    // 获取匹配的文档总数
    total, err := r.getCollection().CountDocuments(ctx, filter)
    if err != nil {
        return nil, 0, err
    }
    
    // 设置查询选项
    opts := options.Find().
        SetSort(sort).
        SetSkip(int64(skip)).
        SetLimit(int64(limit))
    
    // 执行查询
    cursor, err := r.getCollection().Find(ctx, filter, opts)
    if err != nil {
        return nil, 0, err
    }
    defer cursor.Close(ctx)
    
    var tasks []*models.Task
    if err = cursor.All(ctx, &tasks); err != nil {
        return nil, 0, err
    }
    
    return tasks, int(total), nil
}