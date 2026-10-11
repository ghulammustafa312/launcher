package com.yourstudio.aerolauncher.launcher;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;
import android.widget.ImageView;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.yourstudio.aerolauncher.R;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Background icon decode with an in-memory LRU cache.
 */
public final class IconCache {

    private final Context appContext;
    private final ExecutorService executor = Executors.newFixedThreadPool(2);
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final LruCache<String, Drawable> cache;
    private final int densityDpi;

    public IconCache(@NonNull Context context) {
        this.appContext = context.getApplicationContext();
        this.densityDpi = appContext.getResources().getDisplayMetrics().densityDpi;
        // Roughly 1/8 of max memory for bitmaps/drawables.
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
            if (key.startsWith(packageName + "/") || key.contains(packageName)) {
                cache.remove(key);
            }
        }
    }

    /**
     * Loads the icon for {@code app} into {@code target} off the main thread.
     */
    @MainThread
    public void loadInto(@NonNull AppInfo app, @NonNull ImageView target) {
        target.setTag(R.id.tag_icon_key, app.key);
        Drawable cached = cache.get(app.key);
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
                cache.put(app.key, icon);
            }
            Drawable finalIcon = icon;
            mainHandler.post(() -> {
                Object tag = target.getTag(R.id.tag_icon_key);
                if (app.key.equals(tag) && finalIcon != null) {
                    target.setImageDrawable(finalIcon);
                }
            });
        });
    }

    public void shutdown() {
        executor.shutdownNow();
    }
}
