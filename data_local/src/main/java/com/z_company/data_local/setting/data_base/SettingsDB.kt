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
 */

@Database(
    entities = [
        UserSettings::class,
        MonthOfYear::class
    ],
    version = 3,
    exportSchema = true,
    autoMigrations = [
        AutoMigration (from = 1, to = 2),
        AutoMigration (from = 2, to = 3),
    ]
)
internal abstract class SettingsDB: RoomDatabase() {
    abstract fun settingsDao(): SettingsDao
}