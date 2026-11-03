package com.z_company.domain.util

import com.z_company.domain.entities.norma_time.StationNorm
import com.z_company.domain.entities.setting.ServicePhase
import com.z_company.domain.entities.setting.ServicePhasePoint
import com.z_company.domain.entities.setting.ServicePhaseStation
import com.z_company.domain.util.StationDirectory.withDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class StationDirectoryTest {
    private val phase = ServicePhase(
        departureStation = "Луга",
        arrivalStation = "Лужская",
        distance = 201,
        intermediateStations = listOf(ServicePhaseStation(name = "Мшинская"), ServicePhaseStation(name = "Котлы-2", latitude = 59.6, longitude = 28.7)),
        departurePoint = ServicePhasePoint(58.74, 29.84, esr = "072009"),
    )

    @Test
    fun phaseReadsCoordinatesFromDirectory() {
        val dir = listOf(
            StationNorm(name = "лужская ", latitude = 59.62, longitude = 28.57, esr = "076300"),
            StationNorm(name = "Мшинская", latitude = 59.01, longitude = 29.94),
            StationNorm(name = "Котлы-2", latitude = 59.61, longitude = 28.71),
            StationNorm(name = "Луга"), // без координат — у плеча остаются свои
        )
        val r = phase.withDirectory(dir)
        assertEquals(59.62, r.arrivalPoint?.latitude)
        assertEquals("076300", r.arrivalPoint?.esr)
        assertEquals(58.74, r.departurePoint?.latitude)
        assertEquals(59.01, r.intermediateStations[0].latitude)
        assertEquals(59.61, r.intermediateStations[1].latitude) // справочник побеждает копию в плече
    }

    @Test
    fun savedPhaseCreatesAndUpdatesStations() {
        val dir = listOf(StationNorm(stationId = "id-1", name = "Котлы-2", appearanceToStartMin = 20, axisKm = 120))
        val merged = assertNotNull(StationDirectory.mergePhase(dir, phase, now = 5))
        val kotly = merged.first { it.stationId == "id-1" }
        assertEquals(59.6, kotly.latitude)
        assertEquals(20, kotly.appearanceToStartMin) // нормы и км оси не трогаем
        assertEquals(120, kotly.axisKm)
        val luga = merged.first { it.name == "Луга" }
        assertEquals("072009", luga.esr)
        assertNull(merged.firstOrNull { it.name == "Мшинская" }) // без координат запись не создаётся
        assertNull(StationDirectory.mergePhase(merged, phase, now = 6)) // повтор — без изменений
    }

    @Test
    fun backfillDoesNotOverwriteDirectory() {
        val dir = listOf(StationNorm(name = "Котлы-2", latitude = 1.0, longitude = 2.0))
        val merged = assertNotNull(StationDirectory.backfill(dir, listOf(phase), now = 1))
        assertEquals(1.0, merged.first { it.name == "Котлы-2" }.latitude)
        assertEquals(58.74, merged.first { it.name == "Луга" }.latitude)
    }

    @Test
    fun trainMapTargetIsLastTrainStationWithoutCoordinates() {
        val dir = listOf(StationNorm(name = "Луга", latitude = 58.7, longitude = 29.8), StationNorm(name = "Мга"))
        assertEquals("Мга", StationDirectory.stationWithoutCoordinates(listOf("Луга", " мга", null), listOf("Лужская"), dir)?.lowercase()?.replaceFirstChar { it.uppercase() })
        assertEquals("Лужская", StationDirectory.stationWithoutCoordinates(listOf("Луга", null), listOf("Луга", "Лужская"), dir))
        assertNull(StationDirectory.stationWithoutCoordinates(listOf("Луга"), listOf("Луга"), dir))
        assertNull(StationDirectory.stationWithoutCoordinates(emptyList(), emptyList(), dir))
    }

    @Test
    fun fillCoordinatesNeverOverwrites() {
        val dir = listOf(
            StationNorm(name = "Луга", latitude = 58.7, longitude = 29.8),
            StationNorm(name = "Мга", appearanceToStartMin = 5),
        )
        assertNull(StationDirectory.fillCoordinates(dir, "луга", 1.0, 2.0, 7))
        val filled = assertNotNull(StationDirectory.fillCoordinates(dir, "Мга", 59.7, 31.0, 7))
        assertEquals(59.7, filled.first { it.name == "Мга" }.latitude)
        assertEquals(5, filled.first { it.name == "Мга" }.appearanceToStartMin)
        assertEquals(58.7, filled.first { it.name == "Луга" }.latitude)
        val created = assertNotNull(StationDirectory.fillCoordinates(dir, "Тосно", 59.5, 30.9, 7))
        assertEquals(3, created.size)
    }
}
