# 使用更稳定的Go版本
FROM golang:1.23

# 设置工作目录
WORKDIR /app

# 复制Go模块文件
COPY go.mod go.sum ./

# 尝试直接下载依赖，如果有vendor目录则使用vendor
RUN if [ -d "vendor" ]; then \
    echo "Using vendored dependencies"; \
    else \
    go env -w GOPROXY=https://goproxy.cn,direct && \
    go mod download; \
    fi

# 复制应用程序源代码
COPY . .

# 设置时区
ENV TZ=Asia/Shanghai

# 创建用于存储头像的目录
RUN mkdir -p /app/upload/avatars

# 构建Go应用程序
RUN go build -o main ./main.go

# 暴露应用程序端口
EXPOSE 8080

# 启动命令
CMD ["./main"]