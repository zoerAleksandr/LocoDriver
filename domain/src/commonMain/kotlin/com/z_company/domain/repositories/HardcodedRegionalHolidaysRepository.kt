package com.z_company.domain.repositories

import com.z_company.domain.entities.calendar.Region
import com.z_company.domain.entities.calendar.RegionalHoliday

/**
 * Захардкоженный каталог регионов и их праздников. Используется как fallback
 * пока сервер не предоставляет endpoint /regional-holidays.
 *
 * ВАЖНО: даты праздников 2026 года ориентировочные и подлежат уточнению
 * пользователем по республиканским законам. Особенно — мусульманские
 * праздники (Ураза-Байрам, Курбан-Байрам), которые рассчитываются по
 * лунному календарю (UmAlQura) и могут смещаться на ±1 день.
 *
 * Чтобы добавить регион/изменить дату — правьте константу [holidays2026]
 * и список [allRegions] ниже.
 */
class HardcodedRegionalHolidaysRepository : RegionalHolidaysRepository {

    private val allRegions: List<Region> = listOf(
        // ───── Республики РФ ─────
        Region("RU-AD", "Республика Адыгея", "RU"),
        Region("RU-AL", "Республика Алтай", "RU"),
        Region("RU-BA", "Республика Башкортостан", "RU"),
        Region("RU-BU", "Республика Бурятия", "RU"),
        Region("RU-DA", "Республика Дагестан", "RU"),
        Region("RU-IN", "Республика Ингушетия", "RU"),
        Region("RU-KB", "Кабардино-Балкарская Республика", "RU"),
        Region("RU-KL", "Республика Калмыкия", "RU"),
        Region("RU-KC", "Карачаево-Черкесская Республика", "RU"),
        Region("RU-KR", "Республика Крым", "RU"),
        Region("RU-SE", "Республика Северная Осетия — Алания", "RU"),
        Region("RU-TA", "Республика Татарстан", "RU"),
        Region("RU-TY", "Республика Тыва", "RU"),
        Region("RU-CE", "Чеченская Республика", "RU"),
        Region("RU-SA", "Республика Саха (Якутия)", "RU"),
    )

    /**
     * Праздники 2026 года, индексированные по коду региона.
     * Format: region → list of (month, day, name).
     *
     * TODO: уточнить даты исламских праздников 2026 (ориентировочные).
     */
    private val holidays2026: Map<String, List<Triple<Int, Int, String>>> = mapOf(
        "RU-TA" to listOf(
            Triple(8, 30, "День Республики Татарстан"),
            Triple(11, 6, "День Конституции Республики Татарстан"),
        ),
        "RU-BA" to listOf(
            Triple(10, 11, "День Республики Башкортостан"),
            Triple(12, 24, "День Конституции Республики Башкортостан"),
        ),
        "RU-SA" to listOf(
            Triple(4, 27, "День Республики Саха (Якутия)"),
            Triple(6, 21, "Ысыах"),
        ),
        "RU-BU" to listOf(
            Triple(2, 18, "Сагаалган"),
        ),
        "RU-TY" to listOf(
            Triple(2, 18, "Шагаа"),
            Triple(8, 15, "День Республики Тыва"),
        ),
        "RU-AL" to listOf(
            Triple(7, 3, "День образования Республики Алтай"),
        ),
        "RU-KL" to listOf(
            Triple(2, 18, "Цаган Сар"),
            Triple(12, 15, "Зул"),
        ),
        "RU-DA" to listOf(
            Triple(9, 15, "День единства народов Дагестана"),
        ),
        "RU-CE" to listOf(
            Triple(3, 23, "День Конституции Чеченской Республики"),
            Triple(4, 16, "День мира в Чеченской Республике"),
            Triple(9, 30, "День чеченской женщины"),
        ),
        "RU-IN" to listOf(
            Triple(6, 4, "День образования Республики Ингушетия"),
        ),
        "RU-KB" to listOf(
            Triple(3, 28, "День возрождения балкарского народа"),
        ),
        "RU-KC" to listOf(
            Triple(5, 3, "День возрождения карачаевского народа"),
        ),
        "RU-AD" to listOf(
            Triple(10, 5, "День образования Республики Адыгея"),
        ),
        "RU-CR" to listOf(
            Triple(3, 18, "День воссоединения Крыма с Россией"),
        ),
        "RU-SE" to listOf(
            Triple(11, 23, "День Конституции Республики Северная Осетия — Алания"),
        ),
    )

    override fun getRegionsForCountry(country: String): List<Region> =
        allRegions.filter { it.country == country }

    override fun getRegionByCode(code: String): Region? =
        allRegions.firstOrNull { it.code == code }

    override fun getHolidaysForRegionYear(region: String, year: Int): List<RegionalHoliday> {
        // Сейчас поддерживаем только 2026 год. Когда сервер заработает —
        // здесь будет вызов к нему для других годов.
        if (year != 2026) return emptyList()
        val raw = holidays2026[region] ?: return emptyList()
        return raw.map { (month, day, name) ->
            RegionalHoliday(
                region = region,
                year = year,
                month = month,
                dayOfMonth = day,
                name = name,
            )
        }
    }
}
