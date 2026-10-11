package com.yourstudio.aerolauncher;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.checkbox.MaterialCheckBox;
import com.yourstudio.aerolauncher.focus.FocusPreferences;
import com.yourstudio.aerolauncher.launcher.AppInfo;
import com.yourstudio.aerolauncher.launcher.AppRepository;
import com.yourstudio.aerolauncher.launcher.IconCache;
import com.yourstudio.aerolauncher.ui.EdgeToEdge;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Choose Work / Study / Sleep and which apps each mode hides on home.
 */
public class FocusModeActivity extends AppCompatActivity {

    private String editingMode = FocusPreferences.MODE_WORK;
    private FocusAppAdapter adapter;
    private View pickLabel;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        ThemePreferences.applyAccentOverlay(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_focus);
        EdgeToEdge.apply(this, findViewById(R.id.root));

        AppRepository appRepository = ((AeroLauncherApp) getApplication()).getAppRepository();
        pickLabel = findViewById(R.id.focus_pick_label);
        RecyclerView list = findViewById(R.id.focus_app_list);
        adapter = new FocusAppAdapter(appRepository.getIconCache(), appRepository.getApps());
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);

        RadioGroup group = findViewById(R.id.focus_mode_group);
        String mode = FocusPreferences.getMode(this);
        if (FocusPreferences.MODE_WORK.equals(mode)) {
            group.check(R.id.focus_work);
            editingMode = FocusPreferences.MODE_WORK;
        } else if (FocusPreferences.MODE_STUDY.equals(mode)) {
            group.check(R.id.focus_study);
            editingMode = FocusPreferences.MODE_STUDY;
        } else if (FocusPreferences.MODE_SLEEP.equals(mode)) {
            group.check(R.id.focus_sleep);
            editingMode = FocusPreferences.MODE_SLEEP;
        } else {
            group.check(R.id.focus_off);
            editingMode = FocusPreferences.MODE_OFF;
        }
        refreshListVisibility();
        if (!FocusPreferences.MODE_OFF.equals(editingMode)) {
            adapter.bindMode(this, editingMode);
        }

        group.setOnCheckedChangeListener((g, checkedId) -> {
            if (checkedId == R.id.focus_off) {
                FocusPreferences.setMode(this, FocusPreferences.MODE_OFF);
                editingMode = FocusPreferences.MODE_OFF;
            } else if (checkedId == R.id.focus_work) {
                FocusPreferences.setMode(this, FocusPreferences.MODE_WORK);
                editingMode = FocusPreferences.MODE_WORK;
            } else if (checkedId == R.id.focus_study) {
                FocusPreferences.setMode(this, FocusPreferences.MODE_STUDY);
                editingMode = FocusPreferences.MODE_STUDY;
            } else if (checkedId == R.id.focus_sleep) {
                FocusPreferences.setMode(this, FocusPreferences.MODE_SLEEP);
                editingMode = FocusPreferences.MODE_SLEEP;
            }
            refreshListVisibility();
            if (!FocusPreferences.MODE_OFF.equals(editingMode)) {
                adapter.bindMode(this, editingMode);
            }
        });
    }

    private void refreshListVisibility() {
        boolean show = !FocusPreferences.MODE_OFF.equals(editingMode);
        pickLabel.setVisibility(show ? View.VISIBLE : View.GONE);
        findViewById(R.id.focus_app_list).setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private static final class FocusAppAdapter extends RecyclerView.Adapter<FocusAppAdapter.Holder> {
        private final IconCache iconCache;
        private final List<AppInfo> apps;
        private String mode = FocusPreferences.MODE_WORK;
        private Set<String> selected;

        FocusAppAdapter(@NonNull IconCache iconCache, @NonNull List<AppInfo> apps) {
            this.iconCache = iconCache;
            this.apps = new ArrayList<>(apps);
        }

        void bindMode(@NonNull android.content.Context context, @NonNull String mode) {
            this.mode = mode;
            this.selected = FocusPreferences.getAppsForMode(context, mode);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_focus_app, parent, false);
            return new Holder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            AppInfo app = apps.get(position);
            holder.label.setText(app.label);
            iconCache.loadInto(app, holder.icon);
            holder.check.setOnCheckedChangeListener(null);
            holder.check.setChecked(selected != null && selected.contains(app.key));
            holder.check.setOnCheckedChangeListener((buttonView, isChecked) -> {
                FocusPreferences.toggleAppInMode(
                        holder.itemView.getContext(), mode, app.key, isChecked);
                if (selected != null) {
                    if (isChecked) {
                        selected.add(app.key);
                    } else {
                        selected.remove(app.key);
                    }
                }
            });
            holder.itemView.setOnClickListener(v -> holder.check.toggle());
        }

        @Override
        public int getItemCount() {
            return apps.size();
        }

        static final class Holder extends RecyclerView.ViewHolder {
            final ImageView icon;
            final TextView label;
            final MaterialCheckBox check;

            Holder(@NonNull View itemView) {
                super(itemView);
                icon = itemView.findViewById(R.id.focus_icon);
                label = itemView.findViewById(R.id.focus_label);
                check = itemView.findViewById(R.id.focus_check);
            }
        }
    }
}
