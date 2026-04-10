package com.example.bocket.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bocket.R;
import com.example.bocket.model.ChatPartner;
import com.example.bocket.net.RetrofitClient;

import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

// IMPORT ĐÚNG THƯ VIỆN SOCKET.IO
import io.socket.client.IO;
import io.socket.client.Socket;
import java.net.URISyntaxException;

public class ChatPartnerActivity extends AppCompatActivity {

    private RecyclerView rvChatPartners;
    private ChatPartnerAdapter adapter;
    private ImageView btnBack;


    private Socket mSocket;

    private int myUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_partner);

        // Thứ tự quan trọng: Load ID trước khi setup Socket
        loadMyUserId();
        initViews();
        setupSocket();
        loadChatPartners();
    }

    private void initViews() {
        rvChatPartners = findViewById(R.id.rvChatPartners);
        btnBack = findViewById(R.id.btnBack);
        rvChatPartners.setLayoutManager(new LinearLayoutManager(this));
        btnBack.setOnClickListener(v -> finish());
    }

    private void loadMyUserId() {
        SharedPreferences pref = getSharedPreferences("BocketPrefs", MODE_PRIVATE);
        // Đảm bảo key "user_id" trùng với key bạn đã save khi Đăng nhập thành công
        myUserId = pref.getInt("user_id", 0);
        Log.d("BOCKET_SOCKET", "My User ID: " + myUserId);
    }

    private void setupSocket() {
        try {
            // Khởi tạo mSocket theo kiểu io.socket.client.Socket
            mSocket = IO.socket(RetrofitClient.BASE_URL);

            mSocket.on(Socket.EVENT_CONNECT, args -> {
                Log.d("BOCKET_SOCKET", "Đã kết nối Socket Server!");
                // Register với server ngay khi kết nối thành công
                mSocket.emit("register", myUserId);
            });

            mSocket.connect();
            listenForUpdates();

        } catch (URISyntaxException e) {
            Log.e("SOCKET_ERROR", "URL không hợp lệ: " + e.getMessage());
        }
    }

    private void listenForUpdates() {
        if (mSocket != null) {
            mSocket.on("receive_message", args -> {
                runOnUiThread(() -> {
                    Log.d("BOCKET_SOCKET", "Có tin nhắn mới, đang cập nhật danh sách...");
                    loadChatPartners();
                });
            });
        }
    }

    private void loadChatPartners() {
        String token = getAuthToken();
        RetrofitClient.getApiService().getChatPartners(token).enqueue(new Callback<List<ChatPartner>>() {
            @Override
            public void onResponse(Call<List<ChatPartner>> call, Response<List<ChatPartner>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<ChatPartner> partnerList = response.body();
                    if (partnerList.size() > 0) {
                        adapter = new ChatPartnerAdapter(ChatPartnerActivity.this, partnerList);
                        rvChatPartners.setAdapter(adapter);
                    }
                }
            }

            @Override
            public void onFailure(Call<List<ChatPartner>> call, Throwable t) {
                Log.e("BOCKET_DEBUG", "Lỗi kết nối chat: " + t.getMessage());
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mSocket != null) {
            mSocket.disconnect();
            mSocket.off("receive_message");
        }
    }

    private String getAuthToken() {
        SharedPreferences sharedPref = getSharedPreferences("BocketPrefs", Context.MODE_PRIVATE);
        String token = sharedPref.getString("jwt_token", "");
        return "Bearer " + token;
    }
}