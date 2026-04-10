package com.example.bocket.model;


import com.google.gson.annotations.SerializedName;


public class Post {
    @SerializedName("PostID")
    private int PostID;


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
    @SerializedName("UserID")
    private int userID;


    // Các hàm Getter tương ứng
    public int getPostID() {
        return PostID;
    }

    public void setPostID(int postID) {
        this.PostID = postID;
    }
    public int getUserID() {
        return userID;
    }

    public void setUserID(int userID) {
        this.userID = userID;
    }
    public String getDisplayName() { return displayName; }
    public String getCreatedAt() { return createdAt; }
    public String getAvatarURL() { return avatarURL; }
    public String getImageURL() { return imageURL; }
    public String getContent() { return content; }
}
