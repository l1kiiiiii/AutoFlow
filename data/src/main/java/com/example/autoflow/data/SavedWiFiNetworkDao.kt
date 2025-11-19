package com.example.autoflow.data

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.autoflow.data.SavedWiFiNetwork

@Dao
interface SavedWiFiNetworkDao {

    @Query("SELECT * FROM saved_wifi_networks ORDER BY is_favorite DESC, created_at DESC")
    fun getAllNetworks(): LiveData<List<SavedWiFiNetwork>>

    @Query("SELECT * FROM saved_wifi_networks")
    suspend fun getAllNetworksSync(): List<SavedWiFiNetwork>

    @Query("SELECT * FROM saved_wifi_networks WHERE ssid = :ssid LIMIT 1")
    suspend fun getNetworkBySsid(ssid: String): SavedWiFiNetwork?

    @Query("SELECT * FROM saved_wifi_networks WHERE id = :id")
    suspend fun getNetworkById(id: Long): SavedWiFiNetwork?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNetwork(network: SavedWiFiNetwork): Long

    @Update
    suspend fun updateNetwork(network: SavedWiFiNetwork)

    @Delete
    suspend fun deleteNetwork(network: SavedWiFiNetwork)

    @Query("DELETE FROM saved_wifi_networks WHERE id = :id")
    suspend fun deleteNetworkById(id: Long)
}
