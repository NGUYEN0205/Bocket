package com.example.bocket.ui;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.bocket.R;
import com.example.bocket.model.User;
import java.util.List;
import de.hdodenhof.circleimageview.CircleImageView;

public class FriendRequestAdapter extends RecyclerView.Adapter<FriendRequestAdapter.ViewHolder> {
    private List<User> list;
    private boolean isReceivedType; // true: Lời mời nhận được, false: Lời mời đã gửi
    private OnFriendActionListener listener;

    // Đổi tên interface để dùng chung cho cả Accept và Cancel
    public interface OnFriendActionListener {
        void onActionClick(int userId);
    }

    public FriendRequestAdapter(List<User> list, boolean isReceivedType, OnFriendActionListener listener) {
        this.list = list;
        this.isReceivedType = isReceivedType;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_friend_request, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User user = list.get(position);

        String display = (user.getDisplay_name() != null && !user.getDisplay_name().isEmpty())
                ? user.getDisplay_name() : user.getUsername();
        holder.tvDisplayName.setText(display);
        holder.tvUsername.setText("@" + user.getUsername());

        // --- XỬ LÝ ẢNH AVATAR ---
        String avatarData = user.getAvatar();
        if (avatarData != null && !avatarData.isEmpty()) {
            try {
                if (avatarData.contains(",")) {
                    avatarData = avatarData.split(",")[1];
                }
                byte[] imageBytes = Base64.decode(avatarData, Base64.NO_WRAP);
                Glide.with(holder.itemView.getContext())
                        .asBitmap()
                        .load(imageBytes)
                        .placeholder(R.drawable.ic_avatar_placeholder)
                        .circleCrop()
                        .into(holder.ivAvatar);
            } catch (Exception e) {
                Glide.with(holder.itemView.getContext())
                        .load(user.getAvatar())
                        .placeholder(R.drawable.ic_avatar_placeholder)
                        .circleCrop()
                        .into(holder.ivAvatar);
            }
        } else {
            holder.ivAvatar.setImageResource(R.drawable.ic_avatar_placeholder);
        }

        // --- CẤU HÌNH NÚT BẤM ---
        if (isReceivedType) {
            // Trường hợp: Lời mời người khác gửi cho mình
            holder.btnAction.setText("Đồng ý");
            holder.btnAction.setEnabled(true);
            // Màu vàng cho nút Đồng ý
            holder.btnAction.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FFD700")));
        } else {
            // Trường hợp: Lời mời mình gửi đi (đang chờ họ duyệt)
            holder.btnAction.setText("Hủy"); // Thay "Đang chờ" thành "Hủy"
            holder.btnAction.setEnabled(true); // Bật nút để có thể bấm Hủy
            // Màu đỏ nhạt hoặc đỏ đậm tùy bạn chọn
            holder.btnAction.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FF4444")));
        }

        // Sự kiện click dùng chung
        holder.btnAction.setOnClickListener(v -> {
            if (listener != null) {
                listener.onActionClick(user.getUserID());
            }
        });
    }

    @Override
    public int getItemCount() { return list != null ? list.size() : 0; }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        CircleImageView ivAvatar;
        TextView tvDisplayName, tvUsername;
        Button btnAction;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.ivUserAvatar);
            tvDisplayName = itemView.findViewById(R.id.tvDisplayName);
            tvUsername = itemView.findViewById(R.id.tvUsername);
            btnAction = itemView.findViewById(R.id.btnAction);
        }
    }
}