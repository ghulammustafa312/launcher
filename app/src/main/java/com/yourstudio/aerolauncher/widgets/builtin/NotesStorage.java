package com.yourstudio.aerolauncher.widgets.builtin;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Local-only note text keyed by appWidgetId.
 */
public final class NotesStorage {

    private static final String PREFS = "aero_notes_widgets";
    private static final String KEY_PREFIX = "note_";

    private NotesStorage() {
    }

    @NonNull
    public static String get(@NonNull Context context, int appWidgetId) {
        return prefs(context).getString(KEY_PREFIX + appWidgetId, "");
    }

    public static void set(@NonNull Context context, int appWidgetId, @Nullable String text) {
        prefs(context).edit()
                .putString(KEY_PREFIX + appWidgetId, text == null ? "" : text)
                .apply();
    }

    public static void delete(@NonNull Context context, int appWidgetId) {
        prefs(context).edit().remove(KEY_PREFIX + appWidgetId).apply();
    }

    @NonNull
    private static SharedPreferences prefs(@NonNull Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
