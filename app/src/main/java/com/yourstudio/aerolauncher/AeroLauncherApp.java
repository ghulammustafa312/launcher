package com.yourstudio.aerolauncher;

import android.app.Application;

import androidx.annotation.NonNull;

import com.yourstudio.aerolauncher.launcher.AppRepository;
import com.yourstudio.aerolauncher.layout.LayoutRepository;

/**
 * Applies the saved theme and owns the shared app catalog + layout store.
 */
public class AeroLauncherApp extends Application {

    private AppRepository appRepository;
    private LayoutRepository layoutRepository;

    @Override
    public void onCreate() {
        super.onCreate();
        ThemePreferences.applySavedTheme(this);
        appRepository = new AppRepository(this);
        layoutRepository = new LayoutRepository(this);
    }

    @NonNull
    public AppRepository getAppRepository() {
        return appRepository;
    }

    @NonNull
    public LayoutRepository getLayoutRepository() {
        return layoutRepository;
    }
}
