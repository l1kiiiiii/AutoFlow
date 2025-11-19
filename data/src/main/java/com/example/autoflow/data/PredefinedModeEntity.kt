package com.example.autoflow.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "predefined_modes")
data class PredefinedModeEntity(
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0,

    @ColumnInfo(name = "mode_name")
    var modeName: String,

    @ColumnInfo(name = "mode_icon")
    var modeIcon: String,

    @ColumnInfo(name = "mode_color")
    var modeColor: String,

    @ColumnInfo(name = "is_system_mode")
    var isSystemMode: Boolean = true,

    @ColumnInfo(name = "workflow_template")
    var workflowTemplate: String,

    @ColumnInfo(name = "is_enabled")
    var isEnabled: Boolean = true,

    @ColumnInfo(name = "created_at")
    var createdAt: Long = System.currentTimeMillis()
)
