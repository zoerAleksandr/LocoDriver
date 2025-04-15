package com.z_company.loco_driver.di

import org.koin.android.ext.koin.androidApplication
import org.koin.dsl.module
import ru.rustore.sdk.appupdate.manager.RuStoreAppUpdateManager
import ru.rustore.sdk.appupdate.manager.factory.RuStoreAppUpdateManagerFactory

val updateModule = module {
    single<RuStoreAppUpdateManager> {
        RuStoreAppUpdateManagerFactory.create(androidApplication())
    }
}