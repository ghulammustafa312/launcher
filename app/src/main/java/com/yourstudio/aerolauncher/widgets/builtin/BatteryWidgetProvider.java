package com.yourstudio.aerolauncher.widgets.builtin;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.widget.RemoteViews;

import androidx.annotation.NonNull;

import com.yourstudio.aerolauncher.R;

/**
 * Built-in battery level widget. Reads the sticky battery broadcast.
 */
public class BatteryWidgetProvider extends AppWidgetProvider {

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        String action = intent.getAction();
        if (Intent.ACTION_BATTERY_CHANGED.equals(action)
                || Intent.ACTION_POWER_CONNECTED.equals(action)
                || Intent.ACTION_POWER_DISCONNECTED.equals(action)
                || Intent.ACTION_BATTERY_LOW.equals(action)
                || Intent.ACTION_BATTERY_OKAY.equals(action)) {
            AppWidgetManager manager = AppWidgetManager.getInstance(context);
            int[] ids = manager.getAppWidgetIds(
                    new ComponentName(context, BatteryWidgetProvider.class));
            onUpdate(context, manager, ids);
        }
    }

    @Override
    public void onUpdate(
            Context context,
            AppWidgetManager appWidgetManager,
            int[] appWidgetIds) {
        Intent battery = context.registerReceiver(
                null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        for (int id : appWidgetIds) {
            update(context, appWidgetManager, id, battery);
        }
    }

    public static void update(
            @NonNull Context context,
            @NonNull AppWidgetManager manager,
            int appWidgetId,
            Intent battery) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_battery);
        int level = -1;
        int status = BatteryManager.BATTERY_STATUS_UNKNOWN;
        if (battery != null) {
            int rawLevel = battery.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = battery.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
            if (rawLevel >= 0 && scale > 0) {
                level = Math.round(rawLevel * 100f / scale);
            }
            status = battery.getIntExtra(
                    BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN);
        }
        views.setTextViewText(
                R.id.widget_battery_percent,
                level >= 0 ? level + "%" : "—");
        views.setTextViewText(R.id.widget_battery_status, statusLabel(context, status));
        manager.updateAppWidget(appWidgetId, views);
    }

    @NonNull
    private static String statusLabel(@NonNull Context context, int status) {
        switch (status) {
            case BatteryManager.BATTERY_STATUS_CHARGING:
                return context.getString(R.string.widget_battery_charging);
            case BatteryManager.BATTERY_STATUS_FULL:
                return context.getString(R.string.widget_battery_full);
            case BatteryManager.BATTERY_STATUS_DISCHARGING:
            case BatteryManager.BATTERY_STATUS_NOT_CHARGING:
                return context.getString(R.string.widget_battery_discharging);
            default:
                return context.getString(R.string.widget_battery_unknown);
        }
    }
}
