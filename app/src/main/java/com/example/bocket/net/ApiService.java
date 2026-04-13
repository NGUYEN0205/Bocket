package com.example.bocket.net;

import com.example.bocket.model.ChatPartner;
import com.example.bocket.model.CommentRequest;
import com.example.bocket.model.DefaultResponse;
import com.example.bocket.model.LoginRequest;
import com.example.bocket.model.LoginResponse;
import com.example.bocket.model.Message;
import com.example.bocket.model.Post;
import com.example.bocket.model.User;

import java.util.List;
import java.util.Map;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Path;

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

    // Thêm dòng này vào ApiService.java

    @GET("api/auth/profile")
    Call<User> getUserProfile(@Header("Authorization") String token);

    @PUT("api/user/update-profile")
    Call<ResponseBody> updateProfile(@Header("Authorization") String token, @Body User user);

    // Sửa "friends/{id}" thành "api/friends/{id}"
    @GET("api/friends/{id}")
    Call<User> getUserById(@Header("Authorization") String token, @Path("id") int userId);

    // Sửa "friends/request" thành "api/friends/request"
    @POST("api/friends/request")
    Call<Void> sendFriendRequest(@Header("Authorization") String token, @Body Map<String, Integer> body);

    @POST("api/friends/accept")
    Call<Void> acceptFriend(@Header("Authorization") String token, @Body Map<String, Integer> body);

    // Thêm vào trong interface ApiService
    @POST("api/friends/cancel")
    Call<Void> cancelFriendRequest(
            @Header("Authorization") String token,
            @Body Map<String, Integer> body
    );
    @GET("api/friends/pending")
    Call<List<User>> getPendingRequests(@Header("Authorization") String token);
    @GET("api/friends/sent")
    Call<List<User>> getSentRequests(@Header("Authorization") String token);
    @GET("api/friends/my-friends")
    Call<List<User>> getFriendsList(@Header("Authorization") String token);
    @GET("api/posts/user/{userId}")
    Call<PostResponse> getPostsByUser(@Header("Authorization") String token, @Path("userId") int userId);
    @GET("api/posts/friends-feed")
    Call<PostResponse> getFriendPosts(@Header("Authorization") String token);

    @POST("api/auth/reset-password") // Đường dẫn khớp với route ở Server
    Call<ResponseBody> resetPassword(@Body User user); // Dùng model User có chứa email và password

    @POST("api/auth/send-otp-update")
    Call<ResponseBody> sendOtpUpdate(@Body User user);

    @POST("api/messages/comment-to-chat") // Đảm bảo đường dẫn này khớp với NodeJS của bạn
    Call<DefaultResponse> commentToChat(@Header("Authorization") String token, @Body CommentRequest request);

    @GET("api/messages/messages/{friendId}")
    Call<List<Message>> getChatMessages(
            @Header("Authorization") String token,
            @Path("friendId") int friendId
    );
    @GET("api/messages/partners")
    Call<List<ChatPartner>> getChatPartners(@Header("Authorization") String token);
    @POST("api/messages/send") // Đường dẫn phải khớp với Router trong NodeJS của bạn
    Call<DefaultResponse> sendMessage(
            @Header("Authorization") String token,
            @Body Map<String, Object> body // Chứa receiverId và content
    );
    @DELETE("api/posts/{postId}") Call<Void> deletePost(
            @Header("Authorization") String token,
            @Path("id") int postId);
}
