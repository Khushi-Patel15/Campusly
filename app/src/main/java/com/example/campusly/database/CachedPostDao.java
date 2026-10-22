package com.example.campusly.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import java.util.List;

// DAO = "Data Access Object": the list of database operations Room writes the
// SQL code for. It's an abstract class so replaceAll() can have a body.
@Dao
public abstract class CachedPostDao {

    @Query("SELECT * FROM cached_posts ORDER BY createdAtMillis DESC")
    public abstract List<CachedPostEntity> getAll();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract void insertAll(List<CachedPostEntity> posts);

    @Query("DELETE FROM cached_posts")
    public abstract void deleteAll();

    // @Transaction: the delete and insert happen together, so the saved feed
    // is never left half-updated if the app is closed in the middle.
    @Transaction
    public void replaceAll(List<CachedPostEntity> posts) {
        deleteAll();
        insertAll(posts);
    }
}