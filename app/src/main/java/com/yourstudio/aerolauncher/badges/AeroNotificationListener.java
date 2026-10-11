package com.yourstudio.aerolauncher.badges;

import android.app.Notification;
import android.os.Handler;
import android.os.Looper;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import androidx.annotation.Nullable;

import com.yourstudio.aerolauncher.privacy.BadgePreferences;

import java.util.HashMap;
import java.util.Map;

/**
 * Counts active notifications per package for home badges.
 * Does not read, store, or transmit notification content.
 */
public class AeroNotificationListener extends NotificationListenerService {

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    public void onListenerConnected() {
        recount();
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        recount();
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        recount();
    }

    private void recount() {
        if (!BadgePreferences.hasOptedIn(this)) {
            mainHandler.post(() -> BadgeStore.get().clear());
            return;
        }
        StatusBarNotification[] active;
        try {
            active = getActiveNotifications();
        } catch (SecurityException e) {
            return;
        }
        if (active == null) {
            active = new StatusBarNotification[0];
        }
        Map<String, Integer> counts = new HashMap<>();
        String self = getPackageName();
        for (StatusBarNotification sbn : active) {
            if (sbn == null) {
                continue;
            }
            String pkg = sbn.getPackageName();
            if (pkg == null || pkg.equals(self) || shouldSkip(sbn)) {
                continue;
            }
            // Intentionally ignore Notification extras / text / title.
            counts.merge(pkg, 1, Integer::sum);
        }
        mainHandler.post(() -> BadgeStore.get().replaceAll(counts));
    }

    private static boolean shouldSkip(@Nullable StatusBarNotification sbn) {
        if (sbn == null) {
            return true;
        }
        Notification notification = sbn.getNotification();
        if (notification == null) {
            return true;
        }
        // Skip ongoing media/foreground noise when group summary is present.
        if ((notification.flags & Notification.FLAG_GROUP_SUMMARY) != 0) {
            return true;
        }
        return false;
    }
}
