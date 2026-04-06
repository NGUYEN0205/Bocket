package com.example.bocket.ui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.bocket.R;
import com.example.bocket.model.User;
import com.example.bocket.net.ApiService;
import com.example.bocket.net.RetrofitClient;

import de.hdodenhof.circleimageview.CircleImageView;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileActivity extends AppCompatActivity {

    private CircleImageView ivProfileAvatar;
    private TextView tvProfileNickname, tvProfileUsername;
    private ImageButton btnBack;
    private View btnOpenSetting, btnOpenEditProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // 1. Ánh xạ Views
        initViews();

        // 2. Nút Quay lại
        btnBack.setOnClickListener(v -> finish());

        // Trong initViews hoặc onCreate của ProfileActivity
        LinearLayout llAddFriend = findViewById(R.id.btnAddFriendAction); // Bạn cần đặt ID cho Layout chứa "Kết bạn" ở XML
        llAddFriend.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, AddFriendActivity.class);
            startActivity(intent);
        });

        // 3. Tải thông tin User từ Server
        loadUserProfileData();

        // 4. Mở setting
        btnOpenSetting.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, SettingActivity.class);
            startActivity(intent);
        });

        // 5. Mở chỉnh sửa trang cá nhân
        btnOpenEditProfile.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, EditProfileActivity.class);

            startActivity(intent);
        });
    }

    private void initViews() {
        ivProfileAvatar = findViewById(R.id.ivProfileAvatar);
        tvProfileNickname = findViewById(R.id.tvProfileNickname);
        tvProfileUsername = findViewById(R.id.tvProfileUsername);
        btnBack = findViewById(R.id.btnBack);
        btnOpenSetting = findViewById(R.id.btn_open_settings);
        btnOpenEditProfile = findViewById(R.id.btn_open_edit_profile);
    }

    private void loadUserProfileData() {
        SharedPreferences sharedPref = getSharedPreferences("BocketPrefs", Context.MODE_PRIVATE);
        // Lấy Token đã lưu khi đăng nhập thành công
        String token = "Bearer " + sharedPref.getString("jwt_token", "");

        ApiService apiService = RetrofitClient.getApiService();
        apiService.getUserProfile(token).enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                if (response.isSuccessful() && response.body() != null) {
                    User user = response.body();
                    String displayName = user.getDisplay_name();
                    String username = user.getUsername();

                    // HIỂN THỊ DỮ LIỆU LÊN GIAO DIỆN
                    if (displayName != null && !displayName.trim().isEmpty()) {
                        tvProfileNickname.setText(displayName);
                    } else {
                        tvProfileNickname.setText(username);
                    } // Dùng getter tương ứng của bạn
                    tvProfileUsername.setText("@" + user.getUsername());

                    // XỬ LÝ ẢNH AVATAR (BASE64) BẰNG GLIDE
                    String base64Avatar = user.getAvatar();
                    if (base64Avatar != null && !base64Avatar.isEmpty()) {
                        try {
                            // Giải mã Base64
                            byte[] imageBytes = Base64.decode(base64Avatar, Base64.DEFAULT);

                            // Load ảnh bằng Glide và cắt tròn
                            Glide.with(ProfileActivity.this)
                                    .asBitmap()
                                    .load(imageBytes)
                                    .placeholder(R.drawable.ic_avatar_placeholder)
                                    .circleCrop() // Đảm bảo bo tròn hoàn hảo
                                    .into(ivProfileAvatar);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    // THÊM ĐOẠN NÀY ĐỂ XEM MÃ LỖI LÀ BAO NHIÊU
                    int statusCode = response.code();
                    Toast.makeText(ProfileActivity.this, "Lỗi khi lấy thông tin. Mã: " + statusCode, Toast.LENGTH_LONG).show();

                    // Nếu muốn xem chi tiết lỗi server gửi về, có thể log ra:
                    try {
                        android.util.Log.e("BOCKET_PROFILE", "Chi tiết lỗi: " + response.errorBody().string());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {
                Toast.makeText(ProfileActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }
}