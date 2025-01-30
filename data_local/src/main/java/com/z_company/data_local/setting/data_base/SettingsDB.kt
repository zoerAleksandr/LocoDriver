package com.z_company.data_local.setting.data_base

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import com.z_company.data_local.setting.dao.SettingsDao
import com.z_company.data_local.setting.entity.MonthOfYear
import com.z_company.data_local.setting.entity.UserSettings

/**
 * version 2
 * add field isConsiderFutureRoute
 * version 3
 * add field isVisibleNightTime
 * add field isVisiblePassengerTime
 * add field isVisibleRelationTime
 * add field isVisibleHolidayTime
 * add field isVisibleExtendedServicePhase
 * version 4
 * add field stationList
 * version 5
 * add field timeZone
 * version 6
 * add field locomotiveSeriesList
 * version 7
 * add field tariffRate in MonthOfYear
 * version 8
 * add field servicePhases in UserSettings
 */

@Database(
    entities = [
        UserSettings::class,
        MonthOfYear::class
    ],
    version = 8,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3),
        AutoMigration(from = 3, to = 4),
        AutoMigration(from = 4, to = 5),
        AutoMigration(from = 5, to = 6),
        AutoMigration(from = 6, to = 7),
        AutoMigration(from = 7, to = 8),
    ]
)
internal abstract class SettingsDB : RoomDatabase() {
    abstract fun settingsDao(): SettingsDao
}