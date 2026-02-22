package com.z_company.loco_driver.di

import com.z_company.data_local.SharedPreferenceStorage
import com.z_company.data_local.calendar.CalendarStorageLocalImpl
import com.z_company.data_local.setting.DataStoreRepository
import com.z_company.data_local.calendar.SqlDelightCalendarRepository
import com.z_company.data_local.route.SqlDelightHistoryResponseRepository
import com.z_company.data_local.route.SqlDelightRouteRepository
import com.z_company.data_local.setting.SqlDelightSalarySettingRepository
import com.z_company.data_local.setting.SqlDelightSettingRepository
import com.z_company.domain.repositories.CalendarStorage
import com.z_company.domain.repositories.RouteRepository
import com.z_company.domain.repositories.CalendarRepositories
import com.z_company.domain.repositories.HistoryResponseRepository
import com.z_company.domain.repositories.SalarySettingRepository
import com.z_company.domain.repositories.SettingsRepository
import com.z_company.domain.repositories.SharedPreferencesRepositories
import com.z_company.core.ui.snackbar.ISnackbarManager
import com.z_company.core.ui.snackbar.SnackbarManagerImpl
import com.z_company.repository.SecureTokenStorage
import com.z_company.repository.ShareManager
import com.z_company.repository.remote_rest.ApiForSendEmail
import com.z_company.repository.remote_rest.AuthManager
import com.z_company.repository.remote_rest.RemoteRestApi
import com.z_company.repository.remote_rest.RemoteRestClient
import com.z_company.repository.remote_rest.RoutesManager
import com.z_company.repository.remote_rest.SettingManager
import com.z_company.repository.remote_rest.SyncManager
import com.z_company.route.viewmodel.RouteActionsHelper
import com.z_company.use_case.SubscriptionHelper
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val repositoryModule = module {
    single<ISnackbarManager> { SnackbarManagerImpl() }

    single<RouteRepository> { SqlDelightRouteRepository() }

    single<CalendarRepositories> { SqlDelightCalendarRepository() }

    single<CalendarStorage> { CalendarStorageLocalImpl() }

    single { DataStoreRepository(androidContext()) }

    single<SettingsRepository> { SqlDelightSettingRepository() }

    single<HistoryResponseRepository> { SqlDelightHistoryResponseRepository() }

    single<SharedPreferencesRepositories> { SharedPreferenceStorage(application = androidApplication()) }

    single<SalarySettingRepository> { SqlDelightSalarySettingRepository() }

    single<ShareManager> { ShareManager(androidContext()) }

    // Шаг 3 KMP-миграции: абстракция Context (SecureDataStore → SecureTokenStorage)
    single { SecureTokenStorage(androidContext()) }

    // Ktor-based API (заменяет Retrofit RemoteRestClient)
    single<RemoteRestApi> { RemoteRestClient.remoteRestApi }
    single<ApiForSendEmail> { RemoteRestClient.apiForSendEmail }

    // Managers теперь классы с DI (Шаг 2: object → class)
    single { AuthManager(remoteRestApi = get(), apiForSendEmail = get()) }
    single { RoutesManager(remoteRestApi = get()) }
    single { SettingManager(remoteRestApi = get()) }

    single { RouteActionsHelper() }
    single { SubscriptionHelper() }
    single { SyncManager() }
}
