package com.example.data_local.setting.data_base

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.data_local.setting.dao.SettingsDao
import com.example.data_local.setting.entity.MonthOfYear
import com.example.data_local.setting.entity.UserSettings

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