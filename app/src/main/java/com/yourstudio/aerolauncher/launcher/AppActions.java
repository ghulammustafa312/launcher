package com.yourstudio.aerolauncher.launcher;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.LauncherApps;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

import androidx.annotation.NonNull;

/**
 * System actions for an installed app: info screen and uninstall.
 */
public final class AppActions {

    private AppActions() {
    }

    public static void openAppInfo(@NonNull Activity activity, @NonNull AppInfo app) {
        LauncherApps launcherApps = activity.getSystemService(LauncherApps.class);
        if (launcherApps != null) {
            try {
                launcherApps.startAppDetailsActivity(
                        app.componentName,
                        app.userHandle,
                        null,
                        null);
                return;
            } catch (Exception ignored) {
                // Fall through.
            }
        }
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.fromParts("package", app.packageName, null));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        activity.startActivity(intent);
    }

    public static void uninstall(@NonNull Activity activity, @NonNull AppInfo app) {
        LauncherApps launcherApps = activity.getSystemService(LauncherApps.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && launcherApps != null) {
            try {
                // SESSION_BASED or legacy uninstall via package URI still works broadly.
            } catch (Exception ignored) {
                // Fall through.
            }
        }
        Intent intent = new Intent(Intent.ACTION_DELETE);
        intent.setData(Uri.fromParts("package", app.packageName, null));
        intent.putExtra(Intent.EXTRA_USER, app.userHandle);
        activity.startActivity(intent);
    }
}
