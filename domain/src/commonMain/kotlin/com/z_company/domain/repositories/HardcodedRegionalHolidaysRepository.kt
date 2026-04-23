package com.z_company.domain.repositories

import com.z_company.domain.entities.calendar.Region
import com.z_company.domain.entities.calendar.RegionalHoliday

/**
 * Захардкоженный каталог регионов и их праздников. Используется как fallback
 * пока сервер не предоставляет endpoint /regional-holidays.
 *
 * Данные на 2026 год — по источнику kdelo.ru (Региональные праздники в России в 2026).
 *
 * Переходящие даты 2026:
 * - Пасха 2026 = 12 апреля
 * - Радоница 2026 = 21 апреля (вторник 2-й седмицы по Пасхе)
 * - Троица 2026 = 31 мая (50-й день по Пасхе)
 * - Ураза-Байрам 2026 = 20 марта
 * - Курбан-Байрам 2026 = 27 мая
 */
class HardcodedRegionalHolidaysRepository : RegionalHolidaysRepository {

    private val allRegions: List<Region> = listOf(
        // ───── Республики РФ ─────
        Region("RU-AD", "Республика Адыгея", "RU"),
        Region("RU-AL", "Республика Алтай", "RU"),
        Region("RU-BA", "Республика Башкортостан", "RU"),
        Region("RU-BU", "Республика Бурятия", "RU"),
        Region("RU-CU", "Чувашская Республика", "RU"),
        Region("RU-CR", "Республика Крым", "RU"),
        Region("RU-CE", "Чеченская Республика", "RU"),
        Region("RU-DON", "Донецкая Народная Республика", "RU"),
        Region("RU-LU", "Луганская Народная Республика", "RU"),
        Region("RU-TY", "Республика Тыва", "RU"),
        // ───── Края и области ─────
        Region("RU-BEL", "Белгородская область", "RU"),
        Region("RU-BRY", "Брянская область", "RU"),
        Region("RU-KDA", "Краснодарский край", "RU"),
        Region("RU-KHE", "Херсонская область", "RU"),
        Region("RU-ZAP", "Запорожская область", "RU"),
        Region("RU-SEV", "Город Севастополь", "RU"),
    )

    /**
     * Праздники 2026 года, индексированные по коду региона.
     * Format: region → list of (month, day, name).
     */
    private val holidays2026: Map<String, List<Triple<Int, Int, String>>> = mapOf(
        // ───── Республика Адыгея ─────
        "RU-AD" to listOf(
            Triple(3, 20, "Ураза-Байрам"),
            Triple(4, 21, "Радоница (День поминовения усопших)"),
            Triple(5, 27, "Курбан-Байрам"),
            Triple(10, 5, "День образования Республики Адыгея"),
        ),

        // ───── Республика Алтай ─────
        "RU-AL" to listOf(
            Triple(2, 21, "Чага-Байрам"),
        ),

        // ───── Республика Башкортостан ─────
        "RU-BA" to listOf(
            Triple(3, 20, "Ураза-Байрам"),
            Triple(5, 27, "Курбан-Байрам"),
            Triple(10, 11, "День Республики Башкортостан"),
        ),

        // ───── Республика Бурятия ─────
        "RU-BU" to listOf(
            Triple(2, 18, "Сагаалган"),
        ),

        // ───── Чувашская Республика ─────
        "RU-CU" to listOf(
            Triple(6, 24, "День Чувашской Республики"),
        ),

        // ───── Республика Крым ─────
        "RU-CR" to listOf(
            Triple(3, 18, "День воссоединения Крыма с Россией"),
        ),

        // ───── Чеченская Республика ─────
        "RU-CE" to listOf(
            Triple(3, 20, "Ураза-Байрам (Ид аль-Фитр)"),
            Triple(3, 23, "День Конституции Чеченской Республики"),
            Triple(4, 16, "День мира в Чеченской Республике"),
            Triple(5, 27, "Курбан-Байрам (Ид аль-Адха)"),
        ),

        // ───── Донецкая Народная Республика ─────
        "RU-DON" to listOf(
            Triple(4, 12, "Светлое Христово Воскресенье (Пасха)"),
            Triple(5, 11, "День Донецкой Народной Республики"),
            Triple(5, 31, "День Святой Троицы"),
            Triple(9, 30, "День возвращения ДНР в Россию"),
        ),

        // ───── Луганская Народная Республика ─────
        "RU-LU" to listOf(
            Triple(4, 12, "Светлое Христово Воскресенье (Пасха)"),
            Triple(5, 12, "День Луганской Народной Республики"),
            Triple(5, 31, "День Святой Троицы"),
            Triple(9, 30, "День воссоединения ЛНР с Россией"),
        ),

        // ───── Республика Тыва ─────
        "RU-TY" to listOf(
            Triple(2, 18, "Шагаа (Новый год по лунному календарю)"),
            Triple(5, 6, "День Конституции Республики Тыва"),
            Triple(7, 18, "Наадым (праздник животноводов)"),
            Triple(7, 19, "Наадым (праздник животноводов)"),
            Triple(7, 20, "Наадым (праздник животноводов)"),
            Triple(8, 15, "День Республики Тыва"),
        ),

        // ───── Белгородская область ─────
        "RU-BEL" to listOf(
            Triple(7, 12, "День Прохоровского поля"),
        ),

        // ───── Брянская область ─────
        "RU-BRY" to listOf(
            Triple(4, 21, "Радоница (день особого поминовения усопших)"),
        ),

        // ───── Краснодарский край ─────
        "RU-KDA" to listOf(
            Triple(4, 21, "Радоница (День поминовения усопших)"),
        ),

        // ───── Херсонская область ─────
        "RU-KHE" to listOf(
            Triple(3, 20, "Ураза-Байрам"),
            Triple(4, 12, "Светлое Христово Воскресенье (Пасха)"),
            Triple(5, 27, "Курбан-Байрам"),
            Triple(5, 31, "День Святой Троицы"),
            Triple(9, 30, "День воссоединения ДНР, ЛНР, Запорожской и Херсонской областей с Россией"),
        ),

        // ───── Запорожская область ─────
        "RU-ZAP" to listOf(
            Triple(4, 12, "Светлое Христово Воскресенье (Пасха)"),
            Triple(5, 31, "День Святой Троицы"),
            Triple(9, 30, "День воссоединения Запорожской области с Россией"),
        ),

        // ───── Город Севастополь ─────
        "RU-SEV" to listOf(
            Triple(3, 18, "День возвращения города Севастополя в Россию"),
            Triple(6, 14, "День основания города Севастополя"),
        ),
    )

    override suspend fun getRegionsForCountry(country: String): List<Region> =
        allRegions.filter { it.country == country }

    override suspend fun getRegionByCode(code: String): Region? =
        allRegions.firstOrNull { it.code == code }

    override suspend fun getHolidaysForRegionYear(region: String, year: Int): List<RegionalHoliday> {
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
