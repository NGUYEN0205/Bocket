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

        btnContinue.setOnClickListener(v -> {
            validateEmail();
        });

        btnBack.setOnClickListener(v-> finish());


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

    private void sendOtpToServer(String email){
        User user = new User();
        user.setEmail(email);

        Toast.makeText(this, "Đang gửi mã...", Toast.LENGTH_SHORT).show();

        RetrofitClient.getInstance().getApi().sendOTP(user).enqueue(new retrofit2.Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(EmailActivity.this, "Mã OTP đã được gửi!", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(EmailActivity.this, OtpActivity.class);
                    intent.putExtra("email", email);
                    startActivity(intent);
                } else {
                    Toast.makeText(EmailActivity.this, "Lỗi: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e("RetrofitError", t.getMessage() != null ? t.getMessage() : "Unknown error");
                Toast.makeText(EmailActivity.this, "Lỗi kết nối Server!", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
