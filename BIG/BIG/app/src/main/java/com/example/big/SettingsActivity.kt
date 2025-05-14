package com.example.big

import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.big.api.ApiClient
import com.example.big.models.UserPermissionRequest
import com.example.big.utils.UserManager
import kotlinx.coroutines.launch
import android.Manifest

class SettingsActivity : ComponentActivity() {
    // 保存权限状态
    private val permissionStates = mutableStateOf(mapOf<String, Boolean>())

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 加载当前用户权限
        loadUserPermissions()

        setContent {
            MaterialTheme {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("设置") },
                            navigationIcon = {
                                IconButton(onClick = { finish() }) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowBack,
                                        contentDescription = "返回"
                                    )
                                }
                            }
                        )
                    }
                ) { paddingValues ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFFF5F5F5))
                            .padding(paddingValues)
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        PermissionsSection(
                            permissionStates = permissionStates.value,
                            onPermissionChange = { permissionType, isEnabled ->
                                updateUserPermission(permissionType, isEnabled)
                            }
                        )
                    }
                }
            }
        }
    }

    // 从服务器加载用户权限设置
    private fun loadUserPermissions() {
        val userId = UserManager.getUserId() ?: return

        lifecycleScope.launch {
            try {
                val response = ApiClient.userApiService.getUserPermissions(userId)
                if (response.isSuccessful) {
                    val permissions = response.body() ?: emptyMap()
                    permissionStates.value = permissions
                    Log.d("SettingsActivity", "加载用户权限成功: $permissions")
                } else {
                    Log.e("SettingsActivity", "加载用户权限失败: ${response.errorBody()?.string()}")
                    Toast.makeText(this@SettingsActivity, "加载权限设置失败", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("SettingsActivity", "加载用户权限错误: ${e.message}", e)
                Toast.makeText(this@SettingsActivity, "权限设置暂未开启", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 更新用户权限设置到服务器
    private fun updateUserPermission(permissionType: String, isEnabled: Boolean) {
        val userId = UserManager.getUserId() ?: return

        lifecycleScope.launch {
            try {
                val request = UserPermissionRequest(permissionType, isEnabled)
                val response = ApiClient.userApiService.updateUserPermission(userId, request)

                if (response.isSuccessful) {
                    // 更新本地状态
                    val updatedPermissions = permissionStates.value.toMutableMap()
                    updatedPermissions[permissionType] = isEnabled
                    permissionStates.value = updatedPermissions

                    Log.d("SettingsActivity", "更新权限成功: $permissionType = $isEnabled")
                    Toast.makeText(this@SettingsActivity, "权限设置已更新", Toast.LENGTH_SHORT).show()
                } else {
                    Log.e("SettingsActivity", "更新权限失败: ${response.errorBody()?.string()}")
                    Toast.makeText(this@SettingsActivity, "更新权限设置失败", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("SettingsActivity", "更新权限错误: ${e.message}", e)
                Toast.makeText(this@SettingsActivity, "网络错误，请稍后重试", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

@Composable
fun PermissionsSection(
    permissionStates: Map<String, Boolean>,
    onPermissionChange: (String, Boolean) -> Unit
) {
    val context = LocalContext.current

    // 请求通知权限的启动器
    val requestNotificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            onPermissionChange("NOTIFICATION", true)
        } else {
            Toast.makeText(context, "需要通知权限来发送提醒", Toast.LENGTH_SHORT).show()
        }
    }

    // 请求闹钟权限的启动器 (Android 12+)
    val requestAlarmPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            onPermissionChange("ALARM", true)
        } else {
            Toast.makeText(context, "需要闹钟权限来设置提醒", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
    ) {
        Text(
            text = "提醒设置",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "提醒权限",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // 通知权限
                PermissionItem(
                    title = "通知提醒",
                    description = "任务截止前通过通知提醒",
                    isEnabled = permissionStates["NOTIFICATION"] ?: false,
                    onToggle = { isEnabled ->
                        if (isEnabled) {
                            // 在 Android 13+ 上请求通知权限
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                val permissionStatus = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.POST_NOTIFICATIONS
                                )
                                if (permissionStatus != PackageManager.PERMISSION_GRANTED) {
                                    requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    return@PermissionItem
                                }
                            }
                        }
                        onPermissionChange("NOTIFICATION", isEnabled)
                    }
                )

                Divider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = Color(0xFFEEEEEE)
                )

                // 闹钟权限
                PermissionItem(
                    title = "任务闹钟",
                    description = "任务开始时通过闹钟提醒",
                    isEnabled = permissionStates["ALARM"] ?: false,
                    onToggle = { isEnabled ->
                        if (isEnabled) {
                            // 在 Android 12+ 上请求闹钟权限
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                                val permissionStatus = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.SCHEDULE_EXACT_ALARM
                                )
                                if (permissionStatus != PackageManager.PERMISSION_GRANTED) {
                                    requestAlarmPermission.launch(Manifest.permission.SCHEDULE_EXACT_ALARM)
                                    return@PermissionItem
                                }
                            }
                        }
                        onPermissionChange("ALARM", isEnabled)
                    }
                )
            }
        }

        // 提示卡片
        Card(
            modifier = Modifier
                .fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFE3F2FD)
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "信息",
                    tint = Color(0xFF2196F3),
                    modifier = Modifier.padding(end = 8.dp)
                )

                Text(
                    text = "为保证您的后台安全，测试版暂时不支持开启提醒",
                    color = Color(0xFF1976D2),
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun PermissionItem(
    title: String,
    description: String,
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )

            Text(
                text = description,
                fontSize = 14.sp,
                color = Color.Gray
            )
        }

        Switch(
            checked = isEnabled,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF2196F3),
                checkedTrackColor = Color(0xFFBBDEFB)
            )
        )
    }
}