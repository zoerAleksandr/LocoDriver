package com.z_company.repository.remote_rest

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

/**
 * Сигнал «сервер не принял наш bearer-токен» из сетевого слоя.
 *
 * Токен живёт 180 дней и не продлевается; когда он истекает, каждый запрос
 * получает 401. Раньше это замечал только экран «Профиль» при своей
 * загрузке, а синхронизация с главного экрана молча складывала
 * «Invalid creadential» в отчёт об ошибках. Теперь 401 ловится в Ktor-клиенте
 * ([RemoteRestClient]) на любом запросе с `Authorization`, а приложение
 * по этому сигналу разлогинивает пользователя и объясняет причину.
 *
 * Буфер в один элемент с вытеснением: серия из десятка 401 подряд (по запросу
 * на каждый маршрут) схлопывается в одно событие, а подписчик сам проверяет,
 * не разлогинен ли пользователь уже.
 */
object SessionExpiredNotifier {
    private val _events = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    val events: SharedFlow<Unit> = _events

    fun signal() {
        _events.tryEmit(Unit)
    }
}
