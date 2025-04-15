package main

import (
    "context"
    "log"
    "os"
    "time"
    
    "your-project/routes"
    
    "github.com/gin-gonic/gin"
    "go.mongodb.org/mongo-driver/mongo"
    "go.mongodb.org/mongo-driver/mongo/options"
)

func main() {
    // 连接MongoDB
    client, err := connectToMongoDB()
    if err != nil {
        log.Fatalf("Failed to connect to MongoDB: %v", err)
    }
    
    // 获取数据库实例
    dbName := os.Getenv("MONGO_DB")
    if dbName == "" {
        dbName = "Trickledown" // 默认数据库名
    }
    db := client.Database(dbName)
    
    // 创建Gin路由
    router := gin.Default()
    
    // 设置CORS
    router.Use(func(c *gin.Context) {
        c.Writer.Header().Set("Access-Control-Allow-Origin", "*")
        c.Writer.Header().Set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS")
        c.Writer.Header().Set("Access-Control-Allow-Headers", "Origin, Content-Type, Content-Length, Accept-Encoding, X-CSRF-Token, Authorization")
        if c.Request.Method == "OPTIONS" {
            c.AbortWithStatus(204)
            return
        }
        c.Next()
    })
    
    // 设置用户路由
    routes.SetupUserRoutes(router, db)
    
    // 启动服务器
    port := os.Getenv("PORT")
    if port == "" {
        port = "8080" // 默认端口
    }
    
    log.Println("Server starting on port " + port + "...")
    if err := router.Run(":" + port); err != nil {
        log.Fatalf("Failed to start server: %v", err)
    }
}

// 连接到MongoDB
func connectToMongoDB() (*mongo.Client, error) {
    ctx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
    defer cancel()
    
    // 从环境变量获取MongoDB URI
    mongoURI := os.Getenv("MONGO_URI")
    if mongoURI == "" {
        // 默认使用之前的连接字符串
        mongoURI = "mongodb://root:mango12345678@43.138.108.202:27017/admin"
    }
    
    // 设置客户端连接配置
    clientOptions := options.Client().ApplyURI(mongoURI)
    
    // 连接到MongoDB
    client, err := mongo.Connect(ctx, clientOptions)
    if err != nil {
        return nil, err
    }
    
    // 检查连接
    err = client.Ping(ctx, nil)
    if err != nil {
        return nil, err
    }
    
    log.Println("Connected to MongoDB!")
    return client, nil
}