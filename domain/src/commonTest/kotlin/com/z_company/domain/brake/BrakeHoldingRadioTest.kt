package com.z_company.domain.brake

import kotlin.test.Test
import kotlin.test.assertEquals

class BrakeHoldingRadioTest {
    @Test fun stopMessage_filled() {
        val d = HoldingRadioData(surname = "Иванов", trainNumber = "2604", km = "125", picket = "3", trackNumber = "2", stretch = "Луга - Преображенская", cause = "отказа ТЭД")
        assertEquals(
            "Внимание, все! Я, машинист Иванов поезда № 2604, остановился по причине отказа ТЭД на 125 километре, пикете 3 номер пути перегона 2, Будьте бдительны!",
            BrakeHoldingRadio.stopMessage(d),
        )
    }

    @Test fun stopMessage_emptyUsesEllipsis() {
        assertEquals(
            "Внимание, все! Я, машинист … поезда № …, остановился по причине … на … километре, пикете … номер пути перегона …, Будьте бдительны!",
            BrakeHoldingRadio.stopMessage(HoldingRadioData()),
        )
    }

    @Test fun helpLoco_time() {
        val d = HoldingRadioData(surname = "Иванов", trainNumber = "2604", km = "125", picket = "3", stretch = "Луга - Преображенская", locoSection = "А", locoSeries = "2ЭС6", locoNumber = "1661", cause = "отказа ТЭД", time = "09:05")
        assertEquals(
            "Я машинист Иванов поезда № 2604, на 125 км 3 пикете перегона Луга - Преображенская требую вспомогательный локомотив по причине неисправности ТПС, ССПС секции А, серии 2ЭС6, № 1661, из-за отказа ТЭД, точное место остановки сообщу после личного убеждения. Время 9 час 5 мин",
            BrakeHoldingRadio.helpLocoMessage(d),
        )
        assertEquals(true, BrakeHoldingRadio.helpLocoMessage(HoldingRadioData()).endsWith("Время … час … мин"))
        assertEquals(
            "Я машинист Иванов поезда № 2604, нахожусь «головой» поезда на км 125 пикете 3 перегона Луга - Преображенская.",
            BrakeHoldingRadio.verifiedPlaceMessage(HoldingRadioData(surname = "Иванов", trainNumber = "2604", km = "125", picket = "3", stretch = "Луга - Преображенская")),
        )
        assertEquals(10, BrakeHoldingRadio.HELP_LOCO_DEADLINE_MIN)
    }
}
