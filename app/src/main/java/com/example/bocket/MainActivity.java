package com.example.bocket;


import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
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
import com.example.bocket.ui.FriendsAdapter;
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
    private RecyclerView rvFriendsList;
    private TextView tvFriendCount;
    private boolean isFirendsListVisible = false;
    private FriendsAdapter friendsAdapter;
    private List<User> friendsList = new ArrayList<>();


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

        rvFriendsList = findViewById(R.id.rvFriendsList);
        tvFriendCount = findViewById(R.id.tvFriendCount);

        rvFriendsList.setLayoutManager(new LinearLayoutManager(this));
        friendsAdapter = new FriendsAdapter(this, friendsList);
        rvFriendsList.setAdapter(friendsAdapter);

        friendsAdapter.setOnFriendClickListener(friend -> {
            hideFriendsList();
            // Gọi API lấy bài viết của riêng người này
            loadUserPostsOnly(friend.getUserID());
        });
    }
    @Override
    protected void onResume() {
        super.onResume();
        loadUserProfile(); // Cập nhật lại avatar mỗi khi quay lại màn hình chính
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
        // Trong setupClickListeners()
        llHistory.setOnClickListener(v -> {
            loadPostsFromServer(); // Tải tất cả
            showFeed();           // Sau đó mới hiện màn hình
        });

        // Trong initViews(), phần xử lý adapter:
        friendsAdapter.setOnFriendClickListener(friend -> {
            hideFriendsList();
            loadUserPostsOnly(friend.getUserID()); // Tải của 1 người, trong hàm này đã có showFeed() rồi
        });
        ivAvatar.setOnClickListener(v -> {
            Intent intent = new Intent(this, ProfileActivity.class);
            startActivity(intent);
        });


        // --- Sự kiện màn hình Lịch sử ---
        ibCaptureBack.setOnClickListener(v -> hideFeed());
        ibGalleryHistory.setOnClickListener(v -> openGallery());
        ibSwitchCameraBack.setOnClickListener(v -> toggleCamera());

        tvFriendCount.setOnClickListener(v -> {
            if (!isFirendsListVisible){
                showFriendsList();
            } else {
                hideFriendsList();
            }

        });
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


        //loadPostsFromServer();
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
                    String avatarData = response.body().getAvatar(); // Chuỗi này có thể là URL hoặc Base64

                    if (avatarData != null && !avatarData.isEmpty()) {
                        // TRƯỜNG HỢP 1: Nếu avatarData là một URL (http...)
                        if (avatarData.startsWith("http")) {
                            Glide.with(MainActivity.this)
                                    .load(avatarData)
                                    .placeholder(R.drawable.ic_avatar_placeholder)
                                    .circleCrop()
                                    .into(ivAvatar);
                        }
                        // TRƯỜNG HỢP 2: Nếu avatarData là chuỗi Base64
                        else {
                            try {
                                // Xử lý nếu chuỗi Base64 có chứa header "data:image/..."
                                if (avatarData.contains(",")) {
                                    avatarData = avatarData.split(",")[1];
                                }

                                byte[] imageBytes = android.util.Base64.decode(avatarData, android.util.Base64.DEFAULT);
                                Glide.with(MainActivity.this)
                                        .asBitmap()
                                        .load(imageBytes)
                                        .placeholder(R.drawable.ic_avatar_placeholder)
                                        .circleCrop()
                                        .into(ivAvatar);
                            } catch (Exception e) {
                                Log.e("AVATAR_ERROR", "Lỗi decode Base64: " + e.getMessage());
                            }
                        }
                    }
                }
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {
                Log.e("AVATAR_ERROR", "Lỗi kết nối profile: " + t.getMessage());
            }
        });
    }


    private void loadPostsFromServer() {
        SharedPreferences sharedPref = getSharedPreferences("BocketPrefs", Context.MODE_PRIVATE);
        String token = "Bearer " + sharedPref.getString("jwt_token", "");

        ApiService apiService = RetrofitClient.getApiService();

        // Sử dụng đúng kiểu PostResponse nếu server trả về object bọc ngoài
        apiService.getFriendPosts(token).enqueue(new Callback<PostResponse>() {
            @Override
            public void onResponse(Call<PostResponse> call, Response<PostResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    postList.clear();
                    // Giả sử PostResponse có method getData() trả về List<Post>
                    postList.addAll(response.body().getData());

                    if (postAdapter == null) {
                        postAdapter = new PostAdapter(MainActivity.this, postList);
                        rvFeed.setAdapter(postAdapter);
                    } else {
                        postAdapter.notifyDataSetChanged();
                    }
                }
            }

            @Override
            public void onFailure(Call<PostResponse> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
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

    private void showFriendsList(){
        isFirendsListVisible = true;
        rvFriendsList.setVisibility(View.VISIBLE);
        rvFriendsList.bringToFront();
        rvFriendsList.scrollToPosition(0);
        android.util.Log.d("DEBUG_FRIENDS", "Đang bắt đầu load bạn bè...");

        loadFriendsFromServer();
    }

    private void hideFriendsList(){
        isFirendsListVisible = false;
        rvFriendsList.setVisibility(View.GONE);
    }

    @Override
    public void onBackPressed() {
        if (isFirendsListVisible) {
            hideFriendsList();
        } else if (rvFeed.getVisibility() == View.VISIBLE) {
            hideFeed(); // Nếu đang xem Feed thì quay về Camera trước khi thoát app
        } else {
            super.onBackPressed();
        }
    }

    private void loadFriendsFromServer() {
        SharedPreferences sharedPref = getSharedPreferences("BocketPrefs", Context.MODE_PRIVATE);
        String token = "Bearer " + sharedPref.getString("jwt_token", "");

        ApiService apiService = RetrofitClient.getApiService();
        apiService.getFriendsList(token).enqueue(new Callback<List<User>>() {
            @Override
            public void onResponse(Call<List<User>> call, Response<List<User>> response) {
                // Trong hàm loadFriendsFromServer()
                if (response.isSuccessful() && response.body() != null) {
                    friendsList.clear();
                    friendsList.addAll(response.body());
                    friendsAdapter.notifyDataSetChanged();

                    // Log kiểm tra số lượng thực tế
                    android.util.Log.d("DEBUG_FRIENDS", "Đã nạp: " + friendsList.size());
                }else {
                    android.util.Log.e("DEBUG_FRIENDS", "Lỗi phản hồi: " + response.code());
                }
            }
            @Override
            public void onFailure(Call<List<User>> call, Throwable t) {
                android.util.Log.e("DEBUG_FRIENDS", "Lỗi kết nối: " + t.getMessage());
            }
        });
    }

    private void loadUserPostsOnly(int targetUserId) {
        SharedPreferences sharedPref = getSharedPreferences("BocketPrefs", Context.MODE_PRIVATE);
        String token = "Bearer " + sharedPref.getString("jwt_token", "");

        ApiService apiService = RetrofitClient.getApiService();
        apiService.getPostsByUser(token, targetUserId).enqueue(new Callback<PostResponse>() {
            @Override
            public void onResponse(Call<PostResponse> call, Response<PostResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // 1. Xóa dữ liệu cũ của feed chung
                    postList.clear();

                    // 2. Thêm dữ liệu của người dùng cụ thể
                    postList.addAll(response.body().getData());

                    // 3. Cập nhật Adapter
                    if (postAdapter == null) {
                        postAdapter = new PostAdapter(MainActivity.this, postList);
                        rvFeed.setAdapter(postAdapter);
                    } else {
                        postAdapter.notifyDataSetChanged();
                    }

                    // 4. Cuộn lên đầu trang
                    rvFeed.scrollToPosition(0);

                    // 5. Hiển thị màn hình Feed
                    showFeed();
                }
            }
            @Override
            public void onFailure(Call<PostResponse> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }
}