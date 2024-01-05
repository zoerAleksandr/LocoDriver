package com.example.locodriver

import android.app.Application
import com.example.data_local.route.di.roomRouteModule
import com.example.data_local.setting.di.roomSettingsModule
import com.example.locodriver.di.repositoryModule
import com.example.locodriver.di.resourcesModule
import com.example.locodriver.di.useCaseModule
import com.example.locodriver.di.viewModelModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class StartApp: Application()  {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@StartApp)
            modules(
                viewModelModule,
                roomRouteModule,
                roomSettingsModule,
                repositoryModule,
                useCaseModule,
                resourcesModule
            )
        }
    }
}