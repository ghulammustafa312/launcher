package com.yourstudio.aerolauncher.library;

import androidx.annotation.NonNull;

import com.yourstudio.aerolauncher.launcher.AppCategory;
import com.yourstudio.aerolauncher.launcher.AppInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Builds App Library sections from installed apps.
 */
public final class AppLibraryModel {

    private static final long RECENT_WINDOW_MS = TimeUnit.DAYS.toMillis(14);
    private static final int RECENT_LIMIT = 12;

    public static final class Section {
        @NonNull
        public final String title;
        @NonNull
        public final List<AppInfo> apps;

        public Section(@NonNull String title, @NonNull List<AppInfo> apps) {
            this.title = title;
            this.apps = apps;
        }
    }

    private AppLibraryModel() {
    }

    @NonNull
    public static List<AppInfo> visibleApps(
            @NonNull List<AppInfo> all,
            @NonNull Set<String> hiddenKeys) {
        if (hiddenKeys.isEmpty()) {
            return all;
        }
        List<AppInfo> out = new ArrayList<>();
        for (AppInfo app : all) {
            if (!hiddenKeys.contains(app.key)) {
                out.add(app);
            }
        }
        return out;
    }

    @NonNull
    public static List<AppInfo> recentlyAdded(@NonNull List<AppInfo> apps) {
        long cutoff = System.currentTimeMillis() - RECENT_WINDOW_MS;
        List<AppInfo> recent = new ArrayList<>();
        for (AppInfo app : apps) {
            if (app.firstInstallTimeMs >= cutoff) {
                recent.add(app);
            }
        }
        Collections.sort(recent, (a, b) -> Long.compare(b.firstInstallTimeMs, a.firstInstallTimeMs));
        if (recent.size() > RECENT_LIMIT) {
            return new ArrayList<>(recent.subList(0, RECENT_LIMIT));
        }
        return recent;
    }

    @NonNull
    public static Map<AppCategory, List<AppInfo>> byCategory(@NonNull List<AppInfo> apps) {
        Map<AppCategory, List<AppInfo>> map = new EnumMap<>(AppCategory.class);
        for (AppCategory category : AppCategory.values()) {
            map.put(category, new ArrayList<>());
        }
        for (AppInfo app : apps) {
            map.get(app.category).add(app);
        }
        Comparator<AppInfo> byLabel = Comparator.comparing(
                app -> app.label.toString(),
                String.CASE_INSENSITIVE_ORDER);
        for (List<AppInfo> list : map.values()) {
            Collections.sort(list, byLabel);
        }
        return map;
    }

    @NonNull
    public static List<AppInfo> alphabetical(@NonNull List<AppInfo> apps) {
        List<AppInfo> sorted = new ArrayList<>(apps);
        Collections.sort(sorted, Comparator.comparing(
                app -> app.label.toString(),
                String.CASE_INSENSITIVE_ORDER));
        return sorted;
    }

    @NonNull
    public static String letterFor(@NonNull AppInfo app) {
        String label = app.label.toString().trim();
        if (label.isEmpty()) {
            return "#";
        }
        char c = Character.toUpperCase(label.charAt(0));
        if (c >= 'A' && c <= 'Z') {
            return String.valueOf(c);
        }
        return "#";
    }

    @NonNull
    public static String normalizeQuery(@NonNull String query) {
        return query.trim().toLowerCase(Locale.US);
    }
}
