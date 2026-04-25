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

    /**
     * Получить региональные праздники на год для конкретного региона.
     * Реализации МОГУТ возвращать локальный fallback при сетевых ошибках —
     * для оффлайн-сценариев это нормальное поведение.
     */
    suspend fun getHolidaysForRegionYear(region: String, year: Int): List<RegionalHoliday>

    /**
     * Строгая загрузка региональных праздников — БЕЗ fallback на хардкод.
     * Бросает исключение при сетевых/серверных ошибках.
     *
     * Используется когда нам важно знать что данные пришли именно с сервера
     * (например, смена региона пользователем — мы должны явно сообщить об ошибке,
     * а не молча подставить старые/неполные данные).
     *
     * Реализация по умолчанию вызывает [getHolidaysForRegionYear] — для
     * хардкод-репозитория это эквивалентно (он не ходит в сеть).
     */
    suspend fun loadHolidaysForRegionYearStrict(region: String, year: Int): List<RegionalHoliday> =
        getHolidaysForRegionYear(region, year)
}
