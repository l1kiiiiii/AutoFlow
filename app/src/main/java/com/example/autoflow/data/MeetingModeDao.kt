package com.example.autoflow.data

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface MeetingModeDao {

    @Query("SELECT * FROM meeting_modes ORDER BY created_at DESC")
    fun getAllMeetingModes(): LiveData<List<MeetingModeEntity>>

    @Query("SELECT * FROM meeting_modes WHERE is_active = 1 LIMIT 1")
    suspend fun getActiveMeetingMode(): MeetingModeEntity?

    @Query("SELECT * FROM meeting_modes WHERE id = :id")
    suspend fun getMeetingModeById(id: Long): MeetingModeEntity?

    @Insert
    suspend fun insert(meetingMode: MeetingModeEntity): Long

    @Update
    suspend fun update(meetingMode: MeetingModeEntity)

    @Delete
    suspend fun delete(meetingMode: MeetingModeEntity)

    @Query("DELETE FROM meeting_modes WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE meeting_modes SET is_active = 0 WHERE is_active = 1")
    suspend fun deactivateAllMeetingModes()

    @Query("SELECT COUNT(*) FROM meeting_modes WHERE is_active = 1")
    suspend fun getActiveMeetingCount(): Int

    @Query("SELECT * FROM meeting_modes WHERE scheduled_start_time IS NOT NULL AND scheduled_start_time <= :currentTime AND is_active = 0")
    suspend fun getPendingScheduledModes(currentTime: Long): List<MeetingModeEntity>
}
