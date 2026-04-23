package com.z_company.repository.remote_rest

import com.z_company.domain.entities.calendar.Region
import com.z_company.domain.entities.calendar.RegionalHoliday
import com.z_company.domain.repositories.HardcodedRegionalHolidaysRepository
import com.z_company.domain.repositories.RegionalHolidaysRepository

/**
 * Сетевая реализация [RegionalHolidaysRepository]. Загружает регионы и их
 * праздники с сервера (см. [RemoteRestApi.getRegions] /
 * [RemoteRestApi.getRegionalHolidays]).
 *
 * В случае ошибки сети — fallback на [HardcodedRegionalHolidaysRepository]
 * чтобы приложение продолжало работать оффлайн.
 *
 * Регионы кешируются в памяти (меняются редко).
 * Праздники не кешируются здесь — локальное кеширование реализуется
 * через таблицу RegionalHoliday в SQLDelight при необходимости.
 */
class RemoteRegionalHolidaysRepository(
    private val api: RemoteRestApi,
) : RegionalHolidaysRepository {

    private val fallback = HardcodedRegionalHolidaysRepository()

    // Кеш регионов в памяти — они редко меняются
    private val regionsCache = mutableMapOf<String, List<Region>>()

    override suspend fun getRegionsForCountry(country: String): List<Region> {
        regionsCache[country]?.let { return it }
        return try {
            val loaded = api.getRegions(country)
            regionsCache[country] = loaded
            loaded
        } catch (e: Exception) {
            fallback.getRegionsForCountry(country)
        }
    }

    override suspend fun getRegionByCode(code: String): Region? {
        regionsCache.values.flatten().firstOrNull { it.code == code }?.let { return it }
        return fallback.getRegionByCode(code)
    }

    override suspend fun getHolidaysForRegionYear(region: String, year: Int): List<RegionalHoliday> {
        return try {
            api.getRegionalHolidays(region, year)
        } catch (e: Exception) {
            fallback.getHolidaysForRegionYear(region, year)
        }
    }
}
