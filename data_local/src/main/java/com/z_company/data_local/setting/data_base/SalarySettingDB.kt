package com.z_company.data_local.setting.data_base

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import com.z_company.data_local.setting.dao.SalarySettingDao
import com.z_company.data_local.setting.entity.SalarySetting

/**
 * version 2 added field averagePaymentHour, districtCoefficient, nordicCoefficient*/
@Database(
    entities = [
        SalarySetting::class
    ],
    version = 2,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2)
    ]
)
internal abstract class SalarySettingDB : RoomDatabase() {
    abstract fun salarySettingDao(): SalarySettingDao
}