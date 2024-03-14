package com.z_company.data_local.setting

import com.z_company.domain.entities.UserSettings
import com.z_company.data_local.setting.entity.UserSettings as UserSettingsEntity

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