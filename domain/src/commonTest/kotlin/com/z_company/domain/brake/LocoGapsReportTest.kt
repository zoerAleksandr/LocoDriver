package com.z_company.domain.brake

import kotlin.test.Test
import kotlin.test.assertEquals

/** Фиксирует известные пробелы справочника локомотивов (список в docs/VU45_SPEC.md, «Пробелы справочника локомотивов»): при их закрытии тест обновляется. */
class LocoGapsReportTest {
    @Test fun knownGaps() {
        println("GAPS noPressure=" + BrakeLocoCatalog.items.filter { it.pressurePerAxle == null }.map { it.series })
        println("GAPS noGroup=" + BrakeLocoCatalog.items.filter { BrakeDensity.groupBySeries(it.series) == null && it.series != "ВЛ10" }.map { it.series })
        println("GAPS total=" + BrakeLocoCatalog.items.size)
        assertEquals(emptyList(), BrakeLocoCatalog.items.filter { it.pressurePerAxle == null }.map { it.series })
        assertEquals(EXPECTED_NO_GROUP, BrakeLocoCatalog.items.filter { BrakeDensity.groupBySeries(it.series) == null && it.series != "ВЛ10" }.map { it.series })
    }

    private companion object { val EXPECTED_NO_GROUP: List<String> = listOf("ТЭМ16", "ТЭМ17", "ТЭМ18", "ВЛ11К", "2ТЭ25К") }
}
