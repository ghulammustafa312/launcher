package com.yourstudio.aerolauncher.privacy;

import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.yourstudio.aerolauncher.ThemePreferences;
import com.yourstudio.aerolauncher.badges.AeroNotificationListener;

/**
 * Opt-in for notification badges via NotificationListenerService.
 */
public final class BadgePreferences {

    private static final String KEY_OPTED_IN = "badge_opted_in";

    private BadgePreferences() {
    }

    private static SharedPreferences prefs(@NonNull Context context) {
        return ThemePreferences.prefs(context);
    }

    public static boolean hasOptedIn(@NonNull Context context) {
        return prefs(context).getBoolean(KEY_OPTED_IN, false);
    }

    public static void setOptedIn(@NonNull Context context, boolean optedIn) {
        prefs(context).edit().putBoolean(KEY_OPTED_IN, optedIn).apply();
        if (!optedIn) {
            com.yourstudio.aerolauncher.badges.BadgeStore.get().clear();
        }
    }

    public static boolean isListenerEnabled(@NonNull Context context) {
        String flat = new ComponentName(context, AeroNotificationListener.class).flattenToString();
        String enabled = Settings.Secure.getString(
                context.getContentResolver(),
                "enabled_notification_listeners");
        if (TextUtils.isEmpty(enabled)) {
            return false;
        }
        for (String entry : enabled.split(":")) {
            if (flat.equalsIgnoreCase(entry)) {
                return true;
            }
        }
        return false;
    }

    public static boolean badgesActive(@NonNull Context context) {
        return hasOptedIn(context) && isListenerEnabled(context);
    }
}
