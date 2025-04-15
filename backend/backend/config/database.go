package config

import (
    "context"
    "log"
    "time"

    "go.mongodb.org/mongo-driver/mongo"
    "go.mongodb.org/mongo-driver/mongo/options"
)

var DB *mongo.Database

// ConnectDB 连接到MongoDB
func ConnectDB() {
    // 设置客户端连接配置
    clientOptions := options.Client().ApplyURI("mongodb://root:mango12345678@43.138.108.202:27017/admin")
    
    // 连接到MongoDB
    ctx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
    defer cancel()
    
    client, err := mongo.Connect(ctx, clientOptions)
    if err != nil {
        log.Fatal(err)
    }
    
    // 检查连接
    err = client.Ping(ctx, nil)
    if err != nil {
        log.Fatal(err)
    }
    
    log.Println("Connected to MongoDB!")
    
    // 获取数据库实例
    DB = client.Database("admin")
}

// GetCollection 获取集合
func GetCollection(collectionName string) *mongo.Collection {
    return DB.Collection(collectionName)
}