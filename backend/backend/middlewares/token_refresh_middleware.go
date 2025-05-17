package middlewares

import (
    "encoding/binary"
    "strconv"
    "strings"
    "time"
    
    "github.com/Alchuang22-dev/backend/repositories"
    "github.com/Alchuang22-dev/backend/utils"
    
    "github.com/gin-gonic/gin"
    "go.mongodb.org/mongo-driver/bson/primitive"
)

// TokenRefreshMiddleware Token自动续期中间件
func TokenRefreshMiddleware(userRepo *repositories.UserRepository) gin.HandlerFunc {
    return func(c *gin.Context) {
        // 先执行请求
        c.Next()
        
        // 只处理成功的请求
        if c.Writer.Status() >= 200 && c.Writer.Status() < 300 {
            // 获取并验证Authorization头
            authHeader := c.GetHeader("Authorization")
            if authHeader == "" {
                return
            }
            
            parts := strings.Split(authHeader, " ")
            if len(parts) != 2 || parts[0] != "Bearer" {
                return
            }
            
            token := parts[1]
            
            // 解析token
            claims, err := utils.ValidateToken(token)
            if err != nil {
                return // token无效，不处理
            }
            
            // 获取用户ID
            userIDStr := claims.Subject
            userIDInt, err := strconv.ParseInt(userIDStr, 10, 64)
            if err != nil {
                return
            }
            
            // 构造ObjectID
            objectIDBytes := make([]byte, 12)
            binary.BigEndian.PutUint32(objectIDBytes[0:4], uint32(userIDInt))
            objectID := primitive.ObjectID(objectIDBytes)
            
            // 获取用户
            user, err := userRepo.FindByID(objectID)
            if err != nil {
                return
            }
            
            // 检查token是否快过期 
            currentTime := time.Now()
            if user.TokenExpireDate.Sub(currentTime) < 12*time.Hour {
                // 生成新token
                newAccessToken, _ := utils.GenerateAccessToken(uint(userIDInt))
                newRefreshToken, _ := utils.GenerateRefreshToken(uint(userIDInt))
                
                accessTokenDuration := 24 * time.Hour
                user.RefreshUserToken(newAccessToken, currentTime.Add(accessTokenDuration), newRefreshToken)
                
                userRepo.Update(user)
                
                // 在响应头中设置新token
                c.Header("X-New-Access-Token", newAccessToken)
                c.Header("X-New-Refresh-Token", newRefreshToken)
                c.Header("X-Token-Expires-In", "86400") // 24小时(秒)
            }
        }
    }
}