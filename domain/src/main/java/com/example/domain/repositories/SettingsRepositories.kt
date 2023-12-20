package com.example.domain.repositories

import com.example.core.ResultState
import com.example.domain.entities.UserSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepositories {
    fun loadSettings(): Flow<ResultState<UserSettings>>

    fun saveSettings(): Flow<ResultState<Unit>>
}