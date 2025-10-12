package com.example.autoflow.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [WorkflowEntity::class],
    version = 3,  // ✅ Increment version
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
                    .addMigrations(MIGRATION_2_3)  // ✅ Add migration
                    .build()

                INSTANCE = instance
                instance
            }
        }

        //  Migration from version 2 to 3
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add new trigger_logic column with default "AND"
                database.execSQL(
                    "ALTER TABLE workflows ADD COLUMN trigger_logic TEXT NOT NULL DEFAULT 'AND'"
                )

                // Convert existing single triggers/actions to arrays
                // This wraps existing JSON objects in array brackets
                database.execSQL("""
                    UPDATE workflows 
                    SET trigger_details = '[' || trigger_details || ']',
                        action_details = '[' || action_details || ']'
                    WHERE trigger_details NOT LIKE '[%'
                """)
            }
        }

        fun destroyInstance() {
            INSTANCE = null
        }
    }
}

