package com.example.big.ui

//import androidx.compose.foundation.layout.*
//import androidx.compose.material3.*
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.ExitToApp
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.unit.dp
//import com.example.big.models.UserResponse
//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun ProfileScreen(user: UserResponse, onLogout: () -> Unit) {
//    Scaffold(
//        topBar = {
//            TopAppBar(
//                title = { Text("个人资料") },
//                actions = {
//                    IconButton(onClick = onLogout) {
//                        Icon(
//                            imageVector = Icons.Default.ExitToApp,
//                            contentDescription = "退出登录"
//                        )
//                    }
//                }
//            )
//        }
//    ) { paddingValues ->
//        Column(
//            modifier = Modifier
//                .fillMaxSize()
//                .padding(paddingValues)
//                .padding(16.dp),
//            horizontalAlignment = Alignment.CenterHorizontally
//        ) {
//            Spacer(modifier = Modifier.height(32.dp))
//
//            // 用户头像占位
//            Surface(
//                modifier = Modifier.size(120.dp),
//                shape = MaterialTheme.shapes.large,
//                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
//            ) {
//                Box(contentAlignment = Alignment.Center) {
//                    Text(
//                        text = user.nickname?.firstOrNull()?.toString() ?: user.username.firstOrNull()?.toString() ?: "?",
//                        style = MaterialTheme.typography.headlineLarge
//                    )
//                }
//            }
//
//            Spacer(modifier = Modifier.height(24.dp))
//
//            Card(
//                modifier = Modifier.fillMaxWidth(),
//                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
//            ) {
//                Column(
//                    modifier = Modifier.padding(16.dp)
//                ) {
//                    ProfileInfoItem(title = "用户ID", value = user.id)
//                    Divider()
//                    ProfileInfoItem(title = "用户名", value = user.username)
//                    Divider()
//                    ProfileInfoItem(title = "昵称", value = user.nickname ?: "未设置")
//                    Divider()
//                    ProfileInfoItem(title = "邮箱", value = user.email ?: "未设置")
//                    Divider()
//                    ProfileInfoItem(title = "状态", value = user.status)
//                }
//            }
//
//            Spacer(modifier = Modifier.height(32.dp))
//
//            Button(
//                onClick = onLogout,
//                modifier = Modifier.fillMaxWidth()
//            ) {
//                Text("退出登录")
//            }
//        }
//    }
//}

//@Composable
//fun ProfileInfoItem(title: String, value: String) {
//    Row(
//        modifier = Modifier
//            .fillMaxWidth()
//            .padding(vertical = 8.dp),
//        horizontalArrangement = Arrangement.SpaceBetween
//    ) {
//        Text(
//            text = title,
//            style = MaterialTheme.typography.titleMedium
//        )
//        Text(
//            text = value,
//            style = MaterialTheme.typography.bodyLarge
//        )
//    }
//}