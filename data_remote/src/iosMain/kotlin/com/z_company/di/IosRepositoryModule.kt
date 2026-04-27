package com.z_company.di

import com.z_company.repository.SecureTokenStorage
import com.z_company.repository.remote_rest.ApiForSendEmail
import com.z_company.repository.remote_rest.AuthManager
import com.z_company.repository.remote_rest.RemoteRestApi
import com.z_company.repository.remote_rest.RemoteRestClient
import com.z_company.repository.remote_rest.RoutesManager
import com.z_company.repository.remote_rest.SettingManager
import com.z_company.repository.remote_rest.ShareRouteManager
import org.koin.core.qualifier.named
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
 *
 * Два HTTP-клиента (см. RemoteRestClient):
 *  - default-инстанс RemoteRestApi — главный, с retry, для бизнеса.
 *  - named("auth")  RemoteRestApi — без retry, для login/register/forgot/
 *    profile/email-update. 4xx auth-ошибки — не transient, повторять
 *    бессмысленно.
 *
 * TODO(android-1.4): После релиза iOS — переключить Android AuthManager
 * на named("auth") клиент (без retry), как на iOS. Не блокер: retry на
 * 4xx auth-ошибках фактически не активен (retryOnExceptionIf ловит
 * только IOException/HttpRequestTimeoutException/DarwinHttpRequestException,
 * ClientRequestException не наследник IOException).
 */
val iosRepositoryModule = module {
    single { SecureTokenStorage() }

    // Главный (с retry) — для бизнеса.
    single<RemoteRestApi> { RemoteRestClient.remoteRestApi }
    // Auth (без retry) — для login/register/forgot/profile/email.
    single<RemoteRestApi>(named("auth")) { RemoteRestClient.authRestApi }
    // Email API (forgot password) — отдельный домен.
    single<ApiForSendEmail> { RemoteRestClient.apiForSendEmail }

    // AuthManager использует ТОЛЬКО auth-клиент.
    single {
        AuthManager(
            remoteRestApi = get(named("auth")),
            apiForSendEmail = get(),
        )
    }

    // Бизнес-менеджеры — главный клиент (с retry).
    single { RoutesManager(remoteRestApi = get()) }
    single { SettingManager(remoteRestApi = get()) }
    single { ShareRouteManager(remoteRestApi = get()) }
}
