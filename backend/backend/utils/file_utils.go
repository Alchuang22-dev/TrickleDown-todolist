package utils

import (
    "errors"
    "fmt"
    "io"
    "mime/multipart"
    "os"
    "path/filepath"
    "strings"
    "time"
)

// 允许的图片格式
var allowedImageExtensions = map[string]bool{
    ".jpg":  true,
    ".jpeg": true,
    ".png":  true,
    ".gif":  true,
    ".webp": true,
}

// SaveUploadedFile 保存上传的文件到指定目录
func SaveUploadedFile(file *multipart.FileHeader, directory string) (string, error) {
    // 检查文件类型
    ext := strings.ToLower(filepath.Ext(file.Filename))
    if !allowedImageExtensions[ext] {
        return "", errors.New("不支持的图片格式，允许的格式包括: jpg, jpeg, png, gif, webp")
    }

    // 确保目录存在
    if err := os.MkdirAll(directory, 0755); err != nil {
        return "", fmt.Errorf("创建存储目录失败: %w", err)
    }

    // 生成唯一文件名
    filename := fmt.Sprintf("%d%s", time.Now().UnixNano(), ext)
    filePath := filepath.Join(directory, filename)

    // 打开上传的文件
    src, err := file.Open()
    if err != nil {
        return "", fmt.Errorf("打开上传文件失败: %w", err)
    }
    defer src.Close()

    // 创建目标文件
    dst, err := os.Create(filePath)
    if err != nil {
        return "", fmt.Errorf("创建目标文件失败: %w", err)
    }
    defer dst.Close()

    // 复制文件内容
    if _, err = io.Copy(dst, src); err != nil {
        return "", fmt.Errorf("保存文件失败: %w", err)
    }

    // 返回相对路径
    return filename, nil
}

// DeleteFile 删除文件
func DeleteFile(filename string, directory string) error {
    if filename == "" {
        return nil // 文件名为空不需要删除
    }
    
    filePath := filepath.Join(directory, filename)
    
    // 检查文件是否存在
    if _, err := os.Stat(filePath); os.IsNotExist(err) {
        return nil // 文件不存在不需要删除
    }
    
    // 删除文件
    err := os.Remove(filePath)
    if err != nil {
        return fmt.Errorf("删除文件失败: %w", err)
    }
    
    return nil
}