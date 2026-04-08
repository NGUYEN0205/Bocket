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
import com.example.bocket.model.User;

import java.util.List;

public class FriendsAdapter extends RecyclerView.Adapter<FriendsAdapter.FriendViewHolder> {
    private Context context;
    private List<User> friendsList;
    private OnFriendClickListener listener;

    public FriendsAdapter(Context context, List<User> friendsList) {
        this.context = context;
        this.friendsList = friendsList;
    }

    public interface OnFriendClickListener {
        void onArrowClick(User friend);
    }

    public void setOnFriendClickListener(OnFriendClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public FriendViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Bạn có thể tạo một file layout riêng: item_friend.xml
        View view = LayoutInflater.from(context).inflate(R.layout.item_friend, parent, false);
        return new FriendViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FriendViewHolder holder, int position) {
        User friend = friendsList.get(position);
        holder.tvName.setText(friend.getDisplay_name());

        // Đảm bảo dùng đúng getter từ Model của bạn
        holder.tvName.setText(friend.getDisplay_name());

        holder.itemView.findViewById(R.id.ivGoToPosts).setOnClickListener(v -> {
            if (listener != null) listener.onArrowClick(friend);
        });

        String avatar = friend.getAvatar();
        if (avatar != null && !avatar.isEmpty()) {
            byte[] imageBytes = android.util.Base64.decode(avatar, android.util.Base64.DEFAULT);
            Glide.with(context)
                    .asBitmap()
                    .load(imageBytes)
                    .placeholder(R.drawable.ic_avatar_placeholder)
                    .circleCrop()
                    .into(holder.ivAvatar);
        } else {
            holder.ivAvatar.setImageResource(R.drawable.ic_avatar_placeholder);
        }
    }

    @Override
    public int getItemCount() {
        return friendsList.size();
    }
    // Trong FriendsAdapter.java
    public void updateData(List<User> newList) {
        this.friendsList = newList;
        notifyDataSetChanged();
    }

    class FriendViewHolder extends RecyclerView.ViewHolder {
        ImageView ivAvatar;
        TextView tvName;

        public FriendViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.ivFriendAvatar);
            tvName = itemView.findViewById(R.id.tvFriendName);
        }
    }
}
