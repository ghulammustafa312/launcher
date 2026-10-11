package com.yourstudio.aerolauncher.launcher;

import android.content.Context;
import android.content.Intent;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.os.UserHandle;
import android.os.UserManager;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Loads launchable apps on a background thread, caches icons, and listens for
 * install / remove / change events via {@link LauncherApps.Callback}.
 */
public final class AppRepository {

    public interface Listener {
        @MainThread
        void onAppsUpdated(@NonNull List<AppInfo> apps);
    }

    private final Context appContext;
    private final LauncherApps launcherApps;
    private final UserManager userManager;
    private final IconCache iconCache;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final CopyOnWriteArrayList<Listener> listeners = new CopyOnWriteArrayList<>();
    private final AtomicBoolean started = new AtomicBoolean(false);

    @NonNull
    private volatile List<AppInfo> apps = Collections.emptyList();

    @Nullable
    private LauncherApps.Callback callback;

    public AppRepository(@NonNull Context context) {
        this.appContext = context.getApplicationContext();
        this.launcherApps = appContext.getSystemService(LauncherApps.class);
        this.userManager = appContext.getSystemService(UserManager.class);
        this.iconCache = new IconCache(appContext);
    }

    @NonNull
    public IconCache getIconCache() {
        return iconCache;
    }

    @NonNull
    public List<AppInfo> getApps() {
        return apps;
    }

    @Nullable
    public AppInfo findByKey(@NonNull String key) {
        for (AppInfo app : apps) {
            if (app.key.equals(key)) {
                return app;
            }
        }
        return null;
    }

    public void addListener(@NonNull Listener listener) {
        listeners.add(listener);
        if (!apps.isEmpty()) {
            listener.onAppsUpdated(apps);
        }
    }

    public void removeListener(@NonNull Listener listener) {
        listeners.remove(listener);
    }

    public void start() {
        if (!started.compareAndSet(false, true)) {
            return;
        }
        registerPackageCallback();
        reload();
    }

    public void reload() {
        executor.execute(() -> {
            List<AppInfo> loaded = loadInstalledApps();
            Collections.sort(loaded, Comparator.comparing(
                    app -> app.label.toString(),
                    String.CASE_INSENSITIVE_ORDER));
            apps = Collections.unmodifiableList(loaded);
            mainHandler.post(() -> {
                for (Listener listener : listeners) {
                    listener.onAppsUpdated(apps);
                }
            });
        });
    }

    private void registerPackageCallback() {
        if (launcherApps == null || callback != null) {
            return;
        }
        callback = new LauncherApps.Callback() {
            @Override
            public void onPackageRemoved(String packageName, UserHandle user) {
                iconCache.invalidatePackage(packageName);
                reload();
            }

            @Override
            public void onPackageAdded(String packageName, UserHandle user) {
                reload();
            }

            @Override
            public void onPackageChanged(String packageName, UserHandle user) {
                iconCache.invalidatePackage(packageName);
                reload();
            }

            @Override
            public void onPackagesAvailable(String[] packageNames, UserHandle user, boolean replacing) {
                reload();
            }

            @Override
            public void onPackagesUnavailable(String[] packageNames, UserHandle user, boolean replacing) {
                reload();
            }
        };
        launcherApps.registerCallback(callback, mainHandler);
    }

    @NonNull
    private List<AppInfo> loadInstalledApps() {
        if (launcherApps != null && userManager != null) {
            List<AppInfo> result = new ArrayList<>();
            List<UserHandle> profiles = userManager.getUserProfiles();
            for (UserHandle user : profiles) {
                List<LauncherActivityInfo> activities = launcherApps.getActivityList(null, user);
                PackageManager pm = appContext.getPackageManager();
                for (LauncherActivityInfo info : activities) {
                    // Skip this launcher itself on the home grid.
                    String packageName = info.getComponentName().getPackageName();
                    if (appContext.getPackageName().equals(packageName)) {
                        continue;
                    }
                    CharSequence label = info.getLabel();
                    int categoryCode = AppInfo.readCategory(pm, packageName);
                    result.add(new AppInfo(
                            label,
                            info.getComponentName(),
                            info.getUser(),
                            info,
                            AppCategory.from(packageName, label, categoryCode),
                            AppInfo.readInstallTime(pm, packageName)));
                }
            }
            if (!result.isEmpty()) {
                return result;
            }
        }
        return loadViaPackageManager();
    }

    /**
     * Fallback when LauncherApps returns nothing (still uses MAIN/LAUNCHER queries).
     */
    @NonNull
    private List<AppInfo> loadViaPackageManager() {
        PackageManager pm = appContext.getPackageManager();
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> resolved = pm.queryIntentActivities(intent, 0);
        List<AppInfo> result = new ArrayList<>(resolved.size());
        UserHandle user = Process.myUserHandle();
        for (ResolveInfo info : resolved) {
            if (info.activityInfo == null) {
                continue;
            }
            if (appContext.getPackageName().equals(info.activityInfo.packageName)) {
                continue;
            }
            android.content.ComponentName component = new android.content.ComponentName(
                    info.activityInfo.packageName,
                    info.activityInfo.name);
            CharSequence label = info.loadLabel(pm);
            String packageName = info.activityInfo.packageName;
            int categoryCode = AppInfo.readCategory(pm, packageName);
            result.add(new AppInfo(
                    label,
                    component,
                    user,
                    null,
                    AppCategory.from(packageName, label, categoryCode),
                    AppInfo.readInstallTime(pm, packageName)));
        }
        return result;
    }

    public void shutdown() {
        if (launcherApps != null && callback != null) {
            launcherApps.unregisterCallback(callback);
            callback = null;
        }
        executor.shutdownNow();
        iconCache.shutdown();
    }
}
