package com.z_company.domain.repositories

import com.z_company.domain.entities.calendar.Region
import com.z_company.domain.entities.calendar.RegionalHoliday

/**
 * Репозиторий для работы со списком регионов и их праздничными днями.
 *
 * В текущей версии данные захардкожены в реализации (RegionsCatalog).
 * В будущем легко переключить на загрузку с сервера — нужно лишь изменить
 * implementation в DI-модуле, контракт остаётся.
 */
interface RegionalHolidaysRepository {

    /** Список всех доступных регионов для указанной страны. */
    fun getRegionsForCountry(country: String): List<Region>

    /** Получить регион по коду (например "RU-TA"). null — если такого нет. */
    fun getRegionByCode(code: String): Region?

    /** Получить региональные праздники на год для конкретного региона. */
    fun getHolidaysForRegionYear(region: String, year: Int): List<RegionalHoliday>
}
