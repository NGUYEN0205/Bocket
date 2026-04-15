package com.example.bocket.ui;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.RemoteViews;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.AppWidgetTarget;
import com.example.bocket.MainActivity;
import com.example.bocket.R;
import com.example.bocket.model.Post;
import com.example.bocket.net.ApiService;
import com.example.bocket.net.PostResponse;
import com.example.bocket.net.RetrofitClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BocketWidgetProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        SharedPreferences sharedPref = context.getSharedPreferences("BocketPrefs", Context.MODE_PRIVATE);
        String token = "Bearer " + sharedPref.getString("jwt_token", "");

        ApiService apiService = RetrofitClient.getApiService();
        apiService.getFriendPosts(token).enqueue(new Callback<PostResponse>() {
            @Override
            public void onResponse(Call<PostResponse> call, Response<PostResponse> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().getData().isEmpty()) {
                    Post latestPost = response.body().getData().get(0);
                    for (int appWidgetId : appWidgetIds) {
                        updateAppWidget(context, appWidgetManager, appWidgetId, latestPost);
                    }
                }
            }
            @Override
            public void onFailure(Call<PostResponse> call, Throwable t) {
                Log.e("WIDGET_ERROR", "Error: " + t.getMessage());
            }
        });
    }

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Post latestPost) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_layout);

        // 1. Hiển thị nội dung chữ
        views.setTextViewText(R.id.widget_content, latestPost.getContent());

        // 2. Load ảnh chính (Sử dụng .into(ImageView) của Glide cho Widget)
        // Lưu ý: Đôi khi AppWidgetTarget cần context của ứng dụng để hoạt động ổn định
        AppWidgetTarget mainImageTarget = new AppWidgetTarget(context, R.id.widget_image, views, appWidgetId);
        Glide.with(context.getApplicationContext())
                .asBitmap()
                .load(latestPost.getImageURL())
                .override(500, 500)
                .centerCrop()
                .into(mainImageTarget);

        // 3. Load Avatar (Xử lý cả URL và Base64)
        AppWidgetTarget avatarTarget = new AppWidgetTarget(context, R.id.widget_user_avatar, views, appWidgetId);

        String avatarData = latestPost.getAvatarURL(); // Hoặc latestPost.getAvatar() tùy model của bạn

        if (avatarData != null && !avatarData.isEmpty()) {
            if (avatarData.startsWith("http")) {
                // TRƯỜNG HỢP 1: Avatar là link URL
                Glide.with(context.getApplicationContext())
                        .asBitmap()
                        .load(avatarData)
                        .override(100, 100)
                        .circleCrop()
                        .into(avatarTarget);
            } else {
                // TRƯỜNG HỢP 2: Avatar là chuỗi Base64 (giống trong MainActivity của bạn)
                try {
                    if (avatarData.contains(",")) {
                        avatarData = avatarData.split(",")[1];
                    }
                    byte[] imageBytes = android.util.Base64.decode(avatarData, android.util.Base64.DEFAULT);

                    Glide.with(context.getApplicationContext())
                            .asBitmap()
                            .load(imageBytes) // Load từ mảng byte đã decode
                            .override(100, 100)
                            .circleCrop()
                            .into(avatarTarget);
                } catch (Exception e) {
                    Log.e("WIDGET_AVATAR", "Lỗi decode Base64: " + e.getMessage());
                }
            }
        } else {
            // TRƯỜNG HỢP 3: Không có ảnh thì hiện ảnh mặc định
            views.setImageViewResource(R.id.widget_user_avatar, R.drawable.ic_avatar_placeholder);
        }

        // 4. Xử lý sự kiện Click để vào xem bài post
        // Ở đây mình sẽ mở MainActivity và gửi kèm thông tin bài post
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra("post_id", latestPost.getPostID()); // Truyền ID để App biết cần mở bài nào
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        // FLAG_IMMUTABLE hoặc FLAG_UPDATE_CURRENT là bắt buộc từ Android 12+
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                appWidgetId, // Sử dụng ID của widget làm request code để tránh trùng lặp
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Gán sự kiện click cho toàn bộ khung Widget
        views.setOnClickPendingIntent(R.id.widget_image, pendingIntent);

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }
}
