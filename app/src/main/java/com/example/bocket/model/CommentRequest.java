package com.example.bocket.model;

public class CommentRequest {
    private int postId;
    private String content;

    public CommentRequest(int postId, String content) {
        this.postId = postId;
        this.content = content;
    }

    public int getPostId() { return postId; }
    public String getContent() { return content; }
}