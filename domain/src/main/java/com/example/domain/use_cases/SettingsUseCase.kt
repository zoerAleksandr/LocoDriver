package com.example.domain.use_cases

import com.example.core.ErrorEntity
import com.example.core.ResultState
import com.example.domain.entities.UserSettings
import com.example.domain.repositories.SettingsRepositories
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class SettingsUseCase(private val repositories: SettingsRepositories) {
    fun loadSettings(): Flow<ResultState<UserSettings>> {
        return repositories.getSettings()
    }

    fun saveSettings(settings: UserSettings): Flow<ResultState<Unit>> {
        return if (isSettingsValid(settings)) {
            repositories.saveSettings(settings)
        } else {
            flowOf(
                ResultState.Error(
                    ErrorEntity(IllegalStateException(), "-1", "Settings is not valid.")
                )
            )
        }
    }

    private fun isSettingsValid(settings: UserSettings): Boolean {
        // TODO
        return true
    }
}