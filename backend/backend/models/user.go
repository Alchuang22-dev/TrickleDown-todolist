package models

import (
	"time"

	"go.mongodb.org/mongo-driver/bson/primitive"
)

// UserStatus 用户状态枚举
type UserStatus string

const (
	LoggedOut  UserStatus = "LOGGED_OUT"  // 退出登录
	Registered UserStatus = "REGISTERED"  // 已注册但未登录
	LoggedIn   UserStatus = "LOGGED_IN"   // 已登录
)

// PermissionType 权限类型枚举
type PermissionType string

const (
	Alarm        PermissionType = "ALARM"        // 闹钟权限
	Notification PermissionType = "NOTIFICATION" // 通知权限
	Location     PermissionType = "LOCATION"     // 位置权限
	Storage      PermissionType = "STORAGE"      // 存储权限
	Calendar     PermissionType = "CALENDAR"     // 日历权限
	Contacts     PermissionType = "CONTACTS"     // 联系人权限
)

// User 用户类，管理用户信息、状态和权限
type User struct {
	ID              primitive.ObjectID        `json:"id" bson:"_id,omitempty"`   // 用户ID
	Username        string                    `json:"username" bson:"username"`  // 用户名
	Nickname        string                    `json:"nickname" bson:"nickname"`  // 昵称
	Email           string                    `json:"email" bson:"email"`        // 邮箱
	PhoneNumber     string                    `json:"phoneNumber" bson:"phoneNumber"` // 手机号
	HashedPassword  string                    `json:"-" bson:"hashedPassword"`   // 加密后的密码
	AvatarURL       string                    `json:"avatarURL" bson:"avatarURL"` // 头像URL
	CreatedDate     time.Time                 `json:"createdDate" bson:"createdDate"` // 账户创建日期
	LastLoginDate   time.Time                 `json:"lastLoginDate" bson:"lastLoginDate,omitempty"` // 最后登录日期
	Status          UserStatus                `json:"status" bson:"status"`       // 当前状态
	Token           string                    `json:"-" bson:"token,omitempty"`   // 登录令牌
	TokenExpireDate time.Time                 `json:"-" bson:"tokenExpireDate,omitempty"` // 令牌过期时间
	RefreshToken    string                    `json:"-" bson:"refreshToken,omitempty"`   // 刷新令牌
	Permissions     map[PermissionType]bool   `json:"permissions" bson:"permissions"` // 权限设置
	Preferences     map[string]interface{}    `json:"preferences" bson:"preferences"` // 其他用户偏好
	TaskIDs         []primitive.ObjectID      `json:"taskIds" bson:"taskIds"` // 用户绑定的任务ID列表
}

// NewUser 创建一个新用户
func NewUser(username string) *User {
	id := primitive.NewObjectID()
	now := time.Now()
	
	user := &User{
		ID:          id,
		Username:    username,
		Status:      LoggedOut,
		CreatedDate: now,
		Permissions: make(map[PermissionType]bool),
		Preferences: make(map[string]interface{}),
		TaskIDs:     make([]primitive.ObjectID, 0),
	}
	
	// 初始化默认权限
	user.initDefaultPermissions()
	
	return user
}

// NewUserWithDetails 创建一个包含详细信息的用户
func NewUserWithDetails(username, nickname, email, phoneNumber string) *User {
	user := NewUser(username)
	user.Nickname = nickname
	user.Email = email
	user.PhoneNumber = phoneNumber
	
	return user
}

// 初始化默认权限设置
func (u *User) initDefaultPermissions() {
	u.Permissions[Alarm] = false
	u.Permissions[Notification] = false
	u.Permissions[Location] = false
	u.Permissions[Storage] = false
	u.Permissions[Calendar] = false
	u.Permissions[Contacts] = false
}

// ChangeStatus 更改用户状态
func (u *User) ChangeStatus(newStatus UserStatus) bool {
	// 检查状态转换是否有效
	if u.Status == LoggedOut && newStatus == LoggedIn {
		// 需要先注册或有有效凭证
		if u.Token == "" || u.IsTokenExpired() {
			return false
		}
	}
	
	u.Status = newStatus
	
	// 更新最后登录时间
	if newStatus == LoggedIn {
		u.LastLoginDate = time.Now()
	}
	
	return true
}

// Login 登录过程
func (u *User) Login(token string, expireDate time.Time, refreshToken string) bool {
	u.Token = token
	u.TokenExpireDate = expireDate
	u.RefreshToken = refreshToken
	return u.ChangeStatus(LoggedIn)
}

// Logout 退出登录
func (u *User) Logout() bool {
	u.Token = ""
	u.TokenExpireDate = time.Time{}
	u.RefreshToken = ""
	return u.ChangeStatus(LoggedOut)
}

// IsTokenExpired 检查令牌是否过期
func (u *User) IsTokenExpired() bool {
	if u.TokenExpireDate.IsZero() {
		return true
	}
	return time.Now().After(u.TokenExpireDate)
}

// RefreshToken 刷新令牌
func (u *User) RefreshToken(newToken string, newExpireDate time.Time, newRefreshToken string) {
	u.Token = newToken
	u.TokenExpireDate = newExpireDate
	u.RefreshToken = newRefreshToken
}

// UpdateNickname 更新用户昵称
func (u *User) UpdateNickname(nickname string) {
	u.Nickname = nickname
}

// UpdateAvatarURL 更新用户头像URL
func (u *User) UpdateAvatarURL(avatarURL string) {
	u.AvatarURL = avatarURL
}

// UpdateEmail 更新用户邮箱
func (u *User) UpdateEmail(email string) {
	u.Email = email
}

// UpdatePhoneNumber 更新用户手机号
func (u *User) UpdatePhoneNumber(phoneNumber string) {
	u.PhoneNumber = phoneNumber
}

// SetPermission 设置权限
func (u *User) SetPermission(permType PermissionType, enabled bool) {
	u.Permissions[permType] = enabled
}

// HasPermission 检查是否拥有权限
func (u *User) HasPermission(permType PermissionType) bool {
	permission, exists := u.Permissions[permType]
	return exists && permission
}

// SetPreference 设置用户偏好
func (u *User) SetPreference(key string, value interface{}) {
	u.Preferences[key] = value
}

// GetPreference 获取用户偏好
func (u *User) GetPreference(key string) interface{} {
	return u.Preferences[key]
}

// AddTask 添加任务到用户的任务列表
func (u *User) AddTask(taskID primitive.ObjectID) {
	u.TaskIDs = append(u.TaskIDs, taskID)
}

// RemoveTask 从用户的任务列表中移除任务
func (u *User) RemoveTask(taskID primitive.ObjectID) {
	for i, id := range u.TaskIDs {
		if id == taskID {
			u.TaskIDs = append(u.TaskIDs[:i], u.TaskIDs[i+1:]...)
			break
		}
	}
}

// GetUserInfo 获取用户完整信息的Map
func (u *User) GetUserInfo() map[string]interface{} {
	userInfo := map[string]interface{}{
		"id":           u.ID,
		"username":     u.Username,
		"nickname":     u.Nickname,
		"email":        u.Email,
		"phoneNumber":  u.PhoneNumber,
		"avatarURL":    u.AvatarURL,
		"status":       u.Status,
		"createdDate":  u.CreatedDate,
		"lastLoginDate": u.LastLoginDate,
		"permissions":  u.Permissions,
		"preferences":  u.Preferences,
		"taskIds":      u.TaskIDs,
	}
	return userInfo
}