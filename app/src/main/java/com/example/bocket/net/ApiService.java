package com.example.bocket.net;

import com.example.bocket.model.LoginRequest;
import com.example.bocket.model.LoginResponse;
import com.example.bocket.model.User;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;

public interface ApiService {
    @POST("api/auth/register")
    Call<ResponseBody> register(@Body User user);
    @POST("api/auth/login")
    Call<LoginResponse> login(@Body LoginRequest loginRequest);
    @POST("api/auth/sendOTP")
    Call<ResponseBody> sendOTP(@Body User user);
    @POST("api/auth/verifyOTP")
    Call<ResponseBody> verifyOTP(@Body User user);
    @Multipart
    @POST("api/posts/upload")
    Call<ResponseBody> uploadPost(
            @Header("Authorization") String token, // Gửi token để verifyToken ở server
            @Part MultipartBody.Part image,        // File ảnh
            @Part("content") RequestBody caption   // Nội dung mô tả (nếu có)
    );
    @GET("api/posts") // Thay đổi đường dẫn theo đúng API của Server bạn
    Call<PostResponse> getAllPosts(@Header("Authorization") String token);
    @GET("api/user/profile") // Thay đổi endpoint cho đúng với Node.js của bạn
    Call<User> getUserProfile(@Header("Authorization") String token);

    @PUT("api/user/update-profile")
    Call<ResponseBody> updateProfile(@Header("Authorization") String token, @Body User user);
}
