package com.yourstudio.aerolauncher.layout;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import com.yourstudio.aerolauncher.HomePreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * JSON backup / restore through the Storage Access Framework (no storage permission).
 */
public final class LayoutBackup {

    public static final String MIME_TYPE = "application/json";
    public static final String DEFAULT_FILE_NAME = "aero-launcher-layout.json";

    private LayoutBackup() {
    }

    @WorkerThread
    public static void exportToUri(
            @NonNull Context context,
            @NonNull Uri uri,
            @NonNull HomeLayout layout) throws Exception {
        JSONObject root = toJson(layout, context);
        try (OutputStream out = context.getContentResolver().openOutputStream(uri)) {
            if (out == null) {
                throw new IllegalStateException("Unable to open output stream");
            }
            out.write(root.toString(2).getBytes(StandardCharsets.UTF_8));
        }
    }

    @WorkerThread
    @NonNull
    public static HomeLayout importFromUri(
            @NonNull Context context,
            @NonNull Uri uri) throws Exception {
        StringBuilder builder = new StringBuilder();
        try (InputStream in = context.getContentResolver().openInputStream(uri)) {
            if (in == null) {
                throw new IllegalStateException("Unable to open input stream");
            }
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(in, StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
        }
        return fromJson(new JSONObject(builder.toString()), context);
    }

    @NonNull
    static JSONObject toJson(@NonNull HomeLayout layout, @NonNull Context context)
            throws Exception {
        JSONObject root = new JSONObject();
        root.put("version", 1);
        root.put("columns", HomePreferences.getColumns(context));
        root.put("rows", HomePreferences.getRows(context));

        JSONArray pages = new JSONArray();
        for (List<HomeItem> page : layout.pages) {
            JSONArray cells = new JSONArray();
            for (HomeItem item : page) {
                cells.put(item == null ? JSONObject.NULL : itemToJson(item));
            }
            pages.put(cells);
        }
        root.put("pages", pages);

        JSONArray dock = new JSONArray();
        for (HomeItem item : layout.dock) {
            dock.put(item == null ? JSONObject.NULL : itemToJson(item));
        }
        root.put("dock", dock);

        JSONArray hidden = new JSONArray();
        for (String key : layout.hiddenAppKeys) {
            hidden.put(key);
        }
        root.put("hidden", hidden);
        return root;
    }

    @NonNull
    static HomeLayout fromJson(@NonNull JSONObject root, @NonNull Context context)
            throws Exception {
        int columns = root.optInt("columns", HomePreferences.getColumns(context));
        int rows = root.optInt("rows", HomePreferences.getRows(context));
        HomePreferences.setGridSize(context, columns, rows);
        int capacity = columns * rows;

        JSONArray pagesJson = root.getJSONArray("pages");
        List<List<HomeItem>> pages = new ArrayList<>();
        for (int p = 0; p < pagesJson.length(); p++) {
            JSONArray cellsJson = pagesJson.getJSONArray(p);
            List<HomeItem> page = new ArrayList<>(Collections.nCopies(capacity, null));
            int limit = Math.min(capacity, cellsJson.length());
            for (int c = 0; c < limit; c++) {
                if (!cellsJson.isNull(c)) {
                    page.set(c, itemFromJson(cellsJson.getJSONObject(c)));
                }
            }
            pages.add(page);
        }
        if (pages.isEmpty()) {
            pages.add(new ArrayList<>(Collections.nCopies(capacity, null)));
        }

        List<HomeItem> dock = new ArrayList<>(
                Collections.nCopies(HomePreferences.DOCK_SLOTS, null));
        JSONArray dockJson = root.optJSONArray("dock");
        if (dockJson != null) {
            int limit = Math.min(HomePreferences.DOCK_SLOTS, dockJson.length());
            for (int i = 0; i < limit; i++) {
                if (!dockJson.isNull(i)) {
                    dock.set(i, itemFromJson(dockJson.getJSONObject(i)));
                }
            }
        }

        Set<String> hidden = new HashSet<>();
        JSONArray hiddenJson = root.optJSONArray("hidden");
        if (hiddenJson != null) {
            for (int i = 0; i < hiddenJson.length(); i++) {
                hidden.add(hiddenJson.getString(i));
            }
        }
        return new HomeLayout(pages, dock, hidden);
    }

    @NonNull
    private static JSONObject itemToJson(@NonNull HomeItem item) throws Exception {
        JSONObject json = new JSONObject();
        json.put("id", item.id);
        json.put("type", item.type.name());
        json.put("title", item.title);
        if (item.appKey != null) {
            json.put("appKey", item.appKey);
        }
        JSONArray kids = new JSONArray();
        for (String key : item.folderAppKeys) {
            kids.put(key);
        }
        json.put("folderAppKeys", kids);
        return json;
    }

    @NonNull
    private static HomeItem itemFromJson(@NonNull JSONObject json) throws Exception {
        String type = json.getString("type");
        if ("FOLDER".equals(type)) {
            List<String> keys = new ArrayList<>();
            JSONArray kids = json.optJSONArray("folderAppKeys");
            if (kids != null) {
                for (int i = 0; i < kids.length(); i++) {
                    keys.add(kids.getString(i));
                }
            }
            return HomeItem.folder(
                    json.getString("id"),
                    json.optString("title", "Folder"),
                    keys);
        }
        return HomeItem.app(json.getString("appKey"));
    }
}
