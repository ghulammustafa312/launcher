package com.yourstudio.aerolauncher;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.yourstudio.aerolauncher.launcher.AppInfo;
import com.yourstudio.aerolauncher.launcher.AppRepository;
import com.yourstudio.aerolauncher.launcher.IconCache;
import com.yourstudio.aerolauncher.layout.LayoutRepository;
import com.yourstudio.aerolauncher.ui.EdgeToEdge;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Lists apps removed from home via Hide, and lets the user unhide them.
 */
public class HiddenAppsActivity extends AppCompatActivity {

    private LayoutRepository layoutRepository;
    private AppRepository appRepository;
    private Adapter adapter;
    private TextView empty;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        ThemePreferences.applyAccentOverlay(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hidden_apps);
        EdgeToEdge.apply(this, findViewById(R.id.root));

        AeroLauncherApp app = (AeroLauncherApp) getApplication();
        layoutRepository = app.getLayoutRepository();
        appRepository = app.getAppRepository();

        empty = findViewById(R.id.hidden_empty);
        RecyclerView list = findViewById(R.id.hidden_list);
        adapter = new Adapter(appRepository.getIconCache(), key -> {
            layoutRepository.unhideApp(key);
            refresh();
        });
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);
        refresh();
    }

    private void refresh() {
        Set<String> hidden = layoutRepository.getLayout().hiddenAppKeys;
        List<AppInfo> rows = new ArrayList<>();
        for (AppInfo info : appRepository.getApps()) {
            if (hidden.contains(info.key)) {
                rows.add(info);
            }
        }
        adapter.submit(rows);
        empty.setVisibility(rows.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private static final class Adapter extends RecyclerView.Adapter<Adapter.Holder> {
        interface Unhide {
            void onUnhide(@NonNull String appKey);
        }

        private final IconCache iconCache;
        private final Unhide unhide;
        private final List<AppInfo> items = new ArrayList<>();

        Adapter(@NonNull IconCache iconCache, @NonNull Unhide unhide) {
            this.iconCache = iconCache;
            this.unhide = unhide;
        }

        void submit(@NonNull List<AppInfo> next) {
            items.clear();
            items.addAll(next);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_hidden_app, parent, false);
            return new Holder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            AppInfo app = items.get(position);
            holder.label.setText(app.label);
            iconCache.loadInto(app, holder.icon);
            holder.unhide.setOnClickListener(v -> unhide.onUnhide(app.key));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static final class Holder extends RecyclerView.ViewHolder {
            final ImageView icon;
            final TextView label;
            final MaterialButton unhide;

            Holder(@NonNull View itemView) {
                super(itemView);
                icon = itemView.findViewById(R.id.hidden_icon);
                label = itemView.findViewById(R.id.hidden_label);
                unhide = itemView.findViewById(R.id.hidden_unhide);
            }
        }
    }
}
