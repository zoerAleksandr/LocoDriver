package com.z_company.route.viewmodel

import com.z_company.domain.entities.Day
import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.entities.TagForDay
import com.z_company.domain.entities.route.Route
import com.z_company.domain.entities.route.Station
import com.z_company.domain.entities.route.Train
import com.z_company.domain.entities.route.TrainAssist
import com.z_company.domain.entities.setting.SalarySetting
import com.z_company.domain.entities.setting.UserSettings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals

class DoubledTrainSurchargeTest {

    private val tariffRate = 100.0 // 100 руб/час для простоты расчётов

    private fun createHelper(routes: List<Route>): SalaryCalculationHelper {
        val days = (1..30).map { Day(dayOfMonth = it, tag = TagForDay.WORKING_DAY) }
        val monthOfYear = MonthOfYear(tariffRate = tariffRate, days = days)
        val userSettings = UserSettings(selectMonthOfYear = monthOfYear)
        val salarySetting = SalarySetting()
        return SalaryCalculationHelper(
            userSettings = userSettings,
            salarySetting = salarySetting,
            routeList = routes
        )
    }

    private fun trainWithDoubled(isFirst: Boolean, travelTimeMs: Long): Train {
        return Train(
            doubledTrain = TrainAssist(isFirst = isFirst),
            stations = mutableListOf(
                Station(timeDeparture = 0L),
                Station(timeArrival = travelTimeMs)
            )
        )
    }

    // --- Время первый (isFirst=true) ---

    @Test
    fun timeFirstSurchargeReturnsTimeForFirstTrain() = runTest {
        val route = Route(trains = mutableListOf(trainWithDoubled(isFirst = true, travelTimeMs = 3_600_000L)))
        val helper = createHelper(listOf(route))
        val result = helper.getTimeDoubledTrainFirstSurchargeFlow(listOf(route)).first()
        assertEquals(3_600_000L, result)
    }

    @Test
    fun timeFirstSurchargeIgnoresSecondTrain() = runTest {
        val route = Route(trains = mutableListOf(trainWithDoubled(isFirst = false, travelTimeMs = 3_600_000L)))
        val helper = createHelper(listOf(route))
        val result = helper.getTimeDoubledTrainFirstSurchargeFlow(listOf(route)).first()
        assertEquals(0L, result)
    }

    // --- Время второй (isFirst=false) ---

    @Test
    fun timeSecondSurchargeReturnsTimeForSecondTrain() = runTest {
        val route = Route(trains = mutableListOf(trainWithDoubled(isFirst = false, travelTimeMs = 7_200_000L)))
        val helper = createHelper(listOf(route))
        val result = helper.getTimeDoubledTrainSecondSurchargeFlow(listOf(route)).first()
        assertEquals(7_200_000L, result)
    }

    @Test
    fun timeSecondSurchargeIgnoresFirstTrain() = runTest {
        val route = Route(trains = mutableListOf(trainWithDoubled(isFirst = true, travelTimeMs = 7_200_000L)))
        val helper = createHelper(listOf(route))
        val result = helper.getTimeDoubledTrainSecondSurchargeFlow(listOf(route)).first()
        assertEquals(0L, result)
    }

    // --- Деньги первый (30%) ---

    @Test
    fun moneyFirstSurchargeCalculates30Percent() = runTest {
        // 1 час = 3_600_000 мс, тариф 100 руб/час, 30% = 30 руб
        val route = Route(trains = mutableListOf(trainWithDoubled(isFirst = true, travelTimeMs = 3_600_000L)))
        val helper = createHelper(listOf(route))
        val result = helper.getMoneyDoubledTrainFirstSurchargeFlow(listOf(route)).first()
        assertEquals(30.0, result, 0.01)
    }

    // --- Деньги второй (15%) ---

    @Test
    fun moneySecondSurchargeCalculates15Percent() = runTest {
        // 1 час = 3_600_000 мс, тариф 100 руб/час, 15% = 15 руб
        val route = Route(trains = mutableListOf(trainWithDoubled(isFirst = false, travelTimeMs = 3_600_000L)))
        val helper = createHelper(listOf(route))
        val result = helper.getMoneyDoubledTrainSecondSurchargeFlow(listOf(route)).first()
        assertEquals(15.0, result, 0.01)
    }

    // --- Без сдвоенного поезда ---

    @Test
    fun noDoubledTrainReturnsZeroTime() = runTest {
        val route = Route(trains = mutableListOf(Train()))
        val helper = createHelper(listOf(route))
        assertEquals(0L, helper.getTimeDoubledTrainFirstSurchargeFlow(listOf(route)).first())
        assertEquals(0L, helper.getTimeDoubledTrainSecondSurchargeFlow(listOf(route)).first())
    }

    @Test
    fun noDoubledTrainReturnsZeroMoney() = runTest {
        val route = Route(trains = mutableListOf(Train()))
        val helper = createHelper(listOf(route))
        assertEquals(0.0, helper.getMoneyDoubledTrainFirstSurchargeFlow(listOf(route)).first(), 0.01)
        assertEquals(0.0, helper.getMoneyDoubledTrainSecondSurchargeFlow(listOf(route)).first(), 0.01)
    }

    // --- isFirst = null (не выбрано) ---

    @Test
    fun isFirstNullReturnsZero() = runTest {
        val train = Train(
            doubledTrain = TrainAssist(isFirst = null),
            stations = mutableListOf(
                Station(timeDeparture = 0L),
                Station(timeArrival = 3_600_000L)
            )
        )
        val route = Route(trains = mutableListOf(train))
        val helper = createHelper(listOf(route))
        assertEquals(0L, helper.getTimeDoubledTrainFirstSurchargeFlow(listOf(route)).first())
        assertEquals(0L, helper.getTimeDoubledTrainSecondSurchargeFlow(listOf(route)).first())
        assertEquals(0.0, helper.getMoneyDoubledTrainFirstSurchargeFlow(listOf(route)).first(), 0.01)
        assertEquals(0.0, helper.getMoneyDoubledTrainSecondSurchargeFlow(listOf(route)).first(), 0.01)
    }

    // --- Несколько поездов ---

    @Test
    fun multipleTrainsSumsCorrectly() = runTest {
        val train1 = trainWithDoubled(isFirst = true, travelTimeMs = 3_600_000L)  // 1 час
        val train2 = trainWithDoubled(isFirst = true, travelTimeMs = 7_200_000L)  // 2 часа
        val route = Route(trains = mutableListOf(train1, train2))
        val helper = createHelper(listOf(route))
        val time = helper.getTimeDoubledTrainFirstSurchargeFlow(listOf(route)).first()
        assertEquals(10_800_000L, time) // 3 часа
        val money = helper.getMoneyDoubledTrainFirstSurchargeFlow(listOf(route)).first()
        assertEquals(90.0, money, 0.01) // 3 часа * 100 * 0.30 = 90
    }
}
