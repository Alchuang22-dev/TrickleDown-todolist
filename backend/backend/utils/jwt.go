package utils

import (
	"fmt"
	"time"
	
	"github.com/golang-jwt/jwt/v4"
)

// 密钥，实际应用中应该从环境变量或配置文件中读取
var (
	accessTokenSecret  = []byte("your_access_token_secret_key")
	refreshTokenSecret = []byte("your_refresh_token_secret_key")
)

// CustomClaims 自定义JWT声明结构
type CustomClaims struct {
	UserID uint `json:"user_id"`
	jwt.RegisteredClaims
}

// GenerateAccessToken 生成访问令牌
func GenerateAccessToken(userID uint) (string, error) {
	// 设置标准声明
	claims := CustomClaims{
		UserID: userID,
		RegisteredClaims: jwt.RegisteredClaims{
			ExpiresAt: jwt.NewNumericDate(time.Now().Add(24 * time.Hour)),
			IssuedAt:  jwt.NewNumericDate(time.Now()),
			NotBefore: jwt.NewNumericDate(time.Now()),
			Issuer:    "todo-app",
			Subject:   fmt.Sprintf("%d", userID),
		},
	}
	
	// 创建令牌
	token := jwt.NewWithClaims(jwt.SigningMethodHS256, claims)
	
	// 签名令牌
	tokenString, err := token.SignedString(accessTokenSecret)
	if err != nil {
		return "", err
	}
	
	return tokenString, nil
}

// GenerateRefreshToken 生成刷新令牌，有效期更长
func GenerateRefreshToken(userID uint) (string, error) {
	// 设置标准声明，刷新令牌有效期设为7天
	claims := CustomClaims{
		UserID: userID,
		RegisteredClaims: jwt.RegisteredClaims{
			ExpiresAt: jwt.NewNumericDate(time.Now().Add(7 * 24 * time.Hour)),
			IssuedAt:  jwt.NewNumericDate(time.Now()),
			NotBefore: jwt.NewNumericDate(time.Now()),
			Issuer:    "todo-app",
			Subject:   fmt.Sprintf("%d", userID),
		},
	}
	
	// 创建令牌
	token := jwt.NewWithClaims(jwt.SigningMethodHS256, claims)
	
	// 签名令牌
	tokenString, err := token.SignedString(refreshTokenSecret)
	if err != nil {
		return "", err
	}
	
	return tokenString, nil
}

// ValidateToken 验证令牌并返回声明
func ValidateToken(tokenString string) (*jwt.RegisteredClaims, error) {
	// 首先尝试验证访问令牌
	claims := &CustomClaims{}
	token, err := jwt.ParseWithClaims(tokenString, claims, func(token *jwt.Token) (interface{}, error) {
		// 验证签名算法
		if _, ok := token.Method.(*jwt.SigningMethodHMAC); !ok {
			return nil, fmt.Errorf("unexpected signing method: %v", token.Header["alg"])
		}
		return accessTokenSecret, nil
	})
	
	// 如果访问令牌验证成功
	if err == nil && token.Valid {
		return &claims.RegisteredClaims, nil
	}
	
	// 如果访问令牌验证失败，尝试验证刷新令牌
	refreshClaims := &CustomClaims{}
	refreshToken, err := jwt.ParseWithClaims(tokenString, refreshClaims, func(token *jwt.Token) (interface{}, error) {
		if _, ok := token.Method.(*jwt.SigningMethodHMAC); !ok {
			return nil, fmt.Errorf("unexpected signing method: %v", token.Header["alg"])
		}
		return refreshTokenSecret, nil
	})
	
	if err != nil || !refreshToken.Valid {
		return nil, err
	}
	
	return &refreshClaims.RegisteredClaims, nil
}

// GetUserIDFromToken 从令牌中提取用户ID
func GetUserIDFromToken(tokenString string) (uint, error) {
	claims, err := ValidateToken(tokenString)
	if err != nil {
		return 0, err
	}
	
	// 将Subject（用户ID字符串）转换为uint
	var userID uint
	_, err = fmt.Sscanf(claims.Subject, "%d", &userID)
	if err != nil {
		return 0, err
	}
	
	return userID, nil
}