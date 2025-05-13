// ai_controller.go
package controllers

import (
    "bytes"
    "encoding/json"
    "fmt"
    "net/http"
    "time"
	"strings"
    
    //"github.com/Alchuang22-dev/backend/models"
    "github.com/Alchuang22-dev/backend/repositories"
    "github.com/gin-gonic/gin"
    "go.mongodb.org/mongo-driver/bson/primitive"
)

// AIController 处理 AI 交互相关请求
type AIController struct {
    userRepo  *repositories.UserRepository
    taskRepo  repositories.TaskRepository
}

// NewAIController 创建一个新的 AI 控制器
func NewAIController(userRepo *repositories.UserRepository, taskRepo repositories.TaskRepository) *AIController {
    return &AIController{
        userRepo: userRepo,
        taskRepo: taskRepo,
    }
}

type AIRequest struct {
    TaskID          string `json:"task_id" binding:"required"`
    DetailedPrompts string `json:"detailed_prompts"`
}

type DeepseekMessage struct {
    Role    string `json:"role"`
    Content string `json:"content"`
}

type DeepseekRequest struct {
    Model       string            `json:"model"`
    Messages    []DeepseekMessage `json:"messages"`
    Temperature float64           `json:"temperature"`
    MaxTokens   int               `json:"max_tokens"`
}

type DeepseekChoice struct {
    Message      DeepseekMessage `json:"message"`
    FinishReason string          `json:"finish_reason"`
}

type DeepseekResponse struct {
    ID      string           `json:"id"`
    Object  string           `json:"object"`
    Created int64            `json:"created"`
    Choices []DeepseekChoice `json:"choices"`
    Usage   struct {
        PromptTokens     int `json:"prompt_tokens"`
        CompletionTokens int `json:"completion_tokens"`
        TotalTokens      int `json:"total_tokens"`
    } `json:"usage"`
}

// GetAISuggestion 获取 AI 对任务的建议
func (c *AIController) GetAISuggestion(ctx *gin.Context) {
    userID, exists := ctx.Get("userID")
    if !exists {
        ctx.JSON(http.StatusUnauthorized, gin.H{"error": "用户未认证"})
        return
    }
    
    objectID, err := primitive.ObjectIDFromHex(userID.(string))
    if err != nil {
        ctx.JSON(http.StatusBadRequest, gin.H{"error": "无效的用户ID"})
        return
    }
    
    user, err := c.userRepo.FindByID(objectID)
    if err != nil {
        ctx.JSON(http.StatusNotFound, gin.H{"error": "用户未找到"})
        return
    }
    
    if user.APIKey == "" {
        ctx.JSON(http.StatusBadRequest, gin.H{"error": "未设置API密钥"})
        return
    }
    
    var request AIRequest
    if err := ctx.ShouldBindJSON(&request); err != nil {
        ctx.JSON(http.StatusBadRequest, gin.H{"error": "无效的请求数据"})
        return
    }
    
    // taskID, err := primitive.ObjectIDFromHex(request.TaskID)
    // if err != nil {
    //     ctx.JSON(http.StatusBadRequest, gin.H{"error": "无效的任务ID"})
    //     return
    // }
    
    task, err := c.taskRepo.GetByID(ctx, request.TaskID)
    if err != nil {
        ctx.JSON(http.StatusNotFound, gin.H{"error": "任务未找到"})
        return
    }
    
    // 构建提示词
    currentTime := time.Now().Format("2006-01-02 15:04")
    
    prompt := fmt.Sprintf("现在是%s，我计划在%s于%s前完成%s，", 
                         currentTime, 
                         task.Place, 
                         task.Date.Format("2006-01-02 15:04"), 
                         task.Title)
    
    if task.Description != "" {
        prompt += fmt.Sprintf("其详细是%s，", task.Description)
    }
    
    if request.DetailedPrompts != "" {
        prompt += fmt.Sprintf("详细信息是%s，", request.DetailedPrompts)
    }
    
    if task.Category != "" {
        prompt += fmt.Sprintf("这个任务属于%s类别，", task.Category)
    }
    
    isCompleted := "已经完成"
    if !task.IsFinished {
        isCompleted = "尚未完成"
    }
    prompt += "目前" + isCompleted + "。"
    
    prompt += `
请给我一些关于这个任务的建议，注意要求：
1. 必须使用纯文本格式，不要使用任何Markdown语法（如*加粗*、_斜体_、#标题等）
2. 使用平实的聊天语气
3. 直接给出建议，不要添加"以下是我的建议"之类的开场白
4. 不要在回复结尾添加"需要更多帮助吗"、"还有其他问题吗"之类的结束语
5. 可以内容丰富一点，但要整体有条例，是一个很好的建议视角
    `
    
    // 构建 API 请求
    deepseekRequest := DeepseekRequest{
        Model: "deepseek-chat",
        Messages: []DeepseekMessage{
            {
                Role:    "user",
                Content: prompt,
            },
        },
        Temperature: 0.7,
        MaxTokens:   300,
    }
    
    requestBody, err := json.Marshal(deepseekRequest)
    if err != nil {
        ctx.JSON(http.StatusInternalServerError, gin.H{"error": "构建请求失败"})
        return
    }
    
    httpClient := &http.Client{}
    req, err := http.NewRequest("POST", "https://api.deepseek.com/v1/chat/completions", bytes.NewBuffer(requestBody))
    if err != nil {
        ctx.JSON(http.StatusInternalServerError, gin.H{"error": "创建HTTP请求失败"})
        return
    }
    
    req.Header.Set("Content-Type", "application/json")
    req.Header.Set("Authorization", "Bearer "+user.APIKey)
    
    resp, err := httpClient.Do(req)
    if err != nil {
        ctx.JSON(http.StatusInternalServerError, gin.H{"error": "调用DeepSeek API失败"})
        return
    }
    defer resp.Body.Close()
    
    if resp.StatusCode != 200 {
        var errorResponse map[string]interface{}
        json.NewDecoder(resp.Body).Decode(&errorResponse)
        ctx.JSON(http.StatusInternalServerError, gin.H{"error": fmt.Sprintf("DeepSeek API返回错误: %v", errorResponse)})
        return
    }
    
    var deepseekResponse DeepseekResponse
    if err := json.NewDecoder(resp.Body).Decode(&deepseekResponse); err != nil {
        ctx.JSON(http.StatusInternalServerError, gin.H{"error": "解析API响应失败"})
        return
    }
    
    if len(deepseekResponse.Choices) == 0 {
        ctx.JSON(http.StatusInternalServerError, gin.H{"error": "API没有返回有效的回复"})
        return
    }
    
    aiSuggestion := deepseekResponse.Choices[0].Message.Content
    
    // 移除可能的Markdown语法
    aiSuggestion = removeMarkdown(aiSuggestion)
    
    ctx.JSON(http.StatusOK, gin.H{"suggestion": aiSuggestion})
}

// removeMarkdown 移除文本中的 Markdown 语法
func removeMarkdown(text string) string {
    // 简单的替换，可以根据需要扩展
    replacements := map[string]string{
        "*": "",
        "_": "",
        "#": "",
        "```": "",
        "`": "",
    }
    
    for old, new := range replacements {
        text = strings.Replace(text, old, new, -1)
    }
    
    return text
}