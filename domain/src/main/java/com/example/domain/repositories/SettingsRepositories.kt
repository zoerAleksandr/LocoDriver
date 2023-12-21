package com.example.domain.repositories

import com.example.core.ResultState
import com.example.domain.entities.UserSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepositories {
    fun getSettings(): Flow<ResultState<UserSettings>>

    fun saveSettings(userSettings: UserSettings): Flow<ResultState<Unit>>
}