package com.yourstudio.aerolauncher.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.yourstudio.aerolauncher.R;
import com.yourstudio.aerolauncher.launcher.AppInfo;
import com.yourstudio.aerolauncher.launcher.AppLauncher;
import com.yourstudio.aerolauncher.launcher.IconCache;

import java.util.ArrayList;
import java.util.List;

/**
 * Single home-page grid of app icons.
 */
public final class AppGridAdapter extends RecyclerView.Adapter<AppGridAdapter.Holder> {

    public interface LongClickListener {
        void onAppLongClick(@NonNull AppInfo app, @NonNull View anchor);
    }

    private final IconCache iconCache;
    private final List<AppInfo> items = new ArrayList<>();
    private LongClickListener longClickListener;

    public AppGridAdapter(@NonNull IconCache iconCache) {
        this.iconCache = iconCache;
        setHasStableIds(true);
    }

    public void setLongClickListener(@NonNull LongClickListener listener) {
        this.longClickListener = listener;
    }

    public void submit(@NonNull List<AppInfo> apps) {
        items.clear();
        items.addAll(apps);
        notifyDataSetChanged();
    }

    @Override
    public long getItemId(int position) {
        return items.get(position).key.hashCode();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_app_icon, parent, false);
        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        AppInfo app = items.get(position);
        holder.label.setText(app.label);
        iconCache.loadInto(app, holder.icon);
        holder.itemView.setOnClickListener(v ->
                AppLauncher.launch(v.getContext(), app, holder.icon));
        holder.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) {
                longClickListener.onAppLongClick(app, v);
                return true;
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static final class Holder extends RecyclerView.ViewHolder {
        final ImageView icon;
        final TextView label;

        Holder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.app_icon);
            label = itemView.findViewById(R.id.app_label);
        }
    }
}
