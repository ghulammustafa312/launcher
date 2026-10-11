package com.yourstudio.aerolauncher;

import android.app.Application;

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
