package com.yourstudio.aerolauncher.ui;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.os.Build;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.yourstudio.aerolauncher.R;
import com.yourstudio.aerolauncher.launcher.AppInfo;
import com.yourstudio.aerolauncher.launcher.AppLauncher;
import com.yourstudio.aerolauncher.launcher.IconCache;
import com.yourstudio.aerolauncher.layout.HomeItem;
import com.yourstudio.aerolauncher.layout.ItemLocation;
import com.yourstudio.aerolauncher.layout.LayoutRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Folder sheet: scale-and-blur open, 3×3 pages of apps.
 */
public final class FolderPopup {

    private static final int PAGE_SIZE = 9;

    private FolderPopup() {
    }

    public static void show(
            @NonNull android.content.Context context,
            @NonNull HomeItem folder,
            @NonNull Map<String, AppInfo> appsByKey,
            @NonNull IconCache iconCache,
            @NonNull LayoutRepository layoutRepository) {
        Activity activity = (Activity) context;
        View backdrop = activity.findViewById(R.id.root);
        boolean allowMotion = !Motion.reduced(context);
        if (allowMotion && backdrop != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            blurBackdrop(backdrop);
        }

        Dialog dialog = new Dialog(context, android.R.style.Theme_Translucent_NoTitleBar);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        View content = LayoutInflater.from(context).inflate(R.layout.dialog_folder, null, false);
        dialog.setContentView(content);
        dialog.setOnDismissListener(d -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                clearBackdrop(backdrop);
            }
        });

        EditText title = content.findViewById(R.id.folder_title);
        ViewPager2 pager = content.findViewById(R.id.folder_pager);
        LinearLayout dots = content.findViewById(R.id.folder_page_dots);
        title.setText(folder.title);

        List<List<HomeItem>> pages = pagesOf(folder);
        AppGridAdapter.Callbacks callbacks = folderCallbacks(context, appsByKey, dialog);
        pager.setAdapter(new PageAdapter(pages, iconCache, appsByKey, callbacks));
        bindDots(dots, pages.size(), 0);
        pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                for (int i = 0; i < dots.getChildCount(); i++) {
                    dots.getChildAt(i).setSelected(i == position);
                }
            }
        });

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

        if (allowMotion) {
            content.setScaleX(0.82f);
            content.setScaleY(0.82f);
            dialog.setOnShowListener(d -> Motion.springScale(content, 1f));
        }

        dialog.show();
        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT);
            window.setGravity(Gravity.CENTER);
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            window.setDimAmount(allowMotion ? 0.12f : 0.45f);
        }
    }

    private static void blurBackdrop(@NonNull View backdrop) {
        float radius = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                18f,
                backdrop.getResources().getDisplayMetrics());
        backdrop.setRenderEffect(RenderEffect.createBlurEffect(
                radius, radius, Shader.TileMode.CLAMP));
    }

    private static void clearBackdrop(View backdrop) {
        if (backdrop != null) {
            backdrop.setRenderEffect(null);
        }
    }

    @NonNull
    private static List<List<HomeItem>> pagesOf(@NonNull HomeItem folder) {
        List<HomeItem> apps = new ArrayList<>();
        for (String key : folder.folderAppKeys) {
            apps.add(HomeItem.app(key));
        }
        if (apps.isEmpty()) {
            apps.add(null);
        }
        List<List<HomeItem>> pages = new ArrayList<>();
        for (int start = 0; start < apps.size(); start += PAGE_SIZE) {
            List<HomeItem> page = new ArrayList<>();
            int end = Math.min(apps.size(), start + PAGE_SIZE);
            page.addAll(apps.subList(start, end));
            while (page.size() < PAGE_SIZE) {
                page.add(null);
            }
            pages.add(page);
        }
        return pages;
    }

    private static void bindDots(@NonNull LinearLayout dots, int count, int selected) {
        dots.removeAllViews();
        dots.setVisibility(count > 1 ? View.VISIBLE : View.GONE);
        float density = dots.getResources().getDisplayMetrics().density;
        int dot = Math.round(8f * density);
        int margin = Math.round(4f * density);
        for (int i = 0; i < count; i++) {
            View dotView = new View(dots.getContext());
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dot, dot);
            params.setMarginStart(margin);
            params.setMarginEnd(margin);
            dotView.setLayoutParams(params);
            dotView.setBackgroundResource(R.drawable.page_dot);
            dotView.setSelected(i == selected);
            dots.addView(dotView);
        }
    }

    @NonNull
    private static AppGridAdapter.Callbacks folderCallbacks(
            @NonNull android.content.Context context,
            @NonNull Map<String, AppInfo> appsByKey,
            @NonNull Dialog dialog) {
        return new AppGridAdapter.Callbacks() {
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
                    @NonNull ItemLocation location,
                    @NonNull View anchor) {
            }

            @Override
            public void onRemoveClick(@NonNull ItemLocation location) {
            }

            @Override
            public void onItemDropped(@NonNull ItemLocation from, @NonNull ItemLocation to) {
            }

            @Override
            public boolean isEditMode() {
                return false;
            }

            @Override
            public void onDragStarted() {
            }
        };
    }

    private static final class PageAdapter extends RecyclerView.Adapter<PageAdapter.Holder> {
        private final List<List<HomeItem>> pages;
        private final IconCache iconCache;
        private final Map<String, AppInfo> appsByKey;
        private final AppGridAdapter.Callbacks callbacks;

        PageAdapter(
                @NonNull List<List<HomeItem>> pages,
                @NonNull IconCache iconCache,
                @NonNull Map<String, AppInfo> appsByKey,
                @NonNull AppGridAdapter.Callbacks callbacks) {
            this.pages = pages;
            this.iconCache = iconCache;
            this.appsByKey = appsByKey;
            this.callbacks = callbacks;
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            RecyclerView grid = new RecyclerView(parent.getContext());
            grid.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));
            grid.setLayoutManager(new GridLayoutManager(parent.getContext(), 3));
            grid.setNestedScrollingEnabled(false);
            grid.setOverScrollMode(View.OVER_SCROLL_NEVER);
            return new Holder(grid);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            if (holder.grid.getAdapter() == null) {
                AppGridAdapter adapter = new AppGridAdapter(iconCache, appsByKey);
                adapter.setCallbacks(callbacks);
                holder.grid.setAdapter(adapter);
            }
            ((AppGridAdapter) holder.grid.getAdapter()).submit(0, pages.get(position));
        }

        @Override
        public int getItemCount() {
            return pages.size();
        }

        static final class Holder extends RecyclerView.ViewHolder {
            final RecyclerView grid;

            Holder(@NonNull RecyclerView grid) {
                super(grid);
                this.grid = grid;
            }
        }
    }
}
