package com.example.bocket.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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


public class OtpActivity extends AppCompatActivity {
    private Button btnRegister;
    private ImageButton btnBack;
    private EditText otp1, otp2, otp3, otp4, otp5, otp6;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_otp);
        String email = getIntent().getStringExtra("email");

        btnRegister = findViewById(R.id.btnRegister);
        btnBack = findViewById(R.id.btnBack);
        otp1 = findViewById(R.id.otp1);
        otp2 = findViewById(R.id.otp2);
        otp3 = findViewById(R.id.otp3);
        otp4 = findViewById(R.id.otp4);
        otp5 = findViewById(R.id.otp5);
        otp6 = findViewById(R.id.otp6);

        setUpOTPInputs();

        btnRegister.setOnClickListener(v -> {
            String fullCode = getOtpFromInputs(); // Hàm phụ để gom 6 ô thành 1 chuỗi

            if (fullCode.length() < 6) {
                Toast.makeText(this, "Vui lòng nhập đủ mã xác thực 6 số", Toast.LENGTH_SHORT).show();
                return;
            }

            verifyOtp(email, fullCode);
        });

        btnBack.setOnClickListener(v -> finish());
    }
    // Hàm hỗ trợ gom mã OTP
    private String getOtpFromInputs() {
        return otp1.getText().toString().trim() +
                otp2.getText().toString().trim() +
                otp3.getText().toString().trim() +
                otp4.getText().toString().trim() +
                otp5.getText().toString().trim() +
                otp6.getText().toString().trim();
    }
    private void setUpOTPInputs(){
        otp1.addTextChangedListener(new GenericTextWatcher(otp1, otp2));
        otp2.addTextChangedListener(new GenericTextWatcher(otp2, otp3));
        otp3.addTextChangedListener(new GenericTextWatcher(otp3, otp4));
        otp4.addTextChangedListener(new GenericTextWatcher(otp4, otp5));
        otp5.addTextChangedListener(new GenericTextWatcher(otp5, otp6));
        otp6.addTextChangedListener(new GenericTextWatcher(otp6, null));
    }
    public class GenericTextWatcher implements TextWatcher {
        private final EditText currentView;
        private final EditText nextView;
        public GenericTextWatcher(EditText currentView, EditText nextView){
            this.currentView = currentView;
            this.nextView = nextView;
        }
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            // Nếu đã nhập 1 ký tự và có ô tiếp theo -> Nhảy sang ô đó
            if (s.length() == 1 && nextView != null) {
                nextView.requestFocus();
            }
        }
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void afterTextChanged(Editable s) {
            // Nếu xóa ký tự (độ dài = 0) -> Bạn có thể code thêm để quay lại ô trước nếu muốn
        }
    }
    private void verifyOtp(String email, String otp) {
        User user = new User();
        user.setEmail(email);
        user.setOtp(otp); // Đảm bảo model User có trường otp

        RetrofitClient.getInstance().getApi().verifyOTP(user).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    String mode = getIntent().getStringExtra("mode");

                    if ("forgot_password".equals(mode)) {
                        // ĐIỀU HƯỚING SANG TRANG MẬT KHẨU MỚI
                        Intent intent = new Intent(OtpActivity.this, ResetPasswordActivity.class);
                        intent.putExtra("email", email);
                        startActivity(intent);
                    } else {
                        // ĐIỀU HƯỚNG SANG TRANG ĐĂNG KÝ (CŨ)
                        Intent intent = new Intent(OtpActivity.this, RegisterActivity.class);
                        intent.putExtra("email", email);
                        startActivity(intent);
                    }
                    finish();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(OtpActivity.this, "Lỗi kết nối!", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
