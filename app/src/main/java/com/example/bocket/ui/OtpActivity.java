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
            String s1 = otp1.getText().toString().trim();
            String s2 = otp2.getText().toString().trim();
            String s3 = otp3.getText().toString().trim();
            String s4 = otp4.getText().toString().trim();
            String s5 = otp5.getText().toString().trim();
            String s6 = otp6.getText().toString().trim();
            if (s1.isEmpty() || s2.isEmpty() || s3.isEmpty() ||
                    s4.isEmpty() || s5.isEmpty() || s6.isEmpty()) {

                // Thông báo nếu chưa nhập đủ
                Toast.makeText(OtpActivity.this, "Vui lòng nhập đủ 6 chữ số mã xác nhận!", Toast.LENGTH_SHORT).show();
            } else {
                // 3. Ghép chuỗi để lấy mã OTP hoàn chỉnh
                String fullCode = s1 + s2 + s3 + s4 + s5 + s6;
                verifyOtp(email, fullCode);
            }
        });
        btnBack.setOnClickListener(v -> finish());
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
                    // OTP ĐÚNG -> Mới cho chuyển sang đăng ký
                    Toast.makeText(OtpActivity.this, "Xác thực thành công!", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(OtpActivity.this, RegisterActivity.class);
                    intent.putExtra("email", email);
                    startActivity(intent);
                    finish();
                } else {
                    // OTP SAI
                    Toast.makeText(OtpActivity.this, "Mã OTP không chính xác!", Toast.LENGTH_SHORT).show();
                    try {
                        // Đọc nội dung lỗi thực sự từ server trả về
                        String errorBody = response.errorBody().string();
                        android.util.Log.e("SERVER_ERROR", errorBody);
                        Toast.makeText(OtpActivity.this, "Server trả về: " + errorBody, Toast.LENGTH_LONG).show();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(OtpActivity.this, "Lỗi kết nối!", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
