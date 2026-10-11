package com.yourstudio.aerolauncher.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "layout_items")
public class LayoutItemEntity {

    public static final String TYPE_APP = "APP";
    public static final String TYPE_FOLDER = "FOLDER";
    public static final String TYPE_WIDGET = "WIDGET";
    public static final String TYPE_WIDGET_STACK = "WIDGET_STACK";

    public static final String CONTAINER_WORKSPACE = "WORKSPACE";
    public static final String CONTAINER_DOCK = "DOCK";
    public static final String CONTAINER_FOLDER = "FOLDER";
    public static final String CONTAINER_PAGE_WIDGETS = "PAGE_WIDGETS";

    @PrimaryKey
    @NonNull
    public String id;

    @NonNull
    public String type;

    @NonNull
    public String container;

    public String parentId;

    public int pageIndex;
    public int cellIndex;
    public int dockIndex;
    public int rank;

    public String appKey;
    public String title;

    public int appWidgetId;
    public int spanRows;
    /** Comma-separated appWidgetIds for stacks. */
    public String widgetIdsCsv;

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
            String title,
            int appWidgetId,
            int spanRows,
            String widgetIdsCsv) {
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
        this.appWidgetId = appWidgetId;
        this.spanRows = spanRows;
        this.widgetIdsCsv = widgetIdsCsv;
    }
}
