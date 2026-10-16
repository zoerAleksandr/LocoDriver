package com.z_company.domain.brake

/** Данные локомотива для подсчёта обеспечения тормозами: учётный вес, тормозные оси и нажатие на ось. */
data class LocoBrakeData(
    val series: String,
    val weight: Double,
    val axles: Int,
    val pressurePerAxle: Double,
    val note: String? = null,
)

/**
 * Справочник по таблицам 4 и 5 прежней Инструкции 277 (только серии, для которых обе таблицы дают однозначную строку). В действующих Правилах
 * (п. 40 прил. 2) эти данные не приведены: их утверждает владелец инфраструктуры - не подтверждено по действующему документу, значения можно править.
 * Нажатие - груженый/пассажирский режим для чугунных колодок. Остальные серии вводятся вручную.
 */
object BrakeLocoCatalog {
    private const val PASS = "Пассажирский режим (скоростной режим даёт большее нажатие)"

    val items: List<LocoBrakeData> = listOf(
        LocoBrakeData("ВЛ10", 184.0, 8, 14.0, "ВЛ10 с № 19; для № 1-18 вес тот же"),
        LocoBrakeData("ВЛ10У", 200.0, 8, 14.0),
        LocoBrakeData("ВЛ11", 184.0, 8, 14.0),
        LocoBrakeData("ВЛ11М", 184.0, 8, 14.0),
        LocoBrakeData("ВЛ15", 288.0, 12, 14.0),
        LocoBrakeData("ВЛ60", 138.0, 6, 11.0, "Все индексы"),
        LocoBrakeData("ВЛ65", 138.0, 6, 14.0),
        LocoBrakeData("ВЛ80К", 184.0, 8, 14.0),
        LocoBrakeData("ВЛ80Р", 192.0, 8, 14.0),
        LocoBrakeData("ВЛ80С", 192.0, 8, 14.0),
        LocoBrakeData("ВЛ80Т", 192.0, 8, 14.0),
        LocoBrakeData("ВЛ82", 192.0, 8, 14.0),
        LocoBrakeData("ВЛ82М", 200.0, 8, 14.0),
        LocoBrakeData("ВЛ85", 288.0, 12, 14.0),
        LocoBrakeData("ЧС2", 120.0, 6, 12.0, PASS),
        LocoBrakeData("ЧС2Т", 128.0, 6, 12.0, PASS),
        LocoBrakeData("ЧС4", 126.0, 6, 12.0, PASS),
        LocoBrakeData("ЧС4Т", 126.0, 6, 12.0, PASS),
        LocoBrakeData("ЧС6", 164.0, 8, 12.0, PASS),
        LocoBrakeData("ЧС7", 172.0, 8, 12.0, PASS),
        LocoBrakeData("ЧС8", 175.0, 8, 12.0, PASS),
        LocoBrakeData("ЧС200", 156.0, 8, 12.0, PASS),
        LocoBrakeData("ЭП1", 135.0, 6, 14.0, "Пассажирский режим (на скоростном режиме 18 тс)"),
        LocoBrakeData("ТЭП60", 129.0, 6, 12.0),
        LocoBrakeData("2ТЭП60", 258.0, 12, 12.0),
        LocoBrakeData("ТЭП70", 136.0, 6, 12.0),
        LocoBrakeData("ТЭП10", 129.0, 6, 11.5),
        LocoBrakeData("2ТЭ10В", 276.0, 12, 12.0),
        LocoBrakeData("2ТЭ10У", 276.0, 12, 12.0),
        LocoBrakeData("3ТЭ10М", 414.0, 18, 12.0),
        LocoBrakeData("4ТЭ10С", 552.0, 24, 12.0),
        LocoBrakeData("2ТЭ121", 300.0, 12, 12.0),
        LocoBrakeData("2М62", 240.0, 12, 12.0),
        LocoBrakeData("2М62У", 252.0, 12, 12.0),
        LocoBrakeData("3М62У", 378.0, 18, 12.0),
        LocoBrakeData("ЧМЭ3", 123.0, 6, 11.0),
        LocoBrakeData("ЧМЭ3Т", 123.0, 6, 11.0),
        LocoBrakeData("ЧМЭ3Э", 123.0, 6, 11.0),
        LocoBrakeData("ТЭМ2А", 123.0, 6, 11.0),
        LocoBrakeData("ТЭМ2У", 123.0, 6, 11.0),
        LocoBrakeData("ТЭМ2УМ", 123.0, 6, 11.0),
        LocoBrakeData("ТЭМ16", 123.0, 6, 11.0),
        LocoBrakeData("ТЭМ17", 123.0, 6, 11.0),
        LocoBrakeData("ТЭМ18", 123.0, 6, 11.0),
        LocoBrakeData("ТЭМ7", 180.0, 8, 13.0),
        LocoBrakeData("ТЭМ7А", 180.0, 8, 13.0),
    )

    private fun norm(s: String): String = s.trim().uppercase().filter { it != ' ' && it != '-' && it != '_' }.map {
        when (it) {
            'A' -> 'А'; 'B' -> 'В'; 'C' -> 'С'; 'E' -> 'Е'; 'H' -> 'Н'; 'K' -> 'К'; 'M' -> 'М'
            'O' -> 'О'; 'P' -> 'Р'; 'T' -> 'Т'; 'X' -> 'Х'; 'Y' -> 'У'; else -> it
        }
    }.joinToString("")

    /** Точное совпадение серии (регистр, пробелы, дефисы и латиница-двойники не учитываются). */
    fun find(series: String?): LocoBrakeData? {
        val n = norm(series ?: return null)
        if (n.isEmpty()) return null
        return items.firstOrNull { norm(it.series) == n }
    }
}

/** Данные для предзаполнения справки из маршрута (поезд и локомотив); строки как в `Train`/`Locomotive`. */
data class BrakePrefill(
    val weight: String? = null,
    val axles: String? = null,
    val trainNumber: String? = null,
    val locoSeries: String? = null,
    val locoNumber: String? = null,
    /** Маршрут и поезд-источник (для повторного «Заполнить из маршрута»); в расчёте не участвуют. */
    val routeId: String? = null,
    val trainId: String? = null,
) {
    /** Без пустых и нулевых значений: поле поезда «0» или пустое за данные не считается. */
    fun sanitized(): BrakePrefill {
        fun num(v: String?): String? {
            val t = v?.trim().orEmpty().replace(',', '.')
            return t.takeIf { (it.toDoubleOrNull() ?: 0.0) > 0.0 }?.let { v!!.trim() }
        }
        return BrakePrefill(
            weight = num(weight),
            axles = axles?.trim()?.takeIf { (it.toIntOrNull() ?: 0) > 0 },
            trainNumber = trainNumber?.trim()?.takeIf { it.isNotEmpty() },
            locoSeries = locoSeries?.trim()?.takeIf { it.isNotEmpty() },
            locoNumber = locoNumber?.trim()?.takeIf { it.isNotEmpty() },
            routeId = routeId?.takeIf { it.isNotBlank() },
            trainId = trainId?.takeIf { it.isNotBlank() },
        )
    }
}
