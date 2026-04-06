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


        // 1. Hiển thị tên người đăng bài này
        if (post.getDisplayName() != null) {
            holder.tvPosterNameAndTime.setText(post.getDisplayName());
        } else {
            holder.tvPosterNameAndTime.setText("Người dùng Bocket");
        }


        // 2. Hiển thị nội dung caption (TextView giữa ảnh)
        holder.tvPostContent.setText(post.getContent());


        // 3. Hiển thị thời gian
        holder.tvTimeAgo.setText(" • " + formatTime(post.getCreatedAt()));


        // 4. Load ảnh bài đăng
        Glide.with(context)
                .load(post.getImageURL())
                .into(holder.ivPostImage);


        // 5. Load Avatar của CHỦ BÀI ĐĂNG (Avatar thay đổi theo từng trang)
        Glide.with(context)
                .load(post.getAvatarURL())
                .circleCrop()
                .placeholder(R.drawable.ic_avatar_placeholder)
                .into(holder.ivPosterAvatar);
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
