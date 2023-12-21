package com.example.locodriver.di

import com.example.data_local.route.RoomRouteRepository
import com.example.data_local.setting.RoomSettingsRepository
import com.example.domain.repositories.RouteRepositories
import com.example.domain.repositories.SettingsRepositories
import org.koin.dsl.module

val repositoryModule = module {
    single<RouteRepositories> {
        RoomRouteRepository()
    }

    single<SettingsRepositories> {
        RoomSettingsRepository()
    }
}