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
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bocket.ui.PostAdapter; // Bạn cần tạo class này
import com.example.bocket.model.Post;        // Bạn cần tạo class này
import com.example.bocket.net.ApiService;
import com.example.bocket.net.PostResponse; // Class chứa list data trả về
import com.example.bocket.net.RetrofitClient;
import com.example.bocket.ui.PreviewPostActivity;
import com.example.bocket.ui.WelcomeActivity;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    // --- VIEW CAMERA ---
    private PreviewView pvCameraPreview;
    private ConstraintLayout clTopBar, clControls;
    private View flCameraContainer;
    private LinearLayout llHistory;
    private ImageButton ibSwitchCamera, ibGallery, ibCapture;

    // --- VIEW FEED (HISTORY) ---
    private RecyclerView rvFeed;
    private PostAdapter postAdapter;
    private List<Post> postList = new ArrayList<>();

    // --- CAMERA LOGIC ---
    private int lensFacing = CameraSelector.LENS_FACING_BACK;
    private ImageCapture imageCapture;
    private ProcessCameraProvider cameraProvider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. Kiểm tra đăng nhập
        if (!isUserLoggedIn()) {
            navigateToWelcome();
            return;
        }

        setContentView(R.layout.activity_main);

        // 2. Khởi tạo Views
        initViews();

        // 3. Thiết lập RecyclerView cho Feed
        setupRecyclerView();

        // 4. Kiểm tra quyền và chạy Camera
        if (allPermissionsGranted()) {
            startCamera();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }

        // 5. Gán sự kiện Click
        setupClickListeners();
    }

    private void initViews() {
        pvCameraPreview = findViewById(R.id.pvCameraPreview);
        clTopBar = findViewById(R.id.clTopBar);
        clControls = findViewById(R.id.clControls);
        flCameraContainer = findViewById(R.id.flCameraContainer);
        llHistory = findViewById(R.id.llHistory);

        ibSwitchCamera = findViewById(R.id.ibSwitchCamera);
        ibGallery = findViewById(R.id.ibGallery);
        ibCapture = findViewById(R.id.ibCapture);

        rvFeed = findViewById(R.id.rvFeed);
    }

    private void setupRecyclerView() {
        rvFeed.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        // Hiệu ứng lướt từng trang như TikTok
        PagerSnapHelper snapHelper = new PagerSnapHelper();
        snapHelper.attachToRecyclerView(rvFeed);
    }

    private void setupClickListeners() {
        // Chụp ảnh
        ibCapture.setOnClickListener(v -> takePhoto());

        // Đổi Camera
        ibSwitchCamera.setOnClickListener(v -> {
            lensFacing = (lensFacing == CameraSelector.LENS_FACING_BACK) ?
                    CameraSelector.LENS_FACING_FRONT : CameraSelector.LENS_FACING_BACK;
            startCamera();
        });

        // Mở Gallery
        ibGallery.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            galleryActivityResultLauncher.launch(intent);
        });

        // MỞ LỊCH SỬ (FEED)
        llHistory.setOnClickListener(v -> showFeed());
    }

    // --- LOGIC CHUYỂN ĐỔI GIAO DIỆN ---

    private void showFeed() {
        // Ẩn UI Camera
        clTopBar.setVisibility(View.GONE);
        flCameraContainer.setVisibility(View.GONE);
        clControls.setVisibility(View.GONE);
        llHistory.setVisibility(View.GONE);

        // Hiện RecyclerView
        rvFeed.setVisibility(View.VISIBLE);

        // Tải dữ liệu từ Server
        loadPostsFromServer();
    }

    // Hàm này sẽ được gọi từ Adapter khi nhấn nút thoát hoặc nút Capture ở màn hình Feed
    public void hideFeed() {
        rvFeed.setVisibility(View.GONE);
        clTopBar.setVisibility(View.VISIBLE);
        flCameraContainer.setVisibility(View.VISIBLE);
        clControls.setVisibility(View.VISIBLE);
        llHistory.setVisibility(View.VISIBLE);
    }

    private void loadPostsFromServer() {
        SharedPreferences sharedPref = getSharedPreferences("BocketPrefs", Context.MODE_PRIVATE);
        String token = "Bearer " + sharedPref.getString("jwt_token", "");

        ApiService apiService = RetrofitClient.getApiService();
        apiService.getAllPosts(token).enqueue(new Callback<PostResponse>() {
            @Override
            public void onResponse(Call<PostResponse> call, Response<PostResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    postList.clear();
                    postList.addAll(response.body().getData());
                    postAdapter = new PostAdapter(MainActivity.this, postList);
                    rvFeed.setAdapter(postAdapter);
                }
            }

            @Override
            public void onFailure(Call<PostResponse> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Không thể tải bài viết", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // --- CAMERA & AUTH LOGIC (GIỮ LẠI TỪ CODE CŨ) ---

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(pvCameraPreview.getSurfaceProvider());

                imageCapture = new ImageCapture.Builder().build();
                CameraSelector cameraSelector = new CameraSelector.Builder().requireLensFacing(lensFacing).build();

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void takePhoto() {
        if (imageCapture == null) return;
        File photoFile = new File(getExternalFilesDir(null), System.currentTimeMillis() + ".jpg");
        ImageCapture.OutputFileOptions outputOptions = new ImageCapture.OutputFileOptions.Builder(photoFile).build();

        imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(this), new ImageCapture.OnImageSavedCallback() {
            @Override
            public void onImageSaved(@NonNull ImageCapture.OutputFileResults results) {
                navigateToPreview(Uri.fromFile(photoFile));
            }
            @Override
            public void onError(@NonNull ImageCaptureException e) {
                Toast.makeText(MainActivity.this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void navigateToPreview(Uri uri) {
        Intent intent = new Intent(this, PreviewPostActivity.class);
        intent.putExtra("image_uri", uri.toString());
        startActivity(intent);
    }

    private boolean isUserLoggedIn() {
        SharedPreferences sharedPref = getSharedPreferences("BocketPrefs", Context.MODE_PRIVATE);
        String token = sharedPref.getString("jwt_token", null);
        return token != null && !token.isEmpty();
    }

    private void navigateToWelcome() {
        startActivity(new Intent(this, WelcomeActivity.class));
        finish();
    }

    private boolean allPermissionsGranted() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    private final ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) startCamera();
                else finish();
            });

    private final ActivityResultLauncher<Intent> galleryActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    navigateToPreview(result.getData().getData());
                }
            });
}