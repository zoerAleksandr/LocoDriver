package com.z_company.iosapp.di

import com.z_company.data_local.DatabaseDriverFactory
import com.z_company.data_local.calendar.CalendarStorageLocalImpl
import com.z_company.data_local.calendar.SqlDelightCalendarRepository
import com.z_company.data_local.calendar.SqlDelightProductionCalendarRepository
import com.z_company.data_local.calendar.SqlDelightReleaseDayRepository
import com.z_company.data_local.norma_time.SqlDelightLocomotiveSeriesRepository
import com.z_company.data_local.norma_time.SqlDelightPartnerRepository
import com.z_company.data_local.norma_time.SqlDelightStationNormRepository
import com.z_company.data_local.route.SqlDelightHistoryResponseRepository
import com.z_company.data_local.route.SqlDelightRouteRepository
import com.z_company.data_local.route.db.RouteDatabase
import com.z_company.data_local.route.searchdb.SearchResponseDatabase
import com.z_company.data_local.setting.SqlDelightSalarySettingRepository
import com.z_company.data_local.setting.SqlDelightSettingRepository
import com.z_company.data_local.setting.db.SettingsDatabase
import com.z_company.data_local.setting.salarydb.SalarySettingDatabase
import com.z_company.domain.repositories.HistoryResponseRepository
import com.z_company.domain.repositories.CalendarRepositories
import com.z_company.domain.repositories.CalendarStorage
import com.z_company.domain.repositories.LocomotiveSeriesRepository
import com.z_company.domain.repositories.ProductionCalendarRepository
import com.z_company.domain.repositories.ReleaseDayRepository
import com.z_company.domain.repositories.RouteRepository
import com.z_company.domain.repositories.SalarySettingRepository
import com.z_company.domain.repositories.SettingsRepository
import com.z_company.domain.repositories.SharedPreferencesRepositories
import com.z_company.domain.repositories.PartnerRepository
import com.z_company.domain.repositories.StationNormRepository
import com.z_company.domain.use_cases.CalendarUseCase
import com.z_company.domain.use_cases.LoadCalendarFromStorage
import com.z_company.domain.use_cases.PartnerUseCase
import com.z_company.domain.use_cases.LocomotiveUseCase
import com.z_company.domain.use_cases.NormaUseCase
import com.z_company.domain.use_cases.ProductionCalendarUseCase
import com.z_company.domain.use_cases.RoutePartnerUseCase
import com.z_company.domain.use_cases.ReleaseDayUseCase
import com.z_company.domain.use_cases.RouteUseCase
import com.z_company.domain.use_cases.SalarySettingUseCase
import com.z_company.domain.use_cases.SettingsUseCase
import com.z_company.iosapp.repository.IosSharedPreferencesRepository
import com.z_company.iosapp.viewmodel.AppInitIosViewModel
import com.z_company.iosapp.viewmodel.FormIosViewModel
import com.z_company.iosapp.viewmodel.HomeIosViewModel
import com.z_company.iosapp.viewmodel.HomeScreenIosViewModel
import com.z_company.iosapp.viewmodel.NewRouteIosViewModel
import com.z_company.iosapp.viewmodel.AllRoutesScreenIosViewModel
import com.z_company.iosapp.viewmodel.LocoFormIosViewModel
import com.z_company.iosapp.viewmodel.LocoScreenIosViewModel
import com.z_company.iosapp.viewmodel.LocoTimeSheetIosViewModel
import com.z_company.iosapp.viewmodel.SeriesListIosViewModel
import com.z_company.iosapp.viewmodel.StationListIosViewModel
import com.z_company.iosapp.viewmodel.ProfileIosViewModel
import com.z_company.iosapp.viewmodel.SalaryCalculationIosViewModel
import com.z_company.iosapp.viewmodel.SettingsIosViewModel
import com.z_company.iosapp.viewmodel.SettingsScreenIosViewModel
import com.z_company.iosapp.viewmodel.PassengerFormIosViewModel
import com.z_company.iosapp.viewmodel.RouteFormIosViewModel
import com.z_company.iosapp.viewmodel.RouteQuickViewIosViewModel
import com.z_company.iosapp.viewmodel.SearchIosViewModel
import com.z_company.iosapp.viewmodel.TrainFormIosViewModel
import com.z_company.iosapp.viewmodel.TrainScreenIosViewModel
import com.z_company.domain.use_cases.TrainUseCase
import com.z_company.iosapp.viewmodel.TrashIosViewModel
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
    single<PartnerRepository> { SqlDelightPartnerRepository() }
    single<ProductionCalendarRepository> { SqlDelightProductionCalendarRepository() }
    single<SalarySettingRepository> { SqlDelightSalarySettingRepository() }
    single<SharedPreferencesRepositories> { IosSharedPreferencesRepository() }

    // ── UseCases ──────────────────────────────────────────────────────────
    single { RouteUseCase(get()) }
    single { LocomotiveUseCase(get()) }
    single { SettingsUseCase(get()) }
    single { com.z_company.domain.use_cases.StationDirectoryUseCase(get()) }
    single { CalendarUseCase(get()) }
    // С1: базовый производственный календарь (жёстко заданные теги дней) для
    // склейки в AppInitIosViewModel.loadCalendar — как CalendarStorageLocalImpl на Android.
    single<CalendarStorage> { CalendarStorageLocalImpl() }
    single { LoadCalendarFromStorage(get()) }
    single { SalarySettingUseCase(get(), get()) }
    single { ReleaseDayUseCase(get()) }
    single { ProductionCalendarUseCase(get()) }
    single { NormaUseCase(get(), get(), get()) }
    single { RoutePartnerUseCase(get()) }
    single { PartnerUseCase(get()) }
    single { com.z_company.domain.use_cases.PassengerUseCase(get()) }
    single { com.z_company.domain.use_cases.OtherWorkUseCase(get()) }

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
            stationNormRepository = get(),
            partnerRepository = get(),
            remoteRestApi = get(),
        )
    }

    // ── ViewModels (single — живут на протяжении жизни приложения) ────────
    single { HomeIosViewModel(get(), get()) }
    single { SettingsIosViewModel(get()) }
    // Экран «Настройки» §17: factory — по экземпляру на открытый экран.
    factory { SettingsScreenIosViewModel(get(), get(), get(), get(), get(), get()) }
    single { FormIosViewModel(get(), get()) }
    single { SalaryCalculationIosViewModel(get(), get(), get(), get()) }
    single { LocoFormIosViewModel(get()) }
    // Экран «Локомотив» §1 и шторка времени §2: factory — по экземпляру на открытый экран/шторку.
    factory {
        LocoScreenIosViewModel(
            locomotiveUseCase = get(),
            routeUseCase = get(),
            settingsUseCase = get(),
            sharedPrefs = get(),
            seriesRepository = get(),
        )
    }
    factory {
        LocoTimeSheetIosViewModel(
            seriesRepository = get(),
            stationRepository = get(),
            sharedPrefs = get(),
            settingsUseCase = get(),
        )
    }
    // Справочники норм §17.4: списки — factory на экран (редакторы создаёт IosViewModelHelper).
    factory { SeriesListIosViewModel(repository = get(), settingsUseCase = get()) }
    factory { StationListIosViewModel(repository = get(), settingsUseCase = get()) }
    single { TrainFormIosViewModel(get()) }
    // Экран «Поезд» §6: factory — по экземпляру на открытый экран.
    single { TrainUseCase(get()) }
    factory {
        TrainScreenIosViewModel(
            trainUseCase = get(),
            routeUseCase = get(),
            settingsUseCase = get(),
            sharedPrefs = get(),
        )
    }
    single { PassengerFormIosViewModel(get()) }
    // «Пассажиром» §7, «Прочая работа» §8, «Напарники» §8.5: factory — по экземпляру на открытый экран.
    factory {
        com.z_company.iosapp.viewmodel.PassengerScreenIosViewModel(
            passengerUseCase = get(), routeUseCase = get(), settingsUseCase = get(),
            stationDirectory = get(), sharedPrefs = get(),
        )
    }
    factory {
        com.z_company.iosapp.viewmodel.OtherWorkScreenIosViewModel(
            otherWorkUseCase = get(), routeUseCase = get(), settingsUseCase = get(),
            stationDirectory = get(), sharedPrefs = get(),
        )
    }
    factory {
        com.z_company.iosapp.viewmodel.PartnersScreenIosViewModel(
            partnerUseCase = get(), routePartnerUseCase = get(), sharedPrefs = get(),
        )
    }
    // Форма маршрута §5: factory — по экземпляру на открытую форму (копия поверх оригинала).
    factory {
        RouteFormIosViewModel(
            routeUseCase = get(),
            settingsUseCase = get(),
            salarySettingUseCase = get(),
            partnerUseCase = get(),
            routeRepository = get(),
            stationDirectory = get(),
            sharedPrefs = get(),
            secureTokenStorage = get(),
            shareRouteManager = get(),
            syncManager = get(),
        )
    }
    single { ProfileIosViewModel(authManager = get(), syncManager = get(), secureTokenStorage = get(), sharedPrefs = get(), remoteRestApi = get()) }
    single {
        HomeScreenIosViewModel(
            routeUseCase = get(),
            settingsUseCase = get(),
            salarySettingUseCase = get(),
            calendarUseCase = get(),
            normaUseCase = get(),
            sharedPrefs = get(),
            syncManager = get(),
            secureTokenStorage = get(),
        )
    }
    single { NewRouteIosViewModel(routeUseCase = get(), settingsUseCase = get(), secureTokenStorage = get()) }
    // С1 «Производственный календарь»: инициализация при старте (первый запуск,
    // миграции, загрузка календаря). factory — один экземпляр на запуск процесса;
    // SettingManager уже зарегистрирован в iosRepositoryModule (data_remote).
    factory {
        AppInitIosViewModel(
            sharedPrefs = get(),
            settingsUseCase = get(),
            routeUseCase = get(),
            calendarUseCase = get(),
            loadCalendarFromStorage = get(),
            salarySettingUseCase = get(),
            productionCalendarUseCase = get(),
            releaseDayUseCase = get(),
            settingManager = get(),
        )
    }

    // ── Экран «Все маршруты» §9: factory — по экземпляру на открытый экран. ──
    factory {
        AllRoutesScreenIosViewModel(
            routeUseCase = get(),
            settingsUseCase = get(),
            salarySettingUseCase = get(),
            calendarUseCase = get(),
            sharedPrefs = get(),
            syncManager = get(),
            secureTokenStorage = get(),
            routesManager = get(),
            shareRouteManager = get(),
            trackRepository = getOrNull(),
        )
    }
    // Быстрый просмотр маршрута §9.1: factory — по экземпляру на открытую шторку.
    factory {
        RouteQuickViewIosViewModel(
            routeUseCase = get(),
            settingsUseCase = get(),
            salarySettingUseCase = get(),
        )
    }
    // Корзина маршрутов §9.4: factory — по экземпляру на открытый экран.
    factory {
        TrashIosViewModel(
            routeUseCase = get(),
            settingsUseCase = get(),
            syncManager = get(),
            secureTokenStorage = get(),
            trackRepository = getOrNull(),
        )
    }
    // Поиск §10: factory — по экземпляру на открытый экран.
    single<HistoryResponseRepository> { SqlDelightHistoryResponseRepository() }
    factory {
        SearchIosViewModel(
            routeUseCase = get(),
            settingsUseCase = get(),
            historyRepository = get(),
        )
    }
}
