package com.example.bocket.ui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.bocket.MainActivity;
import com.example.bocket.R;
import com.example.bocket.model.LoginRequest;
import com.example.bocket.model.LoginResponse;
import com.example.bocket.net.RetrofitClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {
    private ImageButton btnBack;
    private Button btnLogin;
    private EditText etUsername, etPassword,etEmail;
    private TextView tvForgotPassword;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        btnBack = findViewById(R.id.btn_back);
        btnLogin = findViewById(R.id.btn_login);
        etUsername = findViewById(R.id.et_username);
        etPassword = findViewById(R.id.et_password);
        tvForgotPassword = findViewById(R.id.tv_forgot_password);
        etEmail = findViewById(R.id.et_email);
        btnBack.setOnClickListener(v -> finish());

        btnLogin.setOnClickListener(v -> {
            String user = etUsername.getText().toString().trim();
            String pass = etPassword.getText().toString().trim();
            String email = etEmail.getText().toString().trim();

            if (user.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đủ thông tin", Toast.LENGTH_SHORT).show();
            } else {
                handleLogin(user, pass,email);
            }
        });
        // Giả sử bạn đã thêm TextView tvForgotPassword vào XML
        tvForgotPassword.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, EmailActivity.class);
            intent.putExtra("mode", "forgot_password"); // Gửi "nhãn" quên mật khẩu
            startActivity(intent);
        });
    }

    // Trong LoginActivity.java, hàm handleLogin
    private void handleLogin(String username, String password, String email) {
        // Truyền cả 3 tham số vào constructor
        LoginRequest loginRequest = new LoginRequest(username, password, email);

        RetrofitClient.getApiService().login(loginRequest).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // Lưu token và userId như cũ...
                    saveUserData(response.body().getToken(), response.body().getUser().getUserID());

                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                    finish();
                } else {
                    // Hiển thị lỗi từ Server (Ví dụ: "Tài khoản hoặc Email không chính xác!")
                    try {
                        // Đọc nội dung lỗi từ errorBody
                        String errorMsg = response.errorBody() != null ? response.errorBody().string() : "Lỗi đăng nhập";
                        Toast.makeText(LoginActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Toast.makeText(LoginActivity.this, "Sai thông tin đăng nhập", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                Toast.makeText(LoginActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveUserData(String token, int userId) {
        SharedPreferences sharedPref = getSharedPreferences("BocketPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString("jwt_token", token);
        editor.putInt("user_id", userId); // LƯU QUAN TRỌNG Ở ĐÂY
        editor.apply();
    }
}
