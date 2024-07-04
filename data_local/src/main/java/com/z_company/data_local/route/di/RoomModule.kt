package com.z_company.data_local.route.di

import androidx.room.Room
import com.z_company.data_local.route.data_base.RouteDB
import com.z_company.data_local.route.data_base.SearchResponseDB
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

private const val DB_ROUTE_NAME = "Route.db"
private const val DB_SEARCH_RESPONSE = "SearchResponse.db"
val roomRouteModule = module {
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
            SearchResponseDB::class.java,
            DB_SEARCH_RESPONSE
        ).build()
    }
    single { get<SearchResponseDB>().responseDao() }
}