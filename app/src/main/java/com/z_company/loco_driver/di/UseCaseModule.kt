package com.z_company.loco_driver.di

import com.z_company.data_local.route.SearchRouteUseCase
import com.z_company.use_case.AuthUseCase
import com.z_company.use_case.LoginUseCase
import com.z_company.domain.repositories.RouteRepository
import com.z_company.use_case.RemoteRouteUseCase
import com.z_company.domain.use_cases.*
import com.z_company.use_case.RuStoreUseCase
import org.koin.dsl.module

val useCaseModule = module {
    single { AuthUseCase() }
    single { LoginUseCase() }

    single { RouteUseCase(repository = get<RouteRepository>()) }
    single { LocomotiveUseCase(repository = get()) }
    single { CalendarUseCase(repositories = get()) }
    single { LoadCalendarFromStorage(repositories = get()) }
    single { TrainUseCase(repository = get()) }
    single { PassengerUseCase(repository = get()) }
    single { PhotoUseCase(repository = get()) }
    single { SettingsUseCase(settingsRepository = get()) }

    single { RemoteRouteUseCase(repository = get()) }

    single { SearchRouteUseCase(repository = get()) }
    single { SalarySettingUseCase(repository = get()) }
    single { RuStoreUseCase(ruStoreRepositoryKtor = get()) }
}