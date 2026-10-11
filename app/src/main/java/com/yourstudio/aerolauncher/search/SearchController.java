package com.yourstudio.aerolauncher.search;

import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.annotation.NonNull;

import com.yourstudio.aerolauncher.launcher.AppInfo;
import com.yourstudio.aerolauncher.library.AppLibraryModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Filters apps and settings shortcuts; opens a web search as fallback.
 */
public final class SearchController {

    public static final class Results {
        @NonNull
        public final List<AppInfo> apps = new ArrayList<>();
        @NonNull
        public final List<SettingsShortcut> settings = new ArrayList<>();
        @NonNull
        public String query = "";
    }

    private SearchController() {
    }

    @NonNull
    public static Results search(
            @NonNull Context context,
            @NonNull String rawQuery,
            @NonNull List<AppInfo> installed,
            @NonNull Set<String> hiddenKeys) {
        Results results = new Results();
        String query = AppLibraryModel.normalizeQuery(rawQuery);
        results.query = query;
        if (query.isEmpty()) {
            return results;
        }

        List<AppInfo> visible = AppLibraryModel.visibleApps(installed, hiddenKeys);
        for (AppInfo app : visible) {
            if (app.label.toString().toLowerCase(Locale.US).contains(query)
                    || app.packageName.toLowerCase(Locale.US).contains(query)) {
                results.apps.add(app);
            }
        }

        for (SettingsShortcut shortcut : SettingsShortcut.all()) {
            if (shortcut.title(context).toLowerCase(Locale.US).contains(query)
                    || shortcut.id.contains(query)) {
                results.settings.add(shortcut);
            }
        }
        return results;
    }

    public static void openWebSearch(@NonNull Context context, @NonNull String query) {
        String trimmed = query.trim();
        if (trimmed.isEmpty()) {
            return;
        }
        Intent intent = new Intent(Intent.ACTION_WEB_SEARCH);
        intent.putExtra(SearchManager.QUERY, trimmed);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            context.startActivity(intent);
            return;
        } catch (ActivityNotFoundException ignored) {
            // Fall through to a browser URL.
        }
        Uri uri = Uri.parse("https://www.google.com/search?q=" + Uri.encode(trimmed));
        Intent browser = new Intent(Intent.ACTION_VIEW, uri);
        browser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            context.startActivity(browser);
        } catch (ActivityNotFoundException ignored) {
            // No browser available.
        }
    }

    public static void openSettingsShortcut(
            @NonNull Context context,
            @NonNull SettingsShortcut shortcut) {
        Intent intent = new Intent(shortcut.intent);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            context.startActivity(intent);
        } catch (ActivityNotFoundException ignored) {
            // Shortcut unavailable on this device.
        }
    }
}
