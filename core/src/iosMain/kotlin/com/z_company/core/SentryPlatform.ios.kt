package com.z_company.core

// Sentry iOS остаётся отключён до появления версии, совместимой с Kotlin 2.2.x.
internal actual fun platformInitSentry(dsn: String) = Unit

internal actual fun platformSendToSentry(throwable: Throwable, operation: String) = Unit

internal actual fun platformSendMessageToSentry(message: String) = Unit
