package com.example.campusly.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

// The on-phone database. Stored in the file "campusly.db" in the app's
// private storage. If you change an entity later, increase "version".
@Database(entities = {CachedPostEntity.class, DraftEntity.class}, version = 1, exportSchema = false)
@TypeConverters(Converters.class)
public abstract class CampuslyDatabase extends RoomDatabase {

    private static volatile CampuslyDatabase instance;

    public abstract CachedPostDao cachedPostDao();

    public abstract DraftDao draftDao();

    // Only one database object for the whole app; opening it is expensive.
    public static CampuslyDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (CampuslyDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(context.getApplicationContext(),
                                    CampuslyDatabase.class, "campusly.db")
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return instance;
    }
}