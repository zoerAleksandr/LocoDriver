package com.z_company.data_local.setting.data_base

import androidx.room.Database
import androidx.room.RoomDatabase
import com.z_company.data_local.setting.dao.SettingsDao
import com.z_company.data_local.setting.entity.MonthOfYear
import com.z_company.data_local.setting.entity.UserSettings

@Database(
    entities = [
        UserSettings::class,
        MonthOfYear::class
    ],
    version = 1,
    exportSchema = false
)
internal abstract class SettingsDB: RoomDatabase() {
    abstract fun settingsDao(): SettingsDao
}