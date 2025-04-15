package main

import (
	"context"
	"log"
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
	db := client.Database("Trickledown")
	
	// 创建Gin路由
	router := gin.Default()
	
	// 设置用户路由
	routes.SetupUserRoutes(router, db)
	
	// 启动服务器
	log.Println("Server starting on port 8080...")
	if err := router.Run(":8080"); err != nil {
		log.Fatalf("Failed to start server: %v", err)
	}
}

// 连接到MongoDB
func connectToMongoDB() (*mongo.Client, error) {
	ctx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer cancel()
	
	// 设置客户端连接配置
	clientOptions := options.Client().ApplyURI("mongodb://root:mango12345678@43.138.108.202:27017/admin")
	
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