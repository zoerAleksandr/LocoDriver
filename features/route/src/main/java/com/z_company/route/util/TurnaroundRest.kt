package com.z_company.route.util

import com.z_company.domain.entities.route.Route

/** Данные коннектора «Отдых в ПО» между смежными плечами туда → обратно. */
data class TurnaroundRest(
    val station: String,
    val durationMs: Long,
)

/**
 * Определяет коннектор «отдых в пункте оборота» между двумя СМЕЖНЫМИ в списке
 * маршрутами. Ничего не суммирует — только решает, показывать ли презентационную
 * связку, и считает станцию/длительность отдыха.
 *
 * Пара засчитывается, когда:
 * - у более раннего плеча («туда») стоит флаг `restPointOfTurnover`
 *   (машинист сам пометил отдых в ПО — доверяем ему, длительность не ограничиваем);
 * - у обоих плеч заданы нужные времена (сдача «туда», явка «обратно»);
 * - явка «обратно» строго позже сдачи «туда» (положительный разрыв).
 *
 * Направление не важно: [a] и [b] можно передавать в любом порядке (в списке
 * сортировка бывает и по возрастанию, и по убыванию).
 *
 * Три плеча подряд A→B→C обрабатываются вызывающим кодом попарно (A,B) и (B,C).
 *
 * @return данные коннектора или null, если пары нет.
 */
fun turnaroundRestBetween(
    a: Route,
    b: Route,
): TurnaroundRest? {
    val ta = a.basicData.timeStartWork ?: return null
    val tb = b.basicData.timeStartWork ?: return null
    val outbound = if (ta <= tb) a else b   // раньше начал = «туда»
    val ret = if (ta <= tb) b else a         // позже начал = «обратно»

    if (!outbound.basicData.restPointOfTurnover) return null

    val returnStart = ret.basicData.timeStartWork ?: return null
    val outboundEnd = outbound.basicData.timeEndWork ?: return null
    val gap = returnStart - outboundEnd
    // Верхнего потолка нет — длительность отдыха определяет пользователь.
    if (gap <= 0L) return null

    // Пункт оборота — станция прибытия «туда», иначе станция отправления «обратно».
    val station = outbound.trains.lastOrNull()?.stations
        ?.lastOrNull()?.stationName?.takeIf { it.isNotBlank() }
        ?: ret.trains.firstOrNull()?.stations
            ?.firstOrNull()?.stationName?.takeIf { it.isNotBlank() }
        ?: ""

    return TurnaroundRest(station = station, durationMs = gap)
}
