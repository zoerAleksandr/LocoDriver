package com.z_company.core

internal actual fun platformInitSentry(dsn: String) = Unit

internal actual fun platformSendToSentry(throwable: Throwable, operation: String) = Unit

internal actual fun platformSendMessageToSentry(message: String) = Unit
