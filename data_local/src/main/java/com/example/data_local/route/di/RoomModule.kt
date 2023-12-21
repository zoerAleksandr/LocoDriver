package com.example.data_local.route.di

import androidx.room.Room
import com.example.data_local.route.data_base.RouteDB
import com.example.data_local.setting.data_base.SettingsDB
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module


private const val DB_ROUTE_NAME = "Route.db"
private const val DB_SETTINGS_NAME = "Settings.db"
val roomModule = module {
    single {
        Room.databaseBuilder(
            androidContext(),
            RouteDB::class.java,
            DB_ROUTE_NAME
        ).build()
    }

    single { get<RouteDB>().routeDao() }

    single {
        Room.databaseBuilder(
            androidContext(),
            SettingsDB::class.java,
            DB_SETTINGS_NAME
        ).build()
    }

    single { get<SettingsDB>().settingsDao() }
}