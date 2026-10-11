package com.yourstudio.aerolauncher.launcher;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.UserHandle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Launchable app entry resolved from LauncherApps / PackageManager.
 */
public final class AppInfo {

    @NonNull
    public final String key;
    @NonNull
    public final CharSequence label;
    @NonNull
    public final String packageName;
    @NonNull
    public final ComponentName componentName;
    @NonNull
    public final UserHandle userHandle;

    @Nullable
    private final LauncherActivityInfo launcherActivityInfo;

    public AppInfo(
            @NonNull CharSequence label,
            @NonNull ComponentName componentName,
            @NonNull UserHandle userHandle,
            @Nullable LauncherActivityInfo launcherActivityInfo) {
        this.label = label;
        this.componentName = componentName;
        this.packageName = componentName.getPackageName();
        this.userHandle = userHandle;
        this.launcherActivityInfo = launcherActivityInfo;
        this.key = componentName.flattenToShortString() + "#" + userHandle.hashCode();
    }

    @Nullable
    public Drawable loadIcon(@NonNull Context context, int densityDpi) {
        if (launcherActivityInfo != null) {
            return launcherActivityInfo.getBadgedIcon(densityDpi);
        }
        try {
            return context.getPackageManager().getActivityIcon(componentName);
        } catch (PackageManager.NameNotFoundException ignored) {
            return null;
        }
    }
}
