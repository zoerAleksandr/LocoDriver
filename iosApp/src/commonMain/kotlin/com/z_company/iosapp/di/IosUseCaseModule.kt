package com.z_company.iosapp.di

import com.z_company.data_local.calendar.SqlDelightCalendarRepository
import com.z_company.data_local.route.SqlDelightRouteRepository
import com.z_company.data_local.setting.SqlDelightSalarySettingRepository
import com.z_company.data_local.setting.SqlDelightSettingRepository
import com.z_company.domain.repositories.CalendarRepositories
import com.z_company.domain.repositories.RouteRepository
import com.z_company.domain.repositories.SalarySettingRepository
import com.z_company.domain.repositories.SettingsRepository
import com.z_company.domain.use_cases.CalendarUseCase
import com.z_company.domain.use_cases.LocomotiveUseCase
import com.z_company.domain.use_cases.PassengerUseCase
import com.z_company.domain.use_cases.RouteUseCase
import com.z_company.domain.use_cases.SalarySettingUseCase
import com.z_company.domain.use_cases.SettingsUseCase
import com.z_company.domain.use_cases.TrainUseCase
import com.z_company.iosapp.viewmodel.AllRouteIosViewModel
import com.z_company.iosapp.viewmodel.FormIosViewModel
import com.z_company.iosapp.viewmodel.HomeIosViewModel
import com.z_company.iosapp.viewmodel.LocoFormIosViewModel
import com.z_company.iosapp.viewmodel.MoreInfoIosViewModel
import com.z_company.iosapp.viewmodel.PassengerFormIosViewModel
import com.z_company.iosapp.viewmodel.ProfileIosViewModel
import com.z_company.iosapp.viewmodel.SalaryCalculationIosViewModel
import com.z_company.iosapp.viewmodel.SearchIosViewModel
import com.z_company.iosapp.viewmodel.SettingSalaryIosViewModel
import com.z_company.iosapp.viewmodel.SettingsIosViewModel
import com.z_company.iosapp.viewmodel.TrainFormIosViewModel
import com.z_company.iosapp.viewmodel.WorkScheduleIosViewModel
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
 *                                                                     → ProfileIosViewModel
 *                                                                     → SearchIosViewModel
 *   SettingsDatabase     → SqlDelightSettingRepository → SettingsUseCase → HomeIosViewModel
 *                                                                        → SettingsIosViewModel
 *                                                                        → SalaryCalculationIosViewModel
 *                       → SqlDelightCalendarRepository → CalendarUseCase → SalarySettingUseCase
 *   SalarySettingDatabase → SqlDelightSalarySettingRepository → SalarySettingUseCase
 *                                                             → SettingSalaryIosViewModel
 */
val iosUseCaseModule = module {
    // Репозитории (KoinComponent внутри — получают DB из Koin-контекста)
    single<RouteRepository> { SqlDelightRouteRepository() }
    single<SettingsRepository> { SqlDelightSettingRepository() }
    single<SalarySettingRepository> { SqlDelightSalarySettingRepository() }
    single<CalendarRepositories> { SqlDelightCalendarRepository() }

    // UseCases
    single { RouteUseCase(get()) }
    single { SettingsUseCase(get()) }
    single { CalendarUseCase(get()) }
    single { SalarySettingUseCase(get(), get()) }
    single { LocomotiveUseCase(get()) }
    single { TrainUseCase(get()) }
    single { PassengerUseCase(get()) }

    // ViewModels (single — живут на протяжении жизни приложения)
    single { HomeIosViewModel(get(), get()) }
    single { SettingsIosViewModel(get()) }
    single { FormIosViewModel(get()) }
    single { SalaryCalculationIosViewModel(get(), get()) }
    single { ProfileIosViewModel(get()) }
    single { SearchIosViewModel(get()) }
    single { SettingSalaryIosViewModel(get()) }
    single { LocoFormIosViewModel(get()) }
    single { TrainFormIosViewModel(get()) }
    single { PassengerFormIosViewModel(get()) }
    single { AllRouteIosViewModel(get()) }
    single { WorkScheduleIosViewModel(get(), get()) }
    single { MoreInfoIosViewModel(get(), get()) }
}
