package com.yourstudio.aerolauncher.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "hidden_apps")
public class HiddenAppEntity {

    @PrimaryKey
    @NonNull
    public String appKey;

    public HiddenAppEntity(@NonNull String appKey) {
        this.appKey = appKey;
    }
}
