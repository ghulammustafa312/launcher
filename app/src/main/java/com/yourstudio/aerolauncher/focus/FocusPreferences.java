package com.yourstudio.aerolauncher.focus;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import com.yourstudio.aerolauncher.ThemePreferences;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Focus modes that temporarily hide selected apps from the home screen.
 */
public final class FocusPreferences {

    public static final String MODE_OFF = "off";
    public static final String MODE_WORK = "work";
    public static final String MODE_STUDY = "study";
    public static final String MODE_SLEEP = "sleep";

    private static final String KEY_MODE = "focus_mode";
    private static final String KEY_PREFIX = "focus_apps_";

    private FocusPreferences() {
    }

    private static SharedPreferences prefs(@NonNull Context context) {
        return ThemePreferences.prefs(context);
    }

    @NonNull
    public static String getMode(@NonNull Context context) {
        return prefs(context).getString(KEY_MODE, MODE_OFF);
    }

    public static void setMode(@NonNull Context context, @NonNull String mode) {
        prefs(context).edit().putString(KEY_MODE, mode).apply();
    }

    public static boolean isActive(@NonNull Context context) {
        return !MODE_OFF.equals(getMode(context));
    }

    @NonNull
    public static Set<String> getAppsForMode(@NonNull Context context, @NonNull String mode) {
        if (MODE_OFF.equals(mode)) {
            return Collections.emptySet();
        }
        return new HashSet<>(prefs(context).getStringSet(KEY_PREFIX + mode, Collections.emptySet()));
    }

    public static void setAppsForMode(
            @NonNull Context context,
            @NonNull String mode,
            @NonNull Set<String> appKeys) {
        prefs(context).edit()
                .putStringSet(KEY_PREFIX + mode, new HashSet<>(appKeys))
                .apply();
    }

    public static void toggleAppInMode(
            @NonNull Context context,
            @NonNull String mode,
            @NonNull String appKey,
            boolean included) {
        Set<String> apps = getAppsForMode(context, mode);
        if (included) {
            apps.add(appKey);
        } else {
            apps.remove(appKey);
        }
        setAppsForMode(context, mode, apps);
    }

    /** Apps that should be hidden on home for the currently active focus mode. */
    @NonNull
    public static Set<String> activeHiddenKeys(@NonNull Context context) {
        String mode = getMode(context);
        if (MODE_OFF.equals(mode)) {
            return Collections.emptySet();
        }
        return getAppsForMode(context, mode);
    }
}
