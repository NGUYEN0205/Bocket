package com.example.bocket;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.example.bocket.ui.WelcomeActivity;
import com.google.common.util.concurrent.ListenableFuture;
import androidx.camera.core.impl.utils.futures.Futures;

import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_PERMISSIONS = 101;
    // Mảng chứa các quyền cần thiết
    private static final String[] REQUIRED_PERMISSIONS = new String[]{Manifest.permission.CAMERA};

    // Ánh xạ các View từ XML
    private PreviewView pvCameraPreview;
    private ImageButton ibSwitchCamera, ibGallery, ibCapture;

    // Biến để quản lý camera đang được sử dụng (mặc định là Camera Sau)
    private int lensFacing = CameraSelector.LENS_FACING_BACK;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        resetToken();
        // --- BƯỚC 1: KIỂM TRA ĐĂNG NHẬP TRƯỚC KHI SET CONTENT VIEW ---
        if (!isUserLoggedIn()) {
            navigateToWelcome();
            return; // Dừng thực hiện các lệnh bên dưới
        }

        setContentView(R.layout.activity_main);

        // Ánh xạ views
        pvCameraPreview = findViewById(R.id.pvCameraPreview);
        ibSwitchCamera = findViewById(R.id.ibSwitchCamera);
        ibGallery = findViewById(R.id.ibGallery);
        ibCapture = findViewById(R.id.ibCapture); // Dành cho nút chụp

        // 1. KIỂM TRA QUYỀN CAMERA TRƯỚC KHI BẮT ĐẦU
        if (allPermissionsGranted()) {
            startCamera(); // Nếu đã có quyền, bắt đầu camera
        } else {
            // Nếu chưa, yêu cầu cấp quyền
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }

        // 2. XỬ LÝ SỰ KIỆN NÚT ĐỔI CAMERA (Xoay vòng)
        ibSwitchCamera.setOnClickListener(v -> {
            // Đổi giữa camera sau và trước
            if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                lensFacing = CameraSelector.LENS_FACING_FRONT;
            } else {
                lensFacing = CameraSelector.LENS_FACING_BACK;
            }
            // Khởi động lại camera để áp dụng thay đổi
            startCamera();
        } );

        // 3. XỬ LÝ SỰ KIỆN NÚT THƯ VIỆN (Gallery)
        ibGallery.setOnClickListener(v -> {
            // Sử dụng Intent để mở thư viện ảnh mặc định
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.setType("image/*"); // Chỉ chọn ảnh
            galleryActivityResultLauncher.launch(intent);
        } );

        // Nút chụp (Dành cho chức năng tương lai)
        ibCapture.setOnClickListener(v -> {
            Toast.makeText(this, "Chức năng chụp đang phát triển!", Toast.LENGTH_SHORT).show();
        });
    }
    private void resetToken() {
        SharedPreferences sharedPref = getSharedPreferences("BocketPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.remove("jwt_token"); // Hoặc editor.putString("jwt_token", null);
        editor.apply(); // Xác nhận xóa
    }
    private boolean isUserLoggedIn() {
        // Đọc token từ SharedPreferences (cùng tên với cái bạn đã save ở LoginActivity)
        SharedPreferences sharedPref = getSharedPreferences("BocketPrefs", Context.MODE_PRIVATE);
        String token = sharedPref.getString("jwt_token", null);

        // Nếu token khác null và không rỗng thì coi như đã đăng nhập
        return token != null && !token.isEmpty();
    }

    private void navigateToWelcome() {
        Intent intent = new Intent(this, WelcomeActivity.class);
        // Xóa stack cũ để người dùng không bấm Back quay lại MainActivity được
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish(); // Đóng MainActivity
    }
    // --- CÁC PHƯƠNG THỨC XỬ LÝ CAMERA & QUYỀN ---

    // Khởi động CameraX
    private void startCamera() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                // Lấy đối tượng ProcessCameraProvider đã được kết nối
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                // 1. Thiết lập Use Case: Preview (hiển thị khung hình)
                Preview preview = new Preview.Builder().build();

                // Kết nối Use Case preview với PreviewView trên giao diện XML
                preview.setSurfaceProvider(pvCameraPreview.getSurfaceProvider());

                // 2. Thiết lập Camera Selector (chọn camera sau hoặc trước)
                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(lensFacing)
                        .build();

                // 3. Ràng buộc các Use Case với Lifecycle của Activity
                // (Đảm bảo camera được quản lý tự động, không rò rỉ bộ nhớ)
                cameraProvider.unbindAll(); // Ngắt các kết nối cũ
                cameraProvider.bindToLifecycle(this, cameraSelector, preview);

            } catch (ExecutionException | InterruptedException e) {
                // Xử lý lỗi nếu không thể khởi động camera
                Toast.makeText(this, "Không thể khởi động camera.", Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this)); // Chạy trên UI thread
    }

    // Kiểm tra xem tất cả các quyền đã được cấp chưa
    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    // Launcher mới để yêu cầu quyền (giúp code sạch hơn)
    private final ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    startCamera(); // Đã được cấp quyền, bắt đầu camera
                } else {
                    Toast.makeText(this, "Bạn cần cấp quyền Camera để sử dụng!", Toast.LENGTH_LONG).show();
                    finish(); // Thoát nếu không được quyền
                }
            }
    );

    // Launcher để nhận kết quả khi mở thư viện ảnh
    private final ActivityResultLauncher<Intent> galleryActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == AppCompatActivity.RESULT_OK) {
                    // Người dùng đã chọn ảnh
                    Intent data = result.getData();
                    if (data != null) {
                        Uri selectedImageUri = data.getData();
                        // Tại đây bạn có thể hiển thị ảnh hoặc xử lý nó
                        Toast.makeText(this, "Ảnh đã chọn: " + selectedImageUri.toString(), Toast.LENGTH_LONG).show();
                    }
                }
            }
    );
}