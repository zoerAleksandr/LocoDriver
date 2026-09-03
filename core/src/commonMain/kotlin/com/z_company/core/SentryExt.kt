package com.z_company.core

import kotlin.coroutines.cancellation.CancellationException

fun Throwable.sendToSentry(className: String, methodName: String) {
    if (this is CancellationException) return
    platformSendToSentry(this, "$className.$methodName")
}

fun sendMessageToSentry(message: String) {
    platformSendMessageToSentry(message)
}

internal expect fun platformSendToSentry(throwable: Throwable, operation: String)
internal expect fun platformSendMessageToSentry(message: String)
