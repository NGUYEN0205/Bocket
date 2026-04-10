package com.example.bocket.model;

import com.google.gson.annotations.SerializedName;

public class Message {
    @SerializedName("MessageID")
    private int messageId;

    @SerializedName("SenderID")
    private int senderId;

    @SerializedName("ReceiverID")
    private int receiverId;

    @SerializedName("MessageText")
    private String messageText;

    @SerializedName("SentAt")
    private String sentAt;

    @SerializedName("PostID")
    private int postId;

    // Trường này lấy từ bảng Posts thông qua LEFT JOIN ở Backend
    @SerializedName("PostImageURL")
    private String postImageURL;
    @SerializedName("PostContent") // Tên chính xác từ JSON mà API trả về
    private String postTitle;

    // Constructor không tham số (cho Retrofit/Gson)
    public Message() {}

    // Getter và Setter
    public int getMessageId() { return messageId; }
    public void setMessageId(int messageId) { this.messageId = messageId; }

    public int getSenderId() { return senderId; }
    public void setSenderId(int senderId) { this.senderId = senderId; }

    public int getReceiverId() { return receiverId; }
    public void setReceiverId(int receiverId) { this.receiverId = receiverId; }

    public String getMessageText() { return messageText; }
    public void setMessageText(String messageText) { this.messageText = messageText; }

    public String getSentAt() { return sentAt; }
    public void setSentAt(String sentAt) { this.sentAt = sentAt; }

    public int getPostId() { return postId; }
    public void setPostId(int postId) { this.postId = postId; }

    public String getPostImageURL() { return postImageURL; }
    public void setPostImageURL(String postImageURL) { this.postImageURL = postImageURL; }
    public String getPostTitle() {
        return postTitle;
    }

    public void setPostTitle(String postTitle) {
        this.postTitle = postTitle;
    }
}