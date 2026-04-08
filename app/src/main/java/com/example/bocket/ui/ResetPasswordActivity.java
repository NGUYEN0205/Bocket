package com.example.bocket.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.bocket.R;
import com.example.bocket.model.User;
import com.example.bocket.net.RetrofitClient;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ResetPasswordActivity extends AppCompatActivity {
    private EditText etNewPass, etConfirmPass;
    private Button btn_reset_password;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        String email = getIntent().getStringExtra("email");
        etNewPass = findViewById(R.id.et_new_password);
        etConfirmPass = findViewById(R.id.et_confirm_password);
        btn_reset_password = findViewById(R.id.btn_reset_password);

        btn_reset_password.setOnClickListener(v -> {
            validateAndSubmit(email);
        });
    }

    private void validateAndSubmit(String email) {
        String pass = etNewPass.getText().toString().trim();
        String confirm = etConfirmPass.getText().toString().trim();

        // 2. Kiểm tra mật khẩu khớp nhau
        if (!pass.equals(confirm)) {
            etConfirmPass.setError("Mật khẩu xác nhận không khớp!");
            etConfirmPass.requestFocus();
            return;
        }

        // 3. Nếu mọi thứ ổn, gọi API
        updatePasswordToServer(email, pass);
    }

    private void updatePasswordToServer(String email, String password) {
        User user = new User();
        user.setEmail(email);
        user.setPassword(password);

        // LOG THỬ TRƯỚC KHI GỬI
        Log.d("DEBUG_RESET", "Gửi email: " + user.getEmail() + " | Pass: " + user.getPassword());

        RetrofitClient.getInstance().getApi().resetPassword(user).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(ResetPasswordActivity.this, "Thành công!", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(ResetPasswordActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                } else {
                    try {
                        // Đọc lỗi thật từ server trả về thay vì hiện câu thông báo chung chung
                        String errorBody = response.errorBody().string();
                        Log.e("API_ERROR", errorBody);
                        Toast.makeText(ResetPasswordActivity.this, "Lỗi Server: " + errorBody, Toast.LENGTH_LONG).show();
                    } catch (Exception e) { e.printStackTrace(); }
                }
            }
            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e("NETWORK_ERROR", t.getMessage());
            }
        });
    }
}