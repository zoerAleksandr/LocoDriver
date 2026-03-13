package com.z_company.core

import io.sentry.kotlin.multiplatform.Sentry

fun initSentry(dsn: String) {
    Sentry.init { options ->
        options.dsn = dsn
    }
}
