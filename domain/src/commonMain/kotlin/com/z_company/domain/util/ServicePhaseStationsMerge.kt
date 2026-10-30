package com.z_company.domain.util

import com.z_company.domain.entities.setting.ServicePhaseStation

/**
 * Слияние результата автоподбора с текущими станциями плеча
 * (SCREEN_SPECS §6.6, дизайн design/service_phase_stations, экран 21).
 *
 * - Найденная станция «уже в списке», если совпадает по названию (без учёта регистра,
 *   е/ё, пробелов/дефисов) и типу с имеющейся. Такая не дублируется: у имеющейся
 *   дополняются пустые координаты, км, ЕСР, osmId.
 * - Ручные станции, которых нет в результате, остаются.
 * - Итог упорядочен по км; станции без км стоят сразу после ближайшей предыдущей
 *   станции с км (как стояли).
 */
object ServicePhaseStationsMerge {

    fun normalizeName(name: String): String =
        name.replace('ё', 'е').replace('Ё', 'Е').replace('-', ' ')
            .trim().split(Regex("\\s+")).joinToString(" ").lowercase()
            // «22км» и «22 км» - одна станция.
            .replace(Regex("(\\d)(?=\\p{L})"), "$1 ")

    /** Имеющаяся станция, совпадающая с найденной; null — найденная новая. */
    fun existingMatch(found: ServicePhaseStation, existing: List<ServicePhaseStation>): ServicePhaseStation? {
        val key = normalizeName(found.name)
        return existing.firstOrNull { it.kind == found.kind && normalizeName(it.name) == key }
    }

    /**
     * @param existing текущие станции плеча, по порядку
     * @param found все станции результата автоподбора
     * @param selectedIds id найденных станций, отмеченных пользователем
     * @return (итоговый список, id добавленных станций)
     */
    fun merge(
        existing: List<ServicePhaseStation>,
        found: List<ServicePhaseStation>,
        selectedIds: Set<String>,
    ): Pair<List<ServicePhaseStation>, Set<String>> {
        val updated = existing.toMutableList()
        val added = mutableListOf<ServicePhaseStation>()
        for (f in found) {
            val match = existingMatch(f, updated)
            if (match != null) {
                val idx = updated.indexOfFirst { it.id == match.id }
                updated[idx] = match.enrichedBy(f)
            } else if (f.id in selectedIds) {
                added += f
            }
        }
        data class Keyed(val station: ServicePhaseStation, val km: Double, val afterKm: Int, val seq: Int)
        val keyed = mutableListOf<Keyed>()
        var lastKm = Double.NEGATIVE_INFINITY
        updated.forEachIndexed { i, st ->
            val km = st.kmFromStart
            if (km != null) {
                lastKm = km
                keyed += Keyed(st, km, 0, i)
            } else {
                keyed += Keyed(st, lastKm, 1, i)
            }
        }
        added.forEachIndexed { i, st -> keyed += Keyed(st, st.kmFromStart ?: Double.MAX_VALUE, 0, updated.size + i) }
        val sorted = keyed.sortedWith(compareBy<Keyed>({ it.km }, { it.afterKm }, { it.seq })).map { it.station }
        return sorted to added.map { it.id }.toSet()
    }

    /** Названия, которых нет в справочнике станций (без учёта регистра), без повторов. */
    fun newNames(stations: List<ServicePhaseStation>, stationList: List<String>): List<String> {
        val known = stationList.map { normalizeName(it) }.toMutableSet()
        val out = mutableListOf<String>()
        for (st in stations) {
            if (known.add(normalizeName(st.name))) out += st.name
        }
        return out
    }

    private fun ServicePhaseStation.enrichedBy(f: ServicePhaseStation): ServicePhaseStation {
        val takeCoords = !hasCoordinates && f.hasCoordinates
        return copy(
            latitude = if (takeCoords) f.latitude else latitude,
            longitude = if (takeCoords) f.longitude else longitude,
            kmFromStart = kmFromStart ?: f.kmFromStart,
            esr = esr ?: f.esr,
            osmId = osmId ?: f.osmId,
        )
    }
}
