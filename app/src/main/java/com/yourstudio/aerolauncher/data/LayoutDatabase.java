package com.yourstudio.aerolauncher.data;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(
        entities = {LayoutItemEntity.class, HiddenAppEntity.class},
        version = 2,
        exportSchema = false)
public abstract class LayoutDatabase extends RoomDatabase {

    private static volatile LayoutDatabase instance;

    public abstract LayoutDao layoutDao();

    @NonNull
    public static LayoutDatabase getInstance(@NonNull Context context) {
        if (instance == null) {
            synchronized (LayoutDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    LayoutDatabase.class,
                                    "aero_layout.db")
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return instance;
    }
}
