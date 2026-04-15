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
        // 1. Lấy Token từ SharedPreferences
        SharedPreferences sharedPref = context.getSharedPreferences("BocketPrefs", Context.MODE_PRIVATE);
        String token = "Bearer " + sharedPref.getString("jwt_token", "");

        // 2. Gọi API lấy bài viết mới nhất
        ApiService apiService = RetrofitClient.getApiService();
        apiService.getFriendPosts(token).enqueue(new Callback<PostResponse>() {
            @Override
            public void onResponse(Call<PostResponse> call, Response<PostResponse> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().getData().isEmpty()) {
                    // Lấy bài viết đầu tiên trong danh sách (mới nhất)
                    Post latestPost = response.body().getData().get(0);

                    // 3. Update từng cái Widget đang có trên màn hình chính
                    for (int appWidgetId : appWidgetIds) {
                        updateAppWidget(context, appWidgetManager, appWidgetId, latestPost);
                    }
                }
            }

            @Override
            public void onFailure(Call<PostResponse> call, Throwable t) {
                Log.e("WIDGET_ERROR", "Không thể lấy dữ liệu cho Widget: " + t.getMessage());
            }
        });
    }

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Post latestPost) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_layout);

        // Gán nội dung văn bản
        views.setTextViewText(R.id.widget_content, latestPost.getContent());

        // Load ảnh bài đăng (Sử dụng AppWidgetTarget của Glide)
        AppWidgetTarget mainImageTarget = new AppWidgetTarget(context, R.id.widget_image, views, appWidgetId);

        Glide.with(context.getApplicationContext())
                .asBitmap()
                .load(latestPost.getImageURL())
                .into(mainImageTarget);

        // Load avatar người đăng (Làm tròn)
        AppWidgetTarget avatarTarget = new AppWidgetTarget(context, R.id.widget_user_avatar, views, appWidgetId);

        Glide.with(context.getApplicationContext())
                .asBitmap()
                .load(latestPost.getAvatarURL())
                .circleCrop()
                .into(avatarTarget);

        // Khi nhấn vào Widget sẽ mở MainActivity
        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_image, pendingIntent);

        // Cập nhật Widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }
}
