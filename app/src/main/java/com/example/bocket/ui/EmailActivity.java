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

import org.json.JSONObject; // Bổ sung thư viện này để đọc lỗi JSON

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
        // Vô hiệu hóa nút bấm trong lúc chờ Server phản hồi để tránh bấm 2 lần
        btnContinue.setEnabled(false);
        Toast.makeText(this, "Đang xử lý...", Toast.LENGTH_SHORT).show();

        String mode = getIntent().getStringExtra("mode"); // Lấy nhãn từ Login gửi sang
        User user = new User();
        user.setEmail(email);

        RetrofitClient.getInstance().getApi().sendOTP(user).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                btnContinue.setEnabled(true); // Bật lại nút bấm

                if (response.isSuccessful()) {
                    Toast.makeText(EmailActivity.this, "Mã OTP đã được gửi!", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(EmailActivity.this, OtpActivity.class);
                    intent.putExtra("email", email);
                    intent.putExtra("mode", mode); // Tiếp tục truyền nhãn sang trang OTP
                    startActivity(intent);
                } else {
                    // TRƯỜNG HỢP SERVER BÁO LỖI (Ví dụ: 400 - Email đã tồn tại)
                    try {
                        String errorBody = response.errorBody() != null ? response.errorBody().string() : "";
                        JSONObject jsonObject = new JSONObject(errorBody);
                        String serverMessage = jsonObject.getString("message");

                        // Hiển thị chính xác thông báo lỗi từ Server gửi về
                        Toast.makeText(EmailActivity.this, serverMessage, Toast.LENGTH_LONG).show();
                    } catch (Exception e) {
                        Log.e("EMAIL_ERR", "Lỗi phân tích JSON: " + e.getMessage());
                        Toast.makeText(EmailActivity.this, "Email không hợp lệ hoặc đã tồn tại!", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                btnContinue.setEnabled(true); // Bật lại nút bấm
                Log.e("EMAIL_ERR", "Lỗi kết nối: " + t.getMessage());
                Toast.makeText(EmailActivity.this, "Mất kết nối mạng. Vui lòng thử lại!", Toast.LENGTH_SHORT).show();
            }
        });
    }
}