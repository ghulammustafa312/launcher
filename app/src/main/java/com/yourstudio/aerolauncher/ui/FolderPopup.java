package com.yourstudio.aerolauncher.ui;

import android.app.Dialog;
import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.animation.DecelerateInterpolator;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.yourstudio.aerolauncher.R;
import com.yourstudio.aerolauncher.launcher.AppInfo;
import com.yourstudio.aerolauncher.launcher.AppLauncher;
import com.yourstudio.aerolauncher.launcher.IconCache;
import com.yourstudio.aerolauncher.layout.HomeItem;
import com.yourstudio.aerolauncher.layout.LayoutRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Animated folder popup with rename support.
 */
public final class FolderPopup {

    private FolderPopup() {
    }

    public static void show(
            @NonNull Context context,
            @NonNull HomeItem folder,
            @NonNull Map<String, AppInfo> appsByKey,
            @NonNull IconCache iconCache,
            @NonNull LayoutRepository layoutRepository) {
        Dialog dialog = new Dialog(context, android.R.style.Theme_Translucent_NoTitleBar);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        View content = LayoutInflater.from(context).inflate(R.layout.dialog_folder, null, false);
        dialog.setContentView(content);

        EditText title = content.findViewById(R.id.folder_title);
        RecyclerView grid = content.findViewById(R.id.folder_grid);
        title.setText(folder.title);

        List<HomeItem> apps = new ArrayList<>();
        for (String key : folder.folderAppKeys) {
            apps.add(HomeItem.app(key));
        }

        AppGridAdapter adapter = new AppGridAdapter(iconCache, appsByKey);
        adapter.setCallbacks(new AppGridAdapter.Callbacks() {
            @Override
            public void onItemClick(@NonNull HomeItem item, @NonNull View anchor) {
                AppInfo app = item.appKey != null ? appsByKey.get(item.appKey) : null;
                if (app != null) {
                    AppLauncher.launch(context, app, anchor);
                    dialog.dismiss();
                }
            }

            @Override
            public void onItemLongClick(
                    @NonNull HomeItem item,
                    @NonNull com.yourstudio.aerolauncher.layout.ItemLocation location,
                    @NonNull View anchor) {
                // No edit mode inside folder popup for this step.
            }

            @Override
            public void onRemoveClick(
                    @NonNull com.yourstudio.aerolauncher.layout.ItemLocation location) {
            }

            @Override
            public void onItemDropped(
                    @NonNull com.yourstudio.aerolauncher.layout.ItemLocation from,
                    @NonNull com.yourstudio.aerolauncher.layout.ItemLocation to) {
            }

            @Override
            public boolean isEditMode() {
                return false;
            }

            @Override
            public void onDragStarted() {
            }
        });
        grid.setLayoutManager(new GridLayoutManager(context, 3));
        grid.setAdapter(adapter);
        adapter.submit(0, apps);

        title.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                layoutRepository.renameFolder(folder.id, s.toString());
            }
        });

        content.findViewById(R.id.folder_done).setOnClickListener(v -> dialog.dismiss());

        content.setScaleX(0.85f);
        content.setScaleY(0.85f);
        content.setAlpha(0f);
        dialog.setOnShowListener(d -> content.animate()
                .scaleX(1f)
                .scaleY(1f)
                .alpha(1f)
                .setDuration(220)
                .setInterpolator(new DecelerateInterpolator())
                .start());

        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }
}
