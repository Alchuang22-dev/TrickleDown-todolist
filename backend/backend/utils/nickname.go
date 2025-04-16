package utils

import (
	"fmt"
	"math/rand"
	"time"
)

// 初始化随机数生成器
func init() {
	rand.Seed(time.Now().UnixNano())
}

// 形容词列表
var adjectives = []string{
	"Happy", "Brave", "Clever", "Wise", "Swift", 
	"Calm", "Bright", "Kind", "Bold", "Neat",
	"Cool", "Witty", "Eager", "Proud", "Gentle",
	"Wild", "Jolly", "Sunny", "Quirky", "Funny",
	"Smart", "Lucky", "Noble", "Lively", "Strong",
}

// 名词列表
var nouns = []string{
	"Panda", "Tiger", "Dolphin", "Eagle", "Lion", 
	"Wolf", "Bear", "Owl", "Fox", "Hawk",
	"Deer", "Horse", "Koala", "Falcon", "Otter",
	"Phoenix", "Dragon", "Unicorn", "Wizard", "Knight",
	"Explorer", "Voyager", "Pioneer", "Champion", "Rider",
}

// GenerateRandomNickname 生成一个随机的用户昵称
// 格式为: [形容词][名词][数字]，例如 "HappyPanda42"
func GenerateRandomNickname() string {
	// 随机选择一个形容词
	adj := adjectives[rand.Intn(len(adjectives))]
	
	// 随机选择一个名词
	noun := nouns[rand.Intn(len(nouns))]
	
	// 添加一个随机数字 (0-999)
	num := rand.Intn(1000)
	
	// 将三部分组合起来
	return fmt.Sprintf("%s%s%d", adj, noun, num)
}
EOF