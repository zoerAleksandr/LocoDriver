package com.z_company.data_local.route.di

import com.z_company.data_local.DatabaseDriverFactory
import com.z_company.data_local.route.db.RouteDatabase
import com.z_company.data_local.route.searchdb.SearchResponseDatabase
import org.koin.dsl.module

// iOS-версия Android-модуля data_local/androidMain/route/di/SqlDelightRouteModule.kt.
// Отличие: DatabaseDriverFactory() создаётся без Context — на iOS Context не нужен.
val sqlDelightRouteModule = module {
    single { DatabaseDriverFactory() }

    single {
        RouteDatabase(get<DatabaseDriverFactory>().createRouteDriver())
    }

    single {
        SearchResponseDatabase(get<DatabaseDriverFactory>().createSearchResponseDriver())
    }
}
