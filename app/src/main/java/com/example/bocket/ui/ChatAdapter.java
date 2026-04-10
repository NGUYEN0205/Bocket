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
import com.example.bocket.model.Message;
import com.example.bocket.net.RetrofitClient;

import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_LEFT_NORMAL = 1;
    private static final int TYPE_RIGHT_NORMAL = 2;
    private static final int TYPE_LEFT_POST = 3;
    private static final int TYPE_RIGHT_POST = 4;

    private Context context;
    private List<Message> messageList;
    private int myUserId;
    private String friendAvatarUrl;

    public ChatAdapter(Context context, List<Message> messageList, int myUserId, String friendAvatarUrl) {
        this.context = context;
        this.messageList = messageList;
        this.myUserId = myUserId;
        this.friendAvatarUrl = friendAvatarUrl;
    }

    @Override
    public int getItemViewType(int position) {
        Message msg = messageList.get(position);
        boolean isMe = (msg.getSenderId() == myUserId);
        // Kiểm tra postId khác 0 hoặc khác null tùy theo kiểu dữ liệu trong Model
        boolean isPostReply = (msg.getPostId() != 0);

        if (isMe) {
            return isPostReply ? TYPE_RIGHT_POST : TYPE_RIGHT_NORMAL;
        } else {
            return isPostReply ? TYPE_LEFT_POST : TYPE_LEFT_NORMAL;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        switch (viewType) {
            case TYPE_RIGHT_NORMAL:
                return new RightNormalViewHolder(inflater.inflate(R.layout.item_chat_right_normal, parent, false));
            case TYPE_LEFT_NORMAL:
                return new LeftNormalViewHolder(inflater.inflate(R.layout.item_chat_left_normal, parent, false));
            case TYPE_RIGHT_POST:
                return new RightPostViewHolder(inflater.inflate(R.layout.item_chat_right_post, parent, false));
            case TYPE_LEFT_POST:
            default:
                return new LeftPostViewHolder(inflater.inflate(R.layout.item_chat_left_post, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Message msg = messageList.get(position);

        if (holder instanceof RightNormalViewHolder) {
            ((RightNormalViewHolder) holder).tvMessageRight.setText(msg.getMessageText());
        }
        else if (holder instanceof LeftNormalViewHolder) {
            LeftNormalViewHolder vh = (LeftNormalViewHolder) holder;
            vh.tvMessageLeft.setText(msg.getMessageText());
            loadAvatar(vh.ivAvatarLeft);
        }
        else if (holder instanceof RightPostViewHolder) {
            RightPostViewHolder vh = (RightPostViewHolder) holder;
            vh.tvMessagePostRight.setText(msg.getMessageText());
            vh.tvPostTitleRight.setText(msg.getPostTitle()); // Hiển thị tiêu đề bài đăng
            loadPostImage(msg.getPostImageURL(), vh.ivPostImageRight);
        }
        else if (holder instanceof LeftPostViewHolder) {
            LeftPostViewHolder vh = (LeftPostViewHolder) holder;
            vh.tvMessagePostLeft.setText(msg.getMessageText());
            vh.tvPostTitleLeft.setText(msg.getPostTitle()); // Hiển thị tiêu đề bài đăng
            loadAvatar(vh.ivAvatarLeft);
            loadPostImage(msg.getPostImageURL(), vh.ivPostImageLeft);
        }
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    // --- HELPER METHODS ---

    private void loadAvatar(ImageView imageView) {
        if (friendAvatarUrl == null || friendAvatarUrl.isEmpty()) {
            imageView.setImageResource(R.drawable.ic_avatar_placeholder);
            return;
        }
        String url = friendAvatarUrl.startsWith("http") ? friendAvatarUrl : RetrofitClient.BASE_URL + friendAvatarUrl;
        Glide.with(context).load(url).circleCrop().placeholder(R.drawable.ic_avatar_placeholder).into(imageView);
    }

    private void loadPostImage(String imageUrl, ImageView imageView) {
        if (imageUrl != null && !imageUrl.isEmpty()) {
            String url = imageUrl.startsWith("http") ? imageUrl : RetrofitClient.BASE_URL + imageUrl;
            Glide.with(context).load(url).centerCrop().into(imageView);
        }
    }

    // --- VIEWHOLDERS ---

    static class RightNormalViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessageRight;
        public RightNormalViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessageRight = itemView.findViewById(R.id.tvMessageRight);
        }
    }

    static class LeftNormalViewHolder extends RecyclerView.ViewHolder {
        ImageView ivAvatarLeft;
        TextView tvMessageLeft;
        public LeftNormalViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatarLeft = itemView.findViewById(R.id.ivAvatarLeft);
            tvMessageLeft = itemView.findViewById(R.id.tvMessageLeft);
        }
    }

    static class LeftPostViewHolder extends RecyclerView.ViewHolder {
        ImageView ivAvatarLeft, ivPostImageLeft;
        TextView tvMessagePostLeft, tvPostTitleLeft;
        public LeftPostViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatarLeft = itemView.findViewById(R.id.ivAvatarLeft);
            ivPostImageLeft = itemView.findViewById(R.id.ivPostImageLeft);
            tvMessagePostLeft = itemView.findViewById(R.id.tvMessagePostLeft);
            tvPostTitleLeft = itemView.findViewById(R.id.tvPostTitleLeft);
        }
    }

    static class RightPostViewHolder extends RecyclerView.ViewHolder {
        ImageView ivPostImageRight;
        TextView tvMessagePostRight, tvPostTitleRight;
        public RightPostViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPostImageRight = itemView.findViewById(R.id.ivPostImageRight);
            tvMessagePostRight = itemView.findViewById(R.id.tvMessagePostRight);
            tvPostTitleRight = itemView.findViewById(R.id.tvPostTitleRight);
        }
    }
}