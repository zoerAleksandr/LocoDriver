package com.z_company.iosapp.di

import com.z_company.data_local.route.di.sqlDelightRouteModule
import com.z_company.data_local.setting.di.sqlDelightSettingsModule
import com.z_company.shared.di.sharedModule
import org.koin.core.module.Module

/**
 * Все Koin-модули, необходимые для инициализации iOS-приложения.
 *
 * Оборачивает модули из data_local, чтобы Swift не нуждался
 * в прямом доступе к data_local (он не экспортирован из фреймворка).
 */
val allIosKoinModules: List<Module> = listOf(
    sqlDelightRouteModule,       // DatabaseDriverFactory, RouteDatabase, SearchResponseDatabase
    sqlDelightSettingsModule,    // SettingsDatabase, SalarySettingDatabase
    iosUseCaseModule,            // Repositories, UseCases, iOS ViewModels
    sharedModule,                // Shared ViewModels (SearchShared, WorkScheduleShared, etc.)
)
