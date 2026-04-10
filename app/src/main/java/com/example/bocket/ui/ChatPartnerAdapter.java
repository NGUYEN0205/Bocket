package com.example.bocket.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.bocket.R;
import com.example.bocket.model.ChatPartner;
import com.example.bocket.net.RetrofitClient;
import com.google.android.material.imageview.ShapeableImageView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class ChatPartnerAdapter extends RecyclerView.Adapter<ChatPartnerAdapter.PartnerViewHolder> {

    private Context context;
    private List<ChatPartner> partnerList;


    public ChatPartnerAdapter(Context context, List<ChatPartner> partnerList) {
        this.context = context;
        this.partnerList = partnerList;
    }

    @NonNull
    @Override
    public PartnerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Sử dụng file XML layout mà chúng ta đã thiết kế ở bước trước
        View view = LayoutInflater.from(context).inflate(R.layout.item_chat_partner, parent, false);
        return new PartnerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PartnerViewHolder holder, int position) {
        ChatPartner partner = partnerList.get(position);

        // Sử dụng các hàm getter mới
        holder.tvPartnerName.setText(partner.getDisplayName());

        String msg = partner.getLastMessage();
        holder.tvLastMessage.setText(msg != null ? msg : "Bắt đầu trò chuyện...");

        holder.tvTime.setText(formatShortTime(partner.getSentAt()));

        // Trong ChatPartnerAdapter.java
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ChatActivity.class);
            intent.putExtra("FRIEND_ID", partner.getUserID());
            intent.putExtra("FRIEND_NAME", partner.getDisplayName());
            intent.putExtra("FRIEND_AVATAR", partner.getAvatarURL());
            context.startActivity(intent);
        });

        // Logic hiển thị chưa đọc
        if (partner.isUnread()) {
            holder.tvLastMessage.setTextColor(Color.WHITE);
            holder.tvLastMessage.setTypeface(null, Typeface.BOLD);
            holder.ivUnreadDot.setVisibility(View.VISIBLE);
        } else {
            holder.tvLastMessage.setTextColor(Color.parseColor("#A8A8A8"));
            holder.tvLastMessage.setTypeface(null, Typeface.NORMAL);
            holder.ivUnreadDot.setVisibility(View.GONE);
        }

        // Load Avatar
        String avatarData = partner.getAvatarURL();

        if (avatarData != null && !avatarData.isEmpty()) {
            // TRƯỜNG HỢP 1: Nếu là đường dẫn URL (http hoặc link nội bộ server)
            if (avatarData.startsWith("http") || avatarData.startsWith("uploads/")) {
                String fullUrl = avatarData.startsWith("http") ? avatarData : RetrofitClient.BASE_URL + avatarData;

                Glide.with(context)
                        .load(fullUrl)
                        .placeholder(R.drawable.ic_avatar_placeholder)
                        .error(R.drawable.ic_avatar_placeholder)
                        .circleCrop()
                        .into(holder.ivPartnerAvatar);
            }
            // TRƯỜNG HỢP 2: Nếu là chuỗi Base64
            else {
                try {
                    String base64String = avatarData;
                    // Loại bỏ header nếu có (data:image/jpeg;base64,...)
                    if (base64String.contains(",")) {
                        base64String = base64String.split(",")[1];
                    }

                    byte[] imageBytes = android.util.Base64.decode(base64String, android.util.Base64.DEFAULT);

                    Glide.with(context)
                            .asBitmap()
                            .load(imageBytes)
                            .placeholder(R.drawable.ic_avatar_placeholder)
                            .error(R.drawable.ic_avatar_placeholder)
                            .circleCrop()
                            .into(holder.ivPartnerAvatar);
                } catch (Exception e) {
                    Log.e("BOCKET_DEBUG", "Lỗi decode Base64: " + e.getMessage());
                    holder.ivPartnerAvatar.setImageResource(R.drawable.ic_avatar_placeholder);
                }
            }
        } else {
            // Trường hợp không có dữ liệu avatar
            holder.ivPartnerAvatar.setImageResource(R.drawable.ic_avatar_placeholder);
        }
    }

    @Override
    public int getItemCount() {
        return partnerList.size();
    }

    // Hàm format thời gian ngắn gọn (2g, 5ph, 1ng)
    private String formatShortTime(String dateStr) {
        if (dateStr == null) return "";
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            long time = sdf.parse(dateStr).getTime();
            long now = System.currentTimeMillis();
            long diff = now - time;

            long minutes = diff / (60 * 1000);
            long hours = minutes / 60;
            long days = hours / 24;

            if (minutes < 1) return "vừa xong";
            if (minutes < 60) return minutes + "ph";
            if (hours < 24) return hours + "g";
            return days + "ng";
        } catch (Exception e) {
            return "";
        }
    }

    static class PartnerViewHolder extends RecyclerView.ViewHolder {
        ShapeableImageView ivPartnerAvatar;
        TextView tvPartnerName, tvLastMessage, tvTime;
        View ivUnreadDot;

        public PartnerViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPartnerAvatar = itemView.findViewById(R.id.ivPartnerAvatar);
            tvPartnerName = itemView.findViewById(R.id.tvDisplayName);
            tvLastMessage = itemView.findViewById(R.id.tvLastMessage);
            tvTime = itemView.findViewById(R.id.tvSentAt);
            ivUnreadDot = itemView.findViewById(R.id.ivUnreadDot);
        }
    }
}