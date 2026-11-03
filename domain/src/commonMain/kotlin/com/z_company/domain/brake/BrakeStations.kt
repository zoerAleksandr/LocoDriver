package com.z_company.domain.brake

import com.z_company.domain.util.ServicePhaseStationsMerge

/** Подсказки станций для полей ВУ-45: станции текущего поезда и справочник пользователя. */
object BrakeStations {
    /**
     * Объединение без дубликатов (по нормализованному названию: регистр, «ё», дефис, пробелы): сначала станции текущего поезда в порядке
     * следования, затем остальные станции пользователя по алфавиту (без учёта регистра).
     */
    fun merge(train: List<String>, others: List<String>): List<String> {
        val seen = HashSet<String>()
        val result = ArrayList<String>()
        for (n in train) {
            val name = n.trim()
            if (name.isNotEmpty() && seen.add(ServicePhaseStationsMerge.normalizeName(name))) result += name
        }
        others.map { it.trim() }.filter { it.isNotEmpty() }
            .sortedWith(compareBy({ ServicePhaseStationsMerge.normalizeName(it) }, { if (it.first().isUpperCase()) 0 else 1 }))
            .forEach { if (seen.add(ServicePhaseStationsMerge.normalizeName(it))) result += it }
        return result
    }

    /** Список, отфильтрованный по набранному (подстрока без учёта регистра); пустой запрос - весь список. */
    fun filter(all: List<String>, query: String): List<String> {
        val q = query.trim().lowercase()
        return if (q.isEmpty()) all else all.filter { it.lowercase().contains(q) }
    }

    /** Набранное совпадает с одним из вариантов целиком - подсказки не нужны. */
    fun isExact(all: List<String>, query: String): Boolean {
        val q = ServicePhaseStationsMerge.normalizeName(query)
        return q.isNotEmpty() && all.any { ServicePhaseStationsMerge.normalizeName(it) == q }
    }
}
