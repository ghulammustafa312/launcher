package com.yourstudio.aerolauncher.ui;

import android.content.ClipData;
import android.content.ClipDescription;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.yourstudio.aerolauncher.R;
import com.yourstudio.aerolauncher.badges.BadgeStore;
import com.yourstudio.aerolauncher.launcher.AppInfo;
import com.yourstudio.aerolauncher.launcher.AppLauncher;
import com.yourstudio.aerolauncher.launcher.IconCache;
import com.yourstudio.aerolauncher.layout.HomeItem;
import com.yourstudio.aerolauncher.layout.ItemLocation;
import com.yourstudio.aerolauncher.privacy.BadgePreferences;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Fixed-size home-page grid (includes empty cells) with edit-mode drag support.
 */
public final class AppGridAdapter extends RecyclerView.Adapter<AppGridAdapter.Holder> {

    public static final int COLUMNS = 4;
    public static final int ROWS = 6;
    public static final String DRAG_MIME = "application/x-aero-home-item";

    public interface Callbacks {
        void onItemClick(@NonNull HomeItem item, @NonNull View anchor);

        void onItemLongClick(@NonNull HomeItem item, @NonNull ItemLocation location, @NonNull View anchor);

        void onRemoveClick(@NonNull ItemLocation location);

        void onItemDropped(@NonNull ItemLocation from, @NonNull ItemLocation to);

        boolean isEditMode();

        void onDragStarted();
    }

    private final IconCache iconCache;
    private final Map<String, AppInfo> appsByKey;
    private final List<HomeItem> cells = new ArrayList<>();
    private int pageIndex;
    private int cellHeightPx;
    private Callbacks callbacks;

    public AppGridAdapter(
            @NonNull IconCache iconCache,
            @NonNull Map<String, AppInfo> appsByKey) {
        this.iconCache = iconCache;
        this.appsByKey = appsByKey;
        setHasStableIds(true);
    }

    public void setCallbacks(@Nullable Callbacks callbacks) {
        this.callbacks = callbacks;
    }

    /**
     * Locks the page to 4 columns and distributes leftover space as equal insets
     * so six rows fit every screen size. Sizes come from dp/sp resources.
     */
    public static void install(@NonNull RecyclerView grid) {
        GridLayoutManager layoutManager = new GridLayoutManager(grid.getContext(), COLUMNS);
        grid.setLayoutManager(layoutManager);
        grid.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) ->
                applyEqualSpacing((RecyclerView) v));
    }

    private static void applyEqualSpacing(@NonNull RecyclerView grid) {
        int width = grid.getWidth();
        int height = grid.getHeight();
        if (width <= 0 || height <= 0) {
            return;
        }
        Integer appliedWidth = (Integer) grid.getTag(R.id.tag_home_grid_width);
        Integer appliedHeight = (Integer) grid.getTag(R.id.tag_home_grid_height);
        if (appliedWidth != null && appliedWidth == width
                && appliedHeight != null && appliedHeight == height) {
            return;
        }
        grid.setTag(R.id.tag_home_grid_width, width);
        grid.setTag(R.id.tag_home_grid_height, height);

        int icon = grid.getResources().getDimensionPixelSize(R.dimen.home_icon_size);
        int labelGap = grid.getResources().getDimensionPixelSize(R.dimen.home_icon_label_gap);
        float labelPx = grid.getResources().getDimension(R.dimen.home_label_text_size);
        int contentHeight = icon + labelGap + Math.round(labelPx);
        int padH = equalInset(width, COLUMNS, icon);
        int padV = equalInset(height, ROWS, contentHeight);
        grid.setPadding(padH, padV, padH, padV);
        grid.setClipToPadding(false);
        int cellHeight = Math.max(1, (height - padV * 2) / ROWS);
        grid.post(() -> {
            RecyclerView.Adapter<?> raw = grid.getAdapter();
            if (raw instanceof AppGridAdapter) {
                ((AppGridAdapter) raw).setCellHeight(cellHeight);
            }
        });
    }

    /** Inset that makes the outer margin match the gap between icons. */
    private static int equalInset(int available, int count, int content) {
        float slack = (available / (float) count) - content;
        if (slack <= 0f || count <= 0) {
            return 0;
        }
        return Math.max(0, Math.round(slack * count / (2f * (count + 1))));
    }

    void setCellHeight(int heightPx) {
        if (heightPx <= 0 || heightPx == cellHeightPx) {
            return;
        }
        cellHeightPx = heightPx;
        notifyDataSetChanged();
    }

    public void submit(int pageIndex, @NonNull List<HomeItem> pageCells) {
        this.pageIndex = pageIndex;
        cells.clear();
        cells.addAll(pageCells);
        notifyDataSetChanged();
    }

    public void refreshBadges() {
        notifyDataSetChanged();
    }

    @Override
    public long getItemId(int position) {
        HomeItem item = cells.get(position);
        return item == null ? -1L - position : item.id.hashCode();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_app_icon, parent, false);
        Holder holder = new Holder(view);
        applyLabelShadow(holder.label);
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        applyCellSize(holder.itemView);
        HomeItem item = cells.get(position);
        ItemLocation location = ItemLocation.workspace(pageIndex, position);
        boolean editMode = callbacks != null && callbacks.isEditMode();

        holder.itemView.setOnDragListener(new HomeDragListener(location, callbacks));

        if (item == null) {
            holder.iconContent.setVisibility(View.INVISIBLE);
            holder.removeBadge.setVisibility(View.GONE);
            holder.notificationBadge.setVisibility(View.GONE);
            WiggleAnimation.stop(holder.iconContent);
            holder.itemView.setOnClickListener(null);
            holder.itemView.setOnLongClickListener(null);
            return;
        }

        holder.iconContent.setVisibility(View.VISIBLE);
        holder.label.setText(labelFor(holder.itemView.getContext(), item));
        if (item.isFolder()) {
            holder.icon.setVisibility(View.GONE);
            holder.folderPreview.setVisibility(View.GONE);
            holder.folderMini.setVisibility(View.VISIBLE);
            holder.notificationBadge.setVisibility(View.GONE);
            bindFolderMini(holder, item);
        } else {
            holder.folderPreview.setVisibility(View.GONE);
            holder.folderMini.setVisibility(View.GONE);
            clearFolderMini(holder);
            holder.icon.setVisibility(View.VISIBLE);
            AppInfo app = item.appKey != null ? appsByKey.get(item.appKey) : null;
            if (app != null) {
                iconCache.loadInto(app, holder.icon);
                bindNotificationBadge(holder, app);
            } else {
                holder.icon.setImageResource(R.drawable.ic_app_placeholder);
                holder.notificationBadge.setVisibility(View.GONE);
            }
        }

        holder.removeBadge.setVisibility(editMode ? View.VISIBLE : View.GONE);
        if (editMode) {
            WiggleAnimation.start(holder.iconContent);
        } else {
            WiggleAnimation.stop(holder.iconContent);
        }

        holder.removeBadge.setOnClickListener(v -> {
            if (callbacks != null) {
                callbacks.onRemoveClick(location);
            }
        });

        holder.itemView.setOnClickListener(v -> {
            if (callbacks != null && !callbacks.isEditMode()) {
                Motion.pop(holder.iconContent);
                callbacks.onItemClick(item, holder.iconContent);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (callbacks == null) {
                return false;
            }
            callbacks.onItemLongClick(item, location, v);
            if (callbacks.isEditMode()) {
                Motion.tick(v);
                startDrag(v, item, location);
            }
            return true;
        });
    }

    private void applyCellSize(@NonNull View itemView) {
        if (cellHeightPx <= 0) {
            return;
        }
        ViewGroup.LayoutParams params = itemView.getLayoutParams();
        if (params != null && params.height != cellHeightPx) {
            params.height = cellHeightPx;
            itemView.setLayoutParams(params);
        }
    }

    static void applyLabelShadow(@NonNull TextView label) {
        DisplayMetrics metrics = label.getResources().getDisplayMetrics();
        float radius = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2f, metrics);
        float dy = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1f, metrics);
        label.setShadowLayer(radius, 0f, dy, 0x66000000);
    }

    private void bindFolderMini(@NonNull Holder holder, @NonNull HomeItem item) {
        List<String> keys = item.folderAppKeys;
        for (int i = 0; i < holder.miniIcons.length; i++) {
            ImageView slot = holder.miniIcons[i];
            if (i >= keys.size()) {
                iconCache.clearTarget(slot);
                continue;
            }
            AppInfo app = appsByKey.get(keys.get(i));
            if (app != null) {
                iconCache.loadInto(app, slot);
            } else {
                slot.setImageResource(R.drawable.ic_app_placeholder);
            }
        }
    }

    private void clearFolderMini(@NonNull Holder holder) {
        for (ImageView slot : holder.miniIcons) {
            iconCache.clearTarget(slot);
        }
    }

    private void bindNotificationBadge(@NonNull Holder holder, @NonNull AppInfo app) {
        if (!BadgePreferences.badgesActive(holder.itemView.getContext())) {
            holder.notificationBadge.setVisibility(View.GONE);
            return;
        }
        int count = BadgeStore.get().getCount(app.packageName);
        if (count <= 0) {
            holder.notificationBadge.setVisibility(View.GONE);
            return;
        }
        holder.notificationBadge.setVisibility(View.VISIBLE);
        holder.notificationBadge.setText(count > 99 ? "99+" : String.valueOf(count));
    }

    private void startDrag(
            @NonNull View view,
            @NonNull HomeItem item,
            @NonNull ItemLocation location) {
        ClipData.Item clipItem = new ClipData.Item(item.id + "\n" + location.encode());
        ClipData data = new ClipData(
                item.id,
                new String[]{DRAG_MIME, ClipDescription.MIMETYPE_TEXT_PLAIN},
                clipItem);
        View.DragShadowBuilder shadow = new View.DragShadowBuilder(view);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            view.startDragAndDrop(data, shadow, null, 0);
        } else {
            view.startDrag(data, shadow, null, 0);
        }
        if (callbacks != null) {
            callbacks.onDragStarted();
        }
    }

    @NonNull
    private String labelFor(@NonNull android.content.Context context, @NonNull HomeItem item) {
        if (item.isFolder()) {
            return item.title;
        }
        AppInfo app = item.appKey != null ? appsByKey.get(item.appKey) : null;
        if (app == null) {
            return "";
        }
        if (app.workProfile) {
            return app.label.toString() + context.getString(R.string.work_profile_suffix);
        }
        return app.label.toString();
    }

    @Override
    public void onViewRecycled(@NonNull Holder holder) {
        iconCache.clearTarget(holder.icon);
        clearFolderMini(holder);
        holder.notificationBadge.setVisibility(View.GONE);
        super.onViewRecycled(holder);
    }

    @Override
    public int getItemCount() {
        return cells.size();
    }

    public static void launchApp(
            @NonNull View anchor,
            @NonNull HomeItem item,
            @NonNull Map<String, AppInfo> appsByKey) {
        if (!item.isApp() || item.appKey == null) {
            return;
        }
        AppInfo app = appsByKey.get(item.appKey);
        if (app != null) {
            AppLauncher.launch(anchor.getContext(), app, anchor);
        }
    }

    static final class Holder extends RecyclerView.ViewHolder {
        final View iconContent;
        final ImageView icon;
        final ImageView folderPreview;
        final View folderMini;
        final ImageView[] miniIcons;
        final TextView label;
        final ImageButton removeBadge;
        final TextView notificationBadge;

        Holder(@NonNull View itemView) {
            super(itemView);
            iconContent = itemView.findViewById(R.id.icon_content);
            icon = itemView.findViewById(R.id.app_icon);
            folderPreview = itemView.findViewById(R.id.folder_preview);
            folderMini = itemView.findViewById(R.id.folder_mini);
            miniIcons = new ImageView[]{
                    itemView.findViewById(R.id.mini_icon_0),
                    itemView.findViewById(R.id.mini_icon_1),
                    itemView.findViewById(R.id.mini_icon_2),
                    itemView.findViewById(R.id.mini_icon_3),
                    itemView.findViewById(R.id.mini_icon_4),
                    itemView.findViewById(R.id.mini_icon_5),
                    itemView.findViewById(R.id.mini_icon_6),
                    itemView.findViewById(R.id.mini_icon_7),
                    itemView.findViewById(R.id.mini_icon_8)
            };
            label = itemView.findViewById(R.id.app_label);
            removeBadge = itemView.findViewById(R.id.remove_badge);
            notificationBadge = itemView.findViewById(R.id.notification_badge);
        }
    }
}
