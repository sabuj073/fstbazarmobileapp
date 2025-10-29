package com.app.fstbazar;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.List;

public class BannerAdapter extends RecyclerView.Adapter<BannerAdapter.BannerViewHolder> {

    private final Context context;
    private final List<String> bannerUrls;

    public BannerAdapter(Context context, List<String> bannerUrls) {
        this.context = context;
        this.bannerUrls = bannerUrls;
    }

    @NonNull
    @Override
    public BannerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_banner, parent, false);
        return new BannerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BannerViewHolder holder, int position) {
        String url = bannerUrls.get(position);
        Glide.with(context).load(url).into(holder.imgBanner);
    }

    @Override
    public int getItemCount() {
        return bannerUrls.size();
    }

    public static class BannerViewHolder extends RecyclerView.ViewHolder {
        ImageView imgBanner;

        public BannerViewHolder(@NonNull View itemView) {
            super(itemView);
            imgBanner = itemView.findViewById(R.id.imgBanner);
        }
    }
}
