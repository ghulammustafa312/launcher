package com.yourstudio.aerolauncher.widgets.builtin;

import android.Manifest;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ContentUris;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract;
import android.widget.RemoteViews;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.yourstudio.aerolauncher.R;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * 4×2 calendar card. Shows today's date, and the next event when calendar
 * permission is already granted.
 */
public class CalendarWidgetProvider extends AppWidgetProvider {

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
        Calendar now = Calendar.getInstance();
        String weekday = new SimpleDateFormat("EEEE", Locale.getDefault()).format(now.getTime());
        String day = String.valueOf(now.get(Calendar.DAY_OF_MONTH));
        String event = nextEvent(context, now);

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_calendar);
        views.setTextViewText(R.id.widget_calendar_weekday, weekday);
        views.setTextViewText(R.id.widget_calendar_day, day);
        views.setTextViewText(R.id.widget_calendar_event, event);
        manager.updateAppWidget(appWidgetId, views);
    }

    @NonNull
    private static String nextEvent(@NonNull Context context, @NonNull Calendar now) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR)
                != PackageManager.PERMISSION_GRANTED) {
            return context.getString(R.string.widget_calendar_no_events);
        }
        Calendar end = (Calendar) now.clone();
        end.set(Calendar.HOUR_OF_DAY, 23);
        end.set(Calendar.MINUTE, 59);
        end.set(Calendar.SECOND, 59);
        Uri.Builder builder = CalendarContract.Instances.CONTENT_URI.buildUpon();
        ContentUris.appendId(builder, now.getTimeInMillis());
        ContentUris.appendId(builder, end.getTimeInMillis());
        String[] projection = {CalendarContract.Instances.TITLE};
        try (Cursor cursor = context.getContentResolver().query(
                builder.build(),
                projection,
                null,
                null,
                CalendarContract.Instances.BEGIN + " ASC")) {
            if (cursor == null || !cursor.moveToFirst()) {
                return context.getString(R.string.widget_calendar_no_events);
            }
            String title = cursor.getString(0);
            if (title == null || title.trim().isEmpty()) {
                return context.getString(R.string.widget_calendar_no_events);
            }
            return title;
        } catch (SecurityException ignored) {
            return context.getString(R.string.widget_calendar_no_events);
        }
    }
}
