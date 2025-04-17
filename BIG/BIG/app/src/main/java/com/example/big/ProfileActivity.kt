package com.example.big

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class ProfileActivity : AppCompatActivity() {
    private lateinit var profileImageView: ImageView
    private lateinit var nicknameTextView: TextView
    private lateinit var saveButton: Button
    private var imageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        profileImageView = findViewById(R.id.profile_image)
        nicknameTextView = findViewById(R.id.nickname_text)
        saveButton = findViewById(R.id.save_button)

        // 加载用户信息
        loadUserInfo()

        // 设置点击事件
        profileImageView.setOnClickListener { openGallery() }

        nicknameTextView.setOnClickListener {
            // 这里可以弹出一个对话框来修改昵称
            // 简单起见，我们直接允许文本编辑
            nicknameTextView.isEnabled = true
        }

        saveButton.setOnClickListener {
            saveUserInfo()
            Toast.makeText(this@ProfileActivity, "保存成功", Toast.LENGTH_SHORT).show()
            finish()
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
                profileImageView.setImageURI(imageUri)
            }
        }
    }

    private fun loadUserInfo() {
        val prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        val nickname = prefs.getString("nickname", "用户") ?: "用户"
        nicknameTextView.text = nickname

        // 这里应该加载用户头像，但由于图片处理的复杂性，这里简化处理
    }

    private fun saveUserInfo() {
        val editor = getSharedPreferences("UserPrefs", MODE_PRIVATE).edit()
        editor.putString("nickname", nicknameTextView.text.toString())
        // 这里应该保存头像Uri，但由于存储的复杂性，这里简化处理
        editor.apply()
    }

    companion object {
        private const val PICK_IMAGE = 100
    }
}