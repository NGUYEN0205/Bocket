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
    private EditText etUsername, etPassword;
    private TextView tvForgotPassword;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        btnBack = findViewById(R.id.btn_back);
        btnLogin = findViewById(R.id.btn_login);
        etUsername = findViewById(R.id.et_username);
        etPassword = findViewById(R.id.et_password);
        tvForgotPassword = findViewById(R.id.tv_forgot_password);
        btnBack.setOnClickListener(v -> finish());

        btnLogin.setOnClickListener(v -> {
            String user = etUsername.getText().toString().trim();
            String pass = etPassword.getText().toString().trim();

            if (user.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đủ thông tin", Toast.LENGTH_SHORT).show();
            } else {
                handleLogin(user, pass);
            }
        });
        // Giả sử bạn đã thêm TextView tvForgotPassword vào XML
        tvForgotPassword.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, EmailActivity.class);
            intent.putExtra("mode", "forgot_password"); // Gửi "nhãn" quên mật khẩu
            startActivity(intent);
        });
    }

    private void handleLogin(String username, String password) {
        LoginRequest loginRequest = new LoginRequest(username, password);

        RetrofitClient.getApiService().login(loginRequest).enqueue(new Callback<LoginResponse>() {
            // Tìm đến hàm handleLogin trong LoginActivity.java
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String token = response.body().getToken();

                    // LẤY USER ID TỪ RESPONSE
                    // Giả sử server trả về object user chứa UserID
                    int userId = response.body().getUser().getUserID();

                    // LƯU VÀO SHAREDPREFERENCES
                    SharedPreferences sharedPref = getSharedPreferences("BocketPrefs", Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putString("jwt_token", token);
                    editor.putInt("user_id", userId); // Dòng cực kỳ quan trọng
                    editor.apply();

                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                Toast.makeText(LoginActivity.this, "Lỗi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
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
