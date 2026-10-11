package com.yourstudio.aerolauncher.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.yourstudio.aerolauncher.R;
import com.yourstudio.aerolauncher.launcher.AppInfo;
import com.yourstudio.aerolauncher.launcher.IconCache;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * ViewPager2 pages; each page hosts a RecyclerView app grid.
 */
public final class HomePagerAdapter extends RecyclerView.Adapter<HomePagerAdapter.PageHolder> {

    private final IconCache iconCache;
    private final List<List<AppInfo>> pages = new ArrayList<>();
    private int columns = 4;

    public HomePagerAdapter(@NonNull IconCache iconCache) {
        this.iconCache = iconCache;
    }

    public void setPages(@NonNull List<List<AppInfo>> newPages, int columns) {
        this.columns = Math.max(1, columns);
        pages.clear();
        pages.addAll(newPages);
        if (pages.isEmpty()) {
            pages.add(Collections.emptyList());
        }
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
        return new PageHolder(view, iconCache, columns);
    }

    @Override
    public void onBindViewHolder(@NonNull PageHolder holder, int position) {
        holder.bind(pages.get(position), columns);
    }

    @Override
    public int getItemCount() {
        return pages.size();
    }

    static final class PageHolder extends RecyclerView.ViewHolder {
        private final RecyclerView grid;
        private final AppGridAdapter adapter;

        PageHolder(@NonNull View itemView, @NonNull IconCache iconCache, int columns) {
            super(itemView);
            grid = itemView.findViewById(R.id.page_grid);
            adapter = new AppGridAdapter(iconCache);
            grid.setLayoutManager(new GridLayoutManager(itemView.getContext(), columns));
            grid.setAdapter(adapter);
            grid.setOverScrollMode(View.OVER_SCROLL_NEVER);
            // Nested scrolling off keeps vertical scroll calm inside ViewPager2.
            grid.setNestedScrollingEnabled(false);
        }

        void bind(@NonNull List<AppInfo> apps, int columns) {
            GridLayoutManager layoutManager = (GridLayoutManager) grid.getLayoutManager();
            if (layoutManager != null && layoutManager.getSpanCount() != columns) {
                layoutManager.setSpanCount(columns);
            }
            adapter.submit(apps);
        }
    }
}
