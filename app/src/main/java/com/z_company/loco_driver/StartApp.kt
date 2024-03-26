package com.z_company.loco_driver

import android.app.Application
import com.parse.Parse
import com.z_company.data_local.route.di.roomRouteModule
import com.z_company.data_local.setting.di.roomSettingsModule
import com.z_company.data_remote.Appwrite
import com.z_company.loco_driver.di.repositoryModule
import com.z_company.loco_driver.di.resourcesModule
import com.z_company.loco_driver.di.useCaseModule
import com.z_company.loco_driver.di.viewModelModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class StartApp: Application()  {
    override fun onCreate() {
        super.onCreate()
        Appwrite.init(applicationContext)

        Parse.initialize(
            Parse.Configuration.Builder(this)
                .applicationId(getString(R.string.back4app_app_id))
                .clientKey(getString(R.string.back4app_client_key))
                .server(getString(R.string.back4app_server_url))
                .build());

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