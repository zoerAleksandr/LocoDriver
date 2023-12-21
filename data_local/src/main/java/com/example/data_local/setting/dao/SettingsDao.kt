package com.example.data_local.setting.dao

import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data_local.setting.entity.UserSettings
import kotlinx.coroutines.flow.Flow

interface SettingsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSettings(userSettings: UserSettings)

    @Query("SELECT * FROM UserSettings")
    fun getSettings(): Flow<UserSettings?>
}