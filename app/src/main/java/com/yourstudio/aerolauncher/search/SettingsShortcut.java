package com.yourstudio.aerolauncher.search;

import android.content.Context;
import android.content.Intent;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import com.yourstudio.aerolauncher.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Built-in system settings shortcuts for home search (no contacts).
 */
public final class SettingsShortcut {

    @NonNull
    public final String id;
    @StringRes
    public final int titleRes;
    @NonNull
    public final Intent intent;

    public SettingsShortcut(
            @NonNull String id,
            @StringRes int titleRes,
            @NonNull Intent intent) {
        this.id = id;
        this.titleRes = titleRes;
        this.intent = intent;
    }

    @NonNull
    public String title(@NonNull Context context) {
        return context.getString(titleRes);
    }

    @NonNull
    public static List<SettingsShortcut> all() {
        List<SettingsShortcut> list = new ArrayList<>();
        list.add(new SettingsShortcut(
                "wifi",
                R.string.search_settings_wifi,
                new Intent(Settings.ACTION_WIFI_SETTINGS)));
        list.add(new SettingsShortcut(
                "bluetooth",
                R.string.search_settings_bluetooth,
                new Intent(Settings.ACTION_BLUETOOTH_SETTINGS)));
        list.add(new SettingsShortcut(
                "display",
                R.string.search_settings_display,
                new Intent(Settings.ACTION_DISPLAY_SETTINGS)));
        list.add(new SettingsShortcut(
                "sound",
                R.string.search_settings_sound,
                new Intent(Settings.ACTION_SOUND_SETTINGS)));
        list.add(new SettingsShortcut(
                "apps",
                R.string.search_settings_apps,
                new Intent(Settings.ACTION_APPLICATION_SETTINGS)));
        list.add(new SettingsShortcut(
                "battery",
                R.string.search_settings_battery,
                new Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS)));
        list.add(new SettingsShortcut(
                "storage",
                R.string.search_settings_storage,
                new Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS)));
        list.add(new SettingsShortcut(
                "locale",
                R.string.search_settings_locale,
                new Intent(Settings.ACTION_LOCALE_SETTINGS)));
        list.add(new SettingsShortcut(
                "date",
                R.string.search_settings_date,
                new Intent(Settings.ACTION_DATE_SETTINGS)));
        list.add(new SettingsShortcut(
                "accessibility",
                R.string.search_settings_accessibility,
                new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        list.add(new SettingsShortcut(
                "about",
                R.string.search_settings_about,
                new Intent(Settings.ACTION_DEVICE_INFO_SETTINGS)));
        return list;
    }
}
