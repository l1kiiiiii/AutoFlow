package com.example.autoflow.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.autoflow.model.SavedBluetoothDevice
import com.example.autoflow.model.SavedLocation
import com.example.autoflow.model.SavedWiFiNetwork

@Database(
    entities = [
        WorkflowEntity::class,
        PredefinedModeEntity::class,
        MeetingModeEntity::class,
        SavedLocation::class,
        SavedWiFiNetwork::class,
        SavedBluetoothDevice::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {


    abstract fun workflowDao(): WorkflowDao
    abstract fun predefinedModeDao(): PredefinedModeDao
    abstract fun savedLocationDao(): SavedLocationDao
    abstract fun savedWiFiNetworkDao(): SavedWiFiNetworkDao
    abstract fun savedBluetoothDeviceDao(): SavedBluetoothDeviceDao
    abstract fun meetingModeDao(): MeetingModeDao

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
                    .fallbackToDestructiveMigration() //  THIS WILL CLEAR DATABASE ON VERSION CHANGE
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
