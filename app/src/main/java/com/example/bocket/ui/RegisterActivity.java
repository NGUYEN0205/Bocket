package com.example.bocket.ui;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.bocket.R;
import com.example.bocket.model.User;
import com.example.bocket.net.ApiService;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RegisterActivity extends AppCompatActivity {

    private EditText edtUsername, edtPassword;
    private Button btnRegisterSubmit;
    private ImageButton btnBack;
    private ApiService apiService;
    private ImageView imgAvatar;
    private Bitmap selectedBitmap;

    private final ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    try {
                        selectedBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                        imgAvatar.setImageBitmap(selectedBitmap);
                    } catch (Exception e) { e.printStackTrace(); }
                }
            });

    private final ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    if (result.getData() != null && result.getData().getExtras() != null) {
                        Bundle extras = result.getData().getExtras();
                        selectedBitmap = (Bitmap) extras.get("data");
                        if (selectedBitmap != null) {
                            imgAvatar.setImageBitmap(selectedBitmap);
                        }
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // 1. Ánh xạ View từ XML
        initViews();

        // 2. Cấu hình Retrofit kết nối tới Server Node.js
        // Lưu ý: Dùng 10.0.2.2 nếu chạy máy ảo Android, hoặc IP máy tính nếu chạy máy thật
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://192.168.102.8:3000/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        apiService = retrofit.create(ApiService.class);

        // 3. Xử lý nút quay lại
        btnBack.setOnClickListener(v -> finish());

        imgAvatar.setOnClickListener(v -> showImagePickDialog());

        // 4. Xử lý sự kiện Đăng ký
        btnRegisterSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performRegistration();
            }
        });
    }

    private void initViews() {
        edtUsername = findViewById(R.id.edtUsername);
        edtPassword = findViewById(R.id.edtPassword);
        btnRegisterSubmit = findViewById(R.id.btnRegisterSubmit);
        btnBack = findViewById(R.id.btnBack);
        imgAvatar = findViewById(R.id.imgAvatar);
    }

    private void performRegistration() {
        String username = edtUsername.getText().toString().trim();
        String password = edtPassword.getText().toString().trim();

        // Lấy email được truyền từ EmailActivity/OtpActivity qua Intent
        String email = getIntent().getStringExtra("email");

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            return;
        }

        // 1. Xử lý ảnh đại diện thành chuỗi Base64
        Bitmap smallBitmap = getResizedBitmap(selectedBitmap, 500);
        String avatarBase64 = encodeImageToBase64(smallBitmap);

        // 2. Tạo đối tượng User với đầy đủ các trường
        // display_name truyền vào là null hoặc "" để server tự lấy theo username
        User userRequest = new User();
        userRequest.setUsername(username);
        userRequest.setPassword(password);
        userRequest.setDisplay_name(""); // Để trống để Server xử lý gán bằng username
        userRequest.setEmail(email);     // Email từ màn hình trước
        userRequest.setAvatar(avatarBase64);

        Toast.makeText(this, "Đang đăng ký...", Toast.LENGTH_SHORT).show();

        // 3. Gửi API
        apiService.register(userRequest).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(RegisterActivity.this, "Đăng ký thành công!", Toast.LENGTH_LONG).show();
                    startActivity(new Intent(RegisterActivity.this, WelcomeActivity.class));
                    finish();
                } else {
                    Toast.makeText(RegisterActivity.this, "Lỗi: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e("API_ERROR", "Lỗi: " + t.getMessage());
                Toast.makeText(RegisterActivity.this, "Lỗi kết nối Server!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showImagePickDialog(){
        String[] options = {"Chụp ảnh", "Chọn từ thư viện", "Hủy"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Thêm ảnh đại diện");
        builder.setItems(options, (dialog, which) -> {
            if (which == 0) {
                // Kiểm tra quyền Camera trước khi mở
                if (checkSelfPermission(android.Manifest.permission.CAMERA) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{android.Manifest.permission.CAMERA}, 100);
                } else {
                    openCamera();
                }
            } else if (which == 1) {
                Intent pickPhoto = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                galleryLauncher.launch(pickPhoto);
            } else {
                dialog.dismiss();
            }
        });
        builder.show();
    }

    private void openCamera() {
        Intent takePicture = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraLauncher.launch(takePicture);
    }

    private String encodeImageToBase64(Bitmap bitmap) {
        if (bitmap == null) return null;
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        // Nén ảnh xuống chất lượng 70 để tránh quá tải dung lượng (Request Entity Too Large)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 30, baos);
        byte[] b = baos.toByteArray();
        return android.util.Base64.encodeToString(b, android.util.Base64.DEFAULT);
    }

    private Bitmap getResizedBitmap(Bitmap image, int maxSize) {
        int width = image.getWidth();
        int height = image.getHeight();

        float bitmapRatio = (float)width / (float) height;
        if (bitmapRatio > 1) {
            width = maxSize;
            height = (int) (width / bitmapRatio);
        } else {
            height = maxSize;
            width = (int) (height * bitmapRatio);
        }
        return Bitmap.createScaledBitmap(image, width, height, true);
    }

}