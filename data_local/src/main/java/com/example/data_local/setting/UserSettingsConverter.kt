package com.example.data_local.setting

import com.example.domain.entities.UserSettings
import com.example.data_local.setting.entity.UserSettings as UserSettingsEntity

object UserSettingsConverter {
    fun fromData(userSettings: UserSettings) = UserSettingsEntity(
        minTimeRest = userSettings.minTimeRest
    )

    fun toData(userSettingsEntity: UserSettingsEntity) = UserSettings().apply {
        minTimeRest = userSettingsEntity.minTimeRest
    }
}