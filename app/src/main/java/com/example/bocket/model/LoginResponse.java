package com.example.bocket.model;

public class LoginResponse {
    private String token;
    private User user; // Chứa thông tin user bao gồm UserID

    public String getToken() { return token; }
    public User getUser() { return user; }
}