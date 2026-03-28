package com.example.bocket.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.example.bocket.R;

public class WelcomActivity extends AppCompatActivity {
    private Button btnRegister;
    private Button btnLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        btnRegister = findViewById(R.id.btnCreateAccount);
        btnLogin = findViewById(R.id.btnLogin);

        // Xử lý sự kiện cho nút Đăng ký
        btnRegister.setOnClickListener(v -> {
            Intent intent = new Intent(WelcomActivity.this, EmailActivity.class);
            startActivity(intent);
        });

        // Xử lý sự kiện cho nút Đăng nhập (nếu cần)
        btnLogin.setOnClickListener(v -> {
             Intent intent = new Intent(this, LoginActivity.class);
             startActivity(intent);
        });
    }
}