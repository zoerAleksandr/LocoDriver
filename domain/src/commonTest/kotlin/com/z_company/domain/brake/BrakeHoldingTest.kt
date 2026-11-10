package com.z_company.domain.brake

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Примеры и таблицы памятки ЦТ-162/р от 28.04.2025 (закрепление поезда на перегоне). */
class BrakeHoldingTest {
    private fun calc(i: HoldingInput) = BrakeHolding.calculate(i)!!

    @Test fun loadedExample_allShoes() {
        // 7100 + 300 т, уклон 0,018, груженый: 0,6 башмака на 100 тс -> 44,4 -> 45
        val r = calc(HoldingInput(7100.0, 300.0, HoldingLoad.LOADED, 18.0, shoesAvailable = 45, trainAxles = 384))
        assertEquals(45, r.shoesRequired)
        assertEquals(0.6, r.shoesPer100)
        assertEquals(1.6, r.manualAxlesPer100)
        assertEquals(7400.0, r.totalMass)
        assertEquals(0.0, r.remainingMass)
        assertNull(r.manualAxles)
        assertFalse(r.impossible)
    }

    @Test fun loadedExample_fiveShoes_manualBrakes() {
        // 5 башмаков закрепляют 833 т; остаток 6567 т -> 106 осей; 27 вагонов (ручной тормоз на 4 оси); при раздельном торможении - 54 ручных тормоза (по 2 на вагон)
        val r = calc(HoldingInput(7100.0, 300.0, HoldingLoad.LOADED, 18.0, shoesAvailable = 5, trainAxles = 384))
        assertEquals(833.0, r.coveredMass, 0.5)
        assertEquals(6567.0, r.remainingMass, 0.5)
        assertEquals(106, r.manualAxles)
        assertEquals(27, r.manualWagons)
        assertEquals(54, r.manualBrakesSplit)
        assertFalse(r.impossible)
    }

    @Test fun emptyExample() {
        // 2500 + 200 т, 400 осей, уклон 0,018, порожний: 1,6 башмака; 5 башмаков закрепляют 313 т; остаток 2387 т -> 38,2 -> 39 осей = 10 вагонов
        // (в памятке арифметическая опечатка: 2387/100 x 1,6 = 38,19, а написано 39,19 = 40 осей; число вагонов - 10 - то же)
        val r = calc(HoldingInput(2500.0, 200.0, HoldingLoad.EMPTY, 18.0, shoesAvailable = 5, trainAxles = 400))
        assertEquals(1.6, r.shoesPer100)
        assertEquals(44, r.shoesRequired) // 2700 / 100 * 1,6 = 43,2
        assertEquals(313.0, r.coveredMass, 0.5)
        assertEquals(2387.0, r.remainingMass, 0.5)
        assertEquals(39, r.manualAxles)
        assertEquals(10, r.manualWagons)
    }

    @Test fun slopeNotInTable_takesNearestGreater() {
        // 0,017 -> ступень 0,018; 0,0181 -> 0,020
        assertEquals(18, calc(HoldingInput(1000.0, 0.0, HoldingLoad.LOADED, 17.0)).slopeStepPermille)
        assertTrue(calc(HoldingInput(1000.0, 0.0, HoldingLoad.LOADED, 17.0)).slopeRoundedUp)
        assertEquals(20, calc(HoldingInput(1000.0, 0.0, HoldingLoad.LOADED, 18.1)).slopeStepPermille)
        assertEquals(0, calc(HoldingInput(1000.0, 0.0, HoldingLoad.LOADED, 0.0)).slopeStepPermille)
        assertFalse(calc(HoldingInput(1000.0, 0.0, HoldingLoad.LOADED, 18.0)).slopeRoundedUp)
        assertNull(BrakeHolding.calculate(HoldingInput(1000.0, 0.0, HoldingLoad.LOADED, 41.0)))
    }

    @Test fun steeperThan020_noManualNorm() {
        val r = calc(HoldingInput(3000.0, 0.0, HoldingLoad.LOADED, 24.0, shoesAvailable = 4))
        assertNull(r.manualAxlesPer100)
        assertTrue(r.impossible)
        assertNull(r.manualAxles)
        // башмаков достаточно - закрепление возможно
        val ok = calc(HoldingInput(3000.0, 0.0, HoldingLoad.LOADED, 24.0, shoesAvailable = 24))
        assertFalse(ok.impossible)
    }

    @Test fun notEnoughAxles_impossible() {
        val r = calc(HoldingInput(7100.0, 300.0, HoldingLoad.LOADED, 18.0, shoesAvailable = 0, trainAxles = 100))
        assertTrue(r.impossible)
    }

    @Test fun loadByAxleLoad_threshold10() {
        assertEquals(HoldingLoad.LOADED, BrakeHolding.loadByAxleLoad(3840.0, 384)) // 10,0 тс на ось - груженый
        assertEquals(HoldingLoad.EMPTY, BrakeHolding.loadByAxleLoad(3830.0, 384))
        assertNull(BrakeHolding.loadByAxleLoad(null, 384))
        assertNull(BrakeHolding.loadByAxleLoad(1000.0, 0))
    }

    /** Таблицы памятки (число башмаков по массе и уклону) = ceil(масса / 100 × k). Две ячейки груженой таблицы памятки (1200 т / 0,014 и 4800 т / 0,012) - опечатки. */
    @Test fun memoTableSamples() {
        fun shoes(mass: Double, slope: Double, load: HoldingLoad) = calc(HoldingInput(mass, 0.0, load, slope)).shoesRequired
        assertEquals(42, shoes(7000.0, 18.0, HoldingLoad.LOADED))
        assertEquals(52, shoes(4000.0, 40.0, HoldingLoad.LOADED))
        assertEquals(117, shoes(9000.0, 40.0, HoldingLoad.LOADED))
        assertEquals(152, shoes(4000.0, 40.0, HoldingLoad.EMPTY))
        assertEquals(84, shoes(2200.0, 40.0, HoldingLoad.EMPTY))
        assertEquals(1, shoes(500.0, 0.0, HoldingLoad.LOADED))
        assertEquals(2, shoes(500.0, 0.0, HoldingLoad.EMPTY))
        assertNotNull(BrakeHolding.manualAxlesPer100(10))
        assertNull(BrakeHolding.manualAxlesPer100(11))
    }
}
