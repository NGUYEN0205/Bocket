package com.example.bocket.ui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.bocket.R;
import com.example.bocket.model.User;
import com.example.bocket.net.ApiService;
import com.example.bocket.net.RetrofitClient;

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileActivity extends AppCompatActivity {

    private CircleImageView ivProfileAvatar;
    private TextView tvProfileNickname, tvProfileUsername;
    private ImageButton btnBack;
    private View btnOpenSetting, btnOpenEditProfile,btnAddFriend;
    private User user;
    private RecyclerView rvFriends;
    private FriendsAdapter friendsAdapter;
    private List<User> friendsList = new ArrayList<>();
    private TextView tvFriendsTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        initViews();
        setupClickListeners();
    }
    @Override
    protected void onResume(){
        super.onResume();
        loadUserProfileData();
        loadFriendsData();
    }
    private void initViews() {
        ivProfileAvatar = findViewById(R.id.ivProfileAvatar);
        tvProfileNickname = findViewById(R.id.tvProfileNickname);
        tvProfileUsername = findViewById(R.id.tvProfileUsername);
        btnBack = findViewById(R.id.btnBack);

        // Ánh xạ 3 nút này giống hệt nhau
        btnOpenSetting = findViewById(R.id.btn_open_settings);
        btnOpenEditProfile = findViewById(R.id.btn_open_edit_profile);
        btnAddFriend = findViewById(R.id.btnAddFriendAction);

        rvFriends = findViewById(R.id.rvProfileFriendsList);

        // Thiết lập RecyclerView (Dạng danh sách dọc hoặc ngang tùy bạn)
        rvFriends.setLayoutManager(new LinearLayoutManager(this));
        friendsAdapter = new FriendsAdapter(this, friendsList);
        rvFriends.setAdapter(friendsAdapter);

        tvFriendsTitle = findViewById(R.id.tvFriendsTitle);
    }
    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());

        // Mở màn hình Thêm bạn
        if (btnAddFriend != null) {
            btnAddFriend.setOnClickListener(v -> {
                Intent intent = new Intent(ProfileActivity.this, AddFriendActivity.class);
                startActivity(intent);
            });
        }

        // Mở setting
        btnOpenSetting.setOnClickListener(v -> {
            startActivity(new Intent(ProfileActivity.this, SettingActivity.class));
        });

        // Mở chỉnh sửa
        btnOpenEditProfile.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, EditProfileActivity.class);
            intent.putExtra("current_DisplayName", user.getDisplay_name());
            Log.d("DEBUG_TRANSFER", "Email chuẩn bị gửi đi: " + user.getEmail());
            intent.putExtra("current_Email", user.getEmail());
            intent.putExtra("current_Avatar", user.getAvatar());
            startActivity(intent);
        });
    }
    private void loadUserProfileData() {
        SharedPreferences sharedPref = getSharedPreferences("BocketPrefs", Context.MODE_PRIVATE);
        // Lấy Token đã lưu khi đăng nhập thành công
        String token = "Bearer " + sharedPref.getString("jwt_token", "");

        ApiService apiService = RetrofitClient.getApiService();
        apiService.getUserProfile(token).enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                try {
                    String rawJson = new com.google.gson.Gson().toJson(response.body());
                } catch (Exception e) { }

                if (response.isSuccessful() && response.body() != null) {
                    user = response.body();
                    String displayName = user.getDisplay_name();
                    String username = user.getUsername();


                    // HIỂN THỊ DỮ LIỆU LÊN GIAO DIỆN
                    if (displayName != null && !displayName.trim().isEmpty()) {
                        tvProfileNickname.setText(displayName);
                    } else {
                        tvProfileNickname.setText(username);
                    } // Dùng getter tương ứng của bạn
                    tvProfileUsername.setText("@" + user.getUserID());

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
    private void loadFriendsData() {
        SharedPreferences sharedPref = getSharedPreferences("BocketPrefs", Context.MODE_PRIVATE);
        String token = "Bearer " + sharedPref.getString("jwt_token", "");
        int myID = sharedPref.getInt("user_id", -1);

        ApiService apiService = RetrofitClient.getApiService();
        apiService.getFriendsList(token).enqueue(new Callback<List<User>>() {
            @Override
            public void onResponse(Call<List<User>> call, Response<List<User>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    friendsList.clear();
                    // Lọc bỏ chính mình nếu API trả về cả bản thân
                    for (User u : response.body()) {
                        if (u.getUserID() != myID) {
                            friendsList.add(u);
                        }
                    }
                    friendsAdapter.notifyDataSetChanged();

                    if (tvFriendsTitle != null) {
                        tvFriendsTitle.setText("Bạn bè (" + friendsList.size() + ")");
                    }
                }
            }

            @Override
            public void onFailure(Call<List<User>> call, Throwable t) {
                Log.e("PROFILE_FRIENDS", "Lỗi: " + t.getMessage());
                if (tvFriendsTitle != null) {
                    tvFriendsTitle.setText("Bạn bè (0)");
                }
            }
        });
    }
}