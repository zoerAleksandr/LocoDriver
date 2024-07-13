package com.z_company.loco_driver.di

import com.z_company.data_local.SharedPreferenceStorage
import com.z_company.data_local.calendar.CalendarStorageLocalImpl
import com.z_company.data_local.route.RoomRouteRepository
import com.z_company.data_local.setting.DataStoreRepository
import com.z_company.data_local.calendar.RoomCalendarRepository
import com.z_company.data_local.route.RoomHistoryResponseRepository
import com.z_company.data_local.setting.RoomSettingRepository
import com.z_company.repository.B4ARouteRepository
import com.z_company.repository.RemoteRouteRepository
import com.z_company.domain.repositories.CalendarStorage
import com.z_company.domain.repositories.RouteRepository
import com.z_company.domain.repositories.CalendarRepositories
import com.z_company.domain.repositories.HistoryResponseRepository
import com.z_company.domain.repositories.SettingsRepository
import org.koin.android.ext.koin.androidApplication
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

    single<RemoteRouteRepository> { B4ARouteRepository(androidContext()) }

    single<SettingsRepository> {
        RoomSettingRepository()
    }
    single<HistoryResponseRepository> {
        RoomHistoryResponseRepository()
    }
    single { SharedPreferenceStorage(application = androidApplication()) }
}