package com.example.autoflow.data;

import android.content.Context; // Import android.content.Context
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {WorkflowEntity.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    public abstract WorkflowDao workflowDao();

    private static volatile AppDatabase INSTANCE;

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "autoflow_database")
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    // For testing or cleanup
    @SuppressWarnings("unused")
    public static void destroyInstance() {
        INSTANCE = null;
    }
}