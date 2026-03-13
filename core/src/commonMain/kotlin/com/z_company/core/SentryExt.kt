package com.z_company.core

import io.sentry.kotlin.multiplatform.Sentry
import io.sentry.kotlin.multiplatform.protocol.Breadcrumb

fun Throwable.sendToSentry(className: String, methodName: String) {
    Sentry.addBreadcrumb(
        Breadcrumb.info("$className.$methodName")
    )
    Sentry.captureException(this)
}
