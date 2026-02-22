package com.z_company.data_local.setting.di

import com.z_company.data_local.DatabaseDriverFactory
import com.z_company.data_local.setting.db.SettingsDatabase
import com.z_company.data_local.setting.salarydb.SalarySettingDatabase
import org.koin.dsl.module

val sqlDelightSettingsModule = module {
    single {
        SettingsDatabase(get<DatabaseDriverFactory>().createSettingsDriver())
    }

    single {
        SalarySettingDatabase(get<DatabaseDriverFactory>().createSalarySettingDriver())
    }
}
