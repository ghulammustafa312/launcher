package com.yourstudio.aerolauncher.layout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A workspace / dock / folder entry: either an app shortcut or a folder.
 */
public final class HomeItem {

    public enum Type {
        APP,
        FOLDER
    }

    @NonNull
    public final String id;
    @NonNull
    public final Type type;
    @Nullable
    public final String appKey;
    @NonNull
    public String title;
    @NonNull
    public final List<String> folderAppKeys;

    private HomeItem(
            @NonNull String id,
            @NonNull Type type,
            @Nullable String appKey,
            @NonNull String title,
            @NonNull List<String> folderAppKeys) {
        this.id = id;
        this.type = type;
        this.appKey = appKey;
        this.title = title;
        this.folderAppKeys = folderAppKeys;
    }

    @NonNull
    public static HomeItem app(@NonNull String appKey) {
        return new HomeItem("app:" + appKey, Type.APP, appKey, "", new ArrayList<>());
    }

    @NonNull
    public static HomeItem folder(@NonNull String title, @NonNull List<String> appKeys) {
        return new HomeItem(
                "folder:" + UUID.randomUUID(),
                Type.FOLDER,
                null,
                title,
                new ArrayList<>(appKeys));
    }

    @NonNull
    public static HomeItem folder(
            @NonNull String id,
            @NonNull String title,
            @NonNull List<String> appKeys) {
        return new HomeItem(id, Type.FOLDER, null, title, new ArrayList<>(appKeys));
    }

    public boolean isApp() {
        return type == Type.APP;
    }

    public boolean isFolder() {
        return type == Type.FOLDER;
    }

    @NonNull
    public HomeItem copy() {
        return new HomeItem(id, type, appKey, title, new ArrayList<>(folderAppKeys));
    }
}
