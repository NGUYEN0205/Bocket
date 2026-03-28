package com.example.bocket.net;

import com.example.bocket.model.User;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface ApiService {
    @POST("api/auth/register")
    Call<ResponseBody> register(@Body User user);
    @POST("api/auth/login")
    Call<ResponseBody> login(@Body User user);
    @POST("api/auth/sendOTP")
    Call<ResponseBody> sendOTP(@Body User user);
    @POST("api/auth/verifyOTP")
    Call<ResponseBody> verifyOTP(@Body User user);
}
