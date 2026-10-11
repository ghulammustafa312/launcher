package com.yourstudio.aerolauncher.launcher;

import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.content.pm.LauncherApps;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Starts an app with a scale-up open animation from the tapped icon.
 */
public final class AppLauncher {

    private AppLauncher() {
    }

    public static void launch(@NonNull Context context, @NonNull AppInfo app, @Nullable View source) {
        Bundle options = null;
        if (source != null) {
            options = ActivityOptions.makeScaleUpAnimation(
                    source,
                    0,
                    0,
                    source.getWidth(),
                    source.getHeight()).toBundle();
        }

        LauncherApps launcherApps = context.getSystemService(LauncherApps.class);
        if (launcherApps != null) {
            Rect bounds = null;
            if (source != null) {
                int[] loc = new int[2];
                source.getLocationOnScreen(loc);
                bounds = new Rect(
                        loc[0],
                        loc[1],
                        loc[0] + source.getWidth(),
                        loc[1] + source.getHeight());
            }
            try {
                launcherApps.startMainActivity(app.componentName, app.userHandle, bounds, options);
                return;
            } catch (Exception ignored) {
                // Fall through to Intent launch.
            }
        }

        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.setComponent(app.componentName);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        try {
            if (options != null) {
                context.startActivity(intent, options);
            } else {
                context.startActivity(intent);
            }
        } catch (Exception ignored) {
            // App may have been uninstalled between tap and launch.
        }
    }
}
