package com.example.autoflow.data

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface PredefinedModeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(mode: PredefinedModeEntity): Long

    @Update
    fun update(mode: PredefinedModeEntity): Int

    @Delete
    fun delete(mode: PredefinedModeEntity)

    @Query("SELECT * FROM predefined_modes ORDER BY is_system_mode DESC, mode_name ASC")
    fun getAllModes(): LiveData<List<PredefinedModeEntity>>

    @Query("SELECT * FROM predefined_modes WHERE is_system_mode = 1")
    fun getSystemModes(): LiveData<List<PredefinedModeEntity>>

    @Query("SELECT * FROM predefined_modes WHERE is_system_mode = 0")
    fun getCustomModes(): LiveData<List<PredefinedModeEntity>>

    @Query("SELECT * FROM predefined_modes WHERE id = :id")
    fun getModeById(id: Long): PredefinedModeEntity?

    @Query("DELETE FROM predefined_modes WHERE is_system_mode = 0")
    fun deleteAllCustomModes()
}
