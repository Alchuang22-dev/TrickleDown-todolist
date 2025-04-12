package com.example.big;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class EditTaskActivity extends AppCompatActivity {

    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private Switch importantSwitch;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_task);

        // 初始化视图
        EditText titleEditText = findViewById(R.id.task_title_edit);
        EditText descriptionEditText = findViewById(R.id.task_description_edit);
        EditText dueDateEditText = findViewById(R.id.task_due_date_edit);
        importantSwitch = findViewById(R.id.task_important_switch);
        Button saveButton = findViewById(R.id.save_task_button);

        // 设置返回按钮
        ImageView backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> finish());

        // 获取传入的任务数据
        String title = getIntent().getStringExtra("TASK_TITLE");
        String description = getIntent().getStringExtra("TASK_DESCRIPTION");
        boolean isImportant = getIntent().getBooleanExtra("TASK_IS_IMPORTANT", false);
        String dueDate = getIntent().getStringExtra("TASK_DUE_DATE");

        // 填充数据
        titleEditText.setText(title);
        descriptionEditText.setText(description);
        dueDateEditText.setText(dueDate);
        importantSwitch.setChecked(isImportant);

        // 设置保存按钮点击事件
        saveButton.setOnClickListener(v -> {
            // 保存任务逻辑
            Toast.makeText(EditTaskActivity.this, "任务已保存", Toast.LENGTH_SHORT).show();
            finish();
        });
    }
}