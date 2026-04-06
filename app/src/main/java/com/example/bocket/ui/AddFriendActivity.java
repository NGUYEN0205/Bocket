package com.example.bocket.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Base64;
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

        // 1. Lấy danh sách LỜI MỜI NHẬN ĐƯỢC (Người khác gửi cho mình)
        RetrofitClient.getApiService().getPendingRequests(token).enqueue(new Callback<List<User>>() {
            @Override
            public void onResponse(Call<List<User>> call, Response<List<User>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<User> list = response.body();
                    tvTitleReceived.setVisibility(list.isEmpty() ? View.GONE : View.VISIBLE);

                    FriendRequestAdapter adapter = new FriendRequestAdapter(list, true, userId -> {
                        acceptFriendRequest(userId); // Bấm "Đồng ý"
                    });
                    rvReceived.setAdapter(adapter);
                }
            }
            @Override public void onFailure(Call<List<User>> call, Throwable t) {}
        });

        // 2. Lấy danh sách LỜI MỜI ĐÃ GỬI (Mình gửi cho người khác)
        RetrofitClient.getApiService().getSentRequests(token).enqueue(new Callback<List<User>>() {
            @Override
            public void onResponse(Call<List<User>> call, Response<List<User>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<User> list = response.body();
                    tvTitleSent.setText(list.isEmpty() ? "" : "Lời mời đã gửi");

                    // isReceivedType = false để hiện chữ "Đang chờ"
                    FriendRequestAdapter adapter = new FriendRequestAdapter(list, false, null);
                    rvSent.setAdapter(adapter);
                }
            }
            @Override public void onFailure(Call<List<User>> call, Throwable t) {}
        });
    }

    private void searchUserById(int userId) {
        RetrofitClient.getApiService().getUserById(getAuthToken(), userId).enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                if (response.isSuccessful() && response.body() != null) {
                    User user = response.body();
                    foundUserId = user.getUserID();
                    cvSearchResult.setVisibility(View.VISIBLE);

                    tvResultNickname.setText(user.getDisplay_name());
                    tvResultUsername.setText("@" + user.getUsername());

                    // Xử lý hiển thị ảnh đại diện (Base64)
                    if (user.getAvatar() != null && !user.getAvatar().isEmpty()) {
                        byte[] imageBytes = Base64.decode(user.getAvatar(), Base64.DEFAULT);
                        Glide.with(AddFriendActivity.this)
                                .load(imageBytes)
                                .placeholder(R.drawable.ic_avatar_placeholder)
                                .circleCrop()
                                .into(ivResultAvatar);
                    } else {
                        ivResultAvatar.setImageResource(R.drawable.ic_avatar_placeholder);
                    }
                } else {
                    cvSearchResult.setVisibility(View.GONE);
                    Toast.makeText(AddFriendActivity.this, "Không tìm thấy UserID này", Toast.LENGTH_SHORT).show();
                }
            }

            @Override public void onFailure(Call<User> call, Throwable t) {
                Toast.makeText(AddFriendActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
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
                    cvSearchResult.setVisibility(View.GONE); // Ẩn kết quả tìm kiếm
                    etSearchUsername.setText(""); // Xóa ô nhập
                    loadAllRequests(); // Cập nhật lại danh sách trạng thái ở dưới
                } else {
                    Toast.makeText(AddFriendActivity.this, "Yêu cầu đã tồn tại", Toast.LENGTH_SHORT).show();
                }
            }
            @Override public void onFailure(Call<Void> call, Throwable t) {}
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

    private String getAuthToken() {
        SharedPreferences sharedPref = getSharedPreferences("BocketPrefs", Context.MODE_PRIVATE);
        return "Bearer " + sharedPref.getString("jwt_token", "");
    }
}