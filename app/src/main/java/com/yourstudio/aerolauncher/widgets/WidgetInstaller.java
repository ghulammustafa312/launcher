package com.yourstudio.aerolauncher.widgets;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.yourstudio.aerolauncher.R;
import com.yourstudio.aerolauncher.widgets.builtin.BatteryWidgetProvider;
import com.yourstudio.aerolauncher.widgets.builtin.CalendarWidgetProvider;
import com.yourstudio.aerolauncher.widgets.builtin.ClockWidgetProvider;
import com.yourstudio.aerolauncher.widgets.builtin.NotesWidgetProvider;
import com.yourstudio.aerolauncher.widgets.builtin.WeatherWidgetProvider;

import java.util.ArrayList;
import java.util.List;

/**
 * Allocates, binds, and configures app widgets for the home widget zone.
 */
public final class WidgetInstaller {

    public interface Callbacks {
        void onWidgetReady(int appWidgetId);

        void onWidgetFailed(@NonNull String reason);
    }

    public static final class Option {
        @NonNull
        public final CharSequence label;
        @NonNull
        public final ComponentName provider;
        public final boolean builtin;

        public Option(
                @NonNull CharSequence label,
                @NonNull ComponentName provider,
                boolean builtin) {
            this.label = label;
            this.provider = provider;
            this.builtin = builtin;
        }
    }

    private final WidgetHostManager hostManager;
    private final ActivityResultLauncher<Intent> bindLauncher;
    private final ActivityResultLauncher<Intent> configureLauncher;

    private int pendingAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    @Nullable
    private Callbacks pendingCallbacks;

    public WidgetInstaller(
            @NonNull WidgetHostManager hostManager,
            @NonNull ActivityResultLauncher<Intent> bindLauncher,
            @NonNull ActivityResultLauncher<Intent> configureLauncher) {
        this.hostManager = hostManager;
        this.bindLauncher = bindLauncher;
        this.configureLauncher = configureLauncher;
    }

    @NonNull
    public List<Option> listOptions(@NonNull Context context) {
        List<Option> options = new ArrayList<>();
        options.add(new Option(
                context.getString(R.string.widget_clock_name),
                new ComponentName(context, ClockWidgetProvider.class),
                true));
        options.add(new Option(
                context.getString(R.string.widget_battery_name),
                new ComponentName(context, BatteryWidgetProvider.class),
                true));
        options.add(new Option(
                context.getString(R.string.widget_notes_name),
                new ComponentName(context, NotesWidgetProvider.class),
                true));
        options.add(new Option(
                context.getString(R.string.widget_weather_name),
                new ComponentName(context, WeatherWidgetProvider.class),
                true));
        options.add(new Option(
                context.getString(R.string.widget_calendar_name),
                new ComponentName(context, CalendarWidgetProvider.class),
                true));

        List<AppWidgetProviderInfo> providers =
                hostManager.getAppWidgetManager().getInstalledProviders();
        String self = context.getPackageName();
        for (AppWidgetProviderInfo info : providers) {
            if (info.provider == null) {
                continue;
            }
            if (self.equals(info.provider.getPackageName())) {
                continue;
            }
            CharSequence label = info.loadLabel(context.getPackageManager());
            if (label == null || label.length() == 0) {
                label = info.provider.flattenToShortString();
            }
            options.add(new Option(label, info.provider, false));
        }
        return options;
    }

    public void install(@NonNull ComponentName provider, @NonNull Callbacks callbacks) {
        cancelPending();
        pendingCallbacks = callbacks;
        pendingAppWidgetId = hostManager.allocateAppWidgetId();
        boolean bound = hostManager.bindIfAllowed(pendingAppWidgetId, provider);
        if (bound) {
            maybeConfigure(pendingAppWidgetId);
            return;
        }
        Intent intent = new Intent(AppWidgetManager.ACTION_APPWIDGET_BIND);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, pendingAppWidgetId);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, provider);
        bindLauncher.launch(intent);
    }

    public void onBindResult(int resultCode, @Nullable Intent data) {
        if (pendingAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            return;
        }
        if (resultCode != Activity.RESULT_OK) {
            hostManager.deleteAppWidgetId(pendingAppWidgetId);
            fail("bind");
            return;
        }
        int id = pendingAppWidgetId;
        if (data != null) {
            id = data.getIntExtra(
                    AppWidgetManager.EXTRA_APPWIDGET_ID, pendingAppWidgetId);
        }
        pendingAppWidgetId = id;
        maybeConfigure(id);
    }

    public void onConfigureResult(int resultCode, @Nullable Intent data) {
        if (pendingAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            return;
        }
        if (resultCode != Activity.RESULT_OK) {
            hostManager.deleteAppWidgetId(pendingAppWidgetId);
            fail("configure");
            return;
        }
        int id = pendingAppWidgetId;
        if (data != null) {
            id = data.getIntExtra(
                    AppWidgetManager.EXTRA_APPWIDGET_ID, pendingAppWidgetId);
        }
        complete(id);
    }

    private void maybeConfigure(int appWidgetId) {
        AppWidgetProviderInfo info = hostManager.getInfo(appWidgetId);
        if (info == null) {
            hostManager.deleteAppWidgetId(appWidgetId);
            fail("missing");
            return;
        }
        if (info.configure != null) {
            Intent intent = new Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE);
            intent.setComponent(info.configure);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            try {
                configureLauncher.launch(intent);
            } catch (Exception e) {
                hostManager.deleteAppWidgetId(appWidgetId);
                fail("configure");
            }
            return;
        }
        complete(appWidgetId);
    }

    private void complete(int appWidgetId) {
        Callbacks callbacks = pendingCallbacks;
        pendingCallbacks = null;
        pendingAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
        if (callbacks != null) {
            callbacks.onWidgetReady(appWidgetId);
        }
    }

    private void fail(@NonNull String reason) {
        Callbacks callbacks = pendingCallbacks;
        pendingCallbacks = null;
        pendingAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
        if (callbacks != null) {
            callbacks.onWidgetFailed(reason);
        }
    }

    public void cancelPending() {
        if (pendingAppWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            hostManager.deleteAppWidgetId(pendingAppWidgetId);
        }
        pendingAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
        pendingCallbacks = null;
    }
}
