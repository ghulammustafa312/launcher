package com.yourstudio.aerolauncher;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Home grid size and dock package keys. Defaults: 4 columns × 6 rows, empty dock
 * until filled from installed apps.
 */
public final class HomePreferences {

    public static final int DEFAULT_COLUMNS = 4;
    public static final int DEFAULT_ROWS = 6;
    public static final int DOCK_SLOTS = 4;

    private static final String KEY_COLUMNS = "grid_columns";
    private static final String KEY_ROWS = "grid_rows";
    private static final String KEY_DOCK = "dock_keys";

    private HomePreferences() {
    }

    private static SharedPreferences prefs(Context context) {
        return ThemePreferences.prefs(context);
    }

    public static int getColumns(Context context) {
        return DEFAULT_COLUMNS;
    }

    public static int getRows(Context context) {
        return DEFAULT_ROWS;
    }

    public static void setGridSize(Context context, int columns, int rows) {
        prefs(context).edit()
                .putInt(KEY_COLUMNS, Math.max(3, Math.min(6, columns)))
                .putInt(KEY_ROWS, Math.max(4, Math.min(8, rows)))
                .apply();
    }

    public static int pageCapacity(Context context) {
        return getColumns(context) * getRows(context);
    }

    @NonNull
    public static List<String> getDockKeys(Context context) {
        String raw = prefs(context).getString(KEY_DOCK, "");
        if (raw == null || raw.isEmpty()) {
            return new ArrayList<>();
        }
        String[] parts = raw.split("\u001f", -1);
        List<String> keys = new ArrayList<>(DOCK_SLOTS);
        for (String part : parts) {
            if (!part.isEmpty()) {
                keys.add(part);
            }
            if (keys.size() >= DOCK_SLOTS) {
                break;
            }
        }
        return keys;
    }

    public static void setDockKeys(Context context, @Nullable List<String> keys) {
        if (keys == null || keys.isEmpty()) {
            prefs(context).edit().remove(KEY_DOCK).apply();
            return;
        }
        StringBuilder builder = new StringBuilder();
        int count = Math.min(DOCK_SLOTS, keys.size());
        for (int i = 0; i < count; i++) {
            if (i > 0) {
                builder.append('\u001f');
            }
            builder.append(keys.get(i));
        }
        prefs(context).edit().putString(KEY_DOCK, builder.toString()).apply();
    }

    @NonNull
    public static List<String> ensureDockDefaults(
            Context context,
            @NonNull List<String> availableKeys) {
        List<String> dock = getDockKeys(context);
        if (!dock.isEmpty()) {
            return dock;
        }
        if (availableKeys.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> defaults = new ArrayList<>(DOCK_SLOTS);
        for (String key : availableKeys) {
            defaults.add(key);
            if (defaults.size() >= DOCK_SLOTS) {
                break;
            }
        }
        setDockKeys(context, defaults);
        return defaults;
    }
}
