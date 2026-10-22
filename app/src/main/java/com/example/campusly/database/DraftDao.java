package com.example.campusly.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

@Dao
public interface DraftDao {

    @Query("SELECT * FROM drafts WHERE userId = :userId LIMIT 1")
    DraftEntity getDraft(String userId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void saveDraft(DraftEntity draft);

    @Query("DELETE FROM drafts WHERE userId = :userId")
    void deleteDraft(String userId);
}