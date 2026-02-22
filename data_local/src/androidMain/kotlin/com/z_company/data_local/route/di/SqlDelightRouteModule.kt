package com.z_company.data_local.route.di

import com.z_company.data_local.DatabaseDriverFactory
import com.z_company.data_local.route.SqlDelightHistoryResponseRepository
import com.z_company.data_local.route.SqlDelightRouteRepository
import com.z_company.data_local.route.db.RouteDatabase
import com.z_company.data_local.route.searchdb.SearchResponseDatabase
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val sqlDelightRouteModule = module {
    single { DatabaseDriverFactory(androidContext()) }

    single {
        RouteDatabase(get<DatabaseDriverFactory>().createRouteDriver())
    }

    single {
        SearchResponseDatabase(get<DatabaseDriverFactory>().createSearchResponseDriver())
    }
}
