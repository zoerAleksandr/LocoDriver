package com.z_company.loco_driver.di

import com.z_company.data_local.route.SearchRouteUseCase
import com.z_company.domain.repositories.HardcodedRegionalHolidaysRepository
import com.z_company.domain.repositories.RegionalHolidaysRepository
import com.z_company.domain.repositories.RouteRepository
import com.z_company.domain.use_cases.*
import org.koin.dsl.module

val useCaseModule = module {
    single { RouteUseCase(repository = get<RouteRepository>()) }
    single { LocomotiveUseCase(repository = get()) }
    single<RegionalHolidaysRepository> { HardcodedRegionalHolidaysRepository() }
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
}
