package repositories

import (
	"context"
	"errors"
	"time"
	
	"your-project/models"
	"your-project/utils"
	
	"go.mongodb.org/mongo-driver/bson"
	"go.mongodb.org/mongo-driver/bson/primitive"
	"go.mongodb.org/mongo-driver/mongo"
	"go.mongodb.org/mongo-driver/mongo/options"
)

// UserRepository 用户数据访问层
type UserRepository struct {
	collection *mongo.Collection
}

// NewUserRepository 创建用户仓库
func NewUserRepository(db *mongo.Database) *UserRepository {
	return &UserRepository{
		collection: db.Collection("user"),
	}
}

// Create 创建新用户
func (r *UserRepository) Create(user *models.User) (*models.User, error) {
	ctx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer cancel()
	
	// 检查用户名是否已存在
	var existingUser models.User
	err := r.collection.FindOne(ctx, bson.M{"username": user.Username}).Decode(&existingUser)
	if err == nil {
		return nil, errors.New("username already exists")
	} else if err != mongo.ErrNoDocuments {
		return nil, err
	}
	
	// 如果没有ID，生成一个新的
	if user.ID.IsZero() {
		user.ID = primitive.NewObjectID()
	}
	
	// 设置创建时间
	if user.CreatedDate.IsZero() {
		user.CreatedDate = time.Now()
	}
	
	// 插入到数据库
	_, err = r.collection.InsertOne(ctx, user)
	if err != nil {
		return nil, err
	}
	
	return user, nil
}

// FindByID 通过ID查找用户
func (r *UserRepository) FindByID(id primitive.ObjectID) (*models.User, error) {
	ctx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer cancel()
	
	var user models.User
	err := r.collection.FindOne(ctx, bson.M{"_id": id}).Decode(&user)
	if err != nil {
		if err == mongo.ErrNoDocuments {
			return nil, errors.New("user not found")
		}
		return nil, err
	}
	
	return &user, nil
}

// FindByUsername 通过用户名查找用户
func (r *UserRepository) FindByUsername(username string) (*models.User, error) {
	ctx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer cancel()
	
	var user models.User
	err := r.collection.FindOne(ctx, bson.M{"username": username}).Decode(&user)
	if err != nil {
		if err == mongo.ErrNoDocuments {
			return nil, errors.New("user not found")
		}
		return nil, err
	}
	
	return &user, nil
}

// FindAll 查找所有用户
func (r *UserRepository) FindAll(limit, offset int64) ([]*models.User, error) {
	ctx, cancel := context.WithTimeout(context.Background(), 30*time.Second)
	defer cancel()
	
	options := options.Find()
	if limit > 0 {
		options.SetLimit(limit)
		options.SetSkip(offset)
	}
	
	cursor, err := r.collection.Find(ctx, bson.M{}, options)
	if err != nil {
		return nil, err
	}
	defer cursor.Close(ctx)
	
	var users []*models.User
	if err = cursor.All(ctx, &users); err != nil {
		return nil, err
	}
	
	return users, nil
}

// Update 更新用户信息
func (r *UserRepository) Update(user *models.User) error {
	ctx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer cancel()
	
	_, err := r.collection.ReplaceOne(
		ctx,
		bson.M{"_id": user.ID},
		user,
	)
	
	return err
}

// Delete 删除用户
func (r *UserRepository) Delete(id primitive.ObjectID) error {
	ctx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer cancel()
	
	result, err := r.collection.DeleteOne(ctx, bson.M{"_id": id})
	if err != nil {
		return err
	}
	
	if result.DeletedCount == 0 {
		return errors.New("user not found")
	}
	
	return nil
}

// GenerateUniqueUsername 生成唯一的用户名
func (r *UserRepository) GenerateUniqueUsername() (string, error) {
	ctx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer cancel()
	
	for i := 0; i < 10; i++ { // 尝试10次
		username := utils.GenerateRandomNickname()
		
		// 检查用户名是否已存在
		var existingUser models.User
		err := r.collection.FindOne(ctx, bson.M{"username": username}).Decode(&existingUser)
		if err == mongo.ErrNoDocuments {
			// 未找到，用户名可用
			return username, nil
		}
	}
	
	return "", errors.New("could not generate unique username after multiple attempts")
}

// UpdateUserStatus 更新用户状态
func (r *UserRepository) UpdateUserStatus(id primitive.ObjectID, status models.UserStatus) error {
	ctx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer cancel()
	
	update := bson.M{
		"$set": bson.M{
			"status": status,
		},
	}
	
	if status == models.LoggedIn {
		update["$set"].(bson.M)["lastLoginDate"] = time.Now()
	}
	
	_, err := r.collection.UpdateOne(ctx, bson.M{"_id": id}, update)
	return err
}

// UpdatePermission 更新用户权限
func (r *UserRepository) UpdatePermission(id primitive.ObjectID, permType models.PermissionType, enabled bool) error {
	ctx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer cancel()
	
	update := bson.M{
		"$set": bson.M{
			"permissions." + string(permType): enabled,
		},
	}
	
	_, err := r.collection.UpdateOne(ctx, bson.M{"_id": id}, update)
	return err
}

// AddTaskToUser 添加任务到用户
func (r *UserRepository) AddTaskToUser(userID, taskID primitive.ObjectID) error {
	ctx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer cancel()
	
	update := bson.M{
		"$addToSet": bson.M{
			"taskIds": taskID,
		},
	}
	
	_, err := r.collection.UpdateOne(ctx, bson.M{"_id": userID}, update)
	return err
}

// RemoveTaskFromUser 从用户移除任务
func (r *UserRepository) RemoveTaskFromUser(userID, taskID primitive.ObjectID) error {
	ctx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer cancel()
	
	update := bson.M{
		"$pull": bson.M{
			"taskIds": taskID,
		},
	}
	
	_, err := r.collection.UpdateOne(ctx, bson.M{"_id": userID}, update)
	return err
}