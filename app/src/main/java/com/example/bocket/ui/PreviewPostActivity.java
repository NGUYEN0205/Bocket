package com.example.bocket.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.loader.content.CursorLoader;

import com.example.bocket.R;
import com.example.bocket.net.ApiService;
import com.example.bocket.net.RetrofitClient;

import java.io.File;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PreviewPostActivity extends AppCompatActivity {

    private ImageView ivPreviewImage;
    private EditText etCaption;
    private ImageButton ibCancel, ibSend;
    private Uri imageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preview_post);

        // 1. Ánh xạ views
        ivPreviewImage = findViewById(R.id.ivPreviewImage);
        etCaption = findViewById(R.id.etCaption);
        ibCancel = findViewById(R.id.ibCancel);
        ibSend = findViewById(R.id.ibSend);

        // 2. Lấy Uri ảnh được truyền từ MainActivity
        String uriString = getIntent().getStringExtra("image_uri");
        if (uriString != null) {
            imageUri = Uri.parse(uriString);
            ivPreviewImage.setImageURI(imageUri); // Hiển thị ảnh
        } else {
            Toast.makeText(this, "Không tìm thấy ảnh!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 3. Xử lý nút Hủy (Dấu X) -> Quay lại màn hình chụp
        ibCancel.setOnClickListener(v -> finish());

        // 4. Xử lý nút Gửi -> Upload lên Server
        ibSend.setOnClickListener(v -> {
            String caption = etCaption.getText().toString().trim();
            uploadPost(imageUri, caption);
        });
    }

    // --- COPY HÀM UPLOAD TỪ MAINACTIVITY SANG ĐÂY VÀ CHỈNH SỬA MỘT CHÚT ---
    private void uploadPost(Uri uri, String captionText) {
        try {
            // Bước 1: Chuyển Uri thành File bằng cách copy vào Cache
            // Cách này khắc phục triệt để lỗi "RealPath" trên Android 11+
            File file = getFileFromUri(uri);
            if (file == null) {
                Toast.makeText(this, "Không thể xử lý tệp tin!", Toast.LENGTH_SHORT).show();
                return;
            }

            // Bước 2: Chuẩn bị RequestBody cho File
            RequestBody requestFile = RequestBody.create(MediaType.parse("image/*"), file);
            MultipartBody.Part body = MultipartBody.Part.createFormData("image", file.getName(), requestFile);

            // Bước 3: Chuẩn bị nội dung chữ (Content)
            RequestBody contentPart = RequestBody.create(MediaType.parse("multipart/form-data"), captionText);

            // Bước 4: Lấy Token
            SharedPreferences sharedPref = getSharedPreferences("BocketPrefs", Context.MODE_PRIVATE);
            String token = "Bearer " + sharedPref.getString("jwt_token", "");

            // Bước 5: Gọi API
            ApiService apiService = RetrofitClient.getApiService();
            apiService.uploadPost(token, body, contentPart).enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    if (response.isSuccessful()) {
                        Toast.makeText(PreviewPostActivity.this, "Đăng bài thành công!", Toast.LENGTH_SHORT).show();
                        // Trở về MainActivity và báo thành công
                        finish();
                    } else {
                        Toast.makeText(PreviewPostActivity.this, "Lỗi: " + response.code(), Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    Toast.makeText(PreviewPostActivity.this, "Kết nối thất bại: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    android.util.Log.e("UPLOAD_ERR", t.getMessage());
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Lỗi hệ thống: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // Cần hàm này nếu ảnh chọn từ Gallery được truyền sang đây
    private File getFileFromUri(Uri uri) {
        try {
            java.io.InputStream inputStream = getContentResolver().openInputStream(uri);
            // Tạo một file tạm trong thư mục Cache của App
            File file = new File(getCacheDir(), "temp_upload_" + System.currentTimeMillis() + ".jpg");
            java.io.FileOutputStream outputStream = new java.io.FileOutputStream(file);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
            outputStream.close();
            inputStream.close();
            return file;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}