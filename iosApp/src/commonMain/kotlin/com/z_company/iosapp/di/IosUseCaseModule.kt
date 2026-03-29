package com.z_company.iosapp.di

import com.z_company.data_local.route.SqlDelightRouteRepository
import com.z_company.data_local.setting.SqlDelightSettingRepository
import com.z_company.domain.repositories.RouteRepository
import com.z_company.domain.repositories.SettingsRepository
import com.z_company.domain.use_cases.RouteUseCase
import com.z_company.domain.use_cases.SettingsUseCase
import com.z_company.iosapp.viewmodel.FormIosViewModel
import com.z_company.iosapp.viewmodel.HomeIosViewModel
import com.z_company.iosapp.viewmodel.LocoFormIosViewModel
import com.z_company.iosapp.viewmodel.SalaryCalculationIosViewModel
import com.z_company.iosapp.viewmodel.SettingsIosViewModel
import com.z_company.iosapp.viewmodel.TrainFormIosViewModel
import org.koin.dsl.module

/**
 * Koin-модуль iOS: репозитории, UseCases и ViewModels.
 *
 * Регистрируется в initKoin() поверх:
 *   sqlDelightRouteModule    — DatabaseDriverFactory, RouteDatabase, SearchResponseDatabase
 *   sqlDelightSettingsModule — SettingsDatabase, SalarySettingDatabase
 *
 * Цепочка зависимостей:
 *   RouteDatabase        → SqlDelightRouteRepository  → RouteUseCase → HomeIosViewModel
 *                                                                     → FormIosViewModel
 *                                                                     → SalaryCalculationIosViewModel
 *   SettingsDatabase     → SqlDelightSettingRepository → SettingsUseCase → HomeIosViewModel
 *                                                                        → SettingsIosViewModel
 *                                                                        → SalaryCalculationIosViewModel
 */
val iosUseCaseModule = module {
    // Репозитории (KoinComponent внутри — получают DB из Koin-контекста)
    single<RouteRepository> { SqlDelightRouteRepository() }
    single<SettingsRepository> { SqlDelightSettingRepository() }

    // UseCases
    single { RouteUseCase(get()) }
    single { SettingsUseCase(get()) }

    // ViewModels (single — живут на протяжении жизни приложения)
    single { HomeIosViewModel(get(), get()) }
    single { SettingsIosViewModel(get()) }
    single { FormIosViewModel(get()) }
    single { SalaryCalculationIosViewModel(get(), get()) }
    single { LocoFormIosViewModel(get()) }
    single { TrainFormIosViewModel(get()) }
}
