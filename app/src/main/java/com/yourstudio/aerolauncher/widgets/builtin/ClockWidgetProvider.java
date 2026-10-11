package com.yourstudio.aerolauncher.widgets.builtin;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.widget.RemoteViews;

import androidx.annotation.NonNull;

import com.yourstudio.aerolauncher.R;

/**
 * Built-in clock widget. TextClock handles ticking without host-side timers.
 */
public class ClockWidgetProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(
            Context context,
            AppWidgetManager appWidgetManager,
            int[] appWidgetIds) {
        for (int id : appWidgetIds) {
            update(context, appWidgetManager, id);
        }
    }

    public static void update(
            @NonNull Context context,
            @NonNull AppWidgetManager manager,
            int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_clock);
        manager.updateAppWidget(appWidgetId, views);
    }
}
