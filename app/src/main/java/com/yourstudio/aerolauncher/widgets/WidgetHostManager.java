package com.yourstudio.aerolauncher.widgets;

import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Owns the process {@link LauncherAppWidgetHost} and creates host views.
 */
public final class WidgetHostManager {

    private final Context appContext;
    private final AppWidgetManager appWidgetManager;
    private final LauncherAppWidgetHost host;
    private boolean listening;

    public WidgetHostManager(@NonNull Context context) {
        this.appContext = context.getApplicationContext();
        this.appWidgetManager = AppWidgetManager.getInstance(appContext);
        this.host = new LauncherAppWidgetHost(appContext);
    }

    @NonNull
    public AppWidgetManager getAppWidgetManager() {
        return appWidgetManager;
    }

    @NonNull
    public LauncherAppWidgetHost getHost() {
        return host;
    }

    public void startListening() {
        if (!listening) {
            host.startListening();
            listening = true;
        }
    }

    public void stopListening() {
        if (listening) {
            host.stopListening();
            listening = false;
        }
    }

    public int allocateAppWidgetId() {
        return host.allocateAppWidgetId();
    }

    public void deleteAppWidgetId(int appWidgetId) {
        host.deleteAppWidgetId(appWidgetId);
    }

    @Nullable
    public AppWidgetHostView createView(int appWidgetId, int minWidthDp, int minHeightDp) {
        AppWidgetProviderInfo info = appWidgetManager.getAppWidgetInfo(appWidgetId);
        if (info == null) {
            return null;
        }
        AppWidgetHostView view = host.createView(appContext, appWidgetId, info);
        Bundle options = new Bundle();
        options.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, minWidthDp);
        options.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, minHeightDp);
        options.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, minWidthDp);
        options.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, minHeightDp);
        view.updateAppWidgetOptions(options);
        view.setAppWidget(appWidgetId, info);
        return view;
    }

    public boolean bindIfAllowed(int appWidgetId, @NonNull ComponentName provider) {
        return appWidgetManager.bindAppWidgetIdIfAllowed(appWidgetId, provider);
    }

    @Nullable
    public AppWidgetProviderInfo getInfo(int appWidgetId) {
        return appWidgetManager.getAppWidgetInfo(appWidgetId);
    }
}
