package com.z_company.loco_driver.di

import com.z_company.use_case.AuthUseCase
import com.z_company.use_case.LoginUseCase
import com.z_company.domain.repositories.RemoteRouteRepository
import com.z_company.domain.repositories.RouteRepository
import com.z_company.domain.use_cases.RemoteRouteUseCase
import com.z_company.domain.use_cases.*
import org.koin.dsl.module

val useCaseModule = module {
    single { AuthUseCase() }
    single { LoginUseCase() }

    single {
        RouteUseCase(
            repository = get<RouteRepository>(),
            remoteRepository = get<RemoteRouteRepository>()
        )
    }
    single { LocomotiveUseCase(repository = get()) }
    single { CalendarUseCase(repositories = get()) }
    single { LoadCalendarFromStorage(repositories = get()) }
    single { TrainUseCase(repository = get()) }
    single { PassengerUseCase(repository = get()) }
    single { PhotoUseCase(repository = get()) }

    single { RemoteRouteUseCase(repository = get()) }
}