package com.z_company.repository.remote_rest

/**
 * Флаги функций, зависящих от версии сервера.
 *
 * [REMOTE_CERTIFICATES_ENABLED] - отправлять ли справки ВУ-45 полем `brakeCertificates` в `POST /v1/route/`.
 * Выключен по умолчанию до деплоя сервера (миграция `055_brake_certificate` ДО кода; контракт - VU45_CERTIFICATES_API.md
 * в репозитории сервера). Приём справок из ответов сервера не зависит от флага (поле необязательное).
 * Включить: поменять константу на `true` в релизе клиента, выпускаемом ПОСЛЕ деплоя сервера.
 */
object RemoteFeatureFlags {
    const val REMOTE_CERTIFICATES_ENABLED = false

    /** Рабочее значение (для тестов и локального стенда можно переопределить); по умолчанию равно константе. */
    @kotlin.concurrent.Volatile
    var remoteCertificatesEnabled: Boolean = REMOTE_CERTIFICATES_ENABLED
}
