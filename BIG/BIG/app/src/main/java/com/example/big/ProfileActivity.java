package com.example.big;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class ProfileActivity extends AppCompatActivity {

    private static final int PICK_IMAGE = 100;
    private ImageView profileImageView;
    private TextView nicknameTextView;
    private Button saveButton;
    private Uri imageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        profileImageView = findViewById(R.id.profile_image);
        nicknameTextView = findViewById(R.id.nickname_text);
        saveButton = findViewById(R.id.save_button);

        // 加载用户信息
        loadUserInfo();

        // 设置点击事件
        profileImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openGallery();
            }
        });

        nicknameTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 这里可以弹出一个对话框来修改昵称
                // 简单起见，我们直接允许文本编辑
                nicknameTextView.setEnabled(true);
            }
        });

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveUserInfo();
                Toast.makeText(ProfileActivity.this, "保存成功", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void openGallery() {
        Intent gallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI);
        startActivityForResult(gallery, PICK_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == PICK_IMAGE) {
            imageUri = data.getData();
            profileImageView.setImageURI(imageUri);
        }
    }

    private void loadUserInfo() {
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String nickname = prefs.getString("nickname", "用户");
        nicknameTextView.setText(nickname);

        // 这里应该加载用户头像，但由于图片处理的复杂性，这里简化处理
    }

    private void saveUserInfo() {
        SharedPreferences.Editor editor = getSharedPreferences("UserPrefs", MODE_PRIVATE).edit();
        editor.putString("nickname", nicknameTextView.getText().toString());
        // 这里应该保存头像Uri，但由于存储的复杂性，这里简化处理
        editor.apply();
    }
}