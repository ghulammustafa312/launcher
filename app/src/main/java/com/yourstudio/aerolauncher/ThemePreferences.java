package com.yourstudio.aerolauncher;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.StyleRes;
import androidx.appcompat.app.AppCompatDelegate;

/**
 * Theme mode, accent color, and icon mask style.
 */
public final class ThemePreferences {

    public static final String MODE_LIGHT = "light";
    public static final String MODE_DARK = "dark";
    public static final String MODE_AUTO = "auto";

    public static final String ACCENT_MIDNIGHT = "midnight";
    public static final String ACCENT_VIOLET = "violet";
    public static final String ACCENT_AQUA = "aqua";
    public static final String ACCENT_ORANGE = "orange";

    public static final String ICON_ROUNDED = "rounded";
    public static final String ICON_CIRCLE = "circle";
    public static final String ICON_SQUIRCLE = "squircle";

    private static final String PREFS_NAME = "aero_launcher_prefs";
    private static final String KEY_THEME_MODE = "theme_mode";
    private static final String KEY_ACCENT = "accent_color";
    private static final String KEY_ICON_STYLE = "icon_style";

    private ThemePreferences() {
    }

    public static SharedPreferences prefs(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static String getThemeMode(Context context) {
        return prefs(context).getString(KEY_THEME_MODE, MODE_AUTO);
    }

    public static void setThemeMode(Context context, String mode) {
        prefs(context).edit().putString(KEY_THEME_MODE, mode).apply();
        applyNightMode(mode);
    }

    public static void applySavedTheme(Context context) {
        applyNightMode(getThemeMode(context));
    }

    public static void applyNightMode(String mode) {
        int nightMode;
        switch (mode) {
            case MODE_LIGHT:
                nightMode = AppCompatDelegate.MODE_NIGHT_NO;
                break;
            case MODE_DARK:
                nightMode = AppCompatDelegate.MODE_NIGHT_YES;
                break;
            case MODE_AUTO:
            default:
                nightMode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
                break;
        }
        AppCompatDelegate.setDefaultNightMode(nightMode);
    }

    @NonNull
    public static String getAccent(@NonNull Context context) {
        return prefs(context).getString(KEY_ACCENT, ACCENT_VIOLET);
    }

    public static void setAccent(@NonNull Context context, @NonNull String accent) {
        prefs(context).edit().putString(KEY_ACCENT, accent).apply();
    }

    @StyleRes
    public static int accentThemeRes(@NonNull Context context) {
        switch (getAccent(context)) {
            case ACCENT_MIDNIGHT:
                return R.style.Theme_AeroLauncher_Accent_Midnight;
            case ACCENT_AQUA:
                return R.style.Theme_AeroLauncher_Accent_Aqua;
            case ACCENT_ORANGE:
                return R.style.Theme_AeroLauncher_Accent_Orange;
            case ACCENT_VIOLET:
            default:
                return R.style.Theme_AeroLauncher_Accent_Violet;
        }
    }

    @StyleRes
    public static int homeAccentThemeRes(@NonNull Context context) {
        switch (getAccent(context)) {
            case ACCENT_MIDNIGHT:
                return R.style.Theme_AeroLauncher_Home_Accent_Midnight;
            case ACCENT_AQUA:
                return R.style.Theme_AeroLauncher_Home_Accent_Aqua;
            case ACCENT_ORANGE:
                return R.style.Theme_AeroLauncher_Home_Accent_Orange;
            case ACCENT_VIOLET:
            default:
                return R.style.Theme_AeroLauncher_Home_Accent_Violet;
        }
    }

    /** Call before {@code setContentView} on settings-style screens. */
    public static void applyAccentOverlay(@NonNull Activity activity) {
        activity.setTheme(accentThemeRes(activity));
    }

    /** Call before {@code setContentView} on the home activity (keeps wallpaper window). */
    public static void applyHomeAccentOverlay(@NonNull Activity activity) {
        activity.setTheme(homeAccentThemeRes(activity));
    }

    @NonNull
    public static String getIconStyle(@NonNull Context context) {
        return prefs(context).getString(KEY_ICON_STYLE, ICON_SQUIRCLE);
    }

    public static void setIconStyle(@NonNull Context context, @NonNull String style) {
        prefs(context).edit().putString(KEY_ICON_STYLE, style).apply();
    }
}
