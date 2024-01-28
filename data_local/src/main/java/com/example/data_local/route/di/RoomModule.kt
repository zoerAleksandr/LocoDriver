package com.example.data_local.route.di

import androidx.room.Room
import com.example.data_local.route.data_base.PreSaveRouteDB
import com.example.data_local.route.data_base.RouteDB
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

private const val DB_ROUTE_NAME = "Route.db"
private const val DB_PRE_SAVE_NAME = "PreSave.db"
val roomRouteModule = module {
    single {
        Room.databaseBuilder(
            androidContext(),
            RouteDB::class.java,
            DB_ROUTE_NAME
        ).build()
    }

    single {
        Room.databaseBuilder(
            androidContext(),
            PreSaveRouteDB::class.java,
            DB_PRE_SAVE_NAME
        ).build()
    }

    single { get<RouteDB>().routeDao() }
    single { get<PreSaveRouteDB>().preSaveDao() }
}