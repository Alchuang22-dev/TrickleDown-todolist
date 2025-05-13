package routes

import (
    "github.com/Alchuang22-dev/backend/controllers"
    "github.com/Alchuang22-dev/backend/middlewares"
    "github.com/Alchuang22-dev/backend/repositories"

    "net/http"
    
    "github.com/gin-gonic/gin"
    "go.mongodb.org/mongo-driver/mongo"
)

// SetupUserRoutes 设置用户相关路由
func SetupUserRoutes(router *gin.Engine, db *mongo.Database) {
    userRepo := repositories.NewUserRepository(db)
    userController := controllers.NewUserController(userRepo)
    
    // 公共API，不需要认证
    public := router.Group("/api")
    {
        // 注册和登录
        public.POST("/register", userController.RegisterUser)
        public.POST("/login", userController.Login)
        public.POST("/token/refresh", userController.RefreshAccessToken)
    }
    
    // 需要认证的API
    authorized := router.Group("/api")
    {
        authorized.GET("/auth/check", userController.CheckAuth)
    }
    authorized.Use(middlewares.AuthMiddleware())
    
    // 添加Token自动刷新中间件
    authorized.Use(middlewares.TokenRefreshMiddleware(userRepo))
    {
        // 用户相关
        users := authorized.Group("/users")
        {
            // 基本CRUD
            users.GET("/", userController.GetAllUsers)
            users.GET("/:id", userController.GetUser)
            users.PUT("/:id", userController.UpdateUser)
            users.DELETE("/:id", userController.DeleteUser)
            
            // 用户状态管理
            users.GET("/:id/status", userController.GetUserStatus)
            users.PUT("/:id/status", userController.UpdateUserStatus)
            
            // 权限管理
            users.GET("/:id/permissions", userController.GetUserPermissions)
            users.PUT("/:id/permissions", userController.UpdateUserPermission)
            
            // 任务管理
            users.GET("/:id/tasks", userController.GetUserTasks)
            users.POST("/:id/tasks", userController.AddUserTask)
            users.DELETE("/:id/tasks", userController.RemoveUserTask)
            
            // 登出
            users.POST("/:id/logout", userController.Logout)

            // 添加头像相关路由
            users.POST("/:id/avatar", userController.UploadAvatar)
            users.PUT("/:id/avatar", userController.ChangeAvatar)
            users.GET("/:id/avatar", userController.GetAvatar)
            
            // 添加 API 密钥相关路由
            users.PUT("/:id/apikey", userController.UpdateAPIKey)
        }

        // 添加静态文件服务路由（用于访问头像文件）
        router.GET("/api/avatars/:filename", userController.ServeAvatar)
    }
}

// 添加 SetupRoutes 函数来设置所有路由
// func SetupRoutes(router *gin.Engine, db *mongo.Database) {
//     // 设置用户路由
//     SetupUserRoutes(router, db)
    
//     // 设置任务路由
//     SetupTaskRoutes(router, db)
    
//     // 健康检查路由
//     router.Any("/health", func(c *gin.Context) {
//         if c.Request.Method == "HEAD" {
//             c.Status(http.StatusOK)
//         } else {
//             c.String(http.StatusOK, "healthy")
//         }
//     })

// }

// 添加 AI 路由配置函数
func SetupAIRoutes(router *gin.Engine, db *mongo.Database) {
    userRepo := repositories.NewUserRepository(db)
    taskRepo := repositories.NewTaskRepository(db)
    aiController := controllers.NewAIController(userRepo, taskRepo)
    
    // 需要认证的API
    authorized := router.Group("/api")
    authorized.Use(middlewares.AuthMiddleware())
    
    // AI 相关路由
    ai := authorized.Group("/ai")
    {
        // 获取 AI 建议
        ai.POST("/suggestion", aiController.GetAISuggestion)
    }
}

// 在 SetupRoutes 函数中添加 AI 路由配置
func SetupRoutes(router *gin.Engine, db *mongo.Database) {
    // 设置用户路由
    SetupUserRoutes(router, db)
    
    // 设置任务路由
    SetupTaskRoutes(router, db)
    
    // 设置 AI 路由
    SetupAIRoutes(router, db)
    
    // 健康检查路由
    router.Any("/health", func(c *gin.Context) {
        if c.Request.Method == "HEAD" {
            c.Status(http.StatusOK)
        } else {
            c.String(http.StatusOK, "healthy")
        }
    })
}