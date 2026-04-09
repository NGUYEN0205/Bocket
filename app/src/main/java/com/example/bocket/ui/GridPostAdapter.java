package com.example.bocket.ui;

import static androidx.core.util.TypedValueCompat.dpToPx;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.example.bocket.R;
import com.example.bocket.model.Post;

import java.util.List;

public class GridPostAdapter extends RecyclerView.Adapter<GridPostAdapter.ViewHolder> {
    private Context context;
    private List<Post> list;
    private OnItemClickListener listener;

    public interface OnItemClickListener { void onItemClick(int position); }

    public GridPostAdapter(Context context, List<Post> list, OnItemClickListener listener) {
        this.context = context;
        this.list = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ImageView imageView = new ImageView(context);
        int screenWidth = context.getResources().getDisplayMetrics().widthPixels;
        int spacingPx = dpToPx(context, 3);
        int itemSize = (screenWidth - (spacingPx * 4)) / 3;
        GridLayoutManager.LayoutParams params = new GridLayoutManager.LayoutParams(itemSize, itemSize);
        params.setMargins(spacingPx, spacingPx, spacingPx, spacingPx);
        imageView.setLayoutParams(params);

        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        imageView.setBackgroundColor(android.graphics.Color.parseColor("#1E1E1E"));

        return new ViewHolder(imageView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String imageUrl = list.get(position).getImageURL();

        Glide.with(context)
                .load(imageUrl)
                .placeholder(R.drawable.ic_avatar_placeholder)
                // CenterCrop để ảnh đầy ô vuông, RoundedCorners để bo góc
                .transform(new com.bumptech.glide.load.resource.bitmap.CenterCrop(),
                        new com.bumptech.glide.load.resource.bitmap.RoundedCorners(dpToPx(context, 16)))
                .into((ImageView) holder.itemView);

        holder.itemView.setOnClickListener(v -> listener.onItemClick(position));
    }

    @Override
    public int getItemCount() { return list.size(); }
    public class ViewHolder extends RecyclerView.ViewHolder {
        public ViewHolder(@NonNull View itemView) { super(itemView); }
    }
    private int dpToPx(Context context, int dp) {
        float density = context.getResources().getDisplayMetrics().density;
        // Ép kiểu float về int sau khi làm tròn
        return (int) (dp * density + 0.5f);
    }
}
