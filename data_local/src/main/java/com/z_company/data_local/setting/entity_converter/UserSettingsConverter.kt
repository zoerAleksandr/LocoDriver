package com.z_company.data_local.setting.entity_converter

import com.z_company.domain.entities.UserSettings
import com.z_company.data_local.setting.entity.UserSettings as UserSettingsEntity

internal object UserSettingsConverter {
    fun fromData(userSettings: UserSettings) = UserSettingsEntity(
        settingsKey = userSettings.key,
        minTimeRest = userSettings.minTimeRest,
        lastEnteredDieselCoefficient = userSettings.lastEnteredDieselCoefficient,
        nightTime = NightTimeConverter.fromData(userSettings.nightTime)
    )

    fun toData(userSettingsEntity: UserSettingsEntity) = UserSettings(
        key = userSettingsEntity.settingsKey,
        minTimeRest = userSettingsEntity.minTimeRest,
        lastEnteredDieselCoefficient = userSettingsEntity.lastEnteredDieselCoefficient,
        nightTime = NightTimeConverter.toData(userSettingsEntity.nightTime)
    )
}