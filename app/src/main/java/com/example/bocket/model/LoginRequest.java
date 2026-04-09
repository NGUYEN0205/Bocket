package com.example.bocket.model;

public class LoginRequest {
    private String username;
    private String password;
    private String email;

    public LoginRequest(String username, String password, String email) {
        this.username = username;
        this.password = password;
        this.email = email;
    }
}
