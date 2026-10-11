package com.yourstudio.aerolauncher.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.yourstudio.aerolauncher.R;
import com.yourstudio.aerolauncher.launcher.AppInfo;
import com.yourstudio.aerolauncher.launcher.IconCache;
import com.yourstudio.aerolauncher.layout.HomeItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * ViewPager2 pages; each page hosts a fixed RecyclerView app grid.
 */
public final class HomePagerAdapter extends RecyclerView.Adapter<HomePagerAdapter.PageHolder> {

    private final IconCache iconCache;
    private final Map<String, AppInfo> appsByKey;
    private final List<List<HomeItem>> pages = new ArrayList<>();
    private int columns = 4;
    private AppGridAdapter.Callbacks callbacks;

    public HomePagerAdapter(
            @NonNull IconCache iconCache,
            @NonNull Map<String, AppInfo> appsByKey) {
        this.iconCache = iconCache;
        this.appsByKey = appsByKey;
    }

    public void setCallbacks(@Nullable AppGridAdapter.Callbacks callbacks) {
        this.callbacks = callbacks;
        notifyDataSetChanged();
    }

    public void setPages(@NonNull List<List<HomeItem>> newPages, int columns) {
        this.columns = Math.max(1, columns);
        pages.clear();
        pages.addAll(newPages);
        if (pages.isEmpty()) {
            pages.add(Collections.emptyList());
        }
        notifyDataSetChanged();
    }

    public void refreshEditMode() {
        notifyDataSetChanged();
    }

    public int getPageCount() {
        return pages.size();
    }

    @NonNull
    @Override
    public PageHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.page_home_grid, parent, false);
        return new PageHolder(view, iconCache, appsByKey, columns);
    }

    @Override
    public void onBindViewHolder(@NonNull PageHolder holder, int position) {
        holder.bind(position, pages.get(position), columns, callbacks);
    }

    @Override
    public int getItemCount() {
        return pages.size();
    }

    static final class PageHolder extends RecyclerView.ViewHolder {
        private final RecyclerView grid;
        private final AppGridAdapter adapter;

        PageHolder(
                @NonNull View itemView,
                @NonNull IconCache iconCache,
                @NonNull Map<String, AppInfo> appsByKey,
                int columns) {
            super(itemView);
            grid = itemView.findViewById(R.id.page_grid);
            adapter = new AppGridAdapter(iconCache, appsByKey);
            grid.setLayoutManager(new GridLayoutManager(itemView.getContext(), columns));
            grid.setAdapter(adapter);
            grid.setOverScrollMode(View.OVER_SCROLL_NEVER);
            grid.setNestedScrollingEnabled(false);
        }

        void bind(
                int pageIndex,
                @NonNull List<HomeItem> cells,
                int columns,
                @Nullable AppGridAdapter.Callbacks callbacks) {
            GridLayoutManager layoutManager = (GridLayoutManager) grid.getLayoutManager();
            if (layoutManager != null && layoutManager.getSpanCount() != columns) {
                layoutManager.setSpanCount(columns);
            }
            adapter.setCallbacks(callbacks);
            adapter.submit(pageIndex, cells);
        }
    }
}
