package com.example.data_local.setting

import com.example.domain.entities.UserSettings
import com.example.data_local.setting.entity.UserSettings as UserSettingsEntity

internal object UserSettingsConverter {
    fun fromData(userSettings: UserSettings) = UserSettingsEntity(
        key = userSettings.key,
        minTimeRest = userSettings.minTimeRest,
        lastEnteredDieselCoefficient = userSettings.lastEnteredDieselCoefficient
    )

    fun toData(userSettingsEntity: UserSettingsEntity) = UserSettings().apply {
        key = userSettingsEntity.key
        minTimeRest = userSettingsEntity.minTimeRest
        lastEnteredDieselCoefficient = userSettingsEntity.lastEnteredDieselCoefficient

    }
}