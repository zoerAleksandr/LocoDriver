package com.z_company.repository.remote_rest

/**
 * Флаги функций, зависящих от версии сервера.
 *
 * [REMOTE_CERTIFICATES_ENABLED] - отправлять ли справки ВУ-45 полем `brakeCertificates` в `POST /v1/route/`.
 * Включён после сквозной проверки на локальном стенде (сервер с миграцией `055_brake_certificate` уже на проде; контракт -
 * VU45_CERTIFICATES_API.md в репозитории сервера). Приём справок из ответов сервера не зависит от флага (поле необязательное).
 * Откатить: поменять константу на `false` (поле `brakeCertificates` тогда не отправляется, сервер справки не трогает).
 */
object RemoteFeatureFlags {
    const val REMOTE_CERTIFICATES_ENABLED = true

    /** Рабочее значение (для тестов и локального стенда можно переопределить); по умолчанию равно константе. */
    @kotlin.concurrent.Volatile
    var remoteCertificatesEnabled: Boolean = REMOTE_CERTIFICATES_ENABLED
}
