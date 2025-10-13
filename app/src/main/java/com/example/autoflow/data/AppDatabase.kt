package com.example.autoflow.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [WorkflowEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun workflowDao(): WorkflowDao

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
                    .fallbackToDestructiveMigration(false) // For development
                    // .addMigrations(MIGRATION_1_2) // Use migrations in production
                    .build()
                INSTANCE = instance
                instance
            }
        }

        /**
         * Example migration from version 1 to 2
         * Uncomment and customize for production
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Example: Add a new column
                // database.execSQL("ALTER TABLE workflows ADD COLUMN new_column TEXT DEFAULT ''")
            }
        }
    }
}
