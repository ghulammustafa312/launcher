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
 * Loads launchable apps (including work-profile users) on a background thread,
 * caches icons, and listens for install / remove / change events via {@link LauncherApps.Callback}.
 */
public final class AppRepository {

    private static final long RELOAD_DEBOUNCE_MS = 350L;

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
    private final Runnable debouncedReload = this::reloadNow;

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
        reloadNow();
    }

    /** Immediate reload (startup / explicit refresh). */
    public void reload() {
        reloadNow();
    }

    /** Coalesce rapid package events (updates often fire multiple callbacks). */
    private void scheduleReload() {
        mainHandler.removeCallbacks(debouncedReload);
        mainHandler.postDelayed(debouncedReload, RELOAD_DEBOUNCE_MS);
    }

    private void reloadNow() {
        mainHandler.removeCallbacks(debouncedReload);
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
                scheduleReload();
            }

            @Override
            public void onPackageAdded(String packageName, UserHandle user) {
                scheduleReload();
            }

            @Override
            public void onPackageChanged(String packageName, UserHandle user) {
                iconCache.invalidatePackage(packageName);
                scheduleReload();
            }

            @Override
            public void onPackagesAvailable(String[] packageNames, UserHandle user, boolean replacing) {
                scheduleReload();
            }

            @Override
            public void onPackagesUnavailable(String[] packageNames, UserHandle user, boolean replacing) {
                scheduleReload();
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
                boolean work = isWorkProfile(user);
                List<LauncherActivityInfo> activities = launcherApps.getActivityList(null, user);
                PackageManager pm = appContext.getPackageManager();
                for (LauncherActivityInfo info : activities) {
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
                            AppInfo.readInstallTime(pm, packageName),
                            work));
                }
            }
            if (!result.isEmpty()) {
                return result;
            }
        }
        return loadViaPackageManager();
    }

    private static boolean isWorkProfile(@NonNull UserHandle user) {
        // Secondary profiles returned by UserManager#getUserProfiles are typically work.
        return !Process.myUserHandle().equals(user);
    }

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
                    AppInfo.readInstallTime(pm, packageName),
                    false));
        }
        return result;
    }

    public void onTrimMemory(int level) {
        iconCache.onTrimMemory(level);
    }

    public void shutdown() {
        mainHandler.removeCallbacks(debouncedReload);
        if (launcherApps != null && callback != null) {
            launcherApps.unregisterCallback(callback);
            callback = null;
        }
        executor.shutdownNow();
        iconCache.shutdown();
    }
}
