package com.example.bocket.ui;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.util.Base64;
import android.util.Log; // Thêm Log để dễ debug
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
    private boolean isReceivedType;
    private OnFriendActionListener listener;

    public interface OnFriendActionListener {
        void onAcceptClick(int userId);
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

        // --- PHẦN SỬA LỖI CRASH BASE64 ---
        String avatarData = user.getAvatar();
        if (avatarData != null && !avatarData.isEmpty()) {
            try {
                // 1. Nếu có tiền tố "data:image/...," thì cắt bỏ
                if (avatarData.contains(",")) {
                    avatarData = avatarData.split(",")[1];
                }

                // 2. Giải mã với NO_WRAP để tránh lỗi ký tự xuống dòng
                byte[] imageBytes = Base64.decode(avatarData, Base64.NO_WRAP);

                Glide.with(holder.itemView.getContext())
                        .asBitmap()
                        .load(imageBytes)
                        .placeholder(R.drawable.ic_avatar_placeholder) // Hoặc ic_avatar_placeholder
                        .circleCrop()
                        .into(holder.ivAvatar);

            } catch (Exception e) {
                // 3. Nếu lỗi Base64 (bad base-64), không cho crash mà thử load như URL
                Log.e("BOCKET_ADAPTER", "Lỗi ảnh User ID: " + user.getUserID() + " - Thử load URL");

                Glide.with(holder.itemView.getContext())
                        .load(user.getAvatar()) // Thử load trực tiếp chuỗi đó (có thể là link web)
                        .placeholder(R.drawable.ic_avatar_placeholder)
                        .circleCrop()
                        .into(holder.ivAvatar);
            }
        } else {
            // Nếu không có dữ liệu ảnh, set ảnh mặc định
            holder.ivAvatar.setImageResource(R.drawable.ic_avatar_placeholder);
        }

        // --- Cấu hình nút bấm (Giữ nguyên logic của bạn nhưng sửa ColorStateList cho chuẩn) ---
        if (isReceivedType) {
            holder.btnAction.setText("Đồng ý");
            holder.btnAction.setEnabled(true);
            holder.btnAction.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FFD700")));
            holder.btnAction.setOnClickListener(v -> {
                if (listener != null) listener.onAcceptClick(user.getUserID());
            });
        } else {
            holder.btnAction.setText("Đang chờ");
            holder.btnAction.setEnabled(false);
            holder.btnAction.setBackgroundTintList(ColorStateList.valueOf(Color.GRAY));
            holder.btnAction.setOnClickListener(null);
        }
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