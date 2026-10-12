package com.yourstudio.aerolauncher.launcher;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.UserHandle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Launchable app entry resolved from LauncherApps / PackageManager.
 * Keys include the user handle so personal and work profile apps stay distinct.
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
    @NonNull
    public final AppCategory category;
    public final long firstInstallTimeMs;
    /** True when this entry belongs to a non-primary user (typically work profile). */
    public final boolean workProfile;

    @Nullable
    private final LauncherActivityInfo launcherActivityInfo;

    public AppInfo(
            @NonNull CharSequence label,
            @NonNull ComponentName componentName,
            @NonNull UserHandle userHandle,
            @Nullable LauncherActivityInfo launcherActivityInfo,
            @NonNull AppCategory category,
            long firstInstallTimeMs,
            boolean workProfile) {
        this.label = label;
        this.componentName = componentName;
        this.packageName = componentName.getPackageName();
        this.userHandle = userHandle;
        this.launcherActivityInfo = launcherActivityInfo;
        this.category = category;
        this.firstInstallTimeMs = firstInstallTimeMs;
        this.workProfile = workProfile;
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

    public static int readCategory(@NonNull PackageManager pm, @NonNull String packageName) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return ApplicationInfo.CATEGORY_UNDEFINED;
        }
        try {
            ApplicationInfo info = pm.getApplicationInfo(packageName, 0);
            return info.category;
        } catch (PackageManager.NameNotFoundException e) {
            return ApplicationInfo.CATEGORY_UNDEFINED;
        }
    }

    public static long readInstallTime(@NonNull PackageManager pm, @NonNull String packageName) {
        try {
            return pm.getPackageInfo(packageName, 0).firstInstallTime;
        } catch (PackageManager.NameNotFoundException e) {
            return 0L;
        }
    }
}
