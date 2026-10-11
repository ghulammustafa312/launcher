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
import com.yourstudio.aerolauncher.search.SearchController;
import com.yourstudio.aerolauncher.search.SettingsShortcut;

import java.util.ArrayList;
import java.util.List;

public final class SearchResultsAdapter extends RecyclerView.Adapter<SearchResultsAdapter.Holder> {

    private static final int TYPE_APP = 0;
    private static final int TYPE_SETTINGS = 1;
    private static final int TYPE_WEB = 2;

    public interface Listener {
        void onResultOpened();
    }

    private final IconCache iconCache;
    private final Listener listener;
    private final List<Row> rows = new ArrayList<>();

    public SearchResultsAdapter(@NonNull IconCache iconCache, @NonNull Listener listener) {
        this.iconCache = iconCache;
        this.listener = listener;
    }

    public void submit(@NonNull SearchController.Results results) {
        rows.clear();
        for (AppInfo app : results.apps) {
            rows.add(Row.app(app));
        }
        for (SettingsShortcut shortcut : results.settings) {
            rows.add(Row.settings(shortcut));
        }
        if (!results.query.isEmpty()) {
            rows.add(Row.web(results.query));
        }
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return rows.get(position).type;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_search_result, parent, false);
        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        Row row = rows.get(position);
        if (row.type == TYPE_APP && row.app != null) {
            holder.title.setText(row.app.label);
            holder.subtitle.setText(R.string.search_section_apps);
            iconCache.loadInto(row.app, holder.icon);
            holder.itemView.setOnClickListener(v -> {
                AppLauncher.launch(v.getContext(), row.app, holder.icon);
                listener.onResultOpened();
            });
            return;
        }
        if (row.type == TYPE_SETTINGS && row.shortcut != null) {
            holder.title.setText(row.shortcut.title(holder.itemView.getContext()));
            holder.subtitle.setText(R.string.search_section_settings);
            holder.icon.setImageResource(R.drawable.ic_settings_gear);
            holder.itemView.setOnClickListener(v -> {
                SearchController.openSettingsShortcut(v.getContext(), row.shortcut);
                listener.onResultOpened();
            });
            return;
        }
        holder.title.setText(holder.itemView.getContext()
                .getString(R.string.search_web_for, row.webQuery));
        holder.subtitle.setText(R.string.search_section_web);
        holder.icon.setImageResource(R.drawable.ic_web_search);
        holder.itemView.setOnClickListener(v -> {
            SearchController.openWebSearch(v.getContext(), row.webQuery);
            listener.onResultOpened();
        });
    }

    @Override
    public int getItemCount() {
        return rows.size();
    }

    private static final class Row {
        final int type;
        final AppInfo app;
        final SettingsShortcut shortcut;
        final String webQuery;

        private Row(int type, AppInfo app, SettingsShortcut shortcut, String webQuery) {
            this.type = type;
            this.app = app;
            this.shortcut = shortcut;
            this.webQuery = webQuery;
        }

        static Row app(@NonNull AppInfo app) {
            return new Row(TYPE_APP, app, null, null);
        }

        static Row settings(@NonNull SettingsShortcut shortcut) {
            return new Row(TYPE_SETTINGS, null, shortcut, null);
        }

        static Row web(@NonNull String query) {
            return new Row(TYPE_WEB, null, null, query);
        }
    }

    static final class Holder extends RecyclerView.ViewHolder {
        final ImageView icon;
        final TextView title;
        final TextView subtitle;

        Holder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.result_icon);
            title = itemView.findViewById(R.id.result_title);
            subtitle = itemView.findViewById(R.id.result_subtitle);
        }
    }
}
