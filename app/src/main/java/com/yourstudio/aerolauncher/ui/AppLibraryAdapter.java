package com.yourstudio.aerolauncher.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.yourstudio.aerolauncher.R;
import com.yourstudio.aerolauncher.launcher.AppCategory;
import com.yourstudio.aerolauncher.launcher.AppInfo;
import com.yourstudio.aerolauncher.launcher.AppLauncher;
import com.yourstudio.aerolauncher.launcher.IconCache;
import com.yourstudio.aerolauncher.library.AppLibraryModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * App Library list: recently added, category sections, then A–Z with letter index.
 */
public final class AppLibraryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>
        implements FastScrollerView.LetterIndex {

    private static final int TYPE_RECENT = 0;
    private static final int TYPE_HEADER = 1;
    private static final int TYPE_APP = 2;

    private final IconCache iconCache;
    private final List<Row> rows = new ArrayList<>();
    private final Map<String, Integer> letterPositions = new HashMap<>();

    public AppLibraryAdapter(@NonNull IconCache iconCache) {
        this.iconCache = iconCache;
    }

    public void submit(
            @NonNull android.content.Context context,
            @NonNull List<AppInfo> installed,
            @NonNull Set<String> hiddenKeys) {
        List<AppInfo> visible = AppLibraryModel.visibleApps(installed, hiddenKeys);
        rows.clear();
        letterPositions.clear();

        List<AppInfo> recent = AppLibraryModel.recentlyAdded(visible);
        if (!recent.isEmpty()) {
            rows.add(Row.recent(context.getString(R.string.library_recently_added), recent));
        }

        Map<AppCategory, List<AppInfo>> byCategory = AppLibraryModel.byCategory(visible);
        for (AppCategory category : AppCategory.values()) {
            List<AppInfo> apps = byCategory.get(category);
            if (apps == null || apps.isEmpty()) {
                continue;
            }
            rows.add(Row.header(context.getString(category.titleRes)));
            for (AppInfo app : apps) {
                rows.add(Row.app(app));
            }
        }

        rows.add(Row.header(context.getString(R.string.library_alphabetical)));
        List<AppInfo> alpha = AppLibraryModel.alphabetical(visible);
        String lastLetter = null;
        for (AppInfo app : alpha) {
            String letter = AppLibraryModel.letterFor(app);
            if (!letter.equals(lastLetter)) {
                letterPositions.put(letter, rows.size());
                lastLetter = letter;
            }
            rows.add(Row.app(app));
        }
        notifyDataSetChanged();
    }

    @Nullable
    @Override
    public Integer positionFor(@NonNull String letter) {
        return letterPositions.get(letter);
    }

    @Override
    public int getItemViewType(int position) {
        return rows.get(position).type;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_RECENT) {
            return new RecentHolder(inflater.inflate(R.layout.item_library_recent_row, parent, false),
                    iconCache);
        }
        if (viewType == TYPE_HEADER) {
            return new HeaderHolder(inflater.inflate(R.layout.item_library_header, parent, false));
        }
        return new AppHolder(inflater.inflate(R.layout.item_library_app_row, parent, false), iconCache);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Row row = rows.get(position);
        if (holder instanceof RecentHolder) {
            ((RecentHolder) holder).bind(row);
        } else if (holder instanceof HeaderHolder) {
            ((HeaderHolder) holder).title.setText(row.title);
        } else if (holder instanceof AppHolder && row.app != null) {
            ((AppHolder) holder).bind(row.app);
        }
    }

    @Override
    public void onViewRecycled(@NonNull RecyclerView.ViewHolder holder) {
        if (holder instanceof AppHolder) {
            ((AppHolder) holder).recycle();
        }
        super.onViewRecycled(holder);
    }

    @Override
    public int getItemCount() {
        return rows.size();
    }

    private static final class Row {
        final int type;
        @Nullable
        final String title;
        @Nullable
        final AppInfo app;
        @Nullable
        final List<AppInfo> recentApps;

        private Row(int type, @Nullable String title, @Nullable AppInfo app,
                    @Nullable List<AppInfo> recentApps) {
            this.type = type;
            this.title = title;
            this.app = app;
            this.recentApps = recentApps;
        }

        static Row header(@NonNull String title) {
            return new Row(TYPE_HEADER, title, null, null);
        }

        static Row app(@NonNull AppInfo app) {
            return new Row(TYPE_APP, null, app, null);
        }

        static Row recent(@NonNull String title, @NonNull List<AppInfo> apps) {
            return new Row(TYPE_RECENT, title, null, apps);
        }
    }

    static final class HeaderHolder extends RecyclerView.ViewHolder {
        final TextView title;

        HeaderHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.section_title);
        }
    }

    static final class AppHolder extends RecyclerView.ViewHolder {
        final ImageView icon;
        final TextView label;
        final IconCache iconCache;

        AppHolder(@NonNull View itemView, @NonNull IconCache iconCache) {
            super(itemView);
            this.iconCache = iconCache;
            icon = itemView.findViewById(R.id.app_icon);
            label = itemView.findViewById(R.id.app_label);
        }

        void bind(@NonNull AppInfo app) {
            if (app.workProfile) {
                label.setText(app.label.toString()
                        + itemView.getContext().getString(R.string.work_profile_suffix));
            } else {
                label.setText(app.label);
            }
            iconCache.loadInto(app, icon);
            itemView.setOnClickListener(v -> AppLauncher.launch(v.getContext(), app, icon));
        }

        void recycle() {
            iconCache.clearTarget(icon);
        }
    }

    static final class RecentHolder extends RecyclerView.ViewHolder {
        final TextView title;
        final RecyclerView grid;
        final MiniGridAdapter adapter;

        RecentHolder(@NonNull View itemView, @NonNull IconCache iconCache) {
            super(itemView);
            title = itemView.findViewById(R.id.section_title);
            grid = itemView.findViewById(R.id.recent_grid);
            adapter = new MiniGridAdapter(iconCache);
            grid.setLayoutManager(new GridLayoutManager(itemView.getContext(), 4));
            grid.setAdapter(adapter);
        }

        void bind(@NonNull Row row) {
            title.setText(row.title);
            adapter.submit(row.recentApps != null ? row.recentApps : new ArrayList<>());
        }
    }

    private static final class MiniGridAdapter extends RecyclerView.Adapter<MiniGridAdapter.Holder> {
        private final IconCache iconCache;
        private final List<AppInfo> apps = new ArrayList<>();

        MiniGridAdapter(@NonNull IconCache iconCache) {
            this.iconCache = iconCache;
        }

        void submit(@NonNull List<AppInfo> items) {
            apps.clear();
            apps.addAll(items);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_app_icon, parent, false);
            view.findViewById(R.id.remove_badge).setVisibility(View.GONE);
            Holder holder = new Holder(view);
            AppGridAdapter.applyLabelShadow(holder.label);
            return holder;
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            AppInfo app = apps.get(position);
            if (app.workProfile) {
                holder.label.setText(app.label.toString()
                        + holder.itemView.getContext().getString(R.string.work_profile_suffix));
            } else {
                holder.label.setText(app.label);
            }
            holder.folderPreview.setVisibility(View.GONE);
            holder.icon.setVisibility(View.VISIBLE);
            iconCache.loadInto(app, holder.icon);
            holder.itemView.setOnClickListener(v ->
                    AppLauncher.launch(v.getContext(), app, holder.icon));
        }

        @Override
        public void onViewRecycled(@NonNull Holder holder) {
            iconCache.clearTarget(holder.icon);
            super.onViewRecycled(holder);
        }

        @Override
        public int getItemCount() {
            return apps.size();
        }

        static final class Holder extends RecyclerView.ViewHolder {
            final ImageView icon;
            final ImageView folderPreview;
            final TextView label;

            Holder(@NonNull View itemView) {
                super(itemView);
                icon = itemView.findViewById(R.id.app_icon);
                folderPreview = itemView.findViewById(R.id.folder_preview);
                label = itemView.findViewById(R.id.app_label);
            }
        }
    }
}
