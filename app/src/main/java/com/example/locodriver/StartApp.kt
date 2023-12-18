package com.example.locodriver

import android.app.Application
import com.example.data_local.di.roomModule
import com.example.locodriver.di.repositoryModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class StartApp: Application()  {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@StartApp)
            modules(
                roomModule,
                repositoryModule
            )
        }
    }
}