package com.example.campusly.database;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

// An unfinished post. userId is the primary key, so each user has at most one
// draft and saving again simply overwrites it.
@Entity(tableName = "drafts")
public class DraftEntity {

    @PrimaryKey
    @NonNull
    public String userId = "";
    public String content;
    public String category;
    public long updatedAtMillis;
}