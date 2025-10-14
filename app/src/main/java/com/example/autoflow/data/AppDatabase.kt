package com.example.autoflow.data
import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.autoflow.data.WorkflowDao

@Database(
    entities = [WorkflowEntity::class, PredefinedModeEntity::class], // ADD THIS
    version = 3, // CHANGE FROM 2 TO 3
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun workflowDao(): WorkflowDao
    abstract fun predefinedModeDao(): PredefinedModeDao // ADD THIS

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
                    .addMigrations(MIGRATION_2_3) // ADD THIS
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        // ADD THIS MIGRATION
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS predefined_modes (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        mode_name TEXT NOT NULL,
                        mode_icon TEXT NOT NULL,
                        mode_color TEXT NOT NULL,
                        is_system_mode INTEGER NOT NULL DEFAULT 1,
                        workflow_template TEXT NOT NULL,
                        is_enabled INTEGER NOT NULL DEFAULT 1,
                        created_at INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }
    }
}
