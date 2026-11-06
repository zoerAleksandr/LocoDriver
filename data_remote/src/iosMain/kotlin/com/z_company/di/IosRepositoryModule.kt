package com.z_company.di

import com.z_company.repository.SecureTokenStorage
import com.z_company.repository.remote_rest.ApiForSendEmail
import com.z_company.repository.remote_rest.AuthManager
import com.z_company.repository.remote_rest.RemoteRestApi
import com.z_company.repository.remote_rest.RemoteRestClient
import com.z_company.repository.remote_rest.RoutesManager
import com.z_company.repository.remote_rest.SessionRefresher
import com.z_company.repository.remote_rest.SettingManager
import com.z_company.repository.remote_rest.ShareRouteManager
import org.koin.dsl.module

/**
 * iOS Koin-модуль для data_remote.
 *
 * Аналог Android-блока из app/di/RepositoryModule.kt, но без:
 * - androidContext() — Context не нужен на iOS
 * - ShareManager    — Android-специфичная утилита (Intent/Share sheet)
 * - DataStoreRepository — используется DataStore (Android), на iOS — n/a
 * - SharedPreferenceStorage — Android SharedPreferences, на iOS — n/a
 * - SyncManager     — зависит от SharedPreferencesRepositories; добавить отдельно
 *                     когда будет iOS-реализация SharedPreferencesRepositories
 */
val iosRepositoryModule = module {
    // Keychain-хранилище токенов (iosMain actual через platform.Security)
    single { SecureTokenStorage() }

    // Ktor HTTP-клиент (Darwin engine — уже подключён через iosMain)
    single<RemoteRestApi> { RemoteRestClient.remoteRestApi }
    single<ApiForSendEmail> { RemoteRestClient.apiForSendEmail }

    // Менеджеры API (конструкторная инжекция, Step 2 миграции)
    // vkClientId — client_id веб-PWA «VK Машинист Web» (54740437), тем же
    // значением настроен VKID SDK в iosApp/iosApp/Shared/VKIDService.swift.
    // Сервер сверяет его с allowlist VK_APP_IDS и берёт vk_id из access-токена
    // (VKID_AUTH_MIGRATION.md); без значения — лишний перебор allowlist.
    single {
        AuthManager(
            remoteRestApi = get(),
            apiForSendEmail = get(),
            vkClientId = "54740437",
        )
    }
    single { SessionRefresher(remoteRestApi = get(), secureTokenStorage = get()) }
    single { RoutesManager(remoteRestApi = get()) }
    single { SettingManager(remoteRestApi = get()) }
    single { ShareRouteManager(remoteRestApi = get()) }
}
