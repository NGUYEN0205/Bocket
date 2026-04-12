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
import androidx.recyclerview.widget.GridLayoutManager;
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
import com.example.bocket.ui.GridPostAdapter;
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
    private androidx.swiperefreshlayout.widget.SwipeRefreshLayout swipeRefreshGrid;


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
    private RecyclerView rvGridHistory;
    private GridPostAdapter gridAdapter;
    private ImageButton ibFilter;
    private int currentFeedPosition = 0;


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
        ibFilter = findViewById(R.id.ibFilter);


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
        rvGridHistory = findViewById(R.id.rvGridHistory); // PHẢI THÊM DÒNG NÀY
        swipeRefreshGrid = findViewById(R.id.swipeRefreshGrid);

        // Thiết lập lưới 3 cột
        if (rvGridHistory != null) {
            rvGridHistory.setLayoutManager(new GridLayoutManager(this, 3));
        }
        // Thiết lập kéo xuống để đóng
        if (swipeRefreshGrid != null) {
            // Tùy chỉnh màu vòng xoay (nếu muốn)
            swipeRefreshGrid.setColorSchemeColors(android.graphics.Color.WHITE);
            swipeRefreshGrid.setProgressBackgroundColorSchemeColor(android.graphics.Color.TRANSPARENT);

            swipeRefreshGrid.setOnRefreshListener(() -> {
                // Khi người dùng lướt xuống đủ mạnh
                swipeRefreshGrid.setRefreshing(false); // Tắt icon loading
                hideFeed(); // Gọi hàm ẩn feed để quay về camera
            });
        }
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

        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);

        // Gán nó vào RecyclerView
        rvFeed.setLayoutManager(layoutManager);

        // Bây giờ bên trong này sẽ không còn bị báo đỏ nữa
        rvFeed.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                // layoutManager lúc này đã tồn tại nên sử dụng được
                currentFeedPosition = layoutManager.findFirstVisibleItemPosition();
            }
        });
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
        ibGallery.setOnClickListener(v -> openGallery());
        ibSwitchCameraBack.setOnClickListener(v -> {
            LinearLayoutManager lm = (LinearLayoutManager) rvFeed.getLayoutManager();
            if (lm != null) {
                int position = lm.findFirstVisibleItemPosition(); // Lấy vị trí ngay tại lúc bấm nút
                if (position != RecyclerView.NO_POSITION && !postList.isEmpty()) {
                    showBottomMenu(postList.get(position), position);
                }
            }
        });

        tvFriendCount.setOnClickListener(v -> {
            if (!isFirendsListVisible){
                showFriendsList();
            } else {
                hideFriendsList();
            }

        });
        // Sự kiện khi bấm vào nút Gallery ở màn hình Feed/Lịch sử
        ibGalleryHistory.setOnClickListener(v -> {
            showGridHistory();
        });

        if (ibFilter != null) {
            ibFilter.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, com.example.bocket.ui.ChatPartnerActivity.class);
                startActivity(intent);
            });
        }
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
        if (swipeRefreshGrid != null) swipeRefreshGrid.setVisibility(View.GONE);

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
                    // --- BƯỚC QUAN TRỌNG: Lưu userId của chính mình vào SharedPreferences ---
                    int myId = response.body().getUserID();
                    sharedPref.edit().putInt("user_id", myId).apply();

                    String avatarData = response.body().getAvatar();
                    // ... (Phần code Glide giữ nguyên như cũ) ...
                    if (avatarData != null && !avatarData.isEmpty()) {
                        if (avatarData.startsWith("http")) {
                            Glide.with(MainActivity.this).load(avatarData).circleCrop().into(ivAvatar);
                        } else {
                            try {
                                if (avatarData.contains(",")) avatarData = avatarData.split(",")[1];
                                byte[] imageBytes = android.util.Base64.decode(avatarData, android.util.Base64.DEFAULT);
                                Glide.with(MainActivity.this).asBitmap().load(imageBytes).circleCrop().into(ivAvatar);
                            } catch (Exception e) { e.printStackTrace(); }
                        }
                    }
                }
            }
            @Override
            public void onFailure(Call<User> call, Throwable t) {
                Log.e("AVATAR_ERROR", "Lỗi: " + t.getMessage());
            }
        });
    }


    private void loadPostsFromServer() {
        SharedPreferences sharedPref = getSharedPreferences("BocketPrefs", Context.MODE_PRIVATE);
        String token = "Bearer " + sharedPref.getString("jwt_token", "");

        // Lấy ID của mình đã lưu từ bước trên, mặc định là -1 nếu chưa có
        int myId = sharedPref.getInt("user_id", -1);

        ApiService apiService = RetrofitClient.getApiService();
        apiService.getFriendPosts(token).enqueue(new Callback<PostResponse>() {
            @Override
            public void onResponse(Call<PostResponse> call, Response<PostResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    postList.clear();
                    postList.addAll(response.body().getData());

                    if (postAdapter == null) {
                        // --- TRUYỀN THÊM myId VÀO ĐÂY ---
                        postAdapter = new PostAdapter(MainActivity.this, postList, myId);
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
        if (swipeRefreshGrid != null && swipeRefreshGrid.getVisibility() == View.VISIBLE) {
            hideFeed();
        } else if (isFirendsListVisible) {
            hideFriendsList();
        } else if (rvFeed.getVisibility() == View.VISIBLE) {
            hideFeed();
        } else {
            super.onBackPressed();
        }
    }

    private void loadFriendsFromServer() {
        SharedPreferences sharedPref = getSharedPreferences("BocketPrefs", Context.MODE_PRIVATE);
        String token = "Bearer " + sharedPref.getString("jwt_token", "");

        int myID = sharedPref.getInt("user_id", -1);

        ApiService apiService = RetrofitClient.getApiService();
        apiService.getFriendsList(token).enqueue(new Callback<List<User>>() {
            @Override
            public void onResponse(Call<List<User>> call, Response<List<User>> response) {
                // Trong hàm loadFriendsFromServer()
                if (response.isSuccessful() && response.body() != null) {
                    friendsList.clear();

                    for(User u : response.body()){
                        if(u.getUserID() != myID){
                            friendsList.add(u);
                        }
                    }

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
        int myId = sharedPref.getInt("user_id", -1);

        ApiService apiService = RetrofitClient.getApiService();
        apiService.getPostsByUser(token, targetUserId).enqueue(new Callback<PostResponse>() {
            @Override
            public void onResponse(Call<PostResponse> call, Response<PostResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    postList.clear();
                    postList.addAll(response.body().getData());

                    if (postAdapter == null) {
                        postAdapter = new PostAdapter(MainActivity.this, postList, myId);
                        rvFeed.setAdapter(postAdapter);
                    } else {
                        postAdapter.notifyDataSetChanged();
                    }
                    rvFeed.scrollToPosition(0);
                    showFeed();
                }
            }
            @Override
            public void onFailure(Call<PostResponse> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showGridHistory() {
        // 1. Ẩn các thành phần không liên quan
        flCameraContainer.setVisibility(View.GONE);
        clControlsCamera.setVisibility(View.GONE);
        clControlsHistory.setVisibility(View.GONE);
        llHistory.setVisibility(View.GONE);
        rvFeed.setVisibility(View.GONE);

        // 2. Hiện Grid
        rvGridHistory.setVisibility(View.VISIBLE);
        clTopBar.setVisibility(View.VISIBLE); // Đảm bảo TopBar vẫn hiện

        // 3. Load dữ liệu (Sử dụng lại postList đã có hoặc gọi lại API)
        if (postList.isEmpty()) {
            loadPostsForGrid(); // Hàm gọi API tương tự loadPostsFromServer
        } else {
            updateGridAdapter();
        }
        // HIỆN CẢ CỤM SWIPE REFRESH
        swipeRefreshGrid.setVisibility(View.VISIBLE);
        clTopBar.setVisibility(View.VISIBLE);

        if (postList.isEmpty()) {
            loadPostsForGrid();
        } else {
            updateGridAdapter();
        }
    }

    private void updateGridAdapter() {
        gridAdapter = new GridPostAdapter(this, postList, position -> {
            // Khi bấm vào 1 ô vuông -> Chuyển sang xem Feed tại vị trí đó
            showFeed();
            rvFeed.scrollToPosition(position);
            rvGridHistory.setVisibility(View.GONE);
        });
        rvGridHistory.setAdapter(gridAdapter);
    }
    private void loadPostsForGrid() {
        SharedPreferences sharedPref = getSharedPreferences("BocketPrefs", Context.MODE_PRIVATE);
        String token = "Bearer " + sharedPref.getString("jwt_token", "");

        ApiService apiService = RetrofitClient.getApiService();
        apiService.getFriendPosts(token).enqueue(new Callback<PostResponse>() {
            @Override
            public void onResponse(Call<PostResponse> call, Response<PostResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    postList.clear();
                    postList.addAll(response.body().getData());
                    updateGridAdapter();
                }
            }

            @Override
            public void onFailure(Call<PostResponse> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Không thể tải lịch sử", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showBottomMenu(Post post, int position) {
        com.google.android.material.bottomsheet.BottomSheetDialog bottomSheetDialog =
                new com.google.android.material.bottomsheet.BottomSheetDialog(this);

        View view = getLayoutInflater().inflate(R.layout.layout_post_options, null);
        bottomSheetDialog.setContentView(view);

        LinearLayout llShare = view.findViewById(R.id.llShare);
        LinearLayout llDelete = view.findViewById(R.id.llDelete);

        // 1. Lấy ID của chính mình từ SharedPreferences
        SharedPreferences sharedPref = getSharedPreferences("BocketPrefs", Context.MODE_PRIVATE);
        int myUserId = sharedPref.getInt("user_id", -1);

        // 2. Lấy ID của người đăng từ đối tượng post
        int postOwnerId = post.getUserID();

        // 3. Log để kiểm tra (Xem trong Logcat)
        android.util.Log.d("BOCKET_DEBUG", "Của tôi: " + myUserId + " | Của bài đăng: " + postOwnerId);

        // 4. So sánh để hiện nút Xóa
        if (myUserId != -1 && myUserId == postOwnerId) {
            llDelete.setVisibility(View.VISIBLE);
        } else {
            llDelete.setVisibility(View.GONE);
        }

        llShare.setOnClickListener(v -> {
            sharePost(post);
            bottomSheetDialog.dismiss();
        });

        llDelete.setOnClickListener(v -> {
            confirmDelete(post, position);
            bottomSheetDialog.dismiss();
        });

        bottomSheetDialog.show();
    }


    private void sharePost(Post post) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, "Xem ảnh của tôi trên Bocket: " + post.getImageURL());
        startActivity(Intent.createChooser(intent, "Chia sẻ qua"));
    }

    private void confirmDelete(Post post, int position) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Xác nhận xóa")
                .setMessage("Bạn có chắc chắn muốn xóa bài viết này?")
                .setPositiveButton("Xóa", (d, w) -> {
                    deletePost(post.getPostID(), position);
                })
                .setNegativeButton("Hủy", null)
                .show();
    }


    private void deletePost(int postId, int position) {
        String token = "Bearer " + getSharedPreferences("BocketPrefs", MODE_PRIVATE).getString("jwt_token", "");

        RetrofitClient.getApiService().deletePost(token, postId).enqueue(new Callback<Void>() {

            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {

                Log.d("DELETE_DEBUG", "Response code: " + response.code());

                if (response.isSuccessful()) {
                    // 1. Tìm vị trí chính xác của bài viết trong list hiện tại
                    int actualPosition = -1;
                    for (int i = 0; i < postList.size(); i++) {
                        if (postList.get(i).getPostID() == postId) {
                            actualPosition = i;
                            break;
                        }
                    }

                    if (actualPosition != -1) {
                        final int finalPos = actualPosition;

                        // Xóa dữ liệu khỏi list trước
                        postList.remove(finalPos);

                        runOnUiThread(() -> {
                            if (postAdapter != null) {
                                postAdapter.notifyDataSetChanged();
                            }

                            if (gridAdapter != null) {
                                gridAdapter.notifyDataSetChanged();
                            }

                            if (postList.isEmpty()) {
                                hideFeed();
                            }
                        });

                        Toast.makeText(MainActivity.this, "Đã xóa bài viết", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(MainActivity.this, "Không có quyền xóa", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }
}