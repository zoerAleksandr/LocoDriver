package com.example.locodriver.di

import com.example.domain.use_cases.*
import org.koin.dsl.module

val useCaseModule = module {
    single { AuthUseCase() }
    single { RouteUseCase(repository = get()) }
    single { LocomotiveUseCase(repository = get()) }
    single { CalendarUseCase(repositories = get()) }
    single { LoadCalendarFromStorage(repositories = get()) }
    single { TrainUseCase(repository = get()) }
    single { PassengerUseCase(repository = get()) }
    single { PhotosUseCase(repository = get()) }
}