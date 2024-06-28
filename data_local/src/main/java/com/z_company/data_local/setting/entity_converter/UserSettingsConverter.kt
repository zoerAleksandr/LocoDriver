package com.z_company.data_local.setting.entity_converter

import com.z_company.domain.entities.UserSettings
import com.z_company.data_local.setting.entity.UserSettings as UserSettingsEntity

internal object UserSettingsConverter {
    fun fromData(userSettings: UserSettings) = UserSettingsEntity(
        settingsKey = userSettings.key,
        minTimeRest = userSettings.minTimeRest,
        minTimeHomeRest = userSettings.minTimeHomeRest,
        lastEnteredDieselCoefficient = userSettings.lastEnteredDieselCoefficient,
        nightTime = NightTimeConverter.fromData(userSettings.nightTime),
        updateAt = userSettings.updateAt,
        defaultWorkTime = userSettings.defaultWorkTime,
        defaultLocoType = userSettings.defaultLocoType,
        monthOfYear = MonthOfYearConverter.fromData(userSettings.selectMonthOfYear),
    )

    fun toData(userSettingsEntity: UserSettingsEntity) = UserSettings(
        key = userSettingsEntity.settingsKey,
        minTimeRest = userSettingsEntity.minTimeRest,
        minTimeHomeRest = userSettingsEntity.minTimeHomeRest,
        lastEnteredDieselCoefficient = userSettingsEntity.lastEnteredDieselCoefficient,
        nightTime = NightTimeConverter.toData(userSettingsEntity.nightTime),
        updateAt = userSettingsEntity.updateAt,
        defaultWorkTime = userSettingsEntity.defaultWorkTime,
        defaultLocoType = userSettingsEntity.defaultLocoType,
        selectMonthOfYear = MonthOfYearConverter.toData(userSettingsEntity.monthOfYear)
    )
}