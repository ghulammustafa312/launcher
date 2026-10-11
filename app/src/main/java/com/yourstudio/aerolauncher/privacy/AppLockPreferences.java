package com.yourstudio.aerolauncher.privacy;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import com.yourstudio.aerolauncher.ThemePreferences;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Apps that require BiometricPrompt before launch.
 */
public final class AppLockPreferences {

    private static final String KEY_LOCKED = "app_lock_keys";

    private AppLockPreferences() {
    }

    private static SharedPreferences prefs(@NonNull Context context) {
        return ThemePreferences.prefs(context);
    }

    @NonNull
    public static Set<String> getLockedKeys(@NonNull Context context) {
        return new HashSet<>(prefs(context).getStringSet(KEY_LOCKED, Collections.emptySet()));
    }

    public static boolean isLocked(@NonNull Context context, @NonNull String appKey) {
        return getLockedKeys(context).contains(appKey);
    }

    public static void setLocked(
            @NonNull Context context,
            @NonNull String appKey,
            boolean locked) {
        Set<String> keys = getLockedKeys(context);
        if (locked) {
            keys.add(appKey);
        } else {
            keys.remove(appKey);
        }
        prefs(context).edit().putStringSet(KEY_LOCKED, keys).apply();
    }
}
