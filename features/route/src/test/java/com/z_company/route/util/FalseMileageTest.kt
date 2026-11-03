package com.z_company.route.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FalseMileageTest {
    @Test
    fun enteredThresholdIsOnePointFive() {
        assertFalse(FalseMileage.isSuspicious(247.5, 165, 0.0))  // ровно 1,5× — ещё можно
        assertTrue(FalseMileage.isSuspicious(247.6, 165, 0.0))   // больше 1,5× — нельзя молча
        assertFalse(FalseMileage.isSuspicious(100.0, 165, 0.0))  // короче введённого — не наш случай
    }

    @Test
    fun straightSumThresholdIsThreeAndWorksWithoutEnteredDistance() {
        assertFalse(FalseMileage.isSuspicious(300.0, 0, 100.0))  // ровно 3×
        assertTrue(FalseMileage.isSuspicious(300.1, 0, 100.0))
        assertTrue(FalseMileage.isSuspicious(710.0, 0, 120.0))   // введённое 0 — только сумма прямых
        assertFalse(FalseMileage.isSuspicious(710.0, 0, 0.0))    // нет прямых — проверять нечем
    }

    @Test
    fun eitherConditionTriggers() {
        // введённое в порядке, но путь втрое длиннее прямых
        assertTrue(FalseMileage.isSuspicious(240.0, 200, 70.0))
        // путь в 3× прямых нет, но больше 1,5× введённого
        assertTrue(FalseMileage.isSuspicious(260.0, 165, 120.0))
        assertFalse(FalseMileage.isSuspicious(0.0, 165, 120.0))
    }

    @Test
    fun userScenarioIsCaught() {
        // 709,8 км при введённых 165 и ~120 км по прямым
        assertTrue(FalseMileage.isSuspicious(709.8, 165, 120.0))
        // нормальный замер 164,9 — молча применяется
        assertFalse(FalseMileage.isSuspicious(164.9, 165, 120.0))
    }

    @Test
    fun straightSumIsSumOfSegments() {
        val km = FalseMileage.straightSumKm(listOf(60.0 to 30.0, 60.0 to 30.0, 61.0 to 30.0))
        assertEquals(111.2, km, 0.3)  // градус широты ≈ 111,2 км; первое звено нулевое
        assertEquals(0.0, FalseMileage.straightSumKm(listOf(60.0 to 30.0)), 0.0)
    }

    @Test
    fun messageUsesEnteredOrStraightSumAndNeverMentionsReversal() {
        val a = FalseMileage.message(709.8, 165, 120.0)
        assertEquals(
            "Расстояние намного больше введённого: 710 км вместо 165 км. Возможно, неверно указана одна из станций — проверьте станции.", a,
        )
        assertTrue(FalseMileage.message(709.8, 0, 120.4).contains("вместо 120 км"))
        assertFalse(a.contains("разворот"))
    }
}
