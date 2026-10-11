package com.yourstudio.aerolauncher.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.yourstudio.aerolauncher.R;
import com.yourstudio.aerolauncher.launcher.AppInfo;
import com.yourstudio.aerolauncher.launcher.IconCache;
import com.yourstudio.aerolauncher.layout.HomeItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * ViewPager2 pages: workspace grids, then a trailing App Library page.
 */
public final class HomePagerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HOME = 0;
    private static final int TYPE_LIBRARY = 1;

    private final IconCache iconCache;
    private final Map<String, AppInfo> appsByKey;
    private final List<List<HomeItem>> pages = new ArrayList<>();
    private final List<AppInfo> installedApps = new ArrayList<>();
    private final Set<String> hiddenKeys = new HashSet<>();
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

    public void setLibraryData(
            @NonNull List<AppInfo> apps,
            @NonNull Set<String> hidden) {
        installedApps.clear();
        installedApps.addAll(apps);
        hiddenKeys.clear();
        hiddenKeys.addAll(hidden);
        // Refresh only the library page when present.
        int libraryIndex = pages.size();
        if (libraryIndex >= 0) {
            notifyItemChanged(libraryIndex);
        }
    }

    public void refreshEditMode() {
        notifyItemRangeChanged(0, pages.size());
    }

    /** Workspace pages + one App Library page. */
    public int getPageCount() {
        return pages.size() + 1;
    }

    public int getLibraryPageIndex() {
        return pages.size();
    }

    public boolean isLibraryPage(int position) {
        return position >= pages.size();
    }

    @Override
    public int getItemViewType(int position) {
        return isLibraryPage(position) ? TYPE_LIBRARY : TYPE_HOME;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_LIBRARY) {
            View view = inflater.inflate(R.layout.page_app_library, parent, false);
            return new LibraryHolder(view, iconCache);
        }
        View view = inflater.inflate(R.layout.page_home_grid, parent, false);
        return new PageHolder(view, iconCache, appsByKey, columns);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof LibraryHolder) {
            ((LibraryHolder) holder).bind(installedApps, hiddenKeys);
            return;
        }
        ((PageHolder) holder).bind(position, pages.get(position), columns, callbacks);
    }

    @Override
    public int getItemCount() {
        return getPageCount();
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

    static final class LibraryHolder extends RecyclerView.ViewHolder {
        private final RecyclerView list;
        private final FastScrollerView scroller;
        private final AppLibraryAdapter adapter;

        LibraryHolder(@NonNull View itemView, @NonNull IconCache iconCache) {
            super(itemView);
            list = itemView.findViewById(R.id.library_list);
            scroller = itemView.findViewById(R.id.fast_scroller);
            adapter = new AppLibraryAdapter(iconCache);
            list.setLayoutManager(new LinearLayoutManager(itemView.getContext()));
            list.setAdapter(adapter);
            scroller.setListener(letter ->
                    FastScrollerView.scrollToLetter(list, letter, adapter));
        }

        void bind(@NonNull List<AppInfo> apps, @NonNull Set<String> hidden) {
            adapter.submit(itemView.getContext(), apps, hidden);
        }
    }
}
