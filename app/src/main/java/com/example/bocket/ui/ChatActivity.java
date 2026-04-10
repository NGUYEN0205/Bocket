package com.example.bocket.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.bocket.R;
import com.example.bocket.model.DefaultResponse;
import com.example.bocket.model.Message;
import com.example.bocket.net.RetrofitClient;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.socket.client.Socket;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatActivity extends AppCompatActivity {

    private RecyclerView rvChat;
    private ChatAdapter adapter;
    private List<Message> messageList = new ArrayList<>();
    private EditText edtMessage;
    private ImageView btnSend, btnBack;
    private TextView tvFriendName;

    private int friendId;
    private String friendName, friendAvatar;
    private int myUserId;
    private ImageView ivFriendAvatarToolbar;
    private Socket mSocket;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // 1. Nhận dữ liệu từ Intent (ChatPartnerAdapter truyền sang)
        friendId = getIntent().getIntExtra("FRIEND_ID", 0);
        friendName = getIntent().getStringExtra("FRIEND_NAME");
        friendAvatar = getIntent().getStringExtra("FRIEND_AVATAR");

        initViews();
        loadMyUserId();
        setupSocket();
        loadMessages();
    }

    private void initViews() {
        rvChat = findViewById(R.id.rvChat);
        edtMessage = findViewById(R.id.edtMessage);
        btnSend = findViewById(R.id.btnSend);
        btnBack = findViewById(R.id.btnBack);
        tvFriendName = findViewById(R.id.tvFriendName);
        ivFriendAvatarToolbar = findViewById(R.id.ivFriendAvatarToolbar);

        tvFriendName.setText(friendName);
        loadAvatarToToolbar(friendAvatar);
        btnBack.setOnClickListener(v -> finish());

        // Cấu hình RecyclerView
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        // layoutManager.setStackFromEnd(true); // Luôn cuộn xuống cuối khi load
        rvChat.setLayoutManager(layoutManager);

        btnSend.setOnClickListener(v -> sendMessage());
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mSocket != null) {
            mSocket.disconnect();
            mSocket.off("receive_message");
        }
    }

    private void loadMyUserId() {
        SharedPreferences pref = getSharedPreferences("BocketPrefs", MODE_PRIVATE);
        // Phải dùng đúng KEY mà bạn đã lưu lúc Login (ví dụ: "user_id")
        myUserId = pref.getInt("user_id", 0);
        Log.d("BOCKET_DEBUG", "My User ID: " + myUserId); // Log ra để kiểm tra
    }

    private void loadMessages() {
        String token = getAuthToken();
        RetrofitClient.getApiService().getChatMessages(token, friendId).enqueue(new Callback<List<Message>>() {
            @Override
            public void onResponse(Call<List<Message>> call, Response<List<Message>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    messageList.clear();
                    messageList.addAll(response.body());
                    adapter = new ChatAdapter(ChatActivity.this, messageList, myUserId, friendAvatar);
                    rvChat.setAdapter(adapter);
                    rvChat.scrollToPosition(messageList.size() - 1); // Cuộn xuống tin nhắn mới nhất
                }
            }

            @Override
            public void onFailure(Call<List<Message>> call, Throwable t) {
                Log.e("CHAT_ERROR", t.getMessage());
            }
        });
    }

    private void sendMessage() {
        String content = edtMessage.getText().toString().trim();
        if (content.isEmpty()) return;

        String token = getAuthToken();

        // Tạo body gửi đi
        Map<String, Object> body = new HashMap<>();
        body.put("receiverId", friendId);
        body.put("content", content);

        // Gọi API
        RetrofitClient.getApiService().sendMessage(token, body).enqueue(new Callback<DefaultResponse>() {
            @Override
            public void onResponse(Call<DefaultResponse> call, Response<DefaultResponse> response) {
                if (response.isSuccessful()) {
                    String sentContent = edtMessage.getText().toString().trim();
                    edtMessage.setText("");

                    // Tối ưu: Thêm trực tiếp tin nhắn của mình vào list mà không cần load lại API
                    Message myMsg = new Message();
                    myMsg.setSenderId(myUserId); // Tin nhắn của tôi
                    myMsg.setMessageText(sentContent);

                    messageList.add(myMsg);
                    adapter.notifyItemInserted(messageList.size() - 1);
                    rvChat.scrollToPosition(messageList.size() - 1);
                }
            }

            @Override
            public void onFailure(Call<DefaultResponse> call, Throwable t) {
                Log.e("SEND_MSG_ERR", t.getMessage());
            }
        });
    }
    private void loadAvatarToToolbar(String avatarData) {
        if (avatarData != null && !avatarData.isEmpty()) {
            if (avatarData.startsWith("http") || avatarData.startsWith("uploads/")) {
                String fullUrl = avatarData.startsWith("http") ? avatarData : RetrofitClient.BASE_URL + avatarData;
                Glide.with(this)
                        .load(fullUrl)
                        .placeholder(R.drawable.ic_avatar_placeholder)
                        .circleCrop()
                        .into(ivFriendAvatarToolbar);
            } else {
                // Xử lý nếu là Base64
                try {
                    String base64String = avatarData.contains(",") ? avatarData.split(",")[1] : avatarData;
                    byte[] imageBytes = android.util.Base64.decode(base64String, android.util.Base64.DEFAULT);
                    Glide.with(this)
                            .asBitmap()
                            .load(imageBytes)
                            .placeholder(R.drawable.ic_avatar_placeholder)
                            .circleCrop()
                            .into(ivFriendAvatarToolbar);
                } catch (Exception e) {
                    ivFriendAvatarToolbar.setImageResource(R.drawable.ic_avatar_placeholder);
                }
            }
        }
    }
    private void setupSocket() {
        try {
            mSocket = io.socket.client.IO.socket(RetrofitClient.BASE_URL);
            mSocket.connect();

            // Đăng ký mình đang online
            mSocket.emit("register", myUserId);

            mSocket.on("receive_message", args -> {
                runOnUiThread(() -> {
                    try {
                        org.json.JSONObject data = (org.json.JSONObject) args[0];
                        int incomingSenderId = data.getInt("senderId");
                        String text = data.getString("messageText");

                        // Nếu tin nhắn từ đúng người đang chat
                        if (incomingSenderId == friendId) {
                            Message m = new Message();
                            m.setSenderId(incomingSenderId);
                            m.setMessageText(text);
                            messageList.add(m);
                            adapter.notifyItemInserted(messageList.size() - 1);
                            rvChat.scrollToPosition(messageList.size() - 1);
                        }
                    } catch (Exception e) { e.printStackTrace(); }
                });
            });
        } catch (Exception e) { e.printStackTrace(); }
    }
    private String getAuthToken() {
        SharedPreferences sharedPref = getSharedPreferences("BocketPrefs", Context.MODE_PRIVATE);
        return "Bearer " + sharedPref.getString("jwt_token", "");
    }
}