package com.yourstudio.aerolauncher.launcher;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;
import android.util.TypedValue;
import android.widget.ImageView;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.yourstudio.aerolauncher.R;
import com.yourstudio.aerolauncher.ThemePreferences;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Background icon decode with adaptive-style masks and an in-memory LRU cache.
 */
public final class IconCache {

    private final Context appContext;
    private final ExecutorService executor = Executors.newFixedThreadPool(2);
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final LruCache<String, Drawable> cache;
    private final int densityDpi;
    private final int iconSizePx;

    public IconCache(@NonNull Context context) {
        this.appContext = context.getApplicationContext();
        this.densityDpi = appContext.getResources().getDisplayMetrics().densityDpi;
        this.iconSizePx = Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                52,
                appContext.getResources().getDisplayMetrics()));
        int maxKb = (int) (Runtime.getRuntime().maxMemory() / 1024 / 8);
        cache = new LruCache<String, Drawable>(Math.max(1024, maxKb)) {
            @Override
            protected int sizeOf(@NonNull String key, @NonNull Drawable value) {
                return 48;
            }
        };
    }

    @Nullable
    public Drawable get(@NonNull String key) {
        return cache.get(key);
    }

    public void put(@NonNull String key, @NonNull Drawable drawable) {
        cache.put(key, drawable);
    }

    public void clear() {
        cache.evictAll();
    }

    public void invalidatePackage(@NonNull String packageName) {
        for (String key : cache.snapshot().keySet()) {
            if (key.contains(packageName)) {
                cache.remove(key);
            }
        }
    }

    /** Clears shaped icons when the user changes icon style. */
    public void invalidateAllShapes() {
        cache.evictAll();
    }

    @MainThread
    public void loadInto(@NonNull AppInfo app, @NonNull ImageView target) {
        String style = ThemePreferences.getIconStyle(appContext);
        String cacheKey = app.key + "|" + style;
        target.setTag(R.id.tag_icon_key, cacheKey);
        Drawable cached = cache.get(cacheKey);
        if (cached != null) {
            target.setImageDrawable(cached);
            return;
        }
        target.setImageDrawable(ContextCompat.getDrawable(appContext, R.drawable.ic_app_placeholder));
        executor.execute(() -> {
            Drawable icon = app.loadIcon(appContext, densityDpi);
            if (icon == null) {
                icon = ContextCompat.getDrawable(appContext, R.drawable.ic_app_placeholder);
            }
            if (icon != null) {
                icon = IconShaper.apply(icon, style, iconSizePx, appContext.getResources());
                cache.put(cacheKey, icon);
            }
            Drawable finalIcon = icon;
            mainHandler.post(() -> {
                Object tag = target.getTag(R.id.tag_icon_key);
                if (cacheKey.equals(tag) && finalIcon != null) {
                    target.setImageDrawable(finalIcon);
                }
            });
        });
    }

    public void shutdown() {
        executor.shutdownNow();
    }
}
