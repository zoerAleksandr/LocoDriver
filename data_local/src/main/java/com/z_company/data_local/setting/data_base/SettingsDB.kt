package com.z_company.data_local.setting.data_base

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.DeleteColumn
import androidx.room.RoomDatabase
import androidx.room.migration.AutoMigrationSpec
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
 * version 9
 * add field servicePhases in UserSettings
 * version 10
 * add field dateSetTariffRate in MonthOfYear
 * version 10
 * add field standardTimesStartWork in UserSettings
 */

@Database(
    entities = [
        UserSettings::class,
        MonthOfYear::class
    ],
    version = 14,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3),
        AutoMigration(from = 3, to = 4),
        AutoMigration(from = 4, to = 5),
        AutoMigration(from = 5, to = 6),
        AutoMigration(from = 6, to = 7),
        AutoMigration(from = 7, to = 8),
        AutoMigration(from = 8, to = 9),
        AutoMigration(from = 9, to = 10),
        AutoMigration(from = 10, to = 11),
        AutoMigration(from = 11, to = 12, spec = DeleteColumns::class),
        AutoMigration(from = 12, to = 13, spec = DeleteColumns::class),
        AutoMigration(from = 13, to = 14),
    ]
)
internal abstract class SettingsDB : RoomDatabase() {
    abstract fun settingsDao(): SettingsDao
}

@DeleteColumn.Entries(
    DeleteColumn(tableName = "UserSettings", columnName = "isVisibleNightTime"),
    DeleteColumn(tableName = "UserSettings", columnName = "isVisiblePassengerTime"),
    DeleteColumn(tableName = "UserSettings", columnName = "isVisibleRelationTime"),
    DeleteColumn(tableName = "UserSettings", columnName = "isVisibleHolidayTime"),
    DeleteColumn(tableName = "UserSettings", columnName = "isVisibleExtendedServicePhase"),
    DeleteColumn(tableName = "UserSettings", columnName = "dateTimePickerType"),
)
class DeleteColumns : AutoMigrationSpec