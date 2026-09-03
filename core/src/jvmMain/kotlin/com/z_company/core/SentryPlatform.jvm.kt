package com.z_company.core

// JVM target используется доменными unit-тестами и не является приложением.
internal actual fun platformInitSentry(dsn: String) = Unit

internal actual fun platformSendToSentry(throwable: Throwable, operation: String) = Unit

internal actual fun platformSendMessageToSentry(message: String) = Unit
