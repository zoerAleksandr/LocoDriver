package com.z_company.loco_driver

import android.app.Application
import com.my.tracker.MyTracker
import com.my.tracker.MyTrackerConfig.LocationTrackingMode
import com.parse.Parse
import com.z_company.data_local.route.di.roomRouteModule
import com.z_company.data_local.setting.di.roomSalarySettingModule
import com.z_company.data_local.setting.di.roomSettingsModule
import com.z_company.loco_driver.di.paymentsModule
import com.z_company.loco_driver.di.repositoryModule
import com.z_company.loco_driver.di.resourcesModule
import com.z_company.loco_driver.di.updateModule
import com.z_company.loco_driver.di.useCaseModule
import com.z_company.loco_driver.di.viewModelModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import ru.ok.tracer.HasTracerConfiguration
import ru.ok.tracer.TracerConfiguration
import ru.ok.tracer.crash.report.CrashFreeConfiguration
import ru.ok.tracer.crash.report.CrashReportConfiguration
import ru.ok.tracer.heap.dumps.HeapDumpConfiguration

class StartApp : Application(), HasTracerConfiguration {
    override val tracerConfiguration: List<TracerConfiguration>
        get() = listOf(
            CrashReportConfiguration.build {

            },
            CrashFreeConfiguration.build {

            },
            HeapDumpConfiguration.build {

            }
        )

    override fun onCreate() {
        super.onCreate()
        val myTrackerConfig = MyTracker.getTrackerConfig()
        myTrackerConfig.locationTrackingMode = LocationTrackingMode.CACHED
        MyTracker.setDebugMode(true)
        MyTracker.initTracker(getString(R.string.my_tracer_sdk_key), this)


        Parse.initialize(
            Parse.Configuration.Builder(this)
                .applicationId(getString(R.string.back4app_app_id))
                .clientKey(getString(R.string.back4app_client_key))
                .server(getString(R.string.back4app_server_url))
                .build()
        )

        startKoin {
            androidContext(this@StartApp)
            modules(
                viewModelModule,
                roomSettingsModule,
                roomSalarySettingModule,
                roomRouteModule,
                repositoryModule,
                useCaseModule,
                resourcesModule,
                paymentsModule,
                updateModule
            )
        }
    }
}