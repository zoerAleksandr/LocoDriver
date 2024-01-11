package com.example.locodriver.di

import com.example.domain.use_cases.AuthUseCase
import com.example.domain.use_cases.CalendarUseCase
import com.example.domain.use_cases.RouteUseCase
import com.example.domain.use_cases.SettingsUseCase
import org.koin.dsl.module

val useCaseModule = module {
    single { AuthUseCase() }
    single { RouteUseCase(repository = get()) }
    single { SettingsUseCase(repositories = get()) }
    single { CalendarUseCase(repositories = get())}
}