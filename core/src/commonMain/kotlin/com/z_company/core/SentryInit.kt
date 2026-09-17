package com.z_company.core

/**
 * @param release  идентификатор сборки (`package@version+build`) — без него
 *                 Sentry не группирует события по версиям и не умеет
 *                 «resolved in next release».
 * @param environment `production` / `debug`: эмулятор разработчика не должен
 *                 попадать в прод-статистику.
 */
fun initSentry(dsn: String, release: String? = null, environment: String? = null) {
    platformInitSentry(dsn, release, environment)
}

internal expect fun platformInitSentry(dsn: String, release: String?, environment: String?)
