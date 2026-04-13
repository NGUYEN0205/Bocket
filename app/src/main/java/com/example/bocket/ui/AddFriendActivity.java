package com.example.bocket.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.bocket.R;
import com.example.bocket.model.User;
import com.example.bocket.net.RetrofitClient;
import java.util.*;
import de.hdodenhof.circleimageview.CircleImageView;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AddFriendActivity extends AppCompatActivity {
    private EditText etSearchUsername;
    private ImageButton btnSearch, btnBack;
    private CardView cvSearchResult;
    private CircleImageView ivResultAvatar;
    private TextView tvResultNickname, tvResultUsername, tvTitleReceived, tvTitleSent;
    private Button btnAddFriendAction;

    private RecyclerView rvReceived, rvSent;
    private int foundUserId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_friend);

        initViews();

        btnBack.setOnClickListener(v -> finish());

        // Xử lý tìm kiếm User theo ID
        btnSearch.setOnClickListener(v -> {
            String input = etSearchUsername.getText().toString().trim();
            if (!input.isEmpty()) {
                try {
                    searchUserById(Integer.parseInt(input));
                } catch (NumberFormatException e) {
                    Toast.makeText(this, "Vui lòng nhập ID là con số", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Xử lý gửi lời mời kết bạn từ ô tìm kiếm
        btnAddFriendAction.setOnClickListener(v -> {
            if (foundUserId != -1) sendFriendRequest(foundUserId);
        });

        // Tải dữ liệu ban đầu
        loadAllRequests();
    }

    private void initViews() {
        etSearchUsername = findViewById(R.id.etSearchUsername);
        btnSearch = findViewById(R.id.btnSearch);
        btnBack = findViewById(R.id.btnBack);
        cvSearchResult = findViewById(R.id.cvSearchResult);

        // Đã mở lại các view này để tránh lỗi NullPointerException
        ivResultAvatar = findViewById(R.id.ivResultAvatar);
        tvResultNickname = findViewById(R.id.tvResultNickname);
        tvResultUsername = findViewById(R.id.tvResultUsername);
        btnAddFriendAction = findViewById(R.id.btnAddFriendAction);

        tvTitleReceived = findViewById(R.id.tvTitleReceived);
        tvTitleSent = findViewById(R.id.tvTitleSent);

        rvReceived = findViewById(R.id.rvReceivedRequests);
        rvSent = findViewById(R.id.rvSentRequests);

        rvReceived.setLayoutManager(new LinearLayoutManager(this));
        rvSent.setLayoutManager(new LinearLayoutManager(this));
    }

    private void loadAllRequests() {
        String token = getAuthToken();

        // 1. LẤY DANH SÁCH LỜI MỜI ĐÃ GỬI (SENT)
        RetrofitClient.getApiService().getSentRequests(token).enqueue(new Callback<List<User>>() {
            @Override
            public void onResponse(Call<List<User>> call, Response<List<User>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<User> list = response.body();

                    if (list.isEmpty()) {
                        tvTitleSent.setText("");
                    } else {
                        tvTitleSent.setText("Lời mời đã gửi (" + list.size() + ")");
                    }

                    // isReceivedType = false vì đây là lời mời mình gửi đi
                    FriendRequestAdapter sentAdapter = new FriendRequestAdapter(list, false, userId -> {
                        cancelFriendRequest(userId);
                    });
                    rvSent.setAdapter(sentAdapter);
                }
            }
            @Override public void onFailure(Call<List<User>> call, Throwable t) {
                Log.e("BOCKET_DEBUG", "Sent Requests Fail: " + t.getMessage());
            }
        });

        // 2. LẤY DANH SÁCH LỜI MỜI NHẬN ĐƯỢC (RECEIVED/PENDING) - ĐOẠN BẠN ĐANG THIẾU
        RetrofitClient.getApiService().getPendingRequests(token).enqueue(new Callback<List<User>>() {
            @Override
            public void onResponse(Call<List<User>> call, Response<List<User>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<User> list = response.body();

                    Log.d("BOCKET_DEBUG", "Số lời mời nhận được: " + list.size());

                    if (list.isEmpty()) {
                        tvTitleReceived.setText("");
                    } else {
                        tvTitleReceived.setText("Lời mời đã nhận (" + list.size() + ")");
                    }

                    // isReceivedType = true để hiện nút "Đồng ý"
                    // Truyền listener để xử lý khi bấm nút "Đồng ý"
                    FriendRequestAdapter receivedAdapter = new FriendRequestAdapter(list, true, userId -> {
                        acceptFriendRequest(userId); // Gọi hàm chấp nhận kết bạn
                    });
                    rvReceived.setAdapter(receivedAdapter);
                }
            }
            @Override public void onFailure(Call<List<User>> call, Throwable t) {
                Log.e("BOCKET_DEBUG", "Pending Requests Fail: " + t.getMessage());
            }
        });
    }

    private void searchUserById(int userId) {
        RetrofitClient.getApiService().getUserById(getAuthToken(), userId).enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                if (response.isSuccessful() && response.body() != null) {
                    User user = response.body();
                    foundUserId = user.getUserID();

                    // Kiểm tra xem View đã được ánh xạ chưa để tránh NullPointerException
                    if (cvSearchResult == null || tvResultNickname == null) return;

                    cvSearchResult.setVisibility(View.VISIBLE);

                    // Hiển thị tên (Fallback nếu tên hiển thị rỗng)
                    String displayName = user.getDisplay_name() != null ? user.getDisplay_name() : user.getUsername();
                    tvResultNickname.setText(displayName);
                    tvResultUsername.setText("@" + user.getUsername());

                    // XỬ LÝ ẢNH AVATAR AN TOÀN CHỐNG CRASH
                    String avatarData = user.getAvatar();
                    if (avatarData != null && !avatarData.isEmpty()) {
                        try {
                            // 1. Nếu là chuỗi Base64 có dính tiền tố "data:image/..." -> Cắt bỏ tiền tố
                            if (avatarData.contains(",")) {
                                avatarData = avatarData.split(",")[1];
                            }

                            // Cố gắng giải mã Base64
                            byte[] imageBytes = Base64.decode(avatarData, Base64.DEFAULT);
                            Glide.with(AddFriendActivity.this)
                                    .load(imageBytes)
                                    .placeholder(R.drawable.ic_avatar_placeholder)
                                    .circleCrop()
                                    .into(ivResultAvatar);

                        } catch (IllegalArgumentException e) {
                            // 2. Nếu nó không phải Base64 (mà là Link URL dạng http://...) thì load thẳng URL
                            Glide.with(AddFriendActivity.this)
                                    .load(user.getAvatar())
                                    .placeholder(R.drawable.ic_avatar_placeholder)
                                    .circleCrop()
                                    .into(ivResultAvatar);
                        }
                    } else {
                        // Không có ảnh thì hiện ảnh mặc định
                        ivResultAvatar.setImageResource(R.drawable.ic_avatar_placeholder);
                    }
                } else {
                    cvSearchResult.setVisibility(View.GONE);
                    Toast.makeText(AddFriendActivity.this, "Không tìm thấy UserID này", Toast.LENGTH_SHORT).show();
                }
            }

            @Override public void onFailure(Call<User> call, Throwable t) {
                Toast.makeText(AddFriendActivity.this, "Lỗi kết nối API", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sendFriendRequest(int friendId) {
        Map<String, Integer> body = new HashMap<>();
        body.put("friendId", friendId);

        RetrofitClient.getApiService().sendFriendRequest(getAuthToken(), body).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(AddFriendActivity.this, "Đã gửi lời mời!", Toast.LENGTH_SHORT).show();
                    cvSearchResult.setVisibility(View.GONE);
                    etSearchUsername.setText("");

                    // Delay 0.5s để SQL Server chắc chắn đã INSERT xong rồi mới tải lại danh sách
                    new android.os.Handler().postDelayed(() -> {
                        loadAllRequests();
                    }, 500);

                } else {
                    Toast.makeText(AddFriendActivity.this, "Yêu cầu đã tồn tại", Toast.LENGTH_SHORT).show();
                }
            }
            @Override public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(AddFriendActivity.this, "Lỗi mạng", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void acceptFriendRequest(int requesterId) {
        Map<String, Integer> body = new HashMap<>();
        body.put("requesterId", requesterId);

        RetrofitClient.getApiService().acceptFriend(getAuthToken(), body).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(AddFriendActivity.this, "Chấp nhận thành công!", Toast.LENGTH_SHORT).show();
                    loadAllRequests(); // Tải lại cả 2 danh sách để cập nhật dữ liệu mới
                }
            }
            @Override public void onFailure(Call<Void> call, Throwable t) {}
        });
    }

    private void cancelFriendRequest(int friendId) {
        Map<String, Integer> body = new HashMap<>();
        body.put("friendId", friendId);

        RetrofitClient.getApiService().cancelFriendRequest(getAuthToken(), body).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(AddFriendActivity.this, "Đã hủy lời mời", Toast.LENGTH_SHORT).show();
                    loadAllRequests(); // Tải lại danh sách
                }
            }
            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(AddFriendActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String getAuthToken() {
        SharedPreferences sharedPref = getSharedPreferences("BocketPrefs", Context.MODE_PRIVATE);
        return "Bearer " + sharedPref.getString("jwt_token", "");
    }
}