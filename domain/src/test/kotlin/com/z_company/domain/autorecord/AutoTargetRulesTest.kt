package com.z_company.domain.autorecord

import com.z_company.domain.entities.route.Station
import com.z_company.domain.entities.route.Train
import com.z_company.domain.track.NO_TRAIN_ID
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AutoTargetRulesTest {
    @Test
    fun `chip is on only for the recorded train`() {
        assertTrue(AutoTargetRules.isRecordingThis(true, "a", "a"))
        assertFalse(AutoTargetRules.isRecordingThis(true, "a", "b"))
        assertFalse(AutoTargetRules.isRecordingThis(false, "a", "a"))
        assertTrue(AutoTargetRules.isRecordingThis(true, "a", null)) // Главный/виджет - общее состояние
        assertTrue(AutoTargetRules.isRecordingThis(true, null, "b")) // цель ещё определяется
        assertFalse(AutoTargetRules.isRecordingThis(true, NO_TRAIN_ID, "b"))
    }

    @Test
    fun `conflict only when another train is recorded`() {
        assertTrue(AutoTargetRules.conflictsWith(true, "a", "b"))
        assertTrue(AutoTargetRules.conflictsWith(true, NO_TRAIN_ID, "b"))
        assertFalse(AutoTargetRules.conflictsWith(true, "a", "a"))
        assertFalse(AutoTargetRules.conflictsWith(false, "a", "b"))
        assertFalse(AutoTargetRules.conflictsWith(true, "a", null))
        assertFalse(AutoTargetRules.conflictsWith(true, null, "b"))
    }

    @Test
    fun `warning text by number, stations or nothing`() {
        val numbered = Train(number = " 2415 ")
        assertEquals(
            "Идёт запись трека поезда № 2415. Если продолжите, она будет прервана.",
            AutoTargetRules.conflictText(numbered, "x"),
        )
        val byStations = Train(
            stations = mutableListOf(
                Station(stationName = "Лужская"), Station(stationName = null), Station(stationName = "Гатчина"),
            ),
        )
        assertEquals(
            "Идёт запись трека поезда «Лужская — Гатчина». Если продолжите, она будет прервана.",
            AutoTargetRules.conflictText(byStations, "x"),
        )
        assertEquals(
            "Идёт запись трека поезда без номера. Если продолжите, она будет прервана.",
            AutoTargetRules.conflictText(Train(), "x"),
        )
        assertEquals(
            "Идёт запись трека поезда без номера. Если продолжите, она будет прервана.",
            AutoTargetRules.conflictText(null, "x"),
        )
        assertEquals(
            "Идёт запись трека без поезда. Если продолжите, она будет прервана.",
            AutoTargetRules.conflictText(null, NO_TRAIN_ID),
        )
        assertNull(AutoTargetRules.trainLabel(null, listOf(null, " ")))
    }

    @Test
    fun `concurrent resolves create exactly one train`() = runTest {
        val db = mutableListOf<Train>()
        val resolver = RecordTargetResolver(
            currentRoute = { "r1" to db.toList() },
            createTrain = { delay(50); db += it }, // запись не мгновенная
        )
        val ids = (1..5).map { async { resolver.resolve(null) } }.awaitAll()
        assertEquals(1, db.size)
        assertEquals(setOf(db.single().trainId), ids.toSet())
    }

    @Test
    fun `existing empty train created by auto is reused`() = runTest {
        val db = mutableListOf<Train>()
        val resolver = RecordTargetResolver({ "r1" to db.toList() }, { db += it })
        val first = resolver.resolve(null)
        val second = resolver.resolve(null)
        assertEquals(first, second)
        assertEquals(1, db.size)
    }

    @Test
    fun `no current route - no train and no creation`() = runTest {
        val db = mutableListOf<Train>()
        val resolver = RecordTargetResolver({ null }, { db += it })
        assertEquals(NO_TRAIN_ID, resolver.resolve(null))
        assertTrue(db.isEmpty())
    }

    @Test
    fun `opened train wins without creating`() = runTest {
        val db = mutableListOf<Train>()
        val resolver = RecordTargetResolver({ "r1" to db.toList() }, { db += it })
        assertEquals("form-train", resolver.resolve("form-train"))
        assertTrue(db.isEmpty())
    }
}
