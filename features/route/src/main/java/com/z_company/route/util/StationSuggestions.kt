package com.z_company.route.util

/**
 * Подходит ли станция под введённый фрагмент: совпадение с начала названия
 * или с начала любого слова («Санкт-Петербург-Главный» находится по «Глав»).
 */
fun stationNameMatches(name: String, query: String): Boolean {
    val q = query.trim()
    if (q.isEmpty()) return true
    if (name.startsWith(q, ignoreCase = true)) return true
    return name.split(' ', '-', '.', '/').any { it.isNotEmpty() && it.startsWith(q, ignoreCase = true) }
}

/**
 * Единый список подсказок станций: имена из настроек (stationList) плюс имена из
 * справочника станций. Станция, заведённая в справочнике, но не попавшая в stationList,
 * иначе не показывалась бы в подсказках. Дубли (без учёта регистра) убираются.
 */
fun mergeStationSuggestions(settingsNames: List<String>, directoryNames: List<String>): List<String> {
    val seen = HashSet<String>()
    return (settingsNames + directoryNames)
        .map { it.trim() }
        .filter { it.isNotEmpty() && seen.add(it.lowercase()) }
}
