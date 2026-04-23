package com.z_company.domain.repositories

import com.z_company.domain.entities.calendar.Region
import com.z_company.domain.entities.calendar.RegionalHoliday

/**
 * Репозиторий для работы со списком регионов и их праздничными днями.
 *
 * Реализации:
 * - HardcodedRegionalHolidaysRepository — fallback с зашитыми данными
 * - RemoteRegionalHolidaysRepository — загрузка с сервера + кеш
 *
 * Все методы suspend для возможности сетевых запросов.
 */
interface RegionalHolidaysRepository {

    /** Список всех доступных регионов для указанной страны. */
    suspend fun getRegionsForCountry(country: String): List<Region>

    /** Получить регион по коду (например "RU-TA"). null — если такого нет. */
    suspend fun getRegionByCode(code: String): Region?

    /** Получить региональные праздники на год для конкретного региона. */
    suspend fun getHolidaysForRegionYear(region: String, year: Int): List<RegionalHoliday>
}
