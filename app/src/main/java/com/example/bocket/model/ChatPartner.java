package com.example.bocket.model;

import com.google.gson.annotations.SerializedName;

public class ChatPartner {
    @SerializedName("UserID")
    private int userID;

    @SerializedName("DisplayName")
    private String displayName;

    @SerializedName("AvatarURL")
    private String avatarURL;

    @SerializedName("LastMessage")
    private String lastMessage;

    @SerializedName("SentAt")
    private String sentAt;

    @SerializedName("IsUnread")
    private int isUnread; // Nhận 0 hoặc 1 từ server

    // Getter cho Adapter
    public int getUserID() { return userID; }
    public String getDisplayName() { return displayName; }
    public String getAvatarURL() { return avatarURL; }
    public String getLastMessage() { return lastMessage; }
    public String getSentAt() { return sentAt; }

    // Hàm này giúp Adapter kiểm tra nhanh
    public boolean isUnread() {
        return isUnread == 1;
    }
}