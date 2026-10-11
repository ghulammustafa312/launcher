package com.yourstudio.aerolauncher.ui;

import android.content.ClipData;
import android.content.ClipDescription;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.yourstudio.aerolauncher.R;
import com.yourstudio.aerolauncher.launcher.AppInfo;
import com.yourstudio.aerolauncher.launcher.AppLauncher;
import com.yourstudio.aerolauncher.launcher.IconCache;
import com.yourstudio.aerolauncher.layout.HomeItem;
import com.yourstudio.aerolauncher.layout.ItemLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Fixed-size home-page grid (includes empty cells) with edit-mode drag support.
 */
public final class AppGridAdapter extends RecyclerView.Adapter<AppGridAdapter.Holder> {

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

    public void submit(int pageIndex, @NonNull List<HomeItem> pageCells) {
        this.pageIndex = pageIndex;
        cells.clear();
        cells.addAll(pageCells);
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
        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        HomeItem item = cells.get(position);
        ItemLocation location = ItemLocation.workspace(pageIndex, position);
        boolean editMode = callbacks != null && callbacks.isEditMode();

        holder.itemView.setOnDragListener(new HomeDragListener(location, callbacks));

        if (item == null) {
            holder.iconContent.setVisibility(View.INVISIBLE);
            holder.removeBadge.setVisibility(View.GONE);
            WiggleAnimation.stop(holder.iconContent);
            holder.itemView.setOnClickListener(null);
            holder.itemView.setOnLongClickListener(null);
            return;
        }

        holder.iconContent.setVisibility(View.VISIBLE);
        holder.label.setText(labelFor(item));
        if (item.isFolder()) {
            holder.icon.setVisibility(View.GONE);
            holder.folderPreview.setVisibility(View.VISIBLE);
        } else {
            holder.folderPreview.setVisibility(View.GONE);
            holder.icon.setVisibility(View.VISIBLE);
            AppInfo app = item.appKey != null ? appsByKey.get(item.appKey) : null;
            if (app != null) {
                iconCache.loadInto(app, holder.icon);
            } else {
                holder.icon.setImageResource(R.drawable.ic_app_placeholder);
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
                callbacks.onItemClick(item, holder.iconContent);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (callbacks == null) {
                return false;
            }
            boolean alreadyEditing = callbacks.isEditMode();
            callbacks.onItemLongClick(item, location, v);
            // First long-press enters edit mode + menu; later long-press starts a drag.
            if (alreadyEditing) {
                startDrag(v, item, location);
            }
            return true;
        });
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
    private String labelFor(@NonNull HomeItem item) {
        if (item.isFolder()) {
            return item.title;
        }
        AppInfo app = item.appKey != null ? appsByKey.get(item.appKey) : null;
        return app != null ? app.label.toString() : "";
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
        final TextView label;
        final ImageButton removeBadge;

        Holder(@NonNull View itemView) {
            super(itemView);
            iconContent = itemView.findViewById(R.id.icon_content);
            icon = itemView.findViewById(R.id.app_icon);
            folderPreview = itemView.findViewById(R.id.folder_preview);
            label = itemView.findViewById(R.id.app_label);
            removeBadge = itemView.findViewById(R.id.remove_badge);
        }
    }
}
