package com.z_company.loco_driver.di

import com.z_company.data_remote.AuthUseCase
import com.z_company.data_remote.LoginUseCase
import com.z_company.domain.use_cases.*
import org.koin.dsl.module

val useCaseModule = module {
    single { AuthUseCase() }
    single { LoginUseCase() }

    single { RouteUseCase(repository = get()) }
    single { LocomotiveUseCase(repository = get()) }
    single { CalendarUseCase(repositories = get()) }
    single { LoadCalendarFromStorage(repositories = get()) }
    single { TrainUseCase(repository = get()) }
    single { PassengerUseCase(repository = get()) }
    single { PhotoUseCase(repository = get()) }
}