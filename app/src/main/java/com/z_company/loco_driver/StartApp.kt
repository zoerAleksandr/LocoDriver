package com.z_company.loco_driver

import android.app.Application
import com.z_company.data_local.route.di.roomRouteModule
import com.z_company.data_local.setting.di.roomSettingsModule
import com.z_company.loco_driver.di.repositoryModule
import com.z_company.loco_driver.di.resourcesModule
import com.z_company.loco_driver.di.useCaseModule
import com.z_company.loco_driver.di.viewModelModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class StartApp: Application()  {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@StartApp)
            modules(
                viewModelModule,
                roomSettingsModule,
                roomRouteModule,
                repositoryModule,
                useCaseModule,
                resourcesModule
            )
        }
    }
}