package com.example.data_local.di

import androidx.room.Room
import com.example.data_local.data_base.RouteDB
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module


private const val DB_ROUTE_NAME = "Route.db"
val roomModule = module {
    single {
        Room.databaseBuilder(
            androidContext(),
            RouteDB::class.java,
            DB_ROUTE_NAME
        ).build()
    }

    single { get<RouteDB>().routeDao() }
}