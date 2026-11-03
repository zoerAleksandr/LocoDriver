package com.z_company.domain.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RailwayByEsrTest {
    @Test
    fun codesMapToRailways() {
        assertEquals("Октябрьская", RailwayByEsr.byEsr("031201"))
        assertEquals("Октябрьская", RailwayByEsr.byEsr("011306"))
        assertEquals("Октябрьская", RailwayByEsr.byEsr("020004"))
        assertEquals("Московская", RailwayByEsr.byEsr("195506"))
        assertEquals("Куйбышевская", RailwayByEsr.byEsr("650001"))
        assertEquals("Южно-Уральская", RailwayByEsr.byEsr("800008"))
        assertEquals("Дальневосточная", RailwayByEsr.byEsr("990005"))
    }

    @Test
    fun railwaysConfirmedByUser() {
        // Сахалин - в составе Дальневосточной
        listOf("990062", "990630", "994345", "993639").forEach { assertEquals("Дальневосточная", RailwayByEsr.byEsr(it), it) }
        // БАМ Хани - Тында
        listOf("904601", "904777", "905017", "910000").forEach { assertEquals("Дальневосточная", RailwayByEsr.byEsr(it), it) }
        // Нерюнгри-Пассажирская - ДВЖД, дальше на Томмот - Железные дороги Якутии
        assertEquals("Дальневосточная", RailwayByEsr.byEsr("911501"))
        listOf("911624", "911643", "912006", "912103", "912203").forEach { assertEquals("Железные дороги Якутии", RailwayByEsr.byEsr(it), it) }
        // стык Астрахань - Волгоград
        listOf("541109", "541734", "541791", "259186").forEach { assertEquals("Приволжская", RailwayByEsr.byEsr(it), it) }
        assertEquals("Октябрьская", RailwayByEsr.byEsr("084059")) // Ивангород САИПС
        assertNull(RailwayByEsr.byEsr("084060"))
        assertEquals("Северо-Кавказская", RailwayByEsr.byEsr("510204"))
        assertEquals("Горьковская", RailwayByEsr.byEsr("259185"))
    }

    @Test
    fun foreignAndBadCodesGiveNull() {
        assertNull(RailwayByEsr.byEsr("090001")) // Латвия
        assertNull(RailwayByEsr.byEsr("130001")) // Белоруссия
        assertNull(RailwayByEsr.byEsr("66"))
        assertNull(RailwayByEsr.byEsr("12ab56"))
        assertNull(RailwayByEsr.byEsr(null))
    }

    @Test
    fun operatorWinsOverCode() {
        assertEquals("Октябрьская ЖД", RailwayByEsr.label("800008", "Октябрьская железная дорога"))
        assertEquals("Южно-Уральская ЖД", RailwayByEsr.label("800008", "РЖД"))
        assertEquals("Южно-Уральская ЖД", RailwayByEsr.label(null, "Южно-Уральская ЖД"))
        assertNull(RailwayByEsr.label(null, "РЖД"))
        assertNull(RailwayByEsr.label(null, "Северная компания"))
    }

    @Test
    fun longerNameWinsInOperator() {
        assertEquals("Северо-Кавказская", RailwayByEsr.byOperator("Северо-Кавказская железная дорога"))
        assertEquals("Юго-Восточная", RailwayByEsr.byOperator("Юго-Восточная железная дорога"))
    }
}
