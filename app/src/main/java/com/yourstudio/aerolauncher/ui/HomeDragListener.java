package com.yourstudio.aerolauncher.ui;

import android.content.ClipData;
import android.view.DragEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.yourstudio.aerolauncher.layout.ItemLocation;

/**
 * Accepts home-item drops on a workspace cell or dock slot.
 */
public final class HomeDragListener implements View.OnDragListener {

    private final ItemLocation location;
    private final AppGridAdapter.Callbacks callbacks;

    public HomeDragListener(
            @NonNull ItemLocation location,
            @Nullable AppGridAdapter.Callbacks callbacks) {
        this.location = location;
        this.callbacks = callbacks;
    }

    @Override
    public boolean onDrag(View v, DragEvent event) {
        if (callbacks == null || !callbacks.isEditMode()) {
            return false;
        }
        switch (event.getAction()) {
            case DragEvent.ACTION_DRAG_STARTED:
                return hasAeroMime(event);
            case DragEvent.ACTION_DRAG_ENTERED:
                v.animate().scaleX(1.08f).scaleY(1.08f).setDuration(120).start();
                return true;
            case DragEvent.ACTION_DRAG_EXITED:
            case DragEvent.ACTION_DRAG_ENDED:
                v.animate().scaleX(1f).scaleY(1f).setDuration(120).start();
                return true;
            case DragEvent.ACTION_DROP:
                v.animate().scaleX(1f).scaleY(1f).setDuration(120).start();
                ClipData data = event.getClipData();
                if (data == null || data.getItemCount() == 0) {
                    return false;
                }
                CharSequence text = data.getItemAt(0).getText();
                if (text == null) {
                    return false;
                }
                String[] parts = text.toString().split("\n", 2);
                if (parts.length < 2) {
                    return false;
                }
                ItemLocation from = ItemLocation.decode(parts[1]);
                callbacks.onItemDropped(from, location);
                return true;
            default:
                return true;
        }
    }

    private static boolean hasAeroMime(@NonNull DragEvent event) {
        if (event.getClipDescription() == null) {
            return false;
        }
        return event.getClipDescription().hasMimeType(AppGridAdapter.DRAG_MIME)
                || event.getClipDescription().hasMimeType(
                android.content.ClipDescription.MIMETYPE_TEXT_PLAIN);
    }
}
