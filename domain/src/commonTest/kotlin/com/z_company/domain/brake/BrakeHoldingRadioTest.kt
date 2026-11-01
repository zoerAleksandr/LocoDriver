package com.z_company.domain.brake

import kotlin.test.Test
import kotlin.test.assertEquals

class BrakeHoldingRadioTest {
    @Test fun stopMessage_filled() {
        val d = HoldingRadioData(surname = "Иванов", trainNumber = "2604", km = "125", picket = "3", evenTrack = true, stretch = "Луга - Преображенская", cause = "отказа ТЭД")
        assertEquals(
            "Внимание все! Я, машинист Иванов поезда № 2604 остановился на 125 километре, 3 пикете чётного пути перегона Луга - Преображенская вследствие отказа ТЭД. Будьте бдительны!",
            BrakeHoldingRadio.stopMessage(d),
        )
    }

    @Test fun stopMessage_emptyUsesEllipsis() {
        assertEquals(
            "Внимание все! Я, машинист … поезда № … остановился на … километре, … пикете чётного (нечётного) пути перегона … вследствие …. Будьте бдительны!",
            BrakeHoldingRadio.stopMessage(HoldingRadioData()),
        )
    }

    @Test fun helpLoco_time() {
        val d = HoldingRadioData(surname = "Иванов", trainNumber = "2604", km = "125", picket = "3", stretch = "Луга - Преображенская", locoSection = "А", locoSeries = "2ЭС6", locoNumber = "1661", cause = "отказа ТЭД", time = "09:05")
        assertEquals(
            "Я машинист Иванов поезда № 2604, на 125 км 3 пикете перегона Луга - Преображенская требую вспомогательный локомотив по причине неисправности локомотива секции А, серии 2ЭС6, № 1661, из-за отказа ТЭД. Время 9 час 5 мин",
            BrakeHoldingRadio.helpLocoMessage(d),
        )
        assertEquals(true, BrakeHoldingRadio.helpLocoMessage(HoldingRadioData()).endsWith("Время … час … мин"))
        assertEquals(10, BrakeHoldingRadio.HELP_LOCO_DEADLINE_MIN)
    }
}
