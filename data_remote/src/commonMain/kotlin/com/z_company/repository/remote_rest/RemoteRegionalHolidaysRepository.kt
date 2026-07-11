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

    // Кеш праздников в памяти по ключу "region:year". Праздники меняются раз в год,
    // а сетевой запрос дорогой (~2.5 c на медленном HTTP-сервере) и раньше делался
    // на КАЖДУЮ загрузку Календаря/переключение месяца — что и тормозило экран.
    private val holidaysCache = mutableMapOf<String, List<RegionalHoliday>>()

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
        val key = "$region:$year"
        holidaysCache[key]?.let { return it }
        return try {
            val loaded = api.getRegionalHolidays(region, year)
            holidaysCache[key] = loaded // кешируем только успешную сетевую загрузку
            loaded
        } catch (e: Exception) {
            // При офлайне — hardcoded fallback, БЕЗ кеширования, чтобы позже повторить сеть.
            fallback.getHolidaysForRegionYear(region, year)
        }
    }


    /**
     * Строгая загрузка — без fallback. Бросает исключение при сетевых ошибках.
     * Вызывается из SettingsViewModel.changeRegion для pre-flight проверки сети.
     */
    override suspend fun loadHolidaysForRegionYearStrict(region: String, year: Int): List<RegionalHoliday> {
        val loaded = api.getRegionalHolidays(region, year)
        holidaysCache["$region:$year"] = loaded // прогреваем кеш при смене региона
        return loaded
    }
}
