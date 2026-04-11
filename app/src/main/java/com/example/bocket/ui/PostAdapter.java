package com.example.bocket.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.bocket.R;
import com.example.bocket.model.CommentRequest;
import com.example.bocket.model.DefaultResponse;
import com.example.bocket.model.Post;
// --- DÒNG QUAN TRỌNG NHẤT ---
// Bạn phải import đúng đường dẫn đến file RetrofitClient đã tạo
import com.example.bocket.net.RetrofitClient;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {
    private List<Post> postList;
    private Context context;
    private int myUserId;


    public PostAdapter(Context context, List<Post> postList,int myUserId) {
        this.context = context;
        this.postList = postList;
        this.myUserId =myUserId;
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_post, parent, false);
        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        Post post = postList.get(position);
        String avatarData = post.getAvatarURL();

        // 1. Hiển thị tên
        holder.tvPosterNameAndTime.setText(post.getDisplayName() != null ? post.getDisplayName() : "Người dùng Bocket");

        // 2. Nội dung caption
        holder.tvPostContent.setText(post.getContent());

        // 3. Thời gian
        holder.tvTimeAgo.setText(" • " + formatTime(post.getCreatedAt()));

        // 4. Load ảnh bài đăng (Lưu ý: Nếu link ảnh cũng là đường dẫn tương đối, hãy thêm RetrofitClient.BASE_URL vào đây)
        String postImageUrl = post.getImageURL();
        if (postImageUrl != null) {
            if (!postImageUrl.startsWith("http")) {
                postImageUrl = RetrofitClient.BASE_URL + postImageUrl;
            }
        }
        Glide.with(context)
                .load(postImageUrl)
                .placeholder(R.drawable.ic_avatar_placeholder)
                .into(holder.ivPostImage);

        holder.btnSend.setOnClickListener(v -> {
            performSendMessage(holder, post);
        });
        // 2. Xử lý khi bấm Enter từ bàn phím
        holder.etComment.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN)) {
                performSendMessage(holder, post);
                return true;
            }
            return false;
        });

        // 5. Xử lý Avatar chủ bài đăng
        if (avatarData != null && !avatarData.isEmpty()) {
            if (avatarData.startsWith("http")) {
                displayAvatar(avatarData, holder.ivPosterAvatar);
            } else if (avatarData.length() > 200) {
                try {
                    if (avatarData.contains(",")) avatarData = avatarData.split(",")[1];
                    byte[] imageBytes = android.util.Base64.decode(avatarData, android.util.Base64.DEFAULT);
                    Glide.with(context).asBitmap().load(imageBytes).circleCrop()
                            .placeholder(R.drawable.ic_avatar_placeholder).into(holder.ivPosterAvatar);
                } catch (Exception e) {
                    holder.ivPosterAvatar.setImageResource(R.drawable.ic_avatar_placeholder);
                }
            } else {
                // SỬ DỤNG RetrofitClient TỪ ĐƯỜNG DẪN ĐÃ IMPORT
                String fullUrl = RetrofitClient.BASE_URL + avatarData;
                displayAvatar(fullUrl, holder.ivPosterAvatar);
            }
        } else {
            holder.ivPosterAvatar.setImageResource(R.drawable.ic_avatar_placeholder);
        }

        holder.etComment.setOnEditorActionListener((v, actionId, event) -> {
            boolean handled = false;
            // Bắt sự kiện bấm nút Send hoặc nút Enter
            if (actionId == EditorInfo.IME_ACTION_SEND ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN)) {

                String commentText = holder.etComment.getText().toString().trim();

                if (!commentText.isEmpty()) {
                    // Lấy token
                    SharedPreferences sharedPref = context.getSharedPreferences("BocketPrefs", Context.MODE_PRIVATE);
                    String token = "Bearer " + sharedPref.getString("jwt_token", "");

                    // Chuẩn bị Request
                    CommentRequest request = new CommentRequest(post.getPostID(), commentText);

                    // Gọi API
                    RetrofitClient.getApiService().commentToChat(token, request).enqueue(new Callback<DefaultResponse>() {
                        @Override
                        public void onResponse(Call<DefaultResponse> call, Response<DefaultResponse> response) {
                            if (response.isSuccessful() && response.body() != null) {
                                Toast.makeText(context, "Đã gửi tin nhắn riêng tư!", Toast.LENGTH_SHORT).show();
                                holder.etComment.setText(""); // Xóa trắng ô nhập
                                holder.etComment.clearFocus(); // Bỏ focus

                                // Ẩn bàn phím
                                InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
                                if (imm != null) {
                                    imm.hideSoftInputFromWindow(holder.etComment.getWindowToken(), 0);
                                }
                            } else {
                                Toast.makeText(context, "Lỗi: Không thể gửi tin nhắn", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<DefaultResponse> call, Throwable t) {
                            Toast.makeText(context, "Lỗi mạng: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
                handled = true;
            }
            return handled;
        });
        if (post.getUserID() == myUserId) {
            holder.llInputArea.setVisibility(View.INVISIBLE);
        } else {
            holder.llInputArea.setVisibility(View.VISIBLE);
        }
    }
    // Hàm dùng chung để gửi tin nhắn
    private void performSendMessage(PostViewHolder holder, Post post) {
        String commentText = holder.etComment.getText().toString().trim();

        if (commentText.isEmpty()) {
            return;
        }

        // Lấy token
        SharedPreferences sharedPref = context.getSharedPreferences("BocketPrefs", Context.MODE_PRIVATE);
        String token = "Bearer " + sharedPref.getString("jwt_token", "");

        // Chuẩn bị Request
        CommentRequest request = new CommentRequest(post.getPostID(), commentText);

        // Gọi API commentToChat
        RetrofitClient.getApiService().commentToChat(token, request).enqueue(new Callback<DefaultResponse>() {
            @Override
            public void onResponse(Call<DefaultResponse> call, Response<DefaultResponse> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(context, "Đã gửi tin nhắn riêng cho " + post.getDisplayName(), Toast.LENGTH_SHORT).show();

                    // Reset giao diện
                    holder.etComment.setText("");
                    holder.etComment.clearFocus();

                    // Ẩn bàn phím
                    InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.hideSoftInputFromWindow(holder.etComment.getWindowToken(), 0);
                    }
                } else {
                    Toast.makeText(context, "Lỗi gửi tin nhắn", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<DefaultResponse> call, Throwable t) {
                Toast.makeText(context, "Lỗi mạng", Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void displayAvatar(Object source, ImageView imageView) {
        Glide.with(context).load(source).circleCrop()
                .placeholder(R.drawable.ic_avatar_placeholder)
                .error(R.drawable.ic_avatar_placeholder).into(imageView);
    }

    @Override
    public int getItemCount() { return postList.size(); }

    private String formatTime(String dateStr) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            long time = sdf.parse(dateStr).getTime();
            long now = System.currentTimeMillis();
            long diff = now - time;
            long minutes = diff / (60 * 1000);
            long hours = minutes / 60;
            if (minutes < 1) return "vừa xong";
            if (minutes < 60) return minutes + " phút trước";
            if (hours < 24) return hours + " giờ trước";
            return (hours / 24) + " ngày trước";
        } catch (Exception e) { return "vừa xong"; }
    }

    static class PostViewHolder extends RecyclerView.ViewHolder {
        ImageView ivPostImage, ivPosterAvatar ,btnSend;
        TextView tvPostContent, tvPosterNameAndTime, tvTimeAgo;
        EditText etComment ;
        View llInputArea;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPostImage = itemView.findViewById(R.id.ivPostImage);
            ivPosterAvatar = itemView.findViewById(R.id.ivPosterAvatar);
            tvPostContent = itemView.findViewById(R.id.tvPostContent);
            tvPosterNameAndTime = itemView.findViewById(R.id.tvPosterNameAndTime);
            tvTimeAgo = itemView.findViewById(R.id.tvTimeAgo);
            etComment = itemView.findViewById(R.id.etComment);
            btnSend = itemView.findViewById(R.id.btnSend);
            llInputArea = itemView.findViewById(R.id.llInputArea);
        }
    }
}