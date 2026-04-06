package com.example.bocket.model;


import com.google.gson.annotations.SerializedName;


public class Post {
    @SerializedName("PostID")
    private int postId;


    @SerializedName("DisplayName") // Viết hoa giống SQL
    private String displayName;


    @SerializedName("AvatarURL")   // i thêm cái này vào SQL và Java
    private String avatarURL;


    @SerializedName("ImageURL")
    private String imageURL;


    @SerializedName("Content")
    private String content;


    @SerializedName("CreatedAt")
    private String createdAt;


    // Các hàm Getter tương ứng
    public String getDisplayName() { return displayName; }
    public String getCreatedAt() { return createdAt; }
    public String getAvatarURL() { return avatarURL; }
    public String getImageURL() { return imageURL; }
    public String getContent() { return content; }
}
