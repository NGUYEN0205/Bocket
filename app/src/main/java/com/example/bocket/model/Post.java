package com.example.bocket.model;

public class Post {
    private int PostID;
    private String DisplayName;
    private String AvatarURL;
    private String ImageURL;
    private String Content;
    private String CreatedAt;

    // Constructor, Getter và Setter
    public String getDisplayName() { return DisplayName; }
    public String getAvatarURL() { return AvatarURL; }
    public String getImageURL() { return ImageURL; }
    public String getContent() { return Content; }
    public String getCreatedAt() { return CreatedAt; }
}
