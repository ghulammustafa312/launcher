package com.yourstudio.aerolauncher;

import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.text.TextUtils;

import androidx.annotation.NonNull;

/**
 * Feature flags that require clear user opt-in (e.g. lock via AccessibilityService).
 */
public final class FeaturePreferences {

    private static final String KEY_LOCK_OPTED_IN = "lock_double_tap_opted_in";
    private static final String KEY_LOCK_SERVICE_ENABLED = "lock_service_reported_enabled";

    private FeaturePreferences() {
    }

    private static SharedPreferences prefs(@NonNull Context context) {
        return ThemePreferences.prefs(context);
    }

    public static boolean hasOptedInToLock(@NonNull Context context) {
        return prefs(context).getBoolean(KEY_LOCK_OPTED_IN, false);
    }

    public static void setOptedInToLock(@NonNull Context context, boolean optedIn) {
        prefs(context).edit().putBoolean(KEY_LOCK_OPTED_IN, optedIn).apply();
    }

    public static void setLockServiceReportedEnabled(@NonNull Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_LOCK_SERVICE_ENABLED, enabled).apply();
    }

    public static boolean isLockServiceEnabled(@NonNull Context context) {
        if (LockAccessibilityService.isConnected()) {
            return true;
        }
        String enabled = Settings.Secure.getString(
                context.getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (TextUtils.isEmpty(enabled)) {
            return false;
        }
        String flat = context.getPackageName() + "/" + LockAccessibilityService.class.getName();
        TextUtils.SimpleStringSplitter splitter = new TextUtils.SimpleStringSplitter(':');
        splitter.setString(enabled);
        while (splitter.hasNext()) {
            if (flat.equalsIgnoreCase(splitter.next())) {
                return true;
            }
        }
        return false;
    }

    public static boolean canLockScreen(@NonNull Context context) {
        return hasOptedInToLock(context) && isLockServiceEnabled(context);
    }
}
