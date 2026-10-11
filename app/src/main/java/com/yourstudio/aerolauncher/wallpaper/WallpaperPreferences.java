package com.yourstudio.aerolauncher.wallpaper;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.yourstudio.aerolauncher.ThemePreferences;

/**
 * Built-in / custom wallpaper and blur strength.
 */
public final class WallpaperPreferences {

    public static final String SOURCE_NONE = "none";
    public static final String SOURCE_BUILTIN = "builtin";
    public static final String SOURCE_CUSTOM = "custom";

    public static final String BUILTIN_DUSK = "dusk";
    public static final String BUILTIN_AURORA = "aurora";
    public static final String BUILTIN_MIST = "mist";
    public static final String BUILTIN_EMBER = "ember";

    private static final String KEY_SOURCE = "wallpaper_source";
    private static final String KEY_BUILTIN = "wallpaper_builtin";
    private static final String KEY_CUSTOM_URI = "wallpaper_custom_uri";
    private static final String KEY_BLUR = "wallpaper_blur";

    private WallpaperPreferences() {
    }

    private static SharedPreferences prefs(@NonNull Context context) {
        return ThemePreferences.prefs(context);
    }

    @NonNull
    public static String getSource(@NonNull Context context) {
        return prefs(context).getString(KEY_SOURCE, SOURCE_NONE);
    }

    public static void setNone(@NonNull Context context) {
        prefs(context).edit()
                .putString(KEY_SOURCE, SOURCE_NONE)
                .remove(KEY_CUSTOM_URI)
                .apply();
    }

    public static void setBuiltin(@NonNull Context context, @NonNull String builtinId) {
        prefs(context).edit()
                .putString(KEY_SOURCE, SOURCE_BUILTIN)
                .putString(KEY_BUILTIN, builtinId)
                .remove(KEY_CUSTOM_URI)
                .apply();
    }

    public static void setCustomUri(@NonNull Context context, @NonNull Uri uri) {
        prefs(context).edit()
                .putString(KEY_SOURCE, SOURCE_CUSTOM)
                .putString(KEY_CUSTOM_URI, uri.toString())
                .apply();
    }

    @NonNull
    public static String getBuiltinId(@NonNull Context context) {
        return prefs(context).getString(KEY_BUILTIN, BUILTIN_DUSK);
    }

    @Nullable
    public static Uri getCustomUri(@NonNull Context context) {
        String value = prefs(context).getString(KEY_CUSTOM_URI, null);
        return value == null || value.isEmpty() ? null : Uri.parse(value);
    }

    /** Blur radius 0–25. */
    public static int getBlur(@NonNull Context context) {
        return Math.max(0, Math.min(25, prefs(context).getInt(KEY_BLUR, 0)));
    }

    public static void setBlur(@NonNull Context context, int blur) {
        prefs(context).edit().putInt(KEY_BLUR, Math.max(0, Math.min(25, blur))).apply();
    }
}
