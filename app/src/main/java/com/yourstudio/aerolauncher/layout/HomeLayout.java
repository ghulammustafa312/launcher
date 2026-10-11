package com.yourstudio.aerolauncher.layout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.yourstudio.aerolauncher.HomePreferences;
import com.yourstudio.aerolauncher.widgets.WidgetPlacement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Full home layout: paged workspace cells, dock slots, page widgets, hidden apps.
 */
public final class HomeLayout {

    @NonNull
    public final List<List<HomeItem>> pages;
    @NonNull
    public final List<HomeItem> dock;
    @NonNull
    public final List<List<WidgetPlacement>> pageWidgets;
    @NonNull
    public final Set<String> hiddenAppKeys;

    public HomeLayout(
            @NonNull List<List<HomeItem>> pages,
            @NonNull List<HomeItem> dock,
            @NonNull List<List<WidgetPlacement>> pageWidgets,
            @NonNull Set<String> hiddenAppKeys) {
        this.pages = pages;
        this.dock = dock;
        this.pageWidgets = pageWidgets;
        this.hiddenAppKeys = hiddenAppKeys;
    }

    @NonNull
    public static HomeLayout empty(int pageCapacity) {
        List<HomeItem> page = new ArrayList<>(Collections.nCopies(pageCapacity, null));
        List<HomeItem> dock = new ArrayList<>(Collections.nCopies(HomePreferences.DOCK_SLOTS, null));
        List<List<HomeItem>> pages = new ArrayList<>();
        pages.add(page);
        List<List<WidgetPlacement>> widgets = new ArrayList<>();
        widgets.add(new ArrayList<>());
        return new HomeLayout(pages, dock, widgets, new HashSet<>());
    }

    @Nullable
    public HomeItem getWorkspace(int page, int cell) {
        if (page < 0 || page >= pages.size()) {
            return null;
        }
        List<HomeItem> cells = pages.get(page);
        if (cell < 0 || cell >= cells.size()) {
            return null;
        }
        return cells.get(cell);
    }

    public void setWorkspace(int page, int cell, @Nullable HomeItem item) {
        ensurePage(page, cell + 1);
        pages.get(page).set(cell, item);
    }

    @Nullable
    public HomeItem getDock(int index) {
        if (index < 0 || index >= dock.size()) {
            return null;
        }
        return dock.get(index);
    }

    public void setDock(int index, @Nullable HomeItem item) {
        while (dock.size() <= index) {
            dock.add(null);
        }
        dock.set(index, item);
    }

    @NonNull
    public List<WidgetPlacement> widgetsForPage(int page) {
        ensureWidgetPage(page);
        return pageWidgets.get(page);
    }

    public void ensurePage(int pageIndex, int capacity) {
        while (pages.size() <= pageIndex) {
            pages.add(new ArrayList<>(Collections.nCopies(capacity, null)));
        }
        ensureWidgetPage(pageIndex);
        List<HomeItem> page = pages.get(pageIndex);
        while (page.size() < capacity) {
            page.add(null);
        }
        while (page.size() > capacity) {
            int last = page.size() - 1;
            if (page.get(last) == null) {
                page.remove(last);
            } else {
                break;
            }
        }
    }

    public void ensureWidgetPage(int pageIndex) {
        while (pageWidgets.size() <= pageIndex) {
            pageWidgets.add(new ArrayList<>());
        }
    }

    public int pageCount() {
        return Math.max(1, pages.size());
    }

    public void trimEmptyPages() {
        while (pages.size() > 1) {
            List<HomeItem> last = pages.get(pages.size() - 1);
            boolean empty = true;
            for (HomeItem item : last) {
                if (item != null) {
                    empty = false;
                    break;
                }
            }
            List<WidgetPlacement> widgets = pageWidgets.size() >= pages.size()
                    ? pageWidgets.get(pages.size() - 1)
                    : Collections.emptyList();
            if (empty && widgets.isEmpty()) {
                pages.remove(pages.size() - 1);
                if (pageWidgets.size() >= pages.size() + 1) {
                    pageWidgets.remove(pageWidgets.size() - 1);
                }
            } else {
                break;
            }
        }
    }

    @NonNull
    public Set<String> collectPlacedAppKeys() {
        Set<String> keys = new HashSet<>();
        for (List<HomeItem> page : pages) {
            for (HomeItem item : page) {
                collectKeys(item, keys);
            }
        }
        for (HomeItem item : dock) {
            collectKeys(item, keys);
        }
        return keys;
    }

    private static void collectKeys(@Nullable HomeItem item, @NonNull Set<String> keys) {
        if (item == null) {
            return;
        }
        if (item.isApp() && item.appKey != null) {
            keys.add(item.appKey);
        } else if (item.isFolder()) {
            keys.addAll(item.folderAppKeys);
        }
    }
}
