package controllers

import (
	"encoding/binary"
	"net/http"
	"strconv"
	"time"
	"path/filepath" 
	"fmt"
	"os"

	"github.com/Alchuang22-dev/backend/models"
	"github.com/Alchuang22-dev/backend/repositories"
	"github.com/Alchuang22-dev/backend/utils"
	
	"github.com/gin-gonic/gin"
	"go.mongodb.org/mongo-driver/bson/primitive"
)

// UserController 用户控制器
type UserController struct {
	userRepo *repositories.UserRepository
}

// NewUserController 创建用户控制器
func NewUserController(userRepo *repositories.UserRepository) *UserController {
	return &UserController{
		userRepo: userRepo,
	}
}

// RegisterUser 注册新用户
func (c *UserController) RegisterUser(ctx *gin.Context) {
    var input struct {
        Username    string `json:"username" binding:"required"` // 设置为必填
        Password    string `json:"password" binding:"required"` // 设置为必填
        Nickname    string `json:"nickname"`
        Email       string `json:"email"`
        PhoneNumber string `json:"phoneNumber"`
    }
    
    if err := ctx.ShouldBindJSON(&input); err != nil {
        ctx.JSON(http.StatusBadRequest, gin.H{"error": "用户名和密码为必填项"})
        return
    }
    
    // 检查用户名是否已存在
    _, err := c.userRepo.FindByUsername(input.Username)
    if err == nil {
        ctx.JSON(http.StatusBadRequest, gin.H{"error": "用户名已存在"})
        return
    }
    
    // 加密密码
    hashedPassword, err := utils.HashPassword(input.Password)
    if err != nil {
        ctx.JSON(http.StatusInternalServerError, gin.H{"error": "密码加密失败"})
        return
    }
    
    // 创建用户
    user := models.NewUserWithDetails(input.Username, input.Nickname, input.Email, input.PhoneNumber)
    user.HashedPassword = hashedPassword
    user.Status = models.Registered
    
    createdUser, err := c.userRepo.Create(user)
    if err != nil {
        ctx.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
        return
    }
    
    // 为了从ObjectID转换为uint，我们使用时间戳作为基础，确保为正数
    // 这不是唯一方法，您可以根据需要调整
    userIDForToken := uint(createdUser.ID.Timestamp().Unix())
    
    // 生成JWT令牌
    accessToken, err := utils.GenerateAccessToken(userIDForToken)
    if err != nil {
        ctx.JSON(http.StatusInternalServerError, gin.H{"error": "生成访问令牌失败"})
        return
    }
    
    refreshToken, err := utils.GenerateRefreshToken(userIDForToken)
    if err != nil {
        ctx.JSON(http.StatusInternalServerError, gin.H{"error": "生成刷新令牌失败"})
        return
    }
    
    // 更新用户的token信息
    accessTokenDuration := 24 * time.Hour // 设置为24小时
    createdUser.Login(accessToken, time.Now().Add(accessTokenDuration), refreshToken)
    err = c.userRepo.Update(createdUser)
    if err != nil {
        ctx.JSON(http.StatusInternalServerError, gin.H{"error": "更新用户token失败"})
        return
    }
    
    ctx.JSON(http.StatusCreated, gin.H{
        "message": "注册成功",
        "user": gin.H{
            "id":       createdUser.ID,
            "username": createdUser.Username,
            "nickname": createdUser.Nickname,
            "email":    createdUser.Email,
            "status":   createdUser.Status,
        },
        "token": gin.H{
            "accessToken":  accessToken,
            "refreshToken": refreshToken,
            "expiresIn":    accessTokenDuration.Seconds(),
        },
    })
}

// Login 用户登录
func (c *UserController) Login(ctx *gin.Context) {
    var input struct {
        Username string `json:"username" binding:"required"`
        Password string `json:"password" binding:"required"`
    }
    
    if err := ctx.ShouldBindJSON(&input); err != nil {
        ctx.JSON(http.StatusBadRequest, gin.H{"error": "用户名和密码为必填项"})
        return
    }
    
    // 查找用户
    user, err := c.userRepo.FindByUsername(input.Username)
    if err != nil {
        ctx.JSON(http.StatusUnauthorized, gin.H{"error": "用户名或密码错误"})
        return
    }
    
    // 验证密码
    err = utils.CheckPassword(user.HashedPassword, input.Password)
    if err != nil {
        ctx.JSON(http.StatusUnauthorized, gin.H{"error": "用户名或密码错误"})
        return
    }
    
    // 为了从ObjectID转换为uint，我们使用时间戳作为基础，确保为正数
    userIDForToken := uint(user.ID.Timestamp().Unix())
    
    // 生成JWT令牌
    accessToken, err := utils.GenerateAccessToken(userIDForToken)
    if err != nil {
        ctx.JSON(http.StatusInternalServerError, gin.H{"error": "生成访问令牌失败"})
        return
    }
    
    refreshToken, err := utils.GenerateRefreshToken(userIDForToken)
    if err != nil {
        ctx.JSON(http.StatusInternalServerError, gin.H{"error": "生成刷新令牌失败"})
        return
    }
    
    // 更新用户的token信息和状态
    accessTokenDuration := 24 * time.Hour // 设置为24小时
    user.Login(accessToken, time.Now().Add(accessTokenDuration), refreshToken)
    err = c.userRepo.Update(user)
    if err != nil {
        ctx.JSON(http.StatusInternalServerError, gin.H{"error": "更新用户token失败"})
        return
    }
    
    ctx.JSON(http.StatusOK, gin.H{
        "message": "登录成功",
        "user": gin.H{
            "id":           user.ID,
            "username":     user.Username,
            "nickname":     user.Nickname,
            "email":        user.Email,
            "phoneNumber":  user.PhoneNumber,
            "avatarURL":    user.AvatarURL,
            "status":       user.Status,
            "createdDate":  user.CreatedDate,
            "lastLoginDate": user.LastLoginDate,
            "taskIds":      user.TaskIDs,
        },
        "token": gin.H{
            "accessToken":  accessToken,
            "refreshToken": refreshToken,
            "expiresIn":    accessTokenDuration.Seconds(),
        },
    })
}


// CheckAuth 检查用户认证状态
func (c *UserController) CheckAuth(ctx *gin.Context) {
    userID, exists := ctx.Get("userID")
    if !exists {
        ctx.JSON(http.StatusUnauthorized, gin.H{"error": "未认证", "authenticated": false})
        return
    }
    
    objectID, err := primitive.ObjectIDFromHex(userID.(string))
    if err != nil {
        ctx.JSON(http.StatusBadRequest, gin.H{"error": "无效的用户ID", "authenticated": false})
        return
    }
    
    user, err := c.userRepo.FindByID(objectID)
    if err != nil {
        ctx.JSON(http.StatusNotFound, gin.H{"error": "用户未找到", "authenticated": false})
        return
    }
    
    ctx.JSON(http.StatusOK, gin.H{
        "authenticated": true,
        "user": gin.H{
            "id":           user.ID,
            "username":     user.Username,
            "nickname":     user.Nickname,
            "email":        user.Email,
            "phoneNumber":  user.PhoneNumber,
            "avatarURL":    user.AvatarURL,
            "status":       user.Status,
            "createdDate":  user.CreatedDate,
            "lastLoginDate": user.LastLoginDate,
        },
    })
}

// Logout 用户登出
func (c *UserController) Logout(ctx *gin.Context) {
	userID, exists := ctx.Get("userID")
	if !exists {
		ctx.JSON(http.StatusUnauthorized, gin.H{"error": "Not authenticated"})
		return
	}
	
	objectID, err := primitive.ObjectIDFromHex(userID.(string))
	if err != nil {
		ctx.JSON(http.StatusBadRequest, gin.H{"error": "Invalid user ID"})
		return
	}
	
	user, err := c.userRepo.FindByID(objectID)
	if err != nil {
		ctx.JSON(http.StatusNotFound, gin.H{"error": "User not found"})
		return
	}
	
	user.Logout()
	err = c.userRepo.Update(user)
	if err != nil {
		ctx.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to update user"})
		return
	}
	
	ctx.JSON(http.StatusOK, gin.H{"message": "Successfully logged out"})
}

// GetUser 获取单个用户
func (c *UserController) GetUser(ctx *gin.Context) {
	id := ctx.Param("id")
	objectID, err := primitive.ObjectIDFromHex(id)
	if err != nil {
		ctx.JSON(http.StatusBadRequest, gin.H{"error": "Invalid ID format"})
		return
	}
	
	user, err := c.userRepo.FindByID(objectID)
	if err != nil {
		ctx.JSON(http.StatusNotFound, gin.H{"error": "User not found"})
		return
	}
	
	ctx.JSON(http.StatusOK, user.GetUserInfo())
}

// GetAllUsers 获取所有用户
func (c *UserController) GetAllUsers(ctx *gin.Context) {
	limit := int64(100)  // 默认限制
	offset := int64(0)   // 默认偏移
	
	users, err := c.userRepo.FindAll(limit, offset)
	if err != nil {
		ctx.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
		return
	}
	
	userList := make([]map[string]interface{}, len(users))
	for i, user := range users {
		userList[i] = user.GetUserInfo()
	}
	
	ctx.JSON(http.StatusOK, userList)
}

// UpdateUser 更新用户基本信息
func (c *UserController) UpdateUser(ctx *gin.Context) {
	id := ctx.Param("id")
	objectID, err := primitive.ObjectIDFromHex(id)
	if err != nil {
		ctx.JSON(http.StatusBadRequest, gin.H{"error": "Invalid ID format"})
		return
	}
	
	user, err := c.userRepo.FindByID(objectID)
	if err != nil {
		ctx.JSON(http.StatusNotFound, gin.H{"error": "User not found"})
		return
	}
	
	var input struct {
		Nickname    *string `json:"nickname"`
		Email       *string `json:"email"`
		PhoneNumber *string `json:"phoneNumber"`
		AvatarURL   *string `json:"avatarURL"`
	}
	
	if err := ctx.ShouldBindJSON(&input); err != nil {
		ctx.JSON(http.StatusBadRequest, gin.H{"error": "Invalid input data"})
		return
	}
	
	// 只更新提供的字段
	if input.Nickname != nil {
		user.UpdateNickname(*input.Nickname)
	}
	
	if input.Email != nil {
		user.UpdateEmail(*input.Email)
	}
	
	if input.PhoneNumber != nil {
		user.UpdatePhoneNumber(*input.PhoneNumber)
	}
	
	if input.AvatarURL != nil {
		user.UpdateAvatarURL(*input.AvatarURL)
	}
	
	err = c.userRepo.Update(user)
	if err != nil {
		ctx.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to update user"})
		return
	}
	
	ctx.JSON(http.StatusOK, user.GetUserInfo())
}

// DeleteUser 删除用户
func (c *UserController) DeleteUser(ctx *gin.Context) {
	id := ctx.Param("id")
	objectID, err := primitive.ObjectIDFromHex(id)
	if err != nil {
		ctx.JSON(http.StatusBadRequest, gin.H{"error": "Invalid ID format"})
		return
	}
	
	err = c.userRepo.Delete(objectID)
	if err != nil {
		ctx.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
		return
	}
	
	ctx.JSON(http.StatusOK, gin.H{"message": "User successfully deleted"})
}

// GetUserStatus 获取用户状态
func (c *UserController) GetUserStatus(ctx *gin.Context) {
	id := ctx.Param("id")
	objectID, err := primitive.ObjectIDFromHex(id)
	if err != nil {
		ctx.JSON(http.StatusBadRequest, gin.H{"error": "Invalid ID format"})
		return
	}
	
	user, err := c.userRepo.FindByID(objectID)
	if err != nil {
		ctx.JSON(http.StatusNotFound, gin.H{"error": "User not found"})
		return
	}
	
	ctx.JSON(http.StatusOK, gin.H{"status": user.Status})
}

// UpdateUserStatus 更新用户状态
func (c *UserController) UpdateUserStatus(ctx *gin.Context) {
	id := ctx.Param("id")
	objectID, err := primitive.ObjectIDFromHex(id)
	if err != nil {
		ctx.JSON(http.StatusBadRequest, gin.H{"error": "Invalid ID format"})
		return
	}
	
	var input struct {
		Status models.UserStatus `json:"status"`
	}
	
	if err := ctx.ShouldBindJSON(&input); err != nil {
		ctx.JSON(http.StatusBadRequest, gin.H{"error": "Invalid input data"})
		return
	}
	
	err = c.userRepo.UpdateUserStatus(objectID, input.Status)
	if err != nil {
		ctx.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to update user status"})
		return
	}
	
	ctx.JSON(http.StatusOK, gin.H{"message": "User status updated"})
}

// GetUserPermissions 获取用户权限
func (c *UserController) GetUserPermissions(ctx *gin.Context) {
	id := ctx.Param("id")
	objectID, err := primitive.ObjectIDFromHex(id)
	if err != nil {
		ctx.JSON(http.StatusBadRequest, gin.H{"error": "Invalid ID format"})
		return
	}
	
	user, err := c.userRepo.FindByID(objectID)
	if err != nil {
		ctx.JSON(http.StatusNotFound, gin.H{"error": "User not found"})
		return
	}
	
	ctx.JSON(http.StatusOK, gin.H{"permissions": user.Permissions})
}

// UpdateUserPermission 更新用户权限
func (c *UserController) UpdateUserPermission(ctx *gin.Context) {
	id := ctx.Param("id")
	objectID, err := primitive.ObjectIDFromHex(id)
	if err != nil {
		ctx.JSON(http.StatusBadRequest, gin.H{"error": "Invalid ID format"})
		return
	}
	
	var input struct {
		PermissionType models.PermissionType `json:"permissionType"`
		Enabled        bool                  `json:"enabled"`
	}
	
	if err := ctx.ShouldBindJSON(&input); err != nil {
		ctx.JSON(http.StatusBadRequest, gin.H{"error": "Invalid input data"})
		return
	}
	
	err = c.userRepo.UpdatePermission(objectID, input.PermissionType, input.Enabled)
	if err != nil {
		ctx.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to update permission"})
		return
	}
	
	ctx.JSON(http.StatusOK, gin.H{"message": "Permission updated"})
}

// GetUserTasks 获取用户任务列表
func (c *UserController) GetUserTasks(ctx *gin.Context) {
	id := ctx.Param("id")
	objectID, err := primitive.ObjectIDFromHex(id)
	if err != nil {
		ctx.JSON(http.StatusBadRequest, gin.H{"error": "Invalid ID format"})
		return
	}
	
	user, err := c.userRepo.FindByID(objectID)
	if err != nil {
		ctx.JSON(http.StatusNotFound, gin.H{"error": "User not found"})
		return
	}
	
	ctx.JSON(http.StatusOK, gin.H{"tasks": user.TaskIDs})
}

// AddUserTask 添加任务到用户
func (c *UserController) AddUserTask(ctx *gin.Context) {
	id := ctx.Param("id")
	userID, err := primitive.ObjectIDFromHex(id)
	if err != nil {
		ctx.JSON(http.StatusBadRequest, gin.H{"error": "Invalid user ID format"})
		return
	}
	
	var input struct {
		TaskID string `json:"taskId"`
	}
	
	if err := ctx.ShouldBindJSON(&input); err != nil {
		ctx.JSON(http.StatusBadRequest, gin.H{"error": "Invalid input data"})
		return
	}
	
	taskID, err := primitive.ObjectIDFromHex(input.TaskID)
	if err != nil {
		ctx.JSON(http.StatusBadRequest, gin.H{"error": "Invalid task ID format"})
		return
	}
	
	err = c.userRepo.AddTaskToUser(userID, taskID)
	if err != nil {
		ctx.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to add task to user"})
		return
	}
	
	ctx.JSON(http.StatusOK, gin.H{"message": "Task added to user"})
}

// RemoveUserTask 从用户移除任务
func (c *UserController) RemoveUserTask(ctx *gin.Context) {
	id := ctx.Param("id")
	userID, err := primitive.ObjectIDFromHex(id)
	if err != nil {
		ctx.JSON(http.StatusBadRequest, gin.H{"error": "Invalid user ID format"})
		return
	}
	
	var input struct {
		TaskID string `json:"taskId"`
	}
	
	if err := ctx.ShouldBindJSON(&input); err != nil {
		ctx.JSON(http.StatusBadRequest, gin.H{"error": "Invalid input data"})
		return
	}
	
	taskID, err := primitive.ObjectIDFromHex(input.TaskID)
	if err != nil {
		ctx.JSON(http.StatusBadRequest, gin.H{"error": "Invalid task ID format"})
		return
	}
	
	err = c.userRepo.RemoveTaskFromUser(userID, taskID)
	if err != nil {
		ctx.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to remove task from user"})
		return
	}
	
	ctx.JSON(http.StatusOK, gin.H{"message": "Task removed from user"})
}

func (c *UserController) RefreshAccessToken(ctx *gin.Context) {
	var input struct {
		RefreshToken string `json:"refreshToken"`
	}
	
	if err := ctx.ShouldBindJSON(&input); err != nil {
		ctx.JSON(http.StatusBadRequest, gin.H{"error": "Invalid input data"})
		return
	}
	
	// 验证刷新令牌
	claims, err := utils.ValidateToken(input.RefreshToken)
	if err != nil {
		ctx.JSON(http.StatusUnauthorized, gin.H{"error": "Invalid refresh token"})
		return
	}
	
	// 从令牌中获取用户ID
	userIDStr := claims.Subject
	userIDInt, err := strconv.ParseInt(userIDStr, 10, 64)
	if err != nil {
		ctx.JSON(http.StatusInternalServerError, gin.H{"error": "Invalid user ID in token"})
		return
	}
	
	// 这里需要将int64转换回ObjectID
	// 我们将使用二进制编码来构建一个ObjectID
	objectIDBytes := make([]byte, 12)
	binary.BigEndian.PutUint32(objectIDBytes[0:4], uint32(userIDInt))
	objectID := primitive.ObjectID(objectIDBytes)
	
	user, err := c.userRepo.FindByID(objectID)
	if err != nil {
		ctx.JSON(http.StatusNotFound, gin.H{"error": "User not found"})
		return
	}
	
	// 生成新的访问令牌
	accessToken, err := utils.GenerateAccessToken(uint(userIDInt))
	if err != nil {
		ctx.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to generate access token"})
		return
	}
	
	// 可选：生成新的刷新令牌
	refreshToken, err := utils.GenerateRefreshToken(uint(userIDInt))
	if err != nil {
		ctx.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to generate refresh token"})
		return
	}
	
	// 更新用户的token信息
	accessTokenDuration := 24 * time.Hour // 设置为24小时
	// 注意: 这里使用了RefreshUserToken方法，确保您已经修改了方法名
	user.RefreshUserToken(accessToken, time.Now().Add(accessTokenDuration), refreshToken)
	err = c.userRepo.Update(user)
	if err != nil {
		ctx.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to update user token"})
		return
	}
	
	ctx.JSON(http.StatusOK, gin.H{
		"token": gin.H{
			"accessToken":  accessToken,
			"refreshToken": refreshToken,
			"expiresIn":    accessTokenDuration.Seconds(),
		},
	})
}

// 图片处理部分

// 设置头像存储目录
const avatarUploadDir = "./uploads/avatars"

// UploadAvatar 上传用户头像
func (c *UserController) UploadAvatar(ctx *gin.Context) {
    id := ctx.Param("id")
    objectID, err := primitive.ObjectIDFromHex(id)
    if err != nil {
        ctx.JSON(http.StatusBadRequest, gin.H{"error": "无效的用户ID格式"})
        return
    }
    
    user, err := c.userRepo.FindByID(objectID)
    if err != nil {
        ctx.JSON(http.StatusNotFound, gin.H{"error": "用户未找到"})
        return
    }
    
    // 获取表单文件
    file, err := ctx.FormFile("avatar")
    if err != nil {
        ctx.JSON(http.StatusBadRequest, gin.H{"error": "获取上传文件失败"})
        return
    }
    
    // 保存文件
    filename, err := utils.SaveUploadedFile(file, avatarUploadDir)
    if err != nil {
        ctx.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
        return
    }
    
    // 删除旧头像文件（如果存在）
    if user.AvatarURL != "" {
        // 从 URL 中提取文件名
        oldFilename := filepath.Base(user.AvatarURL)
        if err := utils.DeleteFile(oldFilename, avatarUploadDir); err != nil {
            // 仅记录错误，不中断流程
            fmt.Printf("删除旧头像文件失败: %s\n", err)
        }
    }
    
    // 构建头像 URL
    avatarURL := fmt.Sprintf("/api/avatars/%s", filename)
    
    // 更新用户头像 URL
    user.UpdateAvatarURL(avatarURL)
    if err := c.userRepo.Update(user); err != nil {
        ctx.JSON(http.StatusInternalServerError, gin.H{"error": "更新用户头像失败"})
        return
    }
    
    ctx.JSON(http.StatusOK, gin.H{
        "message": "头像上传成功",
        "avatarURL": avatarURL,
    })
}

// ChangeAvatar 更改用户头像
func (c *UserController) ChangeAvatar(ctx *gin.Context) {
    // 由于更改头像与上传头像流程基本一致，我们可以直接复用 UploadAvatar 方法
    c.UploadAvatar(ctx)
}

// GetAvatar 获取用户头像
func (c *UserController) GetAvatar(ctx *gin.Context) {
    id := ctx.Param("id")
    objectID, err := primitive.ObjectIDFromHex(id)
    if err != nil {
        ctx.JSON(http.StatusBadRequest, gin.H{"error": "无效的用户ID格式"})
        return
    }
    
    user, err := c.userRepo.FindByID(objectID)
    if err != nil {
        ctx.JSON(http.StatusNotFound, gin.H{"error": "用户未找到"})
        return
    }
    
    if user.AvatarURL == "" {
        ctx.JSON(http.StatusOK, gin.H{"avatarURL": ""})
        return
    }
    
    ctx.JSON(http.StatusOK, gin.H{"avatarURL": user.AvatarURL})
}

// ServeAvatar 提供头像文件
func (c *UserController) ServeAvatar(ctx *gin.Context) {
    filename := ctx.Param("filename")
    filePath := filepath.Join(avatarUploadDir, filename)
    
    // 检查文件是否存在
    if _, err := os.Stat(filePath); os.IsNotExist(err) {
        ctx.JSON(http.StatusNotFound, gin.H{"error": "头像文件未找到"})
        return
    }
    
    // 提供文件
    ctx.File(filePath)
}

// UpdateAPIKey 更新用户的 API 密钥
func (c *UserController) UpdateAPIKey(ctx *gin.Context) {
    id := ctx.Param("id")
    objectID, err := primitive.ObjectIDFromHex(id)
    if err != nil {
        ctx.JSON(http.StatusBadRequest, gin.H{"error": "无效的用户ID格式"})
        return
    }
    
    user, err := c.userRepo.FindByID(objectID)
    if err != nil {
        ctx.JSON(http.StatusNotFound, gin.H{"error": "用户未找到"})
        return
    }
    
    var input struct {
        APIKey string `json:"api_key" binding:"required"`
    }
    
    if err := ctx.ShouldBindJSON(&input); err != nil {
        ctx.JSON(http.StatusBadRequest, gin.H{"error": "无效的输入数据"})
        return
    }
    
    user.UpdateAPIKey(input.APIKey)
    if err := c.userRepo.Update(user); err != nil {
        ctx.JSON(http.StatusInternalServerError, gin.H{"error": "更新API密钥失败"})
        return
    }
    
    ctx.JSON(http.StatusOK, gin.H{"message": "API密钥更新成功"})
}