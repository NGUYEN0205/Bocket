package com.example.bocket.model;

import com.google.gson.annotations.SerializedName;

public class User {
    private String username;
    private String password;
    private String display_name;
    @SerializedName("email")
    private String email;
    @SerializedName("otp")
    private String otp;
    private String avatar;

    public User(){}
    public User(String username, String password, String display_name, String email, String avatar) {
        this.username = username;
        this.password = password;
        this.display_name = display_name;
        this.email = email;
        this.avatar = avatar;
    }
    public void setEmail(String email) {
        this.email = email;
    }

    public String getEmail() {
        return email;
    }

    // Thêm getter/setter cho OTP để dùng ở OtpActivity
    public void setOtp(String otp) {
        this.otp = otp;
    }

    public String getOtp() {
        return otp;
    }

    public void setDisplay_name(String display_name) {
        this.display_name = display_name;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }
}
