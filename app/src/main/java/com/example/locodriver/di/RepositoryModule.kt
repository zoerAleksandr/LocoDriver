package com.example.locodriver.di

import com.example.data_local.calendar.CalendarStorageLocalImpl
import com.example.data_local.route.RoomRouteRepository
import com.example.data_local.setting.DataStoreRepository
import com.example.data_local.setting.RoomCalendarRepository
import com.example.domain.repositories.CalendarStorage
import com.example.domain.repositories.RouteRepository
import com.example.domain.repositories.CalendarRepositories
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val repositoryModule = module {
    single<RouteRepository> {
        RoomRouteRepository()
    }

    single<CalendarRepositories> {
        RoomCalendarRepository()
    }

    single<CalendarStorage> {
        CalendarStorageLocalImpl()
    }

    single { DataStoreRepository(androidContext()) }
}