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
import android.util.Log;
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
        oldName = getIntent().getStringExtra("current_DisplayName");
        oldEmail = getIntent().getStringExtra("current_Email");
        String oldAvatar = getIntent().getStringExtra("current_Avatar");


        etName.setText(oldName != null ? oldName : "");
        etEmail.setText(oldEmail != null ? oldEmail : "");
        if (oldAvatar != null && !oldAvatar.isEmpty()) {
            //Glide.with(this).load(oldAvatar).placeholder(R.drawable.ic_avatar_placeholder).into(ivAvatar);
            try {
                // Giải mã Base64
                byte[] imageBytes = Base64.decode(oldAvatar, Base64.DEFAULT);

                // Load ảnh bằng Glide và cắt tròn
                Glide.with(EditProfileActivity.this)
                        .asBitmap()
                        .load(imageBytes)
                        .placeholder(R.drawable.ic_avatar_placeholder)
                        .circleCrop() // Đảm bảo bo tròn hoàn hảo
                        .into(ivAvatar);
            } catch (Exception e) {
                e.printStackTrace();
            }
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
        String nameInput = etName.getText().toString().trim();
        String emailInput = etEmail.getText().toString().trim();

        // Nếu thay đổi email
        if (!emailInput.isEmpty() && !emailInput.equals(oldEmail)) {
            // 1. Gửi OTP về email cũ trước
            requestOTPForEmailChange(oldEmail, () -> {
                // 2. Sau khi gửi OTP thành công, hiện Dialog nhập mã
                showOtpInputDialog(otp -> {
                    // 3. Thực hiện update kèm mã OTP
                    sendUpdateToService(nameInput, emailInput, otp);
                });
            });
        } else {
            // Không đổi email -> Update bình thường (otp = null)
            sendUpdateToService(nameInput, emailInput, null);
        }
    }

    private void sendUpdateToService(String name, String email, String otp) {
        // 1. Xử lý ảnh (Dùng NO_WRAP để chuỗi Base64 liền mạch)
        String avatarBase64 = null;
        if (selectedBitmap != null) {
            Bitmap smallBitmap = getResizedBitmap(selectedBitmap, 500);
            // Sửa ở đây: Base64.NO_WRAP
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            smallBitmap.compress(Bitmap.CompressFormat.JPEG, 30, baos);
            byte[] b = baos.toByteArray();
            avatarBase64 = Base64.encodeToString(b, Base64.NO_WRAP);
        }

        // 2. Lấy Token từ SharedPreferences
        SharedPreferences sharedPref = getSharedPreferences("BocketPrefs", Context.MODE_PRIVATE);
        String token = "Bearer " + sharedPref.getString("jwt_token", "");

        // 3. Tạo Object Update (Sử dụng trực tiếp tham số name, email đã truyền vào)
        User updateRequest = new User();
        updateRequest.setDisplay_name(name.isEmpty() ? oldName : name);
        updateRequest.setEmail(email.isEmpty() ? oldEmail : email);
        updateRequest.setAvatar(avatarBase64);
        updateRequest.setOtp(otp);

        // 4. Gọi API
        RetrofitClient.getApiService().updateProfile(token, updateRequest).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(EditProfileActivity.this, "Cập nhật hồ sơ thành công!", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                } else {
                    // Đọc lỗi chi tiết từ server để biết tại sao sai (ví dụ: Sai OTP)
                    try {
                        String errorJson = response.errorBody().string();
                        // Nếu server trả về dạng { "message": "..." }
                        org.json.JSONObject jObjError = new org.json.JSONObject(errorJson);
                        Toast.makeText(EditProfileActivity.this, jObjError.getString("message"), Toast.LENGTH_LONG).show();
                    } catch (Exception e) {
                        Toast.makeText(EditProfileActivity.this, "Lỗi: " + response.code(), Toast.LENGTH_SHORT).show();
                    }
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

    private void showOtpInputDialog(OnOtpEntered listener) {
        EditText etOtp = new EditText(this);
        etOtp.setHint("Nhập mã OTP gửi tới " + oldEmail);

        new AlertDialog.Builder(this)
                .setTitle("Xác thực thay đổi Email")
                .setView(etOtp)
                .setPositiveButton("Xác nhận", (dialog, which) -> {
                    listener.onEntered(etOtp.getText().toString().trim());
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    interface OnOtpEntered {
        void onEntered(String otp);
    }
    interface OnOtpSentListener {
        void onSuccess();
    }

    private void requestOTPForEmailChange(String email, OnOtpSentListener listener) {
        User userRequest = new User();
        userRequest.setEmail(email);

        // THAY ĐỔI: Gọi sendOtpUpdate thay vì sendOTP
        RetrofitClient.getApiService().sendOtpUpdate(userRequest).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(EditProfileActivity.this, "Mã xác thực đã gửi!", Toast.LENGTH_SHORT).show();
                    listener.onSuccess();
                } else {
                    try {
                        // Đọc lỗi chi tiết từ Server để biết tại sao bị 400
                        String errorBody = response.errorBody().string();
                        org.json.JSONObject jObj = new org.json.JSONObject(errorBody);
                        String msg = jObj.optString("message", "Dữ liệu không hợp lệ");

                        Toast.makeText(EditProfileActivity.this, msg, Toast.LENGTH_LONG).show();
                        Log.e("DEBUG_OTP", "Server Error: " + errorBody);
                    } catch (Exception e) {
                        Toast.makeText(EditProfileActivity.this, "Lỗi: " + response.code(), Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e("DEBUG_OTP", "Lỗi kết nối: " + t.getMessage());
                Toast.makeText(EditProfileActivity.this, "Không thể kết nối Server!", Toast.LENGTH_SHORT).show();
            }
        });
    }
}