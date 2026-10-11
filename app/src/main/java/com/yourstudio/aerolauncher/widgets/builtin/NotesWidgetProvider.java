package com.yourstudio.aerolauncher.widgets.builtin;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import androidx.annotation.NonNull;

import com.yourstudio.aerolauncher.R;

/**
 * Built-in notes widget. Content is stored on-device in SharedPreferences.
 */
public class NotesWidgetProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(
            Context context,
            AppWidgetManager appWidgetManager,
            int[] appWidgetIds) {
        for (int id : appWidgetIds) {
            update(context, appWidgetManager, id);
        }
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        for (int id : appWidgetIds) {
            NotesStorage.delete(context, id);
        }
    }

    public static void update(
            @NonNull Context context,
            @NonNull AppWidgetManager manager,
            int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_notes);
        String text = NotesStorage.get(context, appWidgetId);
        if (text.trim().isEmpty()) {
            views.setTextViewText(
                    R.id.widget_notes_body,
                    context.getString(R.string.widget_notes_empty));
        } else {
            views.setTextViewText(R.id.widget_notes_body, text);
        }

        Intent edit = new Intent(context, NotesWidgetConfigureActivity.class);
        edit.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        edit.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pending = PendingIntent.getActivity(
                context,
                appWidgetId,
                edit,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_notes_body, pending);
        manager.updateAppWidget(appWidgetId, views);
    }
}
