package com.example.autoflow.domain.trigger

import android.Manifest
import android.content.Context
import androidx.annotation.RequiresPermission
import com.example.autoflow.integrations.WiFiManager
import com.example.autoflow.model.Trigger
import com.example.autoflow.util.Constants
import com.example.autoflow.util.TriggerParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Handler for WiFi-based triggers
 * Follows Single Responsibility Principle
 */
class WiFiTriggerHandler(private val context: Context) : TriggerHandler {
    
    private val wifiManager = WiFiManager(context)
    
    override fun canHandle(trigger: Trigger): Boolean {
        return trigger.type == Constants.TRIGGER_WIFI
    }
    
    override fun getSupportedType(): String = Constants.TRIGGER_WIFI
    
    @RequiresPermission(Manifest.permission.ACCESS_WIFI_STATE)
    override suspend fun evaluate(trigger: Trigger): Result<Boolean> = withContext(Dispatchers.Default) {
        try {
            // Parse WiFi data from trigger
            val wifiData = TriggerParser.parseWifiData(trigger)
                ?: return@withContext Result.failure(IllegalArgumentException("Invalid WiFi data"))
            
            // Get current WiFi state
            val isWiFiEnabled = wifiManager.isWiFiEnabled()
            val currentSsid = wifiManager.getCurrentSsid()
            
            // Evaluate based on trigger state
            val isTriggered = when (wifiData.state.uppercase()) {
                "ON" -> isWiFiEnabled
                "OFF" -> !isWiFiEnabled
                "CONNECTED" -> {
                    if (wifiData.ssid != null) {
                        isWiFiEnabled && currentSsid == wifiData.ssid
                    } else {
                        isWiFiEnabled && currentSsid != null
                    }
                }
                "DISCONNECTED" -> {
                    !isWiFiEnabled || currentSsid == null
                }
                else -> false
            }
            
            Result.success(isTriggered)
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
