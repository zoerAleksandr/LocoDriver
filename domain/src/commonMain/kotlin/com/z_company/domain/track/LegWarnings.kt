package com.z_company.domain.track

/**
 * Какие прошлые предупреждения (ДУ-61) относятся к плечу поезда. Место записи - станция («А») или перегон («А — Б», названия в порядке
 * движения, как их предлагает «Где действует»). Запись подходит плечу, если все названия есть среди станций плеча, а перегон проходится
 * в том же направлении: в обратном ограничение другого пути, его не предлагаем.
 */
object LegWarnings {
    private const val SEP = " — "

    /** [legStops] - названия станций плеча по ходу движения. */
    fun matches(place: String, legStops: List<String>): Boolean {
        val names = place.split(SEP).map { it.trim() }.filter { it.isNotEmpty() }
        if (names.isEmpty() || names.size > 2) return false
        val idx = names.map { n -> legStops.indexOfFirst { SectionPicker.sameStation(it, n) } }
        if (idx.any { it < 0 }) return false
        return names.size == 1 || idx[0] < idx[1]
    }
}
