package com.z_company.loco_driver

import android.app.Application
import com.my.tracker.MyTracker
import com.my.tracker.MyTrackerConfig.LocationTrackingMode
import com.parse.Parse
import com.vk.id.VKID
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
import java.util.Locale
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.z_company.work_manager.SyncWorker
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import kotlin.time.Duration

class StartApp : Application() {

    override fun onCreate() {
        super.onCreate()
        VKID.init(this)
        VKID.instance.setLocale(Locale("ru"))
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

        val workManager = WorkManager.getInstance(this)
        val workInfos = workManager.getWorkInfosForUniqueWork("sync_work").get()

        if (workInfos.isEmpty()) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val periodicWorkRequest = PeriodicWorkRequestBuilder<SyncWorker>(
                repeatInterval = 36, // Период в 36 часов
                repeatIntervalTimeUnit = TimeUnit.HOURS
            )
                .setInitialDelay(6, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(this)
                .enqueueUniquePeriodicWork(
                    "sync_work", // Уникальное имя задачи
                    ExistingPeriodicWorkPolicy.KEEP, // Если задача уже существует, сохранить её (не заменять)
                    periodicWorkRequest
                )
        }
    }
}