package com.z_company.loco_driver.di

import com.z_company.data_local.SharedPreferenceStorage
import com.z_company.data_local.calendar.CalendarStorageLocalImpl
import com.z_company.data_local.calendar.SqlDelightProductionCalendarRepository
import com.z_company.data_local.calendar.SqlDelightReleaseDayRepository
import com.z_company.data_local.norma_time.SqlDelightLocomotiveSeriesRepository
import com.z_company.data_local.norma_time.SqlDelightPartnerRepository
import com.z_company.data_local.norma_time.SqlDelightStationNormRepository
import com.z_company.data_local.setting.DataStoreRepository
import com.z_company.data_local.calendar.SqlDelightCalendarRepository
import com.z_company.data_local.route.SqlDelightHistoryResponseRepository
import com.z_company.data_local.route.SqlDelightRouteRepository
import com.z_company.data_local.setting.SqlDelightSalarySettingRepository
import com.z_company.data_local.setting.SqlDelightSettingRepository
import com.z_company.domain.repositories.CalendarStorage
import com.z_company.domain.repositories.LocomotiveSeriesRepository
import com.z_company.domain.repositories.ProductionCalendarRepository
import com.z_company.domain.repositories.ReleaseDayRepository
import com.z_company.domain.repositories.RouteRepository
import com.z_company.domain.repositories.CalendarRepositories
import com.z_company.domain.repositories.HistoryResponseRepository
import com.z_company.domain.repositories.SalarySettingRepository
import com.z_company.domain.repositories.SettingsRepository
import com.z_company.domain.repositories.SharedPreferencesRepositories
import com.z_company.domain.repositories.PartnerRepository
import com.z_company.domain.repositories.StationNormRepository
import com.z_company.core.ui.snackbar.ISnackbarManager
import com.z_company.core.ui.snackbar.SnackbarManagerImpl
import com.z_company.core.theme.ThemeManager
import com.z_company.loco_driver.ui.theme.ThemeManagerImpl
import com.z_company.repository.SecureTokenStorage
import com.z_company.repository.ShareManager
import com.z_company.repository.remote_rest.ApiForSendEmail
import com.z_company.repository.remote_rest.AuthManager
import com.z_company.repository.remote_rest.RemoteAnnouncementRepository
import com.z_company.repository.remote_rest.RemoteRestApi
import com.z_company.repository.remote_rest.RemoteRestClient
import com.z_company.domain.repositories.AnnouncementRepository
import com.z_company.repository.remote_rest.RoutesManager
import com.z_company.repository.remote_rest.SettingManager
import com.z_company.repository.remote_rest.ShareRouteManager
import com.z_company.repository.remote_rest.SyncManager
import com.z_company.core.widget.WidgetUpdater
import com.z_company.loco_driver.widget.GlanceWidgetUpdater
import com.z_company.route.viewmodel.RouteActionsHelper
import com.z_company.use_case.SubscriptionHelper
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val repositoryModule = module {
    single<ISnackbarManager> { SnackbarManagerImpl() }

    single<RouteRepository> { SqlDelightRouteRepository() }

    single<CalendarRepositories> { SqlDelightCalendarRepository() }

    single<ReleaseDayRepository> { SqlDelightReleaseDayRepository() }

    single<LocomotiveSeriesRepository> { SqlDelightLocomotiveSeriesRepository() }

    single<StationNormRepository> { SqlDelightStationNormRepository() }

    single<PartnerRepository> { SqlDelightPartnerRepository() }

    single<ProductionCalendarRepository> { SqlDelightProductionCalendarRepository() }

    single<CalendarStorage> { CalendarStorageLocalImpl() }

    single { DataStoreRepository(androidContext()) }

    single<SettingsRepository> { SqlDelightSettingRepository() }

    single<HistoryResponseRepository> { SqlDelightHistoryResponseRepository() }

    single<SharedPreferencesRepositories> { SharedPreferenceStorage(application = androidApplication()) }

    single<ThemeManager> { ThemeManagerImpl(prefs = get()) }

    single<SalarySettingRepository> { SqlDelightSalarySettingRepository() }

    single<ShareManager> { ShareManager(androidContext()) }

    // Шаг 3 KMP-миграции: абстракция Context (SecureDataStore → SecureTokenStorage)
    single { SecureTokenStorage(androidContext()) }

    // Ktor-based API (заменяет Retrofit RemoteRestClient)
    single<RemoteRestApi> { RemoteRestClient.remoteRestApi }
    single<ApiForSendEmail> { RemoteRestClient.apiForSendEmail }

    // Managers теперь классы с DI (Шаг 2: object → class)
    single<AnnouncementRepository> { RemoteAnnouncementRepository(api = get()) }

    single { AuthManager(remoteRestApi = get(), apiForSendEmail = get()) }
    single { RoutesManager(remoteRestApi = get()) }
    single { SettingManager(remoteRestApi = get()) }
    single { ShareRouteManager(remoteRestApi = get()) }

    single { RouteActionsHelper() }
    single { SubscriptionHelper() }

    // Widget updater (Glance)
    single<WidgetUpdater> { GlanceWidgetUpdater(androidContext()) }
    // Шаг 10: SyncManager — конструкторная инжекция (KoinComponent → DI)
    single {
        SyncManager(
            settingsUseCase = get(),
            salarySettingUseCase = get(),
            calendarUseCase = get(),
            productionCalendarUseCase = get(),
            releaseDayUseCase = get(),
            routeUseCase = get(),
            routesManager = get(),
            settingManager = get(),
            sharedPrefs = get(),
            locomotiveSeriesRepository = get(),
            stationNormRepository = get(),
            partnerRepository = get()
        )
    }
}
