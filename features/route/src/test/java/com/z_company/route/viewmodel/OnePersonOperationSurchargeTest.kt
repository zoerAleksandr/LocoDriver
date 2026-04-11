package com.z_company.route.viewmodel

import com.z_company.domain.entities.Day
import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.entities.TagForDay
import com.z_company.domain.entities.route.BasicData
import com.z_company.domain.entities.route.Passenger
import com.z_company.domain.entities.route.Route
import com.z_company.domain.entities.route.Train
import com.z_company.domain.entities.setting.SalarySetting
import com.z_company.domain.entities.setting.UserSettings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals

/**
 * Тесты расчёта времени и доплаты за работу в одно лицо с учётом вычета
 * времени следования пассажиром.
 *
 * Логика: если маршрут помечен как работа в одно лицо и в нём указано
 * время следования пассажиром (Route.passengers с timeArrival/timeDeparture),
 * это время вычитается из времени работы в одно лицо, а доплата начисляется
 * на чистое время управления локомотивом.
 *
 * Константы:
 *   ONE_HOUR_MS = 3_600_000 мс = 1 час
 *   tariffRate = 100 руб/час
 *   SalarySetting.onePersonOperationPercent = 40% (по умолчанию)
 *   SalarySetting.onePersonOperationPassengerTrainPercent = 50% (по умолчанию)
 */
class OnePersonOperationSurchargeTest {

    private val tariffRate = 100.0
    private val oneHourMs = 3_600_000L

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

    /**
     * Создаёт маршрут «работа в одно лицо» длиной [workDurationMs]
     * с опциональным временем следования пассажиром [passengerDurationMs]
     * и грузовым номером поезда (вне пассажирского диапазона).
     */
    private fun oneOpRoute(
        workDurationMs: Long,
        passengerDurationMs: Long = 0L,
        trainNumber: String? = "2503" // грузовой, вне passengerTrainNumberList
    ): Route {
        val passengers = if (passengerDurationMs > 0L) {
            mutableListOf(
                Passenger(timeDeparture = 0L, timeArrival = passengerDurationMs)
            )
        } else {
            mutableListOf()
        }
        val trains = if (trainNumber != null) {
            mutableListOf(Train(number = trainNumber))
        } else {
            mutableListOf()
        }
        return Route(
            basicData = BasicData(
                isOnePersonOperation = true,
                timeStartWork = 0L,
                timeEndWork = workDurationMs
            ),
            trains = trains,
            passengers = passengers
        )
    }

    // --- Время без пассажиров (baseline) ---

    @Test
    fun onePersonTime_noPassengers_returnsFullWorkTime() = runTest {
        // 10 часов работы, 0 часов пассажиром → 10 часов
        val route = oneOpRoute(workDurationMs = 10 * oneHourMs)
        val helper = createHelper(listOf(route))
        val result = helper.getTimeOnePersonOperationFlow(listOf(route)).first()
        assertEquals(10 * oneHourMs, result)
    }

    // --- Время с вычетом пассажира ---

    @Test
    fun onePersonTime_withPassengerTime_subtractsIt() = runTest {
        // 10 часов работы, 3 часа пассажиром → 7 часов
        val route = oneOpRoute(
            workDurationMs = 10 * oneHourMs,
            passengerDurationMs = 3 * oneHourMs
        )
        val helper = createHelper(listOf(route))
        val result = helper.getTimeOnePersonOperationFlow(listOf(route)).first()
        assertEquals(7 * oneHourMs, result)
    }

    @Test
    fun onePersonTime_passengerEqualsWork_returnsZero() = runTest {
        // 5 часов работы, 5 часов пассажиром → 0
        val route = oneOpRoute(
            workDurationMs = 5 * oneHourMs,
            passengerDurationMs = 5 * oneHourMs
        )
        val helper = createHelper(listOf(route))
        val result = helper.getTimeOnePersonOperationFlow(listOf(route)).first()
        assertEquals(0L, result)
    }

    @Test
    fun onePersonTime_passengerLongerThanWork_clampsToZero() = runTest {
        // 3 часа работы, 5 часов пассажиром → не должно быть отрицательным, должно быть 0
        val route = oneOpRoute(
            workDurationMs = 3 * oneHourMs,
            passengerDurationMs = 5 * oneHourMs
        )
        val helper = createHelper(listOf(route))
        val result = helper.getTimeOnePersonOperationFlow(listOf(route)).first()
        assertEquals(0L, result)
    }

    // --- isOnePersonOperation = false ---

    @Test
    fun notOnePerson_returnsZeroEvenWithPassenger() = runTest {
        // Маршрут НЕ «в одно лицо» — не должен считаться вообще
        val route = Route(
            basicData = BasicData(
                isOnePersonOperation = false,
                timeStartWork = 0L,
                timeEndWork = 10 * oneHourMs
            ),
            trains = mutableListOf(Train(number = "2503")),
            passengers = mutableListOf(
                Passenger(timeDeparture = 0L, timeArrival = 3 * oneHourMs)
            )
        )
        val helper = createHelper(listOf(route))
        val result = helper.getTimeOnePersonOperationFlow(listOf(route)).first()
        assertEquals(0L, result)
    }

    // --- Несколько маршрутов ---

    @Test
    fun multipleRoutes_sumWithPassengerSubtraction() = runTest {
        // Маршрут 1: 8 часов работы, 2 часа пассажиром → 6 часов
        // Маршрут 2: 5 часов работы, 0 часов пассажиром → 5 часов
        // Маршрут 3: 4 часа работы, 4 часа пассажиром → 0 часов
        // Итого: 11 часов
        val route1 = oneOpRoute(workDurationMs = 8 * oneHourMs, passengerDurationMs = 2 * oneHourMs)
        val route2 = oneOpRoute(workDurationMs = 5 * oneHourMs)
        val route3 = oneOpRoute(workDurationMs = 4 * oneHourMs, passengerDurationMs = 4 * oneHourMs)
        val helper = createHelper(listOf(route1, route2, route3))
        val result = helper.getTimeOnePersonOperationFlow(listOf(route1, route2, route3)).first()
        assertEquals(11 * oneHourMs, result)
    }

    // --- Доплата (40%) с учётом пассажира ---

    @Test
    fun onePersonMoney_noPassenger_fullWorkTime() = runTest {
        // 10 часов * 100 руб/час * 40% = 400 руб
        val route = oneOpRoute(workDurationMs = 10 * oneHourMs)
        val helper = createHelper(listOf(route))
        val result = helper.getMoneyOnePersonOperationFlow().first()
        assertEquals(400.0, result, 0.01)
    }

    @Test
    fun onePersonMoney_withPassenger_reducedByPassengerTime() = runTest {
        // (10 - 3) часов * 100 руб/час * 40% = 7 * 100 * 0.4 = 280 руб
        val route = oneOpRoute(
            workDurationMs = 10 * oneHourMs,
            passengerDurationMs = 3 * oneHourMs
        )
        val helper = createHelper(listOf(route))
        val result = helper.getMoneyOnePersonOperationFlow().first()
        assertEquals(280.0, result, 0.01)
    }

    @Test
    fun onePersonMoney_passengerCoversAllWork_returnsZero() = runTest {
        // 5 часов работы, 5 часов пассажиром → 0 руб доплаты
        val route = oneOpRoute(
            workDurationMs = 5 * oneHourMs,
            passengerDurationMs = 5 * oneHourMs
        )
        val helper = createHelper(listOf(route))
        val result = helper.getMoneyOnePersonOperationFlow().first()
        assertEquals(0.0, result, 0.01)
    }

    @Test
    fun onePersonMoney_mixedRoutes_sumCorrectly() = runTest {
        // Route 1: (8 - 2) * 100 * 0.4 = 240 руб
        // Route 2: 5 * 100 * 0.4 = 200 руб
        // Итого: 440 руб
        val route1 = oneOpRoute(workDurationMs = 8 * oneHourMs, passengerDurationMs = 2 * oneHourMs)
        val route2 = oneOpRoute(workDurationMs = 5 * oneHourMs)
        val helper = createHelper(listOf(route1, route2))
        val result = helper.getMoneyOnePersonOperationFlow().first()
        assertEquals(440.0, result, 0.01)
    }

    // --- Пассажирский поезд (одно лицо в пассажирском движении, 50%) ---

    @Test
    fun onePersonPassengerTrainTime_subtractsPassengerFollowing() = runTest {
        // Пассажирский поезд №100 (в passengerTrainNumberList 1..150)
        // 10 часов работы, 3 часа следования пассажиром → 7 часов
        val route = oneOpRoute(
            workDurationMs = 10 * oneHourMs,
            passengerDurationMs = 3 * oneHourMs,
            trainNumber = "100"
        )
        val helper = createHelper(listOf(route))
        val result = helper.getTimeOnePersonOperationPassengerTrainFlow(listOf(route)).first()
        assertEquals(7 * oneHourMs, result)
    }

    @Test
    fun onePersonPassengerTrainMoney_50percentOfReducedTime() = runTest {
        // (10 - 3) * 100 * 0.5 = 350 руб
        val route = oneOpRoute(
            workDurationMs = 10 * oneHourMs,
            passengerDurationMs = 3 * oneHourMs,
            trainNumber = "100"
        )
        val helper = createHelper(listOf(route))
        val result = helper.getMoneyOnePersonOperationPassengerTrainFlow().first()
        assertEquals(350.0, result, 0.01)
    }

    // --- Грузовой с номером поезда вне пассажирского диапазона (должно считаться как "не пассажирский") ---

    @Test
    fun onePersonTime_freightTrainGoesIntoMainFunction() = runTest {
        // Грузовой 2503 → попадает в getTimeOnePersonOperationFlow (не passenger train)
        val route = oneOpRoute(
            workDurationMs = 5 * oneHourMs,
            passengerDurationMs = 1 * oneHourMs,
            trainNumber = "2503"
        )
        val helper = createHelper(listOf(route))
        val mainResult = helper.getTimeOnePersonOperationFlow(listOf(route)).first()
        val passengerResult = helper.getTimeOnePersonOperationPassengerTrainFlow(listOf(route)).first()
        assertEquals(4 * oneHourMs, mainResult)
        assertEquals(0L, passengerResult)
    }

    // --- Пустой список ---

    @Test
    fun emptyRouteList_returnsZero() = runTest {
        val helper = createHelper(emptyList())
        val time = helper.getTimeOnePersonOperationFlow().first()
        val money = helper.getMoneyOnePersonOperationFlow().first()
        assertEquals(0L, time)
        assertEquals(0.0, money, 0.01)
    }

    // --- Два пассажира в одном маршруте ---

    @Test
    fun multiplePassengersInOneRoute_sumSubtracted() = runTest {
        // 10 часов работы, 2 пассажирских сегмента по 1 часу → (10 - 2) = 8 часов
        val route = Route(
            basicData = BasicData(
                isOnePersonOperation = true,
                timeStartWork = 0L,
                timeEndWork = 10 * oneHourMs
            ),
            trains = mutableListOf(Train(number = "2503")),
            passengers = mutableListOf(
                Passenger(timeDeparture = 0L, timeArrival = oneHourMs),
                Passenger(timeDeparture = 0L, timeArrival = oneHourMs)
            )
        )
        val helper = createHelper(listOf(route))
        val result = helper.getTimeOnePersonOperationFlow(listOf(route)).first()
        assertEquals(8 * oneHourMs, result)
    }
}
