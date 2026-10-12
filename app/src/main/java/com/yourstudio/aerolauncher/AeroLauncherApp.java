package com.yourstudio.aerolauncher;

import android.app.Application;
import android.content.ComponentCallbacks2;

import androidx.annotation.NonNull;

import com.yourstudio.aerolauncher.launcher.AppRepository;
import com.yourstudio.aerolauncher.layout.LayoutRepository;
import com.yourstudio.aerolauncher.widgets.WidgetHostManager;

/**
 * Applies the saved theme and owns the shared app catalog, layout store, and widget host.
 */
public class AeroLauncherApp extends Application {

    private AppRepository appRepository;
    private LayoutRepository layoutRepository;
    private WidgetHostManager widgetHostManager;

    @Override
    public void onCreate() {
        super.onCreate();
        ThemePreferences.applySavedTheme(this);
        appRepository = new AppRepository(this);
        layoutRepository = new LayoutRepository(this);
        widgetHostManager = new WidgetHostManager(this);
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        if (appRepository != null) {
            appRepository.onTrimMemory(level);
        }
        // Drop widget host listening pressure when the UI is gone and memory is tight.
        if (level >= ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN && widgetHostManager != null) {
            widgetHostManager.stopListening();
        }
    }

    @NonNull
    public AppRepository getAppRepository() {
        return appRepository;
    }

    @NonNull
    public LayoutRepository getLayoutRepository() {
        return layoutRepository;
    }

    @NonNull
    public WidgetHostManager getWidgetHostManager() {
        return widgetHostManager;
    }
}
