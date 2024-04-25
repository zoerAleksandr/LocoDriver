package com.z_company.loco_driver.di

import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val resourcesModule = module {
    single { androidContext().resources }
}