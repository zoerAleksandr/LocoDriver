package com.z_company.domain.brake

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BrakeStationsTest {
    @Test fun trainFirstThenAlphabetical_noDuplicates() {
        val r = BrakeStations.merge(listOf("Рыбацкое", "Бабаево", "Вологда"), listOf("Агрыз", "вологда", "Бабаево", "ярославль", "Бологое"))
        assertEquals(listOf("Рыбацкое", "Бабаево", "Вологда", "Агрыз", "Бологое", "ярославль"), r)
    }

    @Test fun duplicatesByNormalizedName() {
        assertEquals(listOf("Усть-Луга"), BrakeStations.merge(emptyList(), listOf("Усть-Луга", "усть луга", " УСТЬ-ЛУГА ")))
        assertEquals(listOf("Берёзовка"), BrakeStations.merge(listOf("Берёзовка"), listOf("Березовка")))
    }

    @Test fun filterCaseInsensitive() {
        val all = listOf("Вологда", "Бологое", "Агрыз")
        assertEquals(listOf("Вологда", "Бологое"), BrakeStations.filter(all, "ОЛО"))
        assertEquals(all, BrakeStations.filter(all, " "))
        assertTrue(BrakeStations.filter(all, "xyz").isEmpty())
        assertTrue(BrakeStations.isExact(all, "вологда"))
    }
}
