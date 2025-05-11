package com.example.big

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.InputType
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.big.api.ApiClient
import com.example.big.models.UpdateUserRequest
import com.example.big.utils.ImageUtils
import com.example.big.utils.TokenManager
import com.example.big.utils.UserManager
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.coroutines.launch

class ProfileActivity : AppCompatActivity() {
    private lateinit var profileImageView: CircleImageView
    private lateinit var nicknameTextView: EditText
    private lateinit var saveButton: Button
    private lateinit var loadingProgressBar: ProgressBar

    private var imageUri: Uri? = null
    private var userId: String? = null
    private var hasChangedImage = false
    private var originalNickname = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        // 初始化视图
        profileImageView = findViewById(R.id.profile_image)
        nicknameTextView = findViewById(R.id.nickname_text)
        saveButton = findViewById(R.id.save_button)
        loadingProgressBar = findViewById(R.id.loading_progress)

        // 获取当前用户ID
        userId = TokenManager.getUserId()

        if (userId == null) {
            Toast.makeText(this, "用户未登录，请先登录", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // 设置工具栏
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        // 加载用户信息
        loadUserInfo()

        // 设置点击事件
        profileImageView.setOnClickListener { openGallery() }

        nicknameTextView.setOnClickListener {
            showEditNicknameDialog()
        }

        saveButton.setOnClickListener {
            saveUserInfo()
        }
    }

    private fun openGallery() {
        val gallery = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI)
        startActivityForResult(gallery, PICK_IMAGE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && requestCode == PICK_IMAGE) {
            data?.data?.let {
                imageUri = it
                hasChangedImage = true

                // 使用Glide加载和显示图片
                Glide.with(this)
                    .load(imageUri)
                    .placeholder(R.drawable.default_avatar)
                    .error(R.drawable.default_avatar)
                    .into(profileImageView)
            }
        }
    }

    private fun loadUserInfo() {
        showLoading(true)

        // 首先尝试从本地获取用户信息
        val cachedUser = UserManager.getUserInfo()
        if (cachedUser != null) {
            // 显示缓存的用户信息
            originalNickname = cachedUser.nickname
            nicknameTextView.setText(cachedUser.nickname)

            // 加载头像
            if (cachedUser.avatarURL.isNotEmpty()) {
                val fullAvatarUrl = if (cachedUser.avatarURL.startsWith("http")) {
                    cachedUser.avatarURL
                } else {
                    ApiClient.BASE_URL.removeSuffix("/") + cachedUser.avatarURL
                }

                Log.d(TAG, "加载缓存头像，完整URL: $fullAvatarUrl")

                Glide.with(this)
                    .load(fullAvatarUrl)
                    .placeholder(R.drawable.default_avatar)
                    .error(R.drawable.default_avatar)
                    .into(profileImageView)
            }
        }

        // 然后从服务器获取最新信息
        lifecycleScope.launch {
            try {
                val response = ApiClient.userApiService.getUser(userId!!)
                if (response.isSuccessful) {
                    val user = response.body()
                    if (user != null) {
                        // 保存到本地
                        UserManager.saveUserInfo(user)

                        // 更新UI
                        originalNickname = user.nickname
                        nicknameTextView.setText(user.nickname)

                        // 加载头像
                        if (user.avatarURL.isNotEmpty()) {
                            val fullAvatarUrl = if (user.avatarURL.startsWith("http")) {
                                user.avatarURL
                            } else {
                                ApiClient.BASE_URL.removeSuffix("/") + user.avatarURL
                            }

                            Log.d(TAG, "加载服务器头像，完整URL: $fullAvatarUrl")

                            Glide.with(this@ProfileActivity)
                                .load(fullAvatarUrl)
                                .placeholder(R.drawable.default_avatar)
                                .error(R.drawable.default_avatar)
                                .into(profileImageView)
                        }
                    }
                } else {
                    Log.e(TAG, "Failed to get user: ${response.errorBody()?.string()}")
                    Toast.makeText(this@ProfileActivity, "获取用户信息失败", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading user info: ${e.message}", e)
                Toast.makeText(this@ProfileActivity, "网络错误，请稍后重试", Toast.LENGTH_SHORT).show()
            } finally {
                showLoading(false)
            }
        }
    }

    private fun saveUserInfo() {
        if (userId == null) {
            Toast.makeText(this, "用户ID不存在", Toast.LENGTH_SHORT).show()
            return
        }

        val currentNickname = nicknameTextView.text.toString().trim()

        // 检查是否有更改
        val hasChangedNickname = currentNickname != originalNickname

        if (!hasChangedNickname && !hasChangedImage) {
            Toast.makeText(this, "未做任何修改", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        Log.d(TAG, "开始保存用户信息，用户ID: $userId")
        Log.d(TAG, "昵称是否更改: $hasChangedNickname, 头像是否更改: $hasChangedImage")

        showLoading(true)

        lifecycleScope.launch {
            try {
                // 如果更改了头像，先上传头像
                var avatarUrl: String? = null
                if (hasChangedImage && imageUri != null) {
                    Log.d(TAG, "准备上传头像，图片URI: $imageUri")

                    // 检查 URI 是否能读取到文件
                    try {
                        val inputStream = contentResolver.openInputStream(imageUri!!)
                        Log.d(TAG, "成功打开图片文件流: ${inputStream != null}")
                        inputStream?.close()
                    } catch (e: Exception) {
                        Log.e(TAG, "无法打开图片文件流: ${e.message}", e)
                    }

                    val multipartImage = ImageUtils.uriToMultipart(this@ProfileActivity, imageUri!!, "avatar")
                    if (multipartImage != null) {
                        Log.d(TAG, "成功创建MultipartBody，开始上传头像")

                        try {
                            val response = ApiClient.userApiService.uploadAvatar(userId!!, multipartImage)
                            Log.d(TAG, "上传头像API调用完成，响应码: ${response.code()}")

                            if (response.isSuccessful) {
                                avatarUrl = response.body()?.get("avatarURL")
                                Log.d(TAG, "头像上传成功，返回的URL: $avatarUrl")
                            } else {
                                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                                Log.e(TAG, "头像上传失败，错误: $errorBody")
                                Toast.makeText(this@ProfileActivity, "上传头像失败: $errorBody", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "头像上传异常: ${e.message}", e)
                            e.printStackTrace()
                        }
                    } else {
                        Log.e(TAG, "创建MultipartBody失败，无法上传头像")
                    }
                }

                // 更新用户信息
                val updateRequest = UpdateUserRequest(
                    nickname = if (hasChangedNickname) currentNickname else null,
                    avatarURL = avatarUrl
                )

                Log.d(TAG, "开始更新用户信息: $updateRequest")

                try {
                    val response = ApiClient.userApiService.updateUser(userId!!, updateRequest)
                    Log.d(TAG, "更新用户信息API调用完成，响应码: ${response.code()}")

                    if (response.isSuccessful) {
                        // 更新本地缓存
                        response.body()?.let {
                            UserManager.saveUserInfo(it)
                            Log.d(TAG, "用户信息更新成功，新的头像URL: ${it.avatarURL}")
                        }

                        Toast.makeText(this@ProfileActivity, "保存成功", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        val errorBody = response.errorBody()?.string() ?: "Unknown error"
                        Log.e(TAG, "更新用户信息失败，错误: $errorBody")
                        Toast.makeText(this@ProfileActivity, "更新用户信息失败: $errorBody", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "更新用户信息异常: ${e.message}", e)
                    e.printStackTrace()
                }
            } catch (e: Exception) {
                Log.e(TAG, "保存用户信息过程中出现异常: ${e.message}", e)
                e.printStackTrace()
                Toast.makeText(this@ProfileActivity, "网络错误，请稍后重试", Toast.LENGTH_SHORT).show()
            } finally {
                showLoading(false)
            }
        }
    }

    private fun showEditNicknameDialog() {
        val editText = EditText(this)
        editText.inputType = InputType.TYPE_CLASS_TEXT
        editText.setText(nicknameTextView.text)

        AlertDialog.Builder(this)
            .setTitle("修改昵称")
            .setView(editText)
            .setPositiveButton("确定") { _, _ ->
                val newNickname = editText.text.toString().trim()
                if (newNickname.isNotEmpty()) {
                    nicknameTextView.setText(newNickname)
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showLoading(isLoading: Boolean) {
        loadingProgressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        saveButton.isEnabled = !isLoading
    }

    companion object {
        private const val TAG = "ProfileActivity"
        private const val PICK_IMAGE = 100
    }
}