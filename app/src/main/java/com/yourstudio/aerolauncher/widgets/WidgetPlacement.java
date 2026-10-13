package com.yourstudio.aerolauncher.widgets;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A widget or widget-stack sitting in a home page widget zone.
 */
public final class WidgetPlacement {

    @NonNull
    public final String id;
    public final boolean stack;
    @NonNull
    public final List<Integer> appWidgetIds;
    /** Height in icon-grid rows. iOS cards use 2. */
    public int spanRows;
    /** Width in the 4-column icon grid: 2 (small) or 4 (medium). */
    public int spanColumns = 2;
    public int rank;

    public WidgetPlacement(
            @NonNull String id,
            boolean stack,
            @NonNull List<Integer> appWidgetIds,
            int spanRows,
            int rank) {
        this.id = id;
        this.stack = stack;
        this.appWidgetIds = new ArrayList<>(appWidgetIds);
        this.spanRows = 2;
        this.rank = rank;
    }

    public int columnSpan() {
        return spanColumns >= 4 ? 4 : 2;
    }

    @NonNull
    public static WidgetPlacement single(int appWidgetId, int spanRows, int rank) {
        List<Integer> ids = new ArrayList<>();
        ids.add(appWidgetId);
        return new WidgetPlacement("widget:" + UUID.randomUUID(), false, ids, spanRows, rank);
    }

    @NonNull
    public static WidgetPlacement stack(@NonNull List<Integer> appWidgetIds, int spanRows, int rank) {
        return new WidgetPlacement("stack:" + UUID.randomUUID(), true, appWidgetIds, spanRows, rank);
    }

    @NonNull
    public WidgetPlacement copy() {
        WidgetPlacement copy = new WidgetPlacement(id, stack, appWidgetIds, spanRows, rank);
        copy.spanColumns = columnSpan();
        return copy;
    }
}
