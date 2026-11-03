package com.z_company.core.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Есть ли сейчас выход в интернет: активная сеть с INTERNET и подтверждённой проверкой связи (VALIDATED).
 * Не гарантирует, что наш сервер достижим (VPN и т.п. см. [vpnAwareErrorMessage]), но отсекает «нет сети».
 * Нет доступа к состоянию сети - считаем, что интернет есть (не блокируем функции зря).
 */
fun isOnline(context: Context): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return true
    return try {
        val caps = cm.getNetworkCapabilities(cm.activeNetwork ?: return false) ?: return false
        caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    } catch (e: Exception) {
        true
    }
}

/** Поток состояния связи ([isOnline]): первое значение сразу, далее при смене сети. */
fun onlineFlow(context: Context): Flow<Boolean> = callbackFlow {
    val app = context.applicationContext
    val cm = app.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    if (cm == null) {
        trySend(true)
        awaitClose { }
        return@callbackFlow
    }
    val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) { trySend(isOnline(app)) }
        override fun onLost(network: Network) { trySend(isOnline(app)) }
        override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) { trySend(isOnline(app)) }
    }
    trySend(isOnline(app))
    val registered = runCatching { cm.registerDefaultNetworkCallback(callback) }.isSuccess
    awaitClose { if (registered) runCatching { cm.unregisterNetworkCallback(callback) } }
}.distinctUntilChanged()

/**
 * Определение активного VPN на устройстве.
 *
 * API-сервер работает по обычному HTTP, и часть VPN (особенно бесплатные /
 * с блокировкой РФ-трафика) рвут такие соединения — пользователь получает
 * «unexpected end of stream», таймауты и т.п. Определяем VPN, чтобы вместо
 * непонятной ошибки подсказать отключить его.
 *
 * Требует разрешения ACCESS_NETWORK_STATE (уже объявлено в манифесте app).
 */
fun isVpnActive(context: Context): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        ?: return false

    // Основной способ — капабилити активной сети.
    cm.activeNetwork?.let { active ->
        cm.getNetworkCapabilities(active)?.let { caps ->
            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) ||
                !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN)
            ) return true
        }
    }

    // Подстраховка: VPN может не быть в activeNetwork — перебираем все сети.
    return try {
        cm.allNetworks.any { net ->
            cm.getNetworkCapabilities(net)?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true
        }
    } catch (e: Exception) {
        false
    }
}

/**
 * Похоже ли сообщение об ошибке на транспортную/сетевую (сервер недоступен),
 * а не на осмысленный ответ сервера (401, «Неверный логин» и т.п.).
 * Нужно, чтобы подсказку про VPN показывать только когда соединение не дошло.
 */
fun isConnectivityErrorMessage(message: String?): Boolean {
    val m = message?.lowercase() ?: return false
    return CONNECTIVITY_MARKERS.any { m.contains(it) }
}

/**
 * Сырая ошибка локальной БД («no such table/column», SQLITE_…) — пользователю её показывать
 * нельзя: текст нечитаем и раскрывает внутреннее устройство. Такие сообщения заменяются общим.
 */
fun isTechnicalDbErrorMessage(message: String?): Boolean {
    val m = message?.lowercase() ?: return false
    return DB_ERROR_MARKERS.any { m.contains(it) }
}

private val DB_ERROR_MARKERS = listOf(
    "no such table", "no such column", "sqlite", "sqlexception", "has no column named",
    "database disk image is malformed", "database is locked"
)

private val CONNECTIVITY_MARKERS = listOf(
    "нет соединения", "нет интернета", "проверьте подключение", "проверьте интернет",
    "не удалось отправить", "сбой связи",
    "unexpected end of stream", "unable to resolve", "connection refused", "econnrefused",
    "connection reset", "failed to connect", "network is unreachable", "software caused",
    "timeout", "timed out", "i/o failure", "stream was reset", "handshake"
)

/** Текст подсказки про VPN — единый для всех мест. */
const val VPN_ERROR_HINT: String =
    "Похоже, включён VPN. Из-за него приложение не может связаться с сервером — " +
        "отключите VPN и попробуйте снова."

/**
 * Возвращает подсказку про VPN, если он включён и ошибка похожа на сетевую;
 * иначе — исходное сообщение (или запасной текст).
 */
fun vpnAwareErrorMessage(context: Context, original: String?): String {
    val fallback = original?.takeIf { it.isNotBlank() && !isTechnicalDbErrorMessage(it) } ?: "Произошла ошибка"
    return if (isConnectivityErrorMessage(original) && isVpnActive(context)) {
        VPN_ERROR_HINT
    } else {
        fallback
    }
}

/**
 * Понятный текст сетевой ошибки без Context: сырые технические сообщения о сбое
 * соединения («Failed to connect…», «timeout» и т.п.) заменяет на человекочитаемое,
 * пустое — на [fallback], остальные (осмысленные серверные) — оставляет как есть.
 */
fun friendlyNetworkErrorMessage(rawMessage: String?, fallback: String): String = when {
    rawMessage.isNullOrBlank() -> fallback
    isTechnicalDbErrorMessage(rawMessage) -> fallback
    isConnectivityErrorMessage(rawMessage) ->
        "Нет связи с сервером. Проверьте подключение к интернету и попробуйте снова."
    else -> rawMessage
}
