package routes

import (
    "github.com/Alchuang22-dev/backend/controllers"
    "github.com/Alchuang22-dev/backend/middlewares"
    "github.com/Alchuang22-dev/backend/repositories"
    
    "github.com/gin-gonic/gin"
    "go.mongodb.org/mongo-driver/mongo"
)

// SetupTaskRoutes 设置任务相关路由
func SetupTaskRoutes(router *gin.Engine, db *mongo.Database) {
    taskRepo := repositories.NewTaskRepository(db)
    taskController := controllers.NewTaskController(taskRepo)
    
    // 需要认证的API
    authorized := router.Group("/api")
    authorized.Use(middlewares.AuthMiddleware())
    
    // 任务相关路由
    tasks := authorized.Group("/tasks")
    {
        // 1. 添加新事项
        tasks.POST("/users/:user_id", taskController.CreateTask)
        
        // 2. 查看事项
        tasks.GET("/:task_id", taskController.GetTaskByID)
        
        // 3. 修改事项
        tasks.PUT("/:task_id", taskController.UpdateTask)
        
        // 4. 查看今日事项
        tasks.GET("/users/:user_id/today", taskController.GetTasksForDay)
        
        // 5. 查看全部事项
        tasks.GET("/users/:user_id", taskController.GetAllTasks)
        
        // 6. 筛选今日事项
        tasks.POST("/users/:user_id/today/filter", taskController.FilterTasksForDay)
        
        // 7. 筛选全部事项
        tasks.POST("/users/:user_id/filter", taskController.FilterAllTasks)
        
        // 8. 搜索全部事项
        tasks.POST("/users/:user_id/search", taskController.SearchTasks)
        
        // 9. 今日专注
        tasks.GET("/users/:user_id/focus/today", taskController.GetFocusStatsForDay)
        
        // 10. 累计专注
        tasks.GET("/users/:user_id/focus/total", taskController.GetTotalFocusStats)
        
        // 11. 专注时长分布
        tasks.GET("/users/:user_id/focus/distribution", taskController.GetFocusDistribution)
        
        // 额外功能：删除任务
        tasks.DELETE("/:task_id", taskController.DeleteTask)
        
        // 额外功能：切换任务完成状态
        tasks.PATCH("/:task_id/toggle", taskController.ToggleTaskFinished)
    }
}