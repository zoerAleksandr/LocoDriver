package com.z_company.data_local.setting.entity_converter

import com.z_company.domain.entities.setting.UserSettings
import com.z_company.data_local.setting.entity.UserSettings as UserSettingsEntity

internal object UserSettingsConverter {
    fun fromData(userSettings: UserSettings) = UserSettingsEntity(
        settingsKey = userSettings.key,
        minTimeRest = userSettings.minTimeRestPointOfTurnover,
        minTimeHomeRest = userSettings.minTimeHomeRest,
        lastEnteredDieselCoefficient = userSettings.lastEnteredDieselCoefficient,
        nightTime = NightTimeConverter.fromData(userSettings.nightTime),
        updateAt = userSettings.updateAt,
        defaultWorkTime = userSettings.defaultWorkTime,
        usingDefaultWorkTime = userSettings.usingDefaultWorkTime,
        isConsiderFutureRoute = userSettings.isConsiderFutureRoute,
        defaultLocoType = userSettings.defaultLocoType,
        monthOfYear = MonthOfYearConverter.fromData(userSettings.selectMonthOfYear),
        stationList = userSettings.stationList,
        timeZone = userSettings.timeZone,
        locomotiveSeriesList = userSettings.locomotiveSeriesList,
        servicePhases = ServicePhasesConverter.fromDataList(userSettings.servicePhases),
        standardTimesStartWork = userSettings.standardTimesStartWork,
        subscriptionPeriod = userSettings.subscriptionPeriod,
        isDecimalTime = userSettings.isDecimalTime
    )

    fun toData(userSettingsEntity: UserSettingsEntity) = UserSettings(
        key = userSettingsEntity.settingsKey,
        minTimeRestPointOfTurnover = userSettingsEntity.minTimeRest,
        minTimeHomeRest = userSettingsEntity.minTimeHomeRest,
        lastEnteredDieselCoefficient = userSettingsEntity.lastEnteredDieselCoefficient,
        nightTime = NightTimeConverter.toData(userSettingsEntity.nightTime),
        updateAt = userSettingsEntity.updateAt,
        defaultWorkTime = userSettingsEntity.defaultWorkTime,
        usingDefaultWorkTime = userSettingsEntity.usingDefaultWorkTime,
        isConsiderFutureRoute = userSettingsEntity.isConsiderFutureRoute,
        defaultLocoType = userSettingsEntity.defaultLocoType,
        selectMonthOfYear = MonthOfYearConverter.toData(userSettingsEntity.monthOfYear),
        stationList = userSettingsEntity.stationList,
        timeZone = userSettingsEntity.timeZone,
        locomotiveSeriesList = userSettingsEntity.locomotiveSeriesList,
        servicePhases = ServicePhasesConverter.toDataList(userSettingsEntity.servicePhases),
        standardTimesStartWork = userSettingsEntity.standardTimesStartWork,
        subscriptionPeriod = userSettingsEntity.subscriptionPeriod,
        isDecimalTime = userSettingsEntity.isDecimalTime
    )
}