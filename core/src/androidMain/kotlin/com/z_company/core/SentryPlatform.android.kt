package com.z_company.core

import io.sentry.kotlin.multiplatform.Sentry
import io.sentry.kotlin.multiplatform.protocol.Breadcrumb

internal actual fun platformInitSentry(dsn: String) {
    Sentry.init { options ->
        options.dsn = dsn
        options.beforeSend = { event ->
            val isCloseSystemDialogs = event.exceptions?.any { exception ->
                exception.type?.contains("SecurityException") == true &&
                    exception.value?.contains("CLOSE_SYSTEM_DIALOGS") == true
            } == true
            val isCancellation = event.exceptions?.any { exception ->
                exception.type?.contains("CancellationException") == true
            } == true
            if (isCloseSystemDialogs || isCancellation) null else event
        }
    }
}

internal actual fun platformSendToSentry(throwable: Throwable, operation: String) {
    Sentry.addBreadcrumb(Breadcrumb.info(operation))
    Sentry.captureException(throwable)
}

internal actual fun platformSendMessageToSentry(message: String) {
    Sentry.captureMessage(message)
}
