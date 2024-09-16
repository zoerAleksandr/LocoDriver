package com.z_company.data_local.setting.di

import androidx.room.Room
import com.z_company.data_local.setting.dao.SalarySettingDao
import com.z_company.data_local.setting.data_base.SalarySettingDB
import com.z_company.data_local.setting.data_base.SettingsDB
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

private const val DB_SETTINGS_NAME = "Settings.db"
private const val DB_SALARY_SETTINGS_NAME = "SalarySetting.db"

val roomSettingsModule = module {
    single {
        Room.databaseBuilder(
            androidContext(),
            SettingsDB::class.java,
            DB_SETTINGS_NAME
        ).build()
    }

    single { get<SettingsDB>().settingsDao() }
}

val roomSalarySettingModule = module {
    single {
        Room.databaseBuilder(
            androidContext(),
            SalarySettingDB::class.java,
            DB_SALARY_SETTINGS_NAME
        ).build()
    }

    single { get<SalarySettingDB>().salarySettingDao() }
}