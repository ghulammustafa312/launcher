package com.yourstudio.aerolauncher;

import android.app.Application;

/**
 * Applies the saved theme mode before any activity draws.
 */
public class AeroLauncherApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        ThemePreferences.applySavedTheme(this);
    }
}
