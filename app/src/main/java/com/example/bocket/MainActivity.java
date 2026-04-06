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


import com.bumptech.glide.Glide;
import com.example.bocket.model.Post;
import com.example.bocket.model.User;
import com.example.bocket.net.ApiService;
import com.example.bocket.net.PostResponse;
import com.example.bocket.net.RetrofitClient;
import com.example.bocket.ui.PostAdapter;
import com.example.bocket.ui.PreviewPostActivity;
import com.example.bocket.ui.ProfileActivity;
import com.example.bocket.ui.WelcomeActivity;
import com.google.common.util.concurrent.ListenableFuture;


import java.io.File;
import java.util.ArrayList;
import java.util.List;


import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class MainActivity extends AppCompatActivity {


    // --- VIEW CAMERA ---
    private PreviewView pvCameraPreview;
    private ConstraintLayout clTopBar, clControlsCamera, clControlsHistory;
    private View flCameraContainer;
    private LinearLayout llHistory;
    private ImageButton ibSwitchCamera, ibGallery, ibCapture, ivAvatar;
    private ImageButton ibCaptureBack, ibGalleryHistory, ibSwitchCameraBack;


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


        // 3. Tải thông tin người dùng (Avatar)
        loadUserProfile();


        // 4. Thiết lập RecyclerView cho Feed
        setupRecyclerView();


        // 5. Kiểm tra quyền và chạy Camera
        if (allPermissionsGranted()) {
            startCamera();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }


        // 6. Gán sự kiện Click
        setupClickListeners();
    }


    private void initViews() {
        pvCameraPreview = findViewById(R.id.pvCameraPreview);
        clTopBar = findViewById(R.id.clTopBar);
        flCameraContainer = findViewById(R.id.flCameraContainer);
        llHistory = findViewById(R.id.llHistory);


        // Bộ nút màn hình Camera
        clControlsCamera = findViewById(R.id.clControlsCamera);
        ibSwitchCamera = findViewById(R.id.ibSwitchCamera);
        ibGallery = findViewById(R.id.ibGallery);
        ibCapture = findViewById(R.id.ibCapture);
        ivAvatar = findViewById(R.id.ivAvatar);


        // Bộ nút màn hình Lịch sử (Feed)
        rvFeed = findViewById(R.id.rvFeed);
        clControlsHistory = findViewById(R.id.clControlsHistory);
        ibCaptureBack = findViewById(R.id.ibCaptureBack);
        ibGalleryHistory = findViewById(R.id.ibGalleryHistory);
        ibSwitchCameraBack = findViewById(R.id.ibSwitchCameraBack);
    }


    private void setupRecyclerView() {
        rvFeed.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        PagerSnapHelper snapHelper = new PagerSnapHelper();
        snapHelper.attachToRecyclerView(rvFeed);
    }


    private void setupClickListeners() {
        // --- Sự kiện màn hình Camera ---
        ibCapture.setOnClickListener(v -> takePhoto());
        ibSwitchCamera.setOnClickListener(v -> toggleCamera());
        ibGallery.setOnClickListener(v -> openGallery());
        llHistory.setOnClickListener(v -> showFeed());
        ivAvatar.setOnClickListener(v -> {
            Intent intent = new Intent(this, ProfileActivity.class);
            startActivity(intent);
        });


        // --- Sự kiện màn hình Lịch sử ---
        ibCaptureBack.setOnClickListener(v -> hideFeed());
        ibGalleryHistory.setOnClickListener(v -> openGallery());
        ibSwitchCameraBack.setOnClickListener(v -> toggleCamera());
    }


    private void toggleCamera() {
        lensFacing = (lensFacing == CameraSelector.LENS_FACING_BACK) ?
                CameraSelector.LENS_FACING_FRONT : CameraSelector.LENS_FACING_BACK;
        startCamera();
    }


    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryActivityResultLauncher.launch(intent);
    }


    // --- LOGIC CHUYỂN ĐỔI GIAO DIỆN ---


    private void showFeed() {
        clTopBar.setVisibility(View.GONE);
        flCameraContainer.setVisibility(View.GONE);
        clControlsCamera.setVisibility(View.GONE);
        llHistory.setVisibility(View.GONE);


        rvFeed.setVisibility(View.VISIBLE);
        clControlsHistory.setVisibility(View.VISIBLE);


        loadPostsFromServer();
    }


    public void hideFeed() {
        rvFeed.setVisibility(View.GONE);
        clControlsHistory.setVisibility(View.GONE);


        clTopBar.setVisibility(View.VISIBLE);
        flCameraContainer.setVisibility(View.VISIBLE);
        clControlsCamera.setVisibility(View.VISIBLE);
        llHistory.setVisibility(View.VISIBLE);
    }


    // --- NETWORK LOGIC ---


    private void loadUserProfile() {
        SharedPreferences sharedPref = getSharedPreferences("BocketPrefs", Context.MODE_PRIVATE);
        String token = "Bearer " + sharedPref.getString("jwt_token", "");


        ApiService apiService = RetrofitClient.getApiService();
        apiService.getUserProfile(token).enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String base64Avatar = response.body().getAvatar();
                    if (base64Avatar != null && !base64Avatar.isEmpty()) {
                        byte[] imageBytes = android.util.Base64.decode(base64Avatar, android.util.Base64.DEFAULT);
                        Glide.with(MainActivity.this)
                                .asBitmap()
                                .load(imageBytes)
                                .placeholder(R.drawable.ic_avatar_placeholder)
                                .circleCrop()
                                .into(ivAvatar);
                    }
                }
            }
            @Override
            public void onFailure(Call<User> call, Throwable t) { }
        });
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


    // --- CAMERAX LOGIC ---


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


    // --- AUTH & PERMISSION HELPERS ---


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
