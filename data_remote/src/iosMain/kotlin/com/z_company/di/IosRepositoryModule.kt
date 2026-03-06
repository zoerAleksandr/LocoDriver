package com.z_company.di

import com.z_company.repository.SecureTokenStorage
import com.z_company.repository.remote_rest.ApiForSendEmail
import com.z_company.repository.remote_rest.AuthManager
import com.z_company.repository.remote_rest.RemoteRestApi
import com.z_company.repository.remote_rest.RemoteRestClient
import com.z_company.repository.remote_rest.RoutesManager
import com.z_company.repository.remote_rest.SettingManager
import org.koin.dsl.module

/**
 * iOS Koin-модуль для data_remote.
 *
 * Аналог Android-блока из app/di/RepositoryModule.kt, но без:
 * - androidContext() — Context не нужен на iOS
 * - ShareManager    — Android-специфичная утилита (Intent/Share sheet)
 * - DataStoreRepository — используется DataStore (Android), на iOS — n/a
 *
 * SharedPreferencesRepositories, SyncManager, AuthProvider, BillingProvider
 * регистрируются в iosUseCaseModule (iosApp), т.к. зависят от data_local.
 */
val iosRepositoryModule = module {
    // Keychain-хранилище токенов (iosMain actual через platform.Security)
    single { SecureTokenStorage() }

    // Ktor HTTP-клиент (Darwin engine — уже подключён через iosMain)
    single<RemoteRestApi> { RemoteRestClient.remoteRestApi }
    single<ApiForSendEmail> { RemoteRestClient.apiForSendEmail }

    // Менеджеры API (конструкторная инжекция, Step 2 миграции)
    single { AuthManager(remoteRestApi = get(), apiForSendEmail = get()) }
    single { RoutesManager(remoteRestApi = get()) }
    single { SettingManager(remoteRestApi = get()) }
}
