package com.example.bocket.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.bocket.R;
import com.example.bocket.model.User;
import com.example.bocket.net.RetrofitClient;

import retrofit2.Callback;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;

public class EmailActivity extends AppCompatActivity {
    private Button btnContinue;
    private ImageButton btnBack;
    private EditText etEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_email);

        btnContinue = findViewById(R.id.btnContinue);
        btnBack = findViewById(R.id.btnBack);
        etEmail = findViewById(R.id.etEmail);

        // --- BỔ SUNG: Nhận dữ liệu tự động điền ---
        String prefillEmail = getIntent().getStringExtra("email_prefill");
        if (prefillEmail != null && !prefillEmail.isEmpty()) {
            etEmail.setText(prefillEmail);
        }

        btnContinue.setOnClickListener(v -> validateEmail());
        btnBack.setOnClickListener(v -> finish());
    }

    private void validateEmail(){
        String emailInput = etEmail.getText().toString().trim();

        // Kiểm tra nếu để trống
        if (emailInput.isEmpty()) {
            etEmail.setError("Bạn không được để trống ô này!");
            etEmail.requestFocus();
        }
        // Kiểm tra định dạng email (phải có @ và tên miền)
        else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(emailInput).matches()) {
            etEmail.setError("Địa chỉ email không hợp lệ (Ví dụ: abc@gmail.com)");
            etEmail.requestFocus();
        }
        // Nếu mọi thứ đều đúng
        else {
            sendOtpToServer(emailInput);
        }
    }

    private void sendOtpToServer(String email) {
        String mode = getIntent().getStringExtra("mode"); // Lấy nhãn từ Login gửi sang
        User user = new User();
        user.setEmail(email);

        RetrofitClient.getInstance().getApi().sendOTP(user).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    Intent intent = new Intent(EmailActivity.this, OtpActivity.class);
                    intent.putExtra("email", email);
                    intent.putExtra("mode", mode); // Tiếp tục truyền nhãn sang trang OTP
                    startActivity(intent);
                }
            }
            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) { /* Log lỗi */ }
        });
    }
}
