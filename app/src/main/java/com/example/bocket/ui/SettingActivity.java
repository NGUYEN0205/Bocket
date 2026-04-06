package com.example.bocket.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.bocket.R;

public class SettingActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        View privacyView = findViewById(R.id.item_privacy);
        TextView txtPrivacy = privacyView.findViewById(R.id.menu_title);
        ImageView imgPrivacy = privacyView.findViewById(R.id.menu_icon);

        txtPrivacy.setText("Quyền riêng tư và dữ liệu");
        imgPrivacy.setImageResource(R.drawable.ic_phone_mockup);

        View notifyView = findViewById(R.id.item_notifications);
        TextView txtNotify = notifyView.findViewById(R.id.menu_title);
        ImageView imgNotify = notifyView.findViewById(R.id.menu_icon);

        txtNotify.setText("Notifications");
        imgNotify.setImageResource(R.drawable.ic_notifications);

        // 1. Nút Back
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        // 2. Thiết lập nội dung cho các Item (Dùng View Binding hoặc findViewById)
        setupMenuItem(R.id.item_report, "Báo cáo sự cố", R.drawable.ic_flash_off);
        setupMenuItem(R.id.item_logout, "Đăng xuất", R.drawable.ic_send); // Ví dụ dùng icon send làm tạm

        // 3. Xử lý sự kiện Đăng xuất
        findViewById(R.id.item_logout).setOnClickListener(v -> {
            SharedPreferences sharedPref = getSharedPreferences("BocketPrefs", MODE_PRIVATE);
            sharedPref.edit().clear().apply();

            Intent intent = new Intent(SettingActivity.this, WelcomeActivity.class);
            // Xóa sạch các Activity cũ (Profile, Home, v.v.) khỏi bộ nhớ
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

            startActivity(intent);

            finish();

            // Thông báo cho người dùng
            Toast.makeText(SettingActivity.this, "Đã đăng xuất!", Toast.LENGTH_SHORT).show();
        });
    }

    private void setupMenuItem(int id, String title, int iconRes) {
        View view = findViewById(id);
        TextView txtTitle = view.findViewById(R.id.menu_title);
        ImageView imgIcon = view.findViewById(R.id.menu_icon);

        txtTitle.setText(title);
        imgIcon.setImageResource(iconRes);

        // Đổi màu riêng cho chữ Đăng xuất nếu muốn
        if (title.equals("Đăng xuất")) {
            txtTitle.setTextColor(Color.parseColor("#FF5252")); // Màu đỏ cảnh báo
        }
    }
}
