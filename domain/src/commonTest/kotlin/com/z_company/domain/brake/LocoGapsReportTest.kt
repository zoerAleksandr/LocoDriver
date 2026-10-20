package com.z_company.domain.brake

import kotlin.test.Test
import kotlin.test.assertEquals

/** Фиксирует известные пробелы справочника локомотивов (список в docs/VU45_SPEC.md, «Пробелы справочника локомотивов»): при их закрытии тест обновляется. */
class LocoGapsReportTest {
    @Test fun knownGaps() {
        assertEquals(
            listOf("2ЭС4К", "3ЭС4К", "2ЭС5К", "3ЭС5К", "2ЭС6"),
            BrakeLocoCatalog.items.filter { it.pressurePerAxle == null }.map { it.series },
        )
        assertEquals(
            listOf("ЧС2", "ЧС2Т", "ЧС4", "ЧС4Т", "2ТЭП60", "ТЭП10", "2ТЭ10В", "3ТЭ10М", "4ТЭ10С", "2М62", "2М62У", "3М62У", "ЧМЭ3Т", "ЧМЭ3Э", "ТЭМ2А", "ТЭМ2УМ", "ТЭМ16", "ТЭМ17", "ТЭМ18", "ТЭМ7", "ТЭМ7А"),
            BrakeLocoCatalog.items.filter { BrakeDensity.groupBySeries(it.series) == null && it.series != "ВЛ10" }.map { it.series },
        )
    }
}
