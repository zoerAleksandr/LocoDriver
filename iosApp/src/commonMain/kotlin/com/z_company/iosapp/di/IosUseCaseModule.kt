package com.z_company.iosapp.di

import com.z_company.data_local.IosSharedPreferenceStorage
import com.z_company.data_local.calendar.SqlDelightCalendarRepository
import com.z_company.data_local.route.SqlDelightRouteRepository
import com.z_company.data_local.setting.SqlDelightSalarySettingRepository
import com.z_company.data_local.setting.SqlDelightSettingRepository
import com.z_company.domain.repositories.CalendarRepositories
import com.z_company.domain.repositories.RouteRepository
import com.z_company.domain.repositories.SalarySettingRepository
import com.z_company.domain.repositories.SettingsRepository
import com.z_company.domain.repositories.SharedPreferencesRepositories
import com.z_company.domain.use_cases.CalendarUseCase
import com.z_company.domain.use_cases.LocomotiveUseCase
import com.z_company.domain.use_cases.PassengerUseCase
import com.z_company.domain.use_cases.RouteUseCase
import com.z_company.domain.use_cases.SalarySettingUseCase
import com.z_company.domain.use_cases.SettingsUseCase
import com.z_company.domain.use_cases.TrainUseCase
import com.z_company.repository.remote_rest.SyncManager
import com.z_company.shared.platform.AuthProvider
import com.z_company.shared.platform.BillingProvider
import com.z_company.shared.platform.IosAuthProvider
import com.z_company.shared.platform.IosBillingProvider
import org.koin.dsl.module

/**
 * Koin-модуль iOS: репозитории, UseCases, и платформенные провайдеры.
 *
 * Все ViewModels теперь зарегистрированы в sharedModule (:features:shared).
 *
 * Регистрируется в initKoin() поверх:
 *   sqlDelightRouteModule    — DatabaseDriverFactory, RouteDatabase, SearchResponseDatabase
 *   sqlDelightSettingsModule — SettingsDatabase, SalarySettingDatabase
 */
val iosUseCaseModule = module {
    // Репозитории (KoinComponent внутри — получают DB из Koin-контекста)
    single<RouteRepository> { SqlDelightRouteRepository() }
    single<SettingsRepository> { SqlDelightSettingRepository() }
    single<SalarySettingRepository> { SqlDelightSalarySettingRepository() }
    single<CalendarRepositories> { SqlDelightCalendarRepository() }

    // iOS SharedPreferences (NSUserDefaults)
    single<SharedPreferencesRepositories> { IosSharedPreferenceStorage() }

    // UseCases
    single { RouteUseCase(get()) }
    single { SettingsUseCase(get()) }
    single { CalendarUseCase(get()) }
    single { SalarySettingUseCase(get(), get()) }
    single { LocomotiveUseCase(get()) }
    single { TrainUseCase(get()) }
    single { PassengerUseCase(get()) }

    // SyncManager (depends on SharedPreferencesRepositories + UseCases)
    single { SyncManager(get(), get(), get(), get(), get(), get(), get()) }

    // Platform providers
    single<AuthProvider> { IosAuthProvider(get(), get(), get(), get(), get(), get()) }
    single<BillingProvider> { IosBillingProvider(get(), get(), get(), get()) }
}
