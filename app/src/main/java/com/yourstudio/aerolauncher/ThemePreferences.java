package com.yourstudio.aerolauncher;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

/**
 * Stores the user's light / dark / auto preference.
 * Auto maps to MODE_NIGHT_FOLLOW_SYSTEM.
 */
public final class ThemePreferences {

    public static final String MODE_LIGHT = "light";
    public static final String MODE_DARK = "dark";
    public static final String MODE_AUTO = "auto";

    private static final String PREFS_NAME = "aero_launcher_prefs";
    private static final String KEY_THEME_MODE = "theme_mode";

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
}
