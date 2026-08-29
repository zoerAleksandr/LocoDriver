package com.z_company.domain.util_for_entities_test

import com.z_company.domain.entities.route.BasicData
import com.z_company.domain.entities.route.Passenger
import com.z_company.domain.entities.route.Route
import com.z_company.domain.entities.route.Train
import com.z_company.domain.entities.route.UtilsForEntities.getTimeInHeavyTrain
import com.z_company.domain.entities.route.UtilsForEntities.getTimeInLongTrain
import com.z_company.domain.entities.route.UtilsForEntities.getTimeInServicePhase
import org.junit.Test
import kotlin.test.assertEquals

/**
 * Доплаты за тяжеловесный / длинносоставный поезд и за плечо обслуживания
 * НЕ должны начисляться на часы следования пассажиром — время смены для
 * этих доплат считается без пассажирских часов.
 */
class SurchargeTimePassengerTest {

    private val hour = 3_600_000L

    /** Смена 10 ч; поезд подходит под все три категории доплат. */
    private fun route(passengerHours: Long?, breakHours: Long = 0L): Route {
        val passengers = if (passengerHours != null) {
            mutableListOf(
                Passenger(timeDeparture = 0L, timeArrival = passengerHours * hour)
            )
        } else {
            mutableListOf()
        }
        return Route(
            basicData = BasicData(
                timeStartWork = 0L,
                timeEndWork = 10 * hour,
                timeStartBreak = 4 * hour,
                timeEndBreak = (4 + breakHours) * hour,
            ),
            trains = mutableListOf(
                Train(weight = "7000", conditionalLength = "80", distance = "150")
            ),
            passengers = passengers
        )
    }

    // Категории: вес >= 6000 (тяжёлый), длина >= 71 (длинный), дистанция >= 100 (плечо)
    private val listWeight = listOf(6000)
    private val listLength = listOf(71)
    private val listDistance = listOf(100)

    // ── Без пассажира: время доплаты = вся смена (10 ч) ──

    @Test
    fun heavyTrain_noPassenger_fullShift() {
        assertEquals(10 * hour, route(null).getTimeInHeavyTrain(listWeight, 0))
    }

    @Test
    fun longTrain_noPassenger_fullShift() {
        assertEquals(10 * hour, route(null).getTimeInLongTrain(listLength, 0))
    }

    @Test
    fun servicePhase_noPassenger_fullShift() {
        assertEquals(10 * hour, route(null).getTimeInServicePhase(listDistance, 0))
    }

    @Test
    fun servicePhase_excludesBreakFromAccrualTime() {
        assertEquals(8 * hour, route(null, breakHours = 2).getTimeInServicePhase(listDistance, 0))
    }

    // ── С пассажиром 2 ч: время доплаты = 10 ч − 2 ч = 8 ч ──

    @Test
    fun heavyTrain_withPassenger_excludesPassengerHours() {
        assertEquals(8 * hour, route(passengerHours = 2).getTimeInHeavyTrain(listWeight, 0))
    }

    @Test
    fun longTrain_withPassenger_excludesPassengerHours() {
        assertEquals(8 * hour, route(passengerHours = 2).getTimeInLongTrain(listLength, 0))
    }

    @Test
    fun servicePhase_withPassenger_excludesPassengerHours() {
        assertEquals(8 * hour, route(passengerHours = 2).getTimeInServicePhase(listDistance, 0))
    }

    // ── Пассажир дольше смены: результат не уходит в минус (clamp в 0) ──

    @Test
    fun heavyTrain_passengerLongerThanShift_clampedToZero() {
        assertEquals(0L, route(passengerHours = 12).getTimeInHeavyTrain(listWeight, 0))
    }

    @Test
    fun longTrain_passengerLongerThanShift_clampedToZero() {
        assertEquals(0L, route(passengerHours = 12).getTimeInLongTrain(listLength, 0))
    }

    @Test
    fun servicePhase_passengerLongerThanShift_clampedToZero() {
        assertEquals(0L, route(passengerHours = 12).getTimeInServicePhase(listDistance, 0))
    }
}
