package com.z_company.route.ui

import com.z_company.core.sendMessageToSentry

/**
 * Отправляет в Sentry сбой VK ID SDK (onFail входа/привязки/миграции).
 *
 * Для чего: раньше такие сбои шли только в snackbar или Log.d, и мы не видели,
 * сколько пользователей упёрлось, например, в «Certificate pinning failure»
 * (VK сменил сертификат, а SDK его не знал).
 *
 * Берём только первую строку описания: SDK дописывает к ней цепочку
 * сертификатов, которая раздувает и дробит группы. Токенов и id в сообщении нет.
 */
internal fun reportVkIdFail(stage: String, description: String) {
    val firstLine = description.lineSequence().firstOrNull().orEmpty().take(160)
    sendMessageToSentry("VKID onFail [$stage]: $firstLine")
}
