package com.example.autoflow.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.autoflow.model.SavedLocation

@Database(
    entities = [
        WorkflowEntity::class,
        PredefinedModeEntity::class,
        SavedLocation::class
    ],
    version = 4, // Increment version
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun workflowDao(): WorkflowDao
    abstract fun predefinedModeDao(): PredefinedModeDao
    abstract fun savedLocationDao(): SavedLocationDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "autoflow_database"
                )
                    .fallbackToDestructiveMigration() // ✅ Recreates DB on version change
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}
