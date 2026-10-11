package com.yourstudio.aerolauncher.layout;

import androidx.annotation.NonNull;

/**
 * Where a home item lives: workspace cell or dock slot.
 */
public final class ItemLocation {

    public enum Container {
        WORKSPACE,
        DOCK
    }

    @NonNull
    public final Container container;
    public final int pageIndex;
    public final int cellIndex;
    public final int dockIndex;

    private ItemLocation(
            @NonNull Container container,
            int pageIndex,
            int cellIndex,
            int dockIndex) {
        this.container = container;
        this.pageIndex = pageIndex;
        this.cellIndex = cellIndex;
        this.dockIndex = dockIndex;
    }

    @NonNull
    public static ItemLocation workspace(int pageIndex, int cellIndex) {
        return new ItemLocation(Container.WORKSPACE, pageIndex, cellIndex, -1);
    }

    @NonNull
    public static ItemLocation dock(int dockIndex) {
        return new ItemLocation(Container.DOCK, -1, -1, dockIndex);
    }

    @NonNull
    public String encode() {
        if (container == Container.DOCK) {
            return "DOCK:" + dockIndex;
        }
        return "WS:" + pageIndex + ":" + cellIndex;
    }

    @NonNull
    public static ItemLocation decode(@NonNull String raw) {
        if (raw.startsWith("DOCK:")) {
            return dock(Integer.parseInt(raw.substring(5)));
        }
        String[] parts = raw.split(":");
        return workspace(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
    }
}
