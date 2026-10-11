package com.yourstudio.aerolauncher.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "layout_items")
public class LayoutItemEntity {

    public static final String TYPE_APP = "APP";
    public static final String TYPE_FOLDER = "FOLDER";

    public static final String CONTAINER_WORKSPACE = "WORKSPACE";
    public static final String CONTAINER_DOCK = "DOCK";
    public static final String CONTAINER_FOLDER = "FOLDER";

    @PrimaryKey
    @NonNull
    public String id;

    @NonNull
    public String type;

    @NonNull
    public String container;

    /** Folder parent id when container is FOLDER; otherwise null/empty. */
    public String parentId;

    public int pageIndex;
    public int cellIndex;
    public int dockIndex;
    public int rank;

    public String appKey;
    public String title;

    public LayoutItemEntity(
            @NonNull String id,
            @NonNull String type,
            @NonNull String container,
            String parentId,
            int pageIndex,
            int cellIndex,
            int dockIndex,
            int rank,
            String appKey,
            String title) {
        this.id = id;
        this.type = type;
        this.container = container;
        this.parentId = parentId;
        this.pageIndex = pageIndex;
        this.cellIndex = cellIndex;
        this.dockIndex = dockIndex;
        this.rank = rank;
        this.appKey = appKey;
        this.title = title;
    }
}
