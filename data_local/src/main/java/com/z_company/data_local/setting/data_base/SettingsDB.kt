package com.z_company.data_local.setting.data_base

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import com.z_company.data_local.setting.dao.SettingsDao
import com.z_company.data_local.setting.entity.MonthOfYear
import com.z_company.data_local.setting.entity.UserSettings

/*
* version 2
* add field isConsiderFutureRoute*/
@Database(
    entities = [
        UserSettings::class,
        MonthOfYear::class
    ],
    version = 2,
    exportSchema = true,
    autoMigrations = [
        AutoMigration (from = 1, to = 2)
    ]
)
internal abstract class SettingsDB: RoomDatabase() {
    abstract fun settingsDao(): SettingsDao
}