package com.example.locodriver

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class StartApp: Application()  {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@StartApp)
            modules(

            )
        }
    }
}