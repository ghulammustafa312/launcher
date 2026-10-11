package com.yourstudio.aerolauncher;

import android.app.Application;

import androidx.annotation.NonNull;

import com.yourstudio.aerolauncher.launcher.AppRepository;

/**
 * Applies the saved theme and owns the shared app catalog.
 */
public class AeroLauncherApp extends Application {

    private AppRepository appRepository;

    @Override
    public void onCreate() {
        super.onCreate();
        ThemePreferences.applySavedTheme(this);
        appRepository = new AppRepository(this);
    }

    @NonNull
    public AppRepository getAppRepository() {
        return appRepository;
    }
}
