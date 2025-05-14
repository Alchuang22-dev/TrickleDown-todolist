package com.example.big.models

data class UserPermissionRequest(
    val permissionType: String,  // 使用字符串而不是枚举以便兼容后端
    val enabled: Boolean
)

data class PermissionsResponse(
    val permissions: Map<String, Boolean>
)