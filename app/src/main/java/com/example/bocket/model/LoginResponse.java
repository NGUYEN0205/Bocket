package com.example.bocket.model;

public class LoginResponse {
    private String message;
    private String token;
    private User user;

    public String getToken() { return token; }
    public String getMessage() { return message; }
}
