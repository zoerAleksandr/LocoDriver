package com.z_company.data_local.setting.data_base

import androidx.room.Database
import androidx.room.RoomDatabase
import com.z_company.data_local.setting.dao.SalarySettingDao
import com.z_company.data_local.setting.entity.SalarySetting

@Database(
    entities = [
        SalarySetting::class
    ],
    version = 1,
    exportSchema = true
)
internal abstract class SalarySettingDB: RoomDatabase() {
    abstract fun salarySettingDao(): SalarySettingDao
}