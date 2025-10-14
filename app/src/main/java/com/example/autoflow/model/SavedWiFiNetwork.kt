package com.example.autoflow.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_wifi_networks")
data class SavedWiFiNetwork(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "ssid")
    val ssid: String,

    @ColumnInfo(name = "bssid")
    val bssid: String? = null,

    @ColumnInfo(name = "display_name")
    val displayName: String,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "is_favorite")
    val isFavorite: Boolean = false
)
