package com.example.conspect.database;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.example.conspect.database.dao.ConspectDao;
import com.example.conspect.database.entities.ConspectEntity;

@Database(entities = {ConspectEntity.class}, version = 2, exportSchema = false)  // version = 2
public abstract class AppDatabase extends RoomDatabase {

    private static AppDatabase instance;

    public abstract ConspectDao conspectDao();

    public static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL(
                    "CREATE TABLE conspects_new (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "server_id INTEGER, " +
                            "title TEXT, " +
                            "subject TEXT, " +
                            "content TEXT, " +
                            "created_at TEXT, " +
                            "updated_at TEXT, " +
                            "user_id TEXT, " +
                            "is_synced INTEGER, " +
                            "is_deleted INTEGER)"
            );

            database.execSQL(
                    "INSERT INTO conspects_new (" +
                            "id, title, subject, content, created_at, updated_at, " +
                            "user_id, is_synced, is_deleted) " +
                            "SELECT id, title, subject, content, created_at, updated_at, " +
                            "user_id, is_synced, is_deleted FROM conspects"
            );

            database.execSQL("DROP TABLE conspects");

            database.execSQL("ALTER TABLE conspects_new RENAME TO conspects");
        }
    };

    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "conspectium_database"
                    )
                    .addMigrations(MIGRATION_1_2)
                    .build();
        }
        return instance;
    }
}