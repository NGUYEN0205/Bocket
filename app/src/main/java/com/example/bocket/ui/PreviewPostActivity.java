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
            File file;
            if (uri.getScheme().equals("content")) {
                file = new File(getRealPathFromURI(uri));
            } else {
                file = new File(uri.getPath());
            }

            // 1. Chuẩn bị File ảnh (vẫn là field "image" như cũ)
            RequestBody requestFile = RequestBody.create(MediaType.parse("image/*"), file);
            MultipartBody.Part body = MultipartBody.Part.createFormData("image", file.getName(), requestFile);

            // 2. QUAN TRỌNG: Chuẩn bị nội dung chữ (Content)
            // Server của bạn đang đợi req.body.content, nên ta tạo RequestBody ở đây
            RequestBody contentPart = RequestBody.create(MediaType.parse("text/plain"), captionText);

            // 3. Lấy Token
            SharedPreferences sharedPref = getSharedPreferences("BocketPrefs", Context.MODE_PRIVATE);
            String token = "Bearer " + sharedPref.getString("jwt_token", "");

            // 4. Gọi API
            ApiService apiService = RetrofitClient.getApiService();
            // Truyền cả file ảnh và nội dung chữ vào
            apiService.uploadPost(token, body, contentPart).enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    if (response.isSuccessful()) {
                        Toast.makeText(PreviewPostActivity.this, "Đăng bài thành công!", Toast.LENGTH_SHORT).show();
                        finish(); // Đóng màn hình preview
                    } else {
                        // Nếu lỗi, in mã lỗi để kiểm tra (ví dụ 400, 500)
                        Toast.makeText(PreviewPostActivity.this, "Lỗi server: " + response.code(), Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    Toast.makeText(PreviewPostActivity.this, "Kết nối thất bại: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Cần hàm này nếu ảnh chọn từ Gallery được truyền sang đây
    private String getRealPathFromURI(Uri contentUri) {
        String[] proj = {MediaStore.Images.Media.DATA};
        CursorLoader loader = new CursorLoader(this, contentUri, proj, null, null, null);
        Cursor cursor = loader.loadInBackground();
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        String result = cursor.getString(column_index);
        cursor.close();
        return result;
    }
}