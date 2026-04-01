package com.example.bocket.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.bocket.MainActivity;
import com.example.bocket.R;
import com.example.bocket.model.Post;

import java.util.List;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {
    private List<Post> postList;
    private Context context;

    public PostAdapter(Context context, List<Post> postList) {
        this.context = context;
        this.postList = postList;
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

        // 1. Hiển thị nội dung chữ (Content) ở giữa ảnh
        holder.tvPostContent.setText(post.getContent());
        if (post.getContent() == null || post.getContent().isEmpty()) {
            holder.tvPostContent.setVisibility(View.GONE);
        }

        // 2. Hiển thị thông tin người đăng (Avatar + Tên + Thời gian)
        holder.tvPosterNameAndTime.setText(post.getDisplayName() + " • " + formatTime(post.getCreatedAt()));

        // 3. Load ảnh bài đăng bằng Glide
        Glide.with(context)
                .load(post.getImageURL())
                .placeholder(R.color.gray_dark) // Màu nền trong lúc chờ
                .into(holder.ivPostImage);

        // 4. Load Avatar người đăng
        Glide.with(context)
                .load(post.getAvatarURL())
                .circleCrop()
                .placeholder(R.drawable.ic_avatar_placeholder)
                .into(holder.ivPosterAvatar);

        // 5. Xử lý nút quay lại (ibCaptureBack) trong từng item nếu cần
        holder.ibCaptureBack.setOnClickListener(v -> {
            if (context instanceof MainActivity) {
                ((MainActivity) context).hideFeed(); // Gọi hàm ẩn feed ở MainActivity
            }
        });
    }

    @Override
    public int getItemCount() { return postList.size(); }

    // Hàm phụ trợ để định dạng thời gian (Ví dụ: 2026-04-01 -> 5 phút trước)
    private String formatTime(String dateStr) {
        // Bạn có thể dùng thư viện PrettyTime hoặc tự viết logic tính khoảng cách giờ
        return "vừa xong";
    }

    static class PostViewHolder extends RecyclerView.ViewHolder {
        ImageView ivPostImage, ivPosterAvatar;
        TextView tvPostContent, tvPosterNameAndTime;
        ImageButton ibCaptureBack;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPostImage = itemView.findViewById(R.id.ivPostImage);
            ivPosterAvatar = itemView.findViewById(R.id.ivPosterAvatar);
            tvPostContent = itemView.findViewById(R.id.tvPostContent);
            tvPosterNameAndTime = itemView.findViewById(R.id.tvPosterNameAndTime);
            ibCaptureBack = itemView.findViewById(R.id.ibCaptureBack);
        }
    }
}
