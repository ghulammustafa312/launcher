package com.yourstudio.aerolauncher.layout;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.yourstudio.aerolauncher.HomePreferences;
import com.yourstudio.aerolauncher.data.HiddenAppEntity;
import com.yourstudio.aerolauncher.data.LayoutDao;
import com.yourstudio.aerolauncher.data.LayoutDatabase;
import com.yourstudio.aerolauncher.data.LayoutItemEntity;
import com.yourstudio.aerolauncher.launcher.AppInfo;
import com.yourstudio.aerolauncher.widgets.WidgetPlacement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Loads / saves {@link HomeLayout} in Room and keeps an in-memory copy for the UI.
 */
public final class LayoutRepository {

    public interface Listener {
        @MainThread
        void onLayoutChanged(@NonNull HomeLayout layout);
    }

    private final Context appContext;
    private final LayoutDao dao;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final CopyOnWriteArrayList<Listener> listeners = new CopyOnWriteArrayList<>();

    @NonNull
    private volatile HomeLayout layout;

    public LayoutRepository(@NonNull Context context) {
        this.appContext = context.getApplicationContext();
        this.dao = LayoutDatabase.getInstance(appContext).layoutDao();
        this.layout = HomeLayout.empty(HomePreferences.pageCapacity(appContext));
    }

    @NonNull
    public HomeLayout getLayout() {
        return layout;
    }

    public void addListener(@NonNull Listener listener) {
        listeners.add(listener);
        listener.onLayoutChanged(layout);
    }

    public void removeListener(@NonNull Listener listener) {
        listeners.remove(listener);
    }

    /**
     * Loads DB layout (or builds a default), then merges with currently installed apps.
     */
    public void loadAndSync(@NonNull List<AppInfo> installedApps) {
        int capacity = HomePreferences.pageCapacity(appContext);
        executor.execute(() -> {
            HomeLayout loaded = readFromDb(capacity);
            if (isEffectivelyEmpty(loaded)) {
                loaded = buildDefault(installedApps, capacity);
                persistBlocking(loaded);
            } else {
                loaded = syncInstalled(loaded, installedApps, capacity);
                persistBlocking(loaded);
            }
            HomeLayout finalLayout = loaded;
            mainHandler.post(() -> {
                layout = finalLayout;
                notifyListeners();
            });
        });
    }

    public void saveAsync(@NonNull HomeLayout newLayout) {
        layout = newLayout;
        notifyListeners();
        executor.execute(() -> persistBlocking(newLayout));
    }

    public void replaceFromBackup(@NonNull HomeLayout newLayout) {
        saveAsync(newLayout);
    }

    @MainThread
    private void notifyListeners() {
        for (Listener listener : listeners) {
            listener.onLayoutChanged(layout);
        }
    }

    private boolean isEffectivelyEmpty(@NonNull HomeLayout homeLayout) {
        for (List<HomeItem> page : homeLayout.pages) {
            for (HomeItem item : page) {
                if (item != null) {
                    return false;
                }
            }
        }
        for (HomeItem item : homeLayout.dock) {
            if (item != null) {
                return false;
            }
        }
        return true;
    }

    @NonNull
    private HomeLayout buildDefault(@NonNull List<AppInfo> apps, int capacity) {
        HomeLayout homeLayout = HomeLayout.empty(capacity);
        List<AppInfo> visible = filterHidden(apps, homeLayout.hiddenAppKeys);
        int dockCount = Math.min(HomePreferences.DOCK_SLOTS, visible.size());
        for (int i = 0; i < dockCount; i++) {
            homeLayout.setDock(i, HomeItem.app(visible.get(i).key));
        }
        List<AppInfo> rest = visible.subList(dockCount, visible.size());
        int page = 0;
        int cell = 0;
        homeLayout.ensurePage(0, capacity);
        for (AppInfo app : rest) {
            if (cell >= capacity) {
                page++;
                cell = 0;
                homeLayout.ensurePage(page, capacity);
            }
            homeLayout.setWorkspace(page, cell, HomeItem.app(app.key));
            cell++;
        }
        return homeLayout;
    }

    @NonNull
    private HomeLayout syncInstalled(
            @NonNull HomeLayout existing,
            @NonNull List<AppInfo> installedApps,
            int capacity) {
        Set<String> installedKeys = new HashSet<>();
        for (AppInfo app : installedApps) {
            installedKeys.add(app.key);
        }

        // Drop uninstalled apps from workspace, dock, and folders.
        for (int p = 0; p < existing.pages.size(); p++) {
            List<HomeItem> page = existing.pages.get(p);
            for (int c = 0; c < page.size(); c++) {
                page.set(c, scrubItem(page.get(c), installedKeys));
            }
        }
        for (int i = 0; i < existing.dock.size(); i++) {
            existing.dock.set(i, scrubItem(existing.dock.get(i), installedKeys));
        }
        existing.hiddenAppKeys.removeIf(key -> !installedKeys.contains(key));

        // Resize pages to current capacity.
        for (int p = 0; p < existing.pages.size(); p++) {
            existing.ensurePage(p, capacity);
        }

        Set<String> placed = existing.collectPlacedAppKeys();
        List<AppInfo> toAdd = new ArrayList<>();
        for (AppInfo app : installedApps) {
            if (existing.hiddenAppKeys.contains(app.key)) {
                continue;
            }
            if (!placed.contains(app.key)) {
                toAdd.add(app);
            }
        }
        appendApps(existing, toAdd, capacity);
        existing.trimEmptyPages();
        return existing;
    }

    @Nullable
    private static HomeItem scrubItem(@Nullable HomeItem item, @NonNull Set<String> installedKeys) {
        if (item == null) {
            return null;
        }
        if (item.isApp()) {
            return installedKeys.contains(item.appKey) ? item : null;
        }
        item.folderAppKeys.removeIf(key -> !installedKeys.contains(key));
        if (item.folderAppKeys.isEmpty()) {
            return null;
        }
        if (item.folderAppKeys.size() == 1) {
            return HomeItem.app(item.folderAppKeys.get(0));
        }
        return item;
    }

    private static void appendApps(
            @NonNull HomeLayout homeLayout,
            @NonNull List<AppInfo> apps,
            int capacity) {
        if (apps.isEmpty()) {
            return;
        }
        int page = Math.max(0, homeLayout.pages.size() - 1);
        homeLayout.ensurePage(page, capacity);
        int cell = 0;
        List<HomeItem> current = homeLayout.pages.get(page);
        while (cell < current.size() && current.get(cell) != null) {
            cell++;
        }
        for (AppInfo app : apps) {
            while (cell >= capacity) {
                page++;
                cell = 0;
                homeLayout.ensurePage(page, capacity);
                current = homeLayout.pages.get(page);
            }
            homeLayout.setWorkspace(page, cell, HomeItem.app(app.key));
            cell++;
        }
    }

    @NonNull
    private static List<AppInfo> filterHidden(
            @NonNull List<AppInfo> apps,
            @NonNull Set<String> hidden) {
        if (hidden.isEmpty()) {
            return apps;
        }
        List<AppInfo> out = new ArrayList<>();
        for (AppInfo app : apps) {
            if (!hidden.contains(app.key)) {
                out.add(app);
            }
        }
        return out;
    }

    @NonNull
    private HomeLayout readFromDb(int capacity) {
        List<LayoutItemEntity> entities = dao.getAllItems();
        List<HiddenAppEntity> hiddenEntities = dao.getHiddenApps();
        Set<String> hidden = new HashSet<>();
        for (HiddenAppEntity entity : hiddenEntities) {
            hidden.add(entity.appKey);
        }

        Map<String, HomeItem> folders = new HashMap<>();
        Map<String, List<String>> folderKids = new HashMap<>();
        for (LayoutItemEntity entity : entities) {
            boolean isWorkspaceOrDockFolder =
                    LayoutItemEntity.TYPE_FOLDER.equals(entity.type)
                            && (LayoutItemEntity.CONTAINER_WORKSPACE.equals(entity.container)
                            || LayoutItemEntity.CONTAINER_DOCK.equals(entity.container));
            if (isWorkspaceOrDockFolder) {
                folders.put(entity.id, HomeItem.folder(
                        entity.id,
                        entity.title != null ? entity.title : "Folder",
                        new ArrayList<>()));
                folderKids.put(entity.id, new ArrayList<>());
            }
        }
        // Folder members
        List<LayoutItemEntity> members = new ArrayList<>();
        for (LayoutItemEntity entity : entities) {
            if (LayoutItemEntity.CONTAINER_FOLDER.equals(entity.container)) {
                members.add(entity);
            }
        }
        Collections.sort(members, (a, b) -> Integer.compare(a.rank, b.rank));
        for (LayoutItemEntity member : members) {
            List<String> kids = folderKids.get(member.parentId);
            if (kids != null && member.appKey != null) {
                kids.add(member.appKey);
            }
        }
        for (Map.Entry<String, HomeItem> entry : folders.entrySet()) {
            List<String> kids = folderKids.get(entry.getKey());
            if (kids != null) {
                entry.getValue().folderAppKeys.clear();
                entry.getValue().folderAppKeys.addAll(kids);
            }
        }

        int maxPage = 0;
        for (LayoutItemEntity entity : entities) {
            if (LayoutItemEntity.CONTAINER_WORKSPACE.equals(entity.container)) {
                maxPage = Math.max(maxPage, entity.pageIndex);
            }
        }
        HomeLayout homeLayout = HomeLayout.empty(capacity);
        for (int p = 0; p <= maxPage; p++) {
            homeLayout.ensurePage(p, capacity);
        }
        while (homeLayout.dock.size() < HomePreferences.DOCK_SLOTS) {
            homeLayout.dock.add(null);
        }

        for (LayoutItemEntity entity : entities) {
            if (LayoutItemEntity.CONTAINER_FOLDER.equals(entity.container)) {
                continue;
            }
            HomeItem item;
            if (LayoutItemEntity.TYPE_FOLDER.equals(entity.type)) {
                item = folders.get(entity.id);
                if (item == null || item.folderAppKeys.isEmpty()) {
                    continue;
                }
            } else if (LayoutItemEntity.TYPE_WIDGET.equals(entity.type)
                    || LayoutItemEntity.TYPE_WIDGET_STACK.equals(entity.type)) {
                continue;
            } else if (entity.appKey != null) {
                item = HomeItem.app(entity.appKey);
            } else {
                continue;
            }
            if (LayoutItemEntity.CONTAINER_DOCK.equals(entity.container)) {
                if (entity.dockIndex >= 0 && entity.dockIndex < HomePreferences.DOCK_SLOTS) {
                    homeLayout.setDock(entity.dockIndex, item);
                }
            } else if (LayoutItemEntity.CONTAINER_WORKSPACE.equals(entity.container)) {
                homeLayout.ensurePage(entity.pageIndex, capacity);
                if (entity.cellIndex >= 0 && entity.cellIndex < capacity) {
                    homeLayout.setWorkspace(entity.pageIndex, entity.cellIndex, item);
                }
            }
        }

        for (LayoutItemEntity entity : entities) {
            if (!LayoutItemEntity.CONTAINER_PAGE_WIDGETS.equals(entity.container)) {
                continue;
            }
            List<Integer> ids = parseWidgetIds(entity);
            if (ids.isEmpty()) {
                continue;
            }
            boolean stack = LayoutItemEntity.TYPE_WIDGET_STACK.equals(entity.type) || ids.size() > 1;
            WidgetPlacement placement = new WidgetPlacement(
                    entity.id,
                    stack,
                    ids,
                    entity.spanRows > 0 ? entity.spanRows : 2,
                    entity.rank);
            homeLayout.ensureWidgetPage(entity.pageIndex);
            homeLayout.pageWidgets.get(entity.pageIndex).add(placement);
        }
        for (List<WidgetPlacement> list : homeLayout.pageWidgets) {
            Collections.sort(list, (a, b) -> Integer.compare(a.rank, b.rank));
        }

        homeLayout.hiddenAppKeys.clear();
        homeLayout.hiddenAppKeys.addAll(hidden);
        homeLayout.trimEmptyPages();
        return homeLayout;
    }

    @NonNull
    private static List<Integer> parseWidgetIds(@NonNull LayoutItemEntity entity) {
        List<Integer> ids = new ArrayList<>();
        if (entity.widgetIdsCsv != null && !entity.widgetIdsCsv.isEmpty()) {
            for (String part : entity.widgetIdsCsv.split(",")) {
                try {
                    ids.add(Integer.parseInt(part.trim()));
                } catch (NumberFormatException ignored) {
                    // Skip bad token.
                }
            }
        } else if (entity.appWidgetId > 0) {
            ids.add(entity.appWidgetId);
        }
        return ids;
    }

    private void persistBlocking(@NonNull HomeLayout homeLayout) {
        List<LayoutItemEntity> entities = new ArrayList<>();
        for (int p = 0; p < homeLayout.pages.size(); p++) {
            List<HomeItem> page = homeLayout.pages.get(p);
            for (int c = 0; c < page.size(); c++) {
                HomeItem item = page.get(c);
                if (item == null) {
                    continue;
                }
                appendItemEntities(
                        entities,
                        item,
                        LayoutItemEntity.CONTAINER_WORKSPACE,
                        null,
                        p,
                        c,
                        -1);
            }
        }
        for (int d = 0; d < homeLayout.dock.size(); d++) {
            HomeItem item = homeLayout.dock.get(d);
            if (item == null) {
                continue;
            }
            appendItemEntities(
                    entities,
                    item,
                    LayoutItemEntity.CONTAINER_DOCK,
                    null,
                    -1,
                    -1,
                    d);
        }
        for (int p = 0; p < homeLayout.pageWidgets.size(); p++) {
            List<WidgetPlacement> widgets = homeLayout.pageWidgets.get(p);
            for (int i = 0; i < widgets.size(); i++) {
                WidgetPlacement widget = widgets.get(i);
                widget.rank = i;
                StringBuilder csv = new StringBuilder();
                for (int w = 0; w < widget.appWidgetIds.size(); w++) {
                    if (w > 0) {
                        csv.append(',');
                    }
                    csv.append(widget.appWidgetIds.get(w));
                }
                int firstId = widget.appWidgetIds.isEmpty() ? 0 : widget.appWidgetIds.get(0);
                outWidget(entities, widget, p, firstId, csv.toString());
            }
        }
        List<HiddenAppEntity> hidden = new ArrayList<>();
        for (String key : homeLayout.hiddenAppKeys) {
            hidden.add(new HiddenAppEntity(key));
        }
        dao.replaceAll(entities, hidden);
    }

    private static void outWidget(
            @NonNull List<LayoutItemEntity> out,
            @NonNull WidgetPlacement widget,
            int page,
            int firstId,
            @NonNull String csv) {
        out.add(new LayoutItemEntity(
                widget.id,
                widget.stack || widget.appWidgetIds.size() > 1
                        ? LayoutItemEntity.TYPE_WIDGET_STACK
                        : LayoutItemEntity.TYPE_WIDGET,
                LayoutItemEntity.CONTAINER_PAGE_WIDGETS,
                null,
                page,
                -1,
                -1,
                widget.rank,
                null,
                null,
                firstId,
                widget.spanRows,
                csv));
    }

    private static void appendItemEntities(
            @NonNull List<LayoutItemEntity> out,
            @NonNull HomeItem item,
            @NonNull String container,
            @Nullable String parentId,
            int page,
            int cell,
            int dock) {
        if (item.isApp()) {
            out.add(new LayoutItemEntity(
                    item.id,
                    LayoutItemEntity.TYPE_APP,
                    container,
                    parentId,
                    page,
                    cell,
                    dock,
                    0,
                    item.appKey,
                    null,
                    0,
                    0,
                    null));
            return;
        }
        String folderId = item.id;
        out.add(new LayoutItemEntity(
                folderId,
                LayoutItemEntity.TYPE_FOLDER,
                container,
                parentId,
                page,
                cell,
                dock,
                0,
                null,
                item.title,
                0,
                0,
                null));
        int rank = 0;
        for (String appKey : item.folderAppKeys) {
            out.add(new LayoutItemEntity(
                    "app:" + appKey + "#in#" + folderId + "#" + UUID.randomUUID(),
                    LayoutItemEntity.TYPE_APP,
                    LayoutItemEntity.CONTAINER_FOLDER,
                    folderId,
                    -1,
                    -1,
                    -1,
                    rank++,
                    appKey,
                    null,
                    0,
                    0,
                    null));
        }
    }

    // --- layout mutations (main thread updates + async save) ---

    public void hideApp(@NonNull String appKey) {
        HomeLayout copy = deepCopy(layout);
        removeAppEverywhere(copy, appKey);
        copy.hiddenAppKeys.add(appKey);
        saveAsync(copy);
    }

    public void unhideApp(@NonNull String appKey) {
        HomeLayout copy = deepCopy(layout);
        if (!copy.hiddenAppKeys.remove(appKey)) {
            return;
        }
        saveAsync(copy);
    }

    public void removeFromHome(@NonNull ItemLocation location) {
        HomeLayout copy = deepCopy(layout);
        setAt(copy, location, null);
        copy.trimEmptyPages();
        saveAsync(copy);
    }

    public void renameFolder(@NonNull String folderId, @NonNull String title) {
        HomeLayout copy = deepCopy(layout);
        HomeItem folder = findById(copy, folderId);
        if (folder != null && folder.isFolder()) {
            folder.title = title.trim().isEmpty() ? "Folder" : title.trim();
            saveAsync(copy);
        }
    }

    public boolean moveOrMerge(
            @NonNull ItemLocation from,
            @NonNull ItemLocation to,
            int pageCapacity) {
        if (from.encode().equals(to.encode())) {
            return false;
        }
        HomeLayout copy = deepCopy(layout);
        copy.ensurePage(Math.max(from.pageIndex, to.pageIndex), pageCapacity);
        if (to.container == ItemLocation.Container.WORKSPACE) {
            copy.ensurePage(to.pageIndex, pageCapacity);
        }

        HomeItem moving = getAt(copy, from);
        if (moving == null) {
            return false;
        }
        HomeItem target = getAt(copy, to);

        setAt(copy, from, null);

        if (target == null) {
            setAt(copy, to, moving);
        } else if (target.isFolder() && moving.isApp() && moving.appKey != null) {
            if (!target.folderAppKeys.contains(moving.appKey)) {
                target.folderAppKeys.add(moving.appKey);
            }
            setAt(copy, to, target);
        } else if (target.isApp() && moving.isApp()
                && target.appKey != null && moving.appKey != null
                && !target.appKey.equals(moving.appKey)) {
            List<String> keys = new ArrayList<>();
            keys.add(target.appKey);
            keys.add(moving.appKey);
            setAt(copy, to, HomeItem.folder("Folder", keys));
        } else if (target.isApp() && moving.isFolder()) {
            // Drop folder onto app: put app into folder, place folder.
            if (target.appKey != null && !moving.folderAppKeys.contains(target.appKey)) {
                moving.folderAppKeys.add(0, target.appKey);
            }
            setAt(copy, to, moving);
        } else if (target.isFolder() && moving.isFolder()) {
            for (String key : moving.folderAppKeys) {
                if (!target.folderAppKeys.contains(key)) {
                    target.folderAppKeys.add(key);
                }
            }
            setAt(copy, to, target);
        } else {
            // Swap as fallback.
            setAt(copy, to, moving);
            setAt(copy, from, target);
        }

        // Ensure an empty page exists when last cell of last page is used.
        ensureTrailingEmptyPage(copy, pageCapacity);
        copy.trimEmptyPages();
        // Keep one empty page for dropping onto a new page if the last is full.
        ensureTrailingEmptyPage(copy, pageCapacity);
        saveAsync(copy);
        return true;
    }

    private static void ensureTrailingEmptyPage(@NonNull HomeLayout homeLayout, int capacity) {
        List<HomeItem> last = homeLayout.pages.get(homeLayout.pages.size() - 1);
        boolean full = true;
        for (HomeItem item : last) {
            if (item == null) {
                full = false;
                break;
            }
        }
        if (full) {
            homeLayout.ensurePage(homeLayout.pages.size(), capacity);
        }
    }

    @Nullable
    private static HomeItem getAt(@NonNull HomeLayout homeLayout, @NonNull ItemLocation location) {
        if (location.container == ItemLocation.Container.DOCK) {
            return homeLayout.getDock(location.dockIndex);
        }
        return homeLayout.getWorkspace(location.pageIndex, location.cellIndex);
    }

    private static void setAt(
            @NonNull HomeLayout homeLayout,
            @NonNull ItemLocation location,
            @Nullable HomeItem item) {
        if (location.container == ItemLocation.Container.DOCK) {
            homeLayout.setDock(location.dockIndex, item);
        } else {
            homeLayout.setWorkspace(location.pageIndex, location.cellIndex, item);
        }
    }

    private static void removeAppEverywhere(@NonNull HomeLayout homeLayout, @NonNull String appKey) {
        for (int p = 0; p < homeLayout.pages.size(); p++) {
            List<HomeItem> page = homeLayout.pages.get(p);
            for (int c = 0; c < page.size(); c++) {
                page.set(c, removeAppFromItem(page.get(c), appKey));
            }
        }
        for (int i = 0; i < homeLayout.dock.size(); i++) {
            homeLayout.dock.set(i, removeAppFromItem(homeLayout.dock.get(i), appKey));
        }
        homeLayout.trimEmptyPages();
    }

    @Nullable
    private static HomeItem removeAppFromItem(@Nullable HomeItem item, @NonNull String appKey) {
        if (item == null) {
            return null;
        }
        if (item.isApp()) {
            return appKey.equals(item.appKey) ? null : item;
        }
        item.folderAppKeys.remove(appKey);
        if (item.folderAppKeys.isEmpty()) {
            return null;
        }
        if (item.folderAppKeys.size() == 1) {
            return HomeItem.app(item.folderAppKeys.get(0));
        }
        return item;
    }

    @Nullable
    private static HomeItem findById(@NonNull HomeLayout homeLayout, @NonNull String id) {
        for (List<HomeItem> page : homeLayout.pages) {
            for (HomeItem item : page) {
                if (item != null && id.equals(item.id)) {
                    return item;
                }
            }
        }
        for (HomeItem item : homeLayout.dock) {
            if (item != null && id.equals(item.id)) {
                return item;
            }
        }
        return null;
    }

    public void addWidgetToPage(int pageIndex, @NonNull WidgetPlacement placement) {
        HomeLayout copy = deepCopy(layout);
        copy.ensureWidgetPage(pageIndex);
        placement.rank = copy.pageWidgets.get(pageIndex).size();
        copy.pageWidgets.get(pageIndex).add(placement);
        saveAsync(copy);
    }

    public void updatePageWidgets(int pageIndex, @NonNull List<WidgetPlacement> widgets) {
        HomeLayout copy = deepCopy(layout);
        copy.ensureWidgetPage(pageIndex);
        copy.pageWidgets.set(pageIndex, new ArrayList<>(widgets));
        for (int i = 0; i < copy.pageWidgets.get(pageIndex).size(); i++) {
            copy.pageWidgets.get(pageIndex).get(i).rank = i;
        }
        saveAsync(copy);
    }

    public void removeWidget(@NonNull String widgetId) {
        HomeLayout copy = deepCopy(layout);
        for (List<WidgetPlacement> list : copy.pageWidgets) {
            list.removeIf(w -> widgetId.equals(w.id));
        }
        saveAsync(copy);
    }

    public void updateWidget(@NonNull WidgetPlacement updated) {
        HomeLayout copy = deepCopy(layout);
        for (List<WidgetPlacement> list : copy.pageWidgets) {
            for (int i = 0; i < list.size(); i++) {
                if (updated.id.equals(list.get(i).id)) {
                    list.set(i, updated.copy());
                    saveAsync(copy);
                    return;
                }
            }
        }
    }

    @NonNull
    public static HomeLayout deepCopy(@NonNull HomeLayout source) {
        List<List<HomeItem>> pages = new ArrayList<>();
        for (List<HomeItem> page : source.pages) {
            List<HomeItem> copyPage = new ArrayList<>(page.size());
            for (HomeItem item : page) {
                copyPage.add(item == null ? null : item.copy());
            }
            pages.add(copyPage);
        }
        List<HomeItem> dock = new ArrayList<>(source.dock.size());
        for (HomeItem item : source.dock) {
            dock.add(item == null ? null : item.copy());
        }
        List<List<WidgetPlacement>> widgets = new ArrayList<>();
        for (List<WidgetPlacement> page : source.pageWidgets) {
            List<WidgetPlacement> copyPage = new ArrayList<>();
            for (WidgetPlacement widget : page) {
                copyPage.add(widget.copy());
            }
            widgets.add(copyPage);
        }
        return new HomeLayout(pages, dock, widgets, new HashSet<>(source.hiddenAppKeys));
    }
}

