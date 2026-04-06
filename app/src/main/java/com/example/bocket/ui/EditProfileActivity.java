package com.example.bocket.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.bocket.R;
import com.example.bocket.model.User;
import com.example.bocket.net.RetrofitClient;

import java.io.ByteArrayOutputStream;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EditProfileActivity extends AppCompatActivity {

    private ImageView ivAvatar;
    private EditText etName, etEmail;
    private Button btnSave;
    private Bitmap selectedBitmap;
    private String oldName, oldEmail;

    // 1. Mở thư viện ảnh
    private final ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    try {
                        selectedBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                        ivAvatar.setImageBitmap(selectedBitmap);
                    } catch (Exception e) { e.printStackTrace(); }
                }
            });

    // 2. Mở Camera - Cập nhật để nhận kết quả và hiển thị ngay
    private final ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Bundle extras = result.getData().getExtras();
                    if (extras != null) {
                        selectedBitmap = (Bitmap) extras.get("data");
                        if (selectedBitmap != null) {
                            ivAvatar.setImageBitmap(selectedBitmap);
                        }
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        initViews();

        // 3. Load dữ liệu cũ
        oldName = getIntent().getStringExtra("current_name");
        oldEmail = getIntent().getStringExtra("current_email");
        String oldAvatar = getIntent().getStringExtra("current_avatar");

        etName.setText(oldName != null ? oldName : "");
        etEmail.setText(oldEmail != null ? oldEmail : "");
        if (oldAvatar != null && !oldAvatar.isEmpty()) {
            Glide.with(this).load(oldAvatar).placeholder(R.drawable.ic_avatar_placeholder).into(ivAvatar);
        }

        // 4. Sự kiện chọn ảnh
        ivAvatar.setOnClickListener(v -> showImagePickDialog());
        findViewById(R.id.btn_change_avatar).setOnClickListener(v -> showImagePickDialog());

        // 5. Nút lưu và Quay lại
        btnSave.setOnClickListener(v -> performUpdate());
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
    }

    private void initViews() {
        ivAvatar = findViewById(R.id.iv_edit_avatar);
        etName = findViewById(R.id.et_edit_displayname);
        etEmail = findViewById(R.id.et_edit_email);
        btnSave = findViewById(R.id.btn_save_profile);
    }

    private void showImagePickDialog() {
        String[] options = {"Chụp ảnh mới", "Chọn từ thư viện", "Hủy"};
        new AlertDialog.Builder(this)
                .setTitle("Thay đổi ảnh đại diện")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        Intent takePicture = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                        cameraLauncher.launch(takePicture);
                    } else if (which == 1) {
                        Intent pickPhoto = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                        galleryLauncher.launch(pickPhoto);
                    }
                }).show();
    }

    private void performUpdate() {
        // Cho phép để trống, Server sẽ giữ nguyên giá trị cũ nếu không có thay đổi
        String nameInput = etName.getText().toString().trim();
        String emailInput = etEmail.getText().toString().trim();

        // LOGIC: Nếu trống thì lấy lại giá trị cũ, nếu không trống thì lấy giá trị mới
        String finalName = nameInput.isEmpty() ? oldName : nameInput;
        String finalEmail = emailInput.isEmpty() ? oldEmail : emailInput;

        String avatarBase64 = null;
        if (selectedBitmap != null) {
            Bitmap smallBitmap = getResizedBitmap(selectedBitmap, 500);
            avatarBase64 = encodeImageToBase64(smallBitmap);
        }

        SharedPreferences sharedPref = getSharedPreferences("BocketPrefs", Context.MODE_PRIVATE);
        String token = "Bearer " + sharedPref.getString("jwt_token", "");

        User updateRequest = new User();
        updateRequest.setDisplay_name(finalName);
        updateRequest.setEmail(finalEmail);
        updateRequest.setAvatar(avatarBase64);

        RetrofitClient.getApiService().updateProfile(token, updateRequest).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(EditProfileActivity.this, "Cập nhật hồ sơ thành công!", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                } else {
                    Toast.makeText(EditProfileActivity.this, "Lỗi cập nhật: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(EditProfileActivity.this, "Lỗi kết nối Server!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String encodeImageToBase64(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 30, baos);
        byte[] b = baos.toByteArray();
        return Base64.encodeToString(b, Base64.DEFAULT);
    }

    private Bitmap getResizedBitmap(Bitmap image, int maxSize) {
        int width = image.getWidth();
        int height = image.getHeight();
        float bitmapRatio = (float) width / (float) height;
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