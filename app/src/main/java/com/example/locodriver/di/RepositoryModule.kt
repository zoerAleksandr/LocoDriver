package com.example.locodriver.di

import com.example.data_local.RoomRepository
import com.example.domain.repositories.RouteRepositories
import org.koin.dsl.module

val repositoryModule = module {
    single<RouteRepositories> {
        RoomRepository()
    }
}