package com.z_company.domain.util

/**
 * Разовая чистка списка станций настроек от обрывков ввода («Ж», «Жа» вместо «Жарковский»),
 * осевших в 3.0.6 из-за автосохранения форм.
 *
 * Обрывок — запись, с которой строго начинается другая запись списка (без учёта регистра) и
 * которой нет среди [protectedNames]: это названия, реально использованные в маршрутах или
 * заведённые в справочнике станций. Защищённые записи остаются всегда.
 */
object StationListCleanup {
    fun removeFragments(stationList: List<String>, protectedNames: Collection<String>): List<String> {
        val protectedKeys = protectedNames.map { key(it) }.toSet()
        val keys = stationList.map { key(it) }
        return stationList.filterIndexed { index, _ ->
            val k = keys[index]
            k in protectedKeys || keys.none { other -> other.length > k.length && other.startsWith(k) }
        }
    }

    private fun key(name: String): String = name.trim().lowercase()
}
