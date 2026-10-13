package com.yourstudio.aerolauncher.widgets.builtin;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.widget.RemoteViews;

import androidx.annotation.NonNull;

import com.yourstudio.aerolauncher.BuildConfig;
import com.yourstudio.aerolauncher.R;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 2×2 weather card. Uses OpenWeatherMap only when WEATHER_API_KEY is supplied
 * at build time (gradle property, not source). Otherwise shows placeholder data.
 */
public class WeatherWidgetProvider extends AppWidgetProvider {

    private static final String PLACEHOLDER_TEMP = "72°";
    private static final String PLACEHOLDER_CONDITION = "Clear";

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
        String key = BuildConfig.WEATHER_API_KEY;
        if (key == null || key.isEmpty()) {
            publish(context, manager, appWidgetId, PLACEHOLDER_TEMP, PLACEHOLDER_CONDITION);
            return;
        }
        Context appContext = context.getApplicationContext();
        new Thread(() -> {
            String[] fetched = fetch(key);
            publish(
                    appContext,
                    AppWidgetManager.getInstance(appContext),
                    appWidgetId,
                    fetched[0],
                    fetched[1]);
        }, "aero-weather").start();
    }

    private static void publish(
            @NonNull Context context,
            @NonNull AppWidgetManager manager,
            int appWidgetId,
            @NonNull String temp,
            @NonNull String condition) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_weather);
        views.setTextViewText(R.id.widget_weather_temp, temp);
        views.setTextViewText(R.id.widget_weather_condition, condition);
        manager.updateAppWidget(appWidgetId, views);
    }

    @NonNull
    private static String[] fetch(@NonNull String key) {
        HttpURLConnection connection = null;
        try {
            String city = URLEncoder.encode("London", StandardCharsets.UTF_8.name());
            URL url = new URL(
                    "https://api.openweathermap.org/data/2.5/weather?q="
                            + city
                            + "&units=metric&appid="
                            + URLEncoder.encode(key, StandardCharsets.UTF_8.name()));
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(8000);
            connection.setReadTimeout(8000);
            connection.setRequestMethod("GET");
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return new String[]{PLACEHOLDER_TEMP, PLACEHOLDER_CONDITION};
            }
            StringBuilder body = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    body.append(line);
                }
            }
            JSONObject json = new JSONObject(body.toString());
            double temp = json.getJSONObject("main").getDouble("temp");
            String condition = json.getJSONArray("weather")
                    .getJSONObject(0)
                    .optString("main", PLACEHOLDER_CONDITION);
            return new String[]{Math.round(temp) + "°", condition};
        } catch (Exception ignored) {
            return new String[]{PLACEHOLDER_TEMP, PLACEHOLDER_CONDITION};
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}
