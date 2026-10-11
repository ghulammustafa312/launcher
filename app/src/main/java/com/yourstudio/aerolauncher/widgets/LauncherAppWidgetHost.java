package com.yourstudio.aerolauncher.widgets;

import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;

/**
 * AppWidgetHost for Aero Launcher home pages.
 */
public final class LauncherAppWidgetHost extends AppWidgetHost {

    public static final int HOST_ID = 0xAE20;

    public LauncherAppWidgetHost(Context context) {
        super(context.getApplicationContext(), HOST_ID);
    }

    @Override
    protected AppWidgetHostView onCreateView(
            Context context,
            int appWidgetId,
            AppWidgetProviderInfo appWidget) {
        return new LauncherAppWidgetHostView(context);
    }
}
