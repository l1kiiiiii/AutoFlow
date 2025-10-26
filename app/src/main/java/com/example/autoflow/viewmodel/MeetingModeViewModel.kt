package com.example.autoflow.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.autoflow.data.AppDatabase
import com.example.autoflow.data.MeetingModeEntity
import com.example.autoflow.util.MeetingModeManager
import kotlinx.coroutines.*

class MeetingModeViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "MeetingModeViewModel"
    }

    private val database = AppDatabase.getDatabase(application)
    private val meetingModeDao = database.meetingModeDao()
    private val meetingModeManager = MeetingModeManager.getInstance(application)
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // LiveData
    val allMeetingModes: LiveData<List<MeetingModeEntity>> = meetingModeDao.getAllMeetingModes()

    private val _activeMeetingMode = MutableLiveData<MeetingModeEntity?>()
    val activeMeetingMode: LiveData<MeetingModeEntity?> = _activeMeetingMode

    private val _successMessage = MutableLiveData<String>()
    val successMessage: LiveData<String> = _successMessage

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    init {
        Log.d(TAG, "MeetingModeViewModel initialized")
        refreshActiveMeetingMode()
    }

    /**
     * Start meeting mode immediately
     */
    fun startMeetingModeImmediate(
        name: String = "Meeting Mode",
        endType: String = "MANUAL",
        durationMinutes: Int? = null,
        autoReplyMessage: String = "I'm currently in a meeting and will get back to you shortly."
    ) {
        Log.d(TAG, "🚀 Starting immediate meeting mode: $name")

        meetingModeManager.startMeetingModeImmediate(
            name = name,
            endType = endType,
            durationMinutes = durationMinutes,
            autoReplyMessage = autoReplyMessage
        ) { success, message ->
            if (success) {
                _successMessage.postValue(message)
                refreshActiveMeetingMode()
            } else {
                _errorMessage.postValue(message)
            }
        }
    }

    /**
     * Schedule meeting mode
     */
    fun scheduleMeetingMode(
        name: String = "Scheduled Meeting",
        startTime: Long,
        endType: String = "DURATION",
        durationMinutes: Int? = 60,
        endTime: Long? = null,
        autoReplyMessage: String = "I'm currently in a meeting and will get back to you shortly."
    ) {
        Log.d(TAG, "📅 Scheduling meeting mode: $name")

        meetingModeManager.scheduleMeetingMode(
            name = name,
            startTime = startTime,
            endType = endType,
            durationMinutes = durationMinutes,
            endTime = endTime,
            autoReplyMessage = autoReplyMessage
        ) { success, message ->
            if (success) {
                _successMessage.postValue(message)
            } else {
                _errorMessage.postValue(message)
            }
        }
    }

    /**
     * Stop active meeting mode
     */
    fun stopMeetingMode() {
        Log.d(TAG, "🛑 Stopping meeting mode")

        meetingModeManager.stopMeetingMode { success, message ->
            if (success) {
                _successMessage.postValue(message)
                refreshActiveMeetingMode()
            } else {
                _errorMessage.postValue(message)
            }
        }
    }

    /**
     * Refresh active meeting mode
     */
    fun refreshActiveMeetingMode() {
        meetingModeManager.getActiveMeetingMode { activeMeeting ->
            _activeMeetingMode.postValue(activeMeeting)
        }
    }

    /**
     * Delete meeting mode
     */
    fun deleteMeetingMode(meetingMode: MeetingModeEntity) {
        scope.launch {
            try {
                meetingModeDao.delete(meetingMode)
                _successMessage.postValue("Meeting mode deleted")
            } catch (e: Exception) {
                _errorMessage.postValue("Failed to delete meeting mode: ${e.message}")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        scope.cancel()
        Log.d(TAG, "🧹 MeetingModeViewModel cleanup complete")
    }
}
