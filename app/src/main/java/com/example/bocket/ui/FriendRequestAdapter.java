package com.example.bocket.ui;

import android.graphics.Color;
import android.util.Base64;
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

        // Hiển thị tên (Fallback về username nếu DisplayName trống)
        String display = (user.getDisplay_name() != null && !user.getDisplay_name().isEmpty())
                ? user.getDisplay_name() : user.getUsername();
        holder.tvDisplayName.setText(display);
        holder.tvUsername.setText("@" + user.getUsername());

        // Xử lý Avatar Base64
        if (user.getAvatar() != null) {
            byte[] imageBytes = Base64.decode(user.getAvatar(), Base64.DEFAULT);
            Glide.with(holder.itemView.getContext()).load(imageBytes).circleCrop().into(holder.ivAvatar);
        }

        // Cấu hình nút bấm
        if (isReceivedType) {
            holder.btnAction.setText("Đồng ý");
            holder.btnAction.setEnabled(true);
            holder.btnAction.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#FFD700")));
            holder.btnAction.setOnClickListener(v -> listener.onAcceptClick(user.getUserID()));
        } else {
            holder.btnAction.setText("Đang chờ");
            holder.btnAction.setEnabled(false);
            holder.btnAction.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.GRAY));
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