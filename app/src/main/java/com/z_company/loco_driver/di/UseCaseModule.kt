package com.z_company.loco_driver.di

import com.z_company.data_local.route.SearchRouteUseCase
import com.z_company.domain.repositories.RegionalHolidaysRepository
import com.z_company.domain.repositories.RouteRepository
import com.z_company.repository.remote_rest.RemoteRegionalHolidaysRepository
import com.z_company.domain.use_cases.*
import org.koin.dsl.module

val useCaseModule = module {
    single { RouteUseCase(repository = get<RouteRepository>()) }
    single { LocomotiveUseCase(repository = get()) }
    // Remote — загружает с сервера /v1/regions и /v1/regional_holidays,
    // при отсутствии сети падает на hardcoded fallback внутри реализации.
    single<RegionalHolidaysRepository> { RemoteRegionalHolidaysRepository(api = get()) }
    single { CalendarUseCase(repositories = get(), regionalHolidaysRepository = get()) }
    single { LoadCalendarFromStorage(repositories = get()) }
    single { TrainUseCase(repository = get()) }
    single { PassengerUseCase(repository = get()) }
    single { PhotoUseCase(repository = get()) }
    single { SettingsUseCase(settingsRepository = get()) }
    single { SearchRouteUseCase(repository = get()) }
    single { SalarySettingUseCase(repository = get(), calendarUseCase = get()) }
    single { ReleaseDayUseCase(repository = get()) }
    single { ProductionCalendarUseCase(repository = get()) }
    // Единая точка расчёта нормы (учитывает регион + отвлечения + производственный календарь)
    single { NormaUseCase(calendarUseCase = get(), releaseDayUseCase = get()) }
}
