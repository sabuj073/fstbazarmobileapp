package com.app.fstbazar;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.List;

public class SiteAdapter extends RecyclerView.Adapter<SiteAdapter.ViewHolder> {

    Context context;
    List<SiteModel> sites;

    public SiteAdapter(Context context, List<SiteModel> sites) {
        this.context = context;
        this.sites = sites;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_site, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SiteModel model = sites.get(position);
        holder.txtName.setText(model.getName());
        Glide.with(context).load(model.getLogo()).into(holder.imgLogo);

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(model.getLink()));
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return sites.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgLogo;
        TextView txtName;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgLogo = itemView.findViewById(R.id.imgLogo);
            txtName = itemView.findViewById(R.id.txtName);
        }
    }
}
