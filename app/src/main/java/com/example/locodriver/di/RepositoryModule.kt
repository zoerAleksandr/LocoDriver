package com.example.locodriver.di

import com.example.data_local.calendar.CalendarRepositoryImpl
import com.example.data_local.route.RoomRouteRepository
import com.example.data_local.setting.RoomSettingsRepository
import com.example.domain.repositories.CalendarRepositories
import com.example.domain.repositories.RouteRepository
import com.example.domain.repositories.SettingsRepositories
import org.koin.dsl.module

val repositoryModule = module {
    single<RouteRepository> {
        RoomRouteRepository()
    }

    single<SettingsRepositories> {
        RoomSettingsRepository()
    }

    single<CalendarRepositories> {
        CalendarRepositoryImpl()
    }
}