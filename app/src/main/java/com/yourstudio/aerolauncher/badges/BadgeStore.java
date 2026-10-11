package com.yourstudio.aerolauncher.badges;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * In-memory notification counts keyed by package name only.
 * Never stores notification titles, text, or other content.
 */
public final class BadgeStore {

    public interface Listener {
        void onBadgesChanged();
    }

    private static final BadgeStore INSTANCE = new BadgeStore();

    private final Map<String, Integer> counts = new HashMap<>();
    private final Set<Listener> listeners = new CopyOnWriteArraySet<>();

    private BadgeStore() {
    }

    @NonNull
    public static BadgeStore get() {
        return INSTANCE;
    }

    public void addListener(@NonNull Listener listener) {
        listeners.add(listener);
    }

    public void removeListener(@NonNull Listener listener) {
        listeners.remove(listener);
    }

    public int getCount(@Nullable String packageName) {
        if (packageName == null) {
            return 0;
        }
        Integer value = counts.get(packageName);
        return value == null ? 0 : value;
    }

    @MainThread
    public void replaceAll(@NonNull Map<String, Integer> next) {
        counts.clear();
        counts.putAll(next);
        for (Listener listener : listeners) {
            listener.onBadgesChanged();
        }
    }

    @MainThread
    public void clear() {
        if (counts.isEmpty()) {
            return;
        }
        counts.clear();
        for (Listener listener : listeners) {
            listener.onBadgesChanged();
        }
    }

    @NonNull
    public Map<String, Integer> snapshot() {
        return Collections.unmodifiableMap(new HashMap<>(counts));
    }
}
