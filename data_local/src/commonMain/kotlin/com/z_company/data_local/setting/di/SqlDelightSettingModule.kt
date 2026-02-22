package com.z_company.data_local.setting.di

import com.z_company.data_local.DatabaseDriverFactory
import com.z_company.data_local.setting.db.SettingsDatabase
import com.z_company.data_local.setting.salarydb.SalarySettingDatabase
import org.koin.dsl.module

// Перемещён в commonMain: нет Android-зависимостей.
// DatabaseDriverFactory поставляется из платформо-специфичного модуля
// (androidMain: DatabaseDriverFactory(androidContext()), iosMain: DatabaseDriverFactory()).
val sqlDelightSettingsModule = module {
    single {
        SettingsDatabase(get<DatabaseDriverFactory>().createSettingsDriver())
    }

    single {
        SalarySettingDatabase(get<DatabaseDriverFactory>().createSalarySettingDriver())
    }
}
