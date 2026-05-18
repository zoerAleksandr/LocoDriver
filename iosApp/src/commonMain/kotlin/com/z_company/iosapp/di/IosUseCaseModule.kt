package com.z_company.iosapp.di

import com.z_company.data_local.DatabaseDriverFactory
import com.z_company.data_local.calendar.SqlDelightCalendarRepository
import com.z_company.data_local.calendar.SqlDelightProductionCalendarRepository
import com.z_company.data_local.calendar.SqlDelightReleaseDayRepository
import com.z_company.data_local.norma_time.SqlDelightLocomotiveSeriesRepository
import com.z_company.data_local.norma_time.SqlDelightStationNormRepository
import com.z_company.data_local.route.SqlDelightRouteRepository
import com.z_company.data_local.route.db.RouteDatabase
import com.z_company.data_local.route.searchdb.SearchResponseDatabase
import com.z_company.data_local.setting.SqlDelightSalarySettingRepository
import com.z_company.data_local.setting.SqlDelightSettingRepository
import com.z_company.data_local.setting.db.SettingsDatabase
import com.z_company.data_local.setting.salarydb.SalarySettingDatabase
import com.z_company.domain.repositories.CalendarRepositories
import com.z_company.domain.repositories.LocomotiveSeriesRepository
import com.z_company.domain.repositories.ProductionCalendarRepository
import com.z_company.domain.repositories.ReleaseDayRepository
import com.z_company.domain.repositories.RouteRepository
import com.z_company.domain.repositories.SalarySettingRepository
import com.z_company.domain.repositories.SettingsRepository
import com.z_company.domain.repositories.SharedPreferencesRepositories
import com.z_company.domain.repositories.StationNormRepository
import com.z_company.domain.use_cases.CalendarUseCase
import com.z_company.domain.use_cases.ProductionCalendarUseCase
import com.z_company.domain.use_cases.ReleaseDayUseCase
import com.z_company.domain.use_cases.RouteUseCase
import com.z_company.domain.use_cases.SalarySettingUseCase
import com.z_company.domain.use_cases.SettingsUseCase
import com.z_company.iosapp.repository.IosSharedPreferencesRepository
import com.z_company.iosapp.viewmodel.FormIosViewModel
import com.z_company.iosapp.viewmodel.HomeIosViewModel
import com.z_company.iosapp.viewmodel.LocoFormIosViewModel
import com.z_company.iosapp.viewmodel.ProfileIosViewModel
import com.z_company.iosapp.viewmodel.SalaryCalculationIosViewModel
import com.z_company.iosapp.viewmodel.SettingsIosViewModel
import com.z_company.iosapp.viewmodel.PassengerFormIosViewModel
import com.z_company.iosapp.viewmodel.TrainFormIosViewModel
import com.z_company.repository.remote_rest.SyncManager
import org.koin.dsl.module

/**
 * Единый Koin-модуль iOS: SQLDelight-драйверы, репозитории, UseCases, ViewModels.
 *
 * data_local не экспортируется из ComposeApp.framework, поэтому
 * sqlDelightRouteModule / sqlDelightSettingsModule недоступны Swift напрямую.
 * Всё собрано здесь — Swift передаёт только этот модуль в doInitKoin().
 *
 * Цепочка зависимостей:
 *   DatabaseDriverFactory → RouteDatabase / SearchResponseDatabase
 *                        → SettingsDatabase / SalarySettingDatabase
 *   RouteDatabase        → SqlDelightRouteRepository  → RouteUseCase → ViewModels
 *   SettingsDatabase     → SqlDelightSettingRepository → SettingsUseCase → ViewModels
 */
val iosUseCaseModule = module {

    // ── SQLDelight: драйвер и базы данных ────────────────────────────────
    single { DatabaseDriverFactory() }

    single { RouteDatabase(get<DatabaseDriverFactory>().createRouteDriver()) }
    single { SearchResponseDatabase(get<DatabaseDriverFactory>().createSearchResponseDriver()) }
    single { SettingsDatabase(get<DatabaseDriverFactory>().createSettingsDriver()) }
    single { SalarySettingDatabase(get<DatabaseDriverFactory>().createSalarySettingDriver()) }

    // ── Репозитории ───────────────────────────────────────────────────────
    single<RouteRepository> { SqlDelightRouteRepository() }
    single<SettingsRepository> { SqlDelightSettingRepository() }
    single<CalendarRepositories> { SqlDelightCalendarRepository() }
    single<ReleaseDayRepository> { SqlDelightReleaseDayRepository() }
    single<LocomotiveSeriesRepository> { SqlDelightLocomotiveSeriesRepository() }
    single<StationNormRepository> { SqlDelightStationNormRepository() }
    single<ProductionCalendarRepository> { SqlDelightProductionCalendarRepository() }
    single<SalarySettingRepository> { SqlDelightSalarySettingRepository() }
    single<SharedPreferencesRepositories> { IosSharedPreferencesRepository() }

    // ── UseCases ──────────────────────────────────────────────────────────
    single { RouteUseCase(get()) }
    single { SettingsUseCase(get()) }
    single { CalendarUseCase(get()) }
    single { SalarySettingUseCase(get(), get()) }
    single { ReleaseDayUseCase(get()) }
    single { ProductionCalendarUseCase(get()) }

    // ── Remote managers (SyncManager нужны все UseCases + remote managers) ─
    single {
        SyncManager(
            settingsUseCase = get(),
            salarySettingUseCase = get(),
            calendarUseCase = get(),
            productionCalendarUseCase = get(),
            releaseDayUseCase = get(),
            routeUseCase = get(),
            routesManager = get(),
            settingManager = get(),
            sharedPrefs = get(),
            locomotiveSeriesRepository = get(),
            stationNormRepository = get()
        )
    }

    // ── ViewModels (single — живут на протяжении жизни приложения) ────────
    single { HomeIosViewModel(get(), get()) }
    single { SettingsIosViewModel(get()) }
    single { FormIosViewModel(get()) }
    single { SalaryCalculationIosViewModel(get(), get()) }
    single { LocoFormIosViewModel(get()) }
    single { TrainFormIosViewModel(get()) }
    single { PassengerFormIosViewModel(get()) }
    single { ProfileIosViewModel(authManager = get(), syncManager = get(), secureTokenStorage = get()) }
}
