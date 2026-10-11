package com.yourstudio.aerolauncher.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import java.util.List;

@Dao
public interface LayoutDao {

    @Query("SELECT * FROM layout_items")
    List<LayoutItemEntity> getAllItems();

    @Query("SELECT * FROM hidden_apps")
    List<HiddenAppEntity> getHiddenApps();

    @Query("DELETE FROM layout_items")
    void clearItems();

    @Query("DELETE FROM hidden_apps")
    void clearHidden();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertItems(List<LayoutItemEntity> items);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertHidden(List<HiddenAppEntity> hidden);

    @Transaction
    default void replaceAll(List<LayoutItemEntity> items, List<HiddenAppEntity> hidden) {
        clearItems();
        clearHidden();
        if (items != null && !items.isEmpty()) {
            insertItems(items);
        }
        if (hidden != null && !hidden.isEmpty()) {
            insertHidden(hidden);
        }
    }
}
