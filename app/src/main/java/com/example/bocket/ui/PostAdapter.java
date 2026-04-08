package com.example.bocket.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.bocket.R;
import com.example.bocket.model.Post;
// --- DÒNG QUAN TRỌNG NHẤT ---
// Bạn phải import đúng đường dẫn đến file RetrofitClient đã tạo
import com.example.bocket.net.RetrofitClient;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

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
        String avatarData = post.getAvatarURL();

        // 1. Hiển thị tên
        holder.tvPosterNameAndTime.setText(post.getDisplayName() != null ? post.getDisplayName() : "Người dùng Bocket");

        // 2. Nội dung caption
        holder.tvPostContent.setText(post.getContent());

        // 3. Thời gian
        holder.tvTimeAgo.setText(" • " + formatTime(post.getCreatedAt()));

        // 4. Load ảnh bài đăng (Lưu ý: Nếu link ảnh cũng là đường dẫn tương đối, hãy thêm RetrofitClient.BASE_URL vào đây)
        String postImageUrl = post.getImageURL();
        if (postImageUrl != null && !postImageUrl.startsWith("http")) {
            postImageUrl = RetrofitClient.BASE_URL + postImageUrl;
        }
        Glide.with(context).load(postImageUrl).into(holder.ivPostImage);

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
        ImageView ivPostImage, ivPosterAvatar;
        TextView tvPostContent, tvPosterNameAndTime, tvTimeAgo;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPostImage = itemView.findViewById(R.id.ivPostImage);
            ivPosterAvatar = itemView.findViewById(R.id.ivPosterAvatar);
            tvPostContent = itemView.findViewById(R.id.tvPostContent);
            tvPosterNameAndTime = itemView.findViewById(R.id.tvPosterNameAndTime);
            tvTimeAgo = itemView.findViewById(R.id.tvTimeAgo);
        }
    }
}