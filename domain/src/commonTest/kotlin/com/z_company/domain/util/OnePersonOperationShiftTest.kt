@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.z_company.domain.util

import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.entities.route.BasicData
import com.z_company.domain.entities.route.Locomotive
import com.z_company.domain.entities.route.Passenger
import com.z_company.domain.entities.route.Route
import com.z_company.domain.entities.route.Station
import com.z_company.domain.entities.route.Train
import com.z_company.domain.entities.route.UtilsForEntities.getOnePersonOperationTime
import com.z_company.domain.entities.route.UtilsForEntities.getOnePersonOperationTimePassengerTrain
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Доплата за работу в одно лицо (153L) начисляется «по маршруту машиниста» —
 * за всю смену от явки до сдачи, а не только за интервал следования поезда.
 * Исключаются перерыв, следование пассажиром и ожидание следования пассажиром.
 * Один пассажирский поезд в смене переводит всю смену на пассажирскую ставку.
 */
class OnePersonOperationShiftTest {
    private val hour = 3_600_000L
    private val moscow = TimeZone.of("GMT+3")
    private val context = TimeCalculationContext(localTZ = moscow, crossMonthTZ = moscow)
    private val january = MonthOfYear(year = 2025, month = 0)
    private val start = instant(month = 1, day = 10, hour = 8)

    private fun instant(month: Int, day: Int, hour: Int, minute: Int = 0): Long =
        LocalDateTime(2025, month, day, hour, minute).toInstant(moscow).toEpochMilliseconds()

    private fun train(number: String, fromHour: Int, toHour: Int) = Train(
        number = number,
        stations = mutableListOf(
            Station(timeDeparture = start + fromHour * hour),
            Station(timeArrival = start + toHour * hour),
        ),
    )

    private fun route(
        hours: Int = 8,
        isOnePerson: Boolean = true,
        trains: List<Train> = emptyList(),
        passengers: List<Passenger> = emptyList(),
        locomotives: List<Locomotive> = emptyList(),
        breakHours: Pair<Int, Int>? = null,
        startWork: Long = start,
    ) = Route(
        basicData = BasicData(
            isOnePersonOperation = isOnePerson,
            timeStartWork = startWork,
            timeEndWork = startWork + hours * hour,
            timeStartBreak = breakHours?.let { startWork + it.first * hour },
            timeEndBreak = breakHours?.let { startWork + it.second * hour },
        ),
        trains = trains.toMutableList(),
        passengers = passengers.toMutableList(),
        locomotives = locomotives.toMutableList(),
    )

    private fun Route.segments(
        tariffChanges: List<TariffChange> = emptyList(),
        month: MonthOfYear = january,
    ) = buildSalarySegments(
        monthOfYear = month,
        context = context,
        initialTariffRatePerHour = 100.0,
        tariffChanges = tariffChanges,
    )

    private fun List<SalarySegment>.duration(condition: AccrualCondition) =
        filter { condition in it.conditions }.sumOf { it.interval.durationMillis }

    @Test
    fun freightTrainWithStationTimesStillCoversWholeShift() {
        // Смена 8 ч, поезд идёт только 3 ч — доплата всё равно за всю смену.
        val segments = route(trains = listOf(train("2503", fromHour = 2, toHour = 5))).segments()

        assertEquals(8 * hour, segments.duration(AccrualCondition.ONE_PERSON_FREIGHT))
        assertEquals(0L, segments.duration(AccrualCondition.ONE_PERSON_PASSENGER))
    }

    @Test
    fun routeWithoutTrainUsesFreightCategoryForWholeShift() {
        val segments = route().segments()

        assertEquals(8 * hour, segments.duration(AccrualCondition.ONE_PERSON_FREIGHT))
        assertEquals(0L, segments.duration(AccrualCondition.ONE_PERSON_PASSENGER))
    }

    @Test
    fun trainWithoutNumberUsesFreightCategory() {
        val segments = route(trains = listOf(Train(number = null))).segments()

        assertEquals(8 * hour, segments.duration(AccrualCondition.ONE_PERSON_FREIGHT))
    }

    @Test
    fun passengerTrainCoversWholeShiftWithPassengerRate() {
        val segments = route(trains = listOf(train(" 101 ", fromHour = 1, toHour = 3))).segments()

        assertEquals(8 * hour, segments.duration(AccrualCondition.ONE_PERSON_PASSENGER))
        assertEquals(0L, segments.duration(AccrualCondition.ONE_PERSON_FREIGHT))
    }

    @Test
    fun suburbanTrainsUsePassengerRate() {
        // Пригородные с пассажирами (859р): 6001–6998, 7001–7098, 7101–7498, 7501–7598.
        listOf("6001", "6500", "6998", "7001", "7098", "7101", "7498", "7501", "7598").forEach { number ->
            val segments = route(trains = listOf(train(number, fromHour = 1, toHour = 3))).segments()
            assertEquals(8 * hour, segments.duration(AccrualCondition.ONE_PERSON_PASSENGER), number)
            assertEquals(0L, segments.duration(AccrualCondition.ONE_PERSON_FREIGHT), number)
        }
    }

    @Test
    fun numbersAroundSuburbanRangesStayFreight() {
        // 5998 — вагоны без пассажиров, 7099/7499/7599 — разрывы диапазонов,
        // 7601+ — служебные и МВПС без пассажиров, 6999 — вне диапазона.
        listOf("5998", "6999", "7099", "7499", "7599", "7601", "7998").forEach { number ->
            val segments = route(trains = listOf(train(number, fromHour = 1, toHour = 3))).segments()
            assertEquals(8 * hour, segments.duration(AccrualCondition.ONE_PERSON_FREIGHT), number)
            assertEquals(0L, segments.duration(AccrualCondition.ONE_PERSON_PASSENGER), number)
        }
    }

    @Test
    fun suburbanTrainInMixedShiftMovesWholeShiftToPassengerRate() {
        val route = route(
            trains = listOf(
                train("2503", fromHour = 1, toHour = 3),
                train("6123", fromHour = 4, toHour = 6),
            ),
        )

        assertTrue(route.usesOnePersonPassengerRate())
        assertEquals(8 * hour, route.segments().duration(AccrualCondition.ONE_PERSON_PASSENGER))
        assertEquals(8 * hour, listOf(route).getOnePersonOperationTimePassengerTrain(january, context))
        assertEquals(0L, listOf(route).getOnePersonOperationTime(january, context))
    }

    @Test
    fun onePassengerTrainMovesWholeMixedShiftToPassengerRate() {
        val segments = route(
            trains = listOf(
                train("2503", fromHour = 1, toHour = 3),
                train("101", fromHour = 4, toHour = 6),
            ),
        ).segments()

        assertEquals(8 * hour, segments.duration(AccrualCondition.ONE_PERSON_PASSENGER))
        assertEquals(0L, segments.duration(AccrualCondition.ONE_PERSON_FREIGHT))
        assertEquals(8 * hour, segments.duration(AccrualCondition.ONE_PERSON))
    }

    @Test
    fun passengerFollowingIsExcluded() {
        // 8–12 поезд с локомотивом до конца, 12–14 пассажиром, 14–16 снова работа.
        val segments = route(
            trains = listOf(train("2503", fromHour = 0, toHour = 4)),
            locomotives = listOf(
                Locomotive(basicId = "", timeStartOfAcceptance = start, timeEndOfDelivery = start + 4 * hour),
            ),
            passengers = listOf(
                Passenger(timeDeparture = start + 4 * hour, timeArrival = start + 6 * hour),
            ),
        ).segments()

        assertEquals(2 * hour, segments.duration(AccrualCondition.PASSENGER))
        assertEquals(6 * hour, segments.duration(AccrualCondition.ONE_PERSON_FREIGHT))
        assertEquals(0L, segments.filter {
            AccrualCondition.PASSENGER in it.conditions && AccrualCondition.ONE_PERSON in it.conditions
        }.sumOf { it.interval.durationMillis })
    }

    @Test
    fun passengerWaitingIsExcludedButLocomotiveDeliveryIsKept() {
        // 8–9 приёмка, 9–12 поезд, 12–13 сдача, 13–14 ожидание, 14–16 пассажиром.
        val segments = route(
            trains = listOf(train("2503", fromHour = 1, toHour = 4)),
            locomotives = listOf(
                Locomotive(
                    basicId = "",
                    timeStartOfAcceptance = start,
                    timeEndOfAcceptance = start + hour,
                    timeStartOfDelivery = start + 4 * hour,
                    timeEndOfDelivery = start + 5 * hour,
                ),
            ),
            passengers = listOf(
                Passenger(timeDeparture = start + 6 * hour, timeArrival = start + 8 * hour),
            ),
        ).segments()

        assertEquals(hour, segments.duration(AccrualCondition.PASSENGER_WAITING))
        assertEquals(5 * hour, segments.duration(AccrualCondition.ONE_PERSON_FREIGHT))
        assertEquals(0L, segments.filter {
            AccrualCondition.PASSENGER_WAITING in it.conditions &&
                    AccrualCondition.ONE_PERSON in it.conditions
        }.sumOf { it.interval.durationMillis })
    }

    @Test
    fun passengerRideBeforeWorkStartDoesNotReduceShift() {
        // «Явка по прибытию пассажиром»: поездка до начала смены не пересекает её.
        val segments = route(
            passengers = listOf(
                Passenger(
                    timeDeparture = start - 3 * hour,
                    timeArrival = start,
                    isWorkStartByArrival = true,
                ),
            ),
        ).segments()

        assertEquals(8 * hour, segments.duration(AccrualCondition.ONE_PERSON_FREIGHT))
    }

    @Test
    fun breakIsExcluded() {
        val segments = route(
            trains = listOf(train("2503", fromHour = 1, toHour = 3)),
            breakHours = 4 to 5,
        ).segments()

        assertEquals(7 * hour, segments.duration(AccrualCondition.ONE_PERSON_FREIGHT))
    }

    @Test
    fun disabledOnePersonProducesNothing() {
        val segments = route(isOnePerson = false, trains = listOf(train("101", 1, 3))).segments()

        assertEquals(0L, segments.duration(AccrualCondition.ONE_PERSON))
    }

    @Test
    fun shiftIsClippedToSelectedMonth() {
        // 31.01 20:00 → 01.02 04:00: в январе 4 ч, в феврале 4 ч.
        val crossMonth = route(
            startWork = instant(month = 1, day = 31, hour = 20),
            trains = listOf(Train(number = "2503")),
        )

        assertEquals(4 * hour, crossMonth.segments().duration(AccrualCondition.ONE_PERSON_FREIGHT))
        assertEquals(
            4 * hour,
            crossMonth.segments(month = MonthOfYear(year = 2025, month = 1))
                .duration(AccrualCondition.ONE_PERSON_FREIGHT),
        )
    }

    @Test
    fun tariffChangeSplitsOnePersonMoney() {
        val segments = route(trains = listOf(train("2503", fromHour = 2, toHour = 3)))
            .segments(tariffChanges = listOf(TariffChange(start + 6 * hour, 200.0)))

        val money = segments.filter { AccrualCondition.ONE_PERSON_FREIGHT in it.conditions }
            .sumOf(SalarySegment::tariffMoney)
        assertEquals(6 * 100.0 + 2 * 200.0, money, 0.001)
    }

    @Test
    fun summaryTimeMatchesSegments() {
        val routes = listOf(
            route(
                trains = listOf(train("2503", fromHour = 2, toHour = 5)),
                breakHours = 6 to 7,
            ),
            route(
                startWork = instant(month = 1, day = 12, hour = 8),
                hours = 6,
                trains = listOf(Train(number = "2503"), Train(number = "101")),
            ),
            route(startWork = instant(month = 1, day = 14, hour = 8), isOnePerson = false),
        )

        assertEquals(7 * hour, routes.getOnePersonOperationTime(january, context))
        assertEquals(6 * hour, routes.getOnePersonOperationTimePassengerTrain(january, context))
        assertEquals(
            routes.sumOf { it.segments().duration(AccrualCondition.ONE_PERSON_FREIGHT) },
            routes.getOnePersonOperationTime(january, context),
        )
        assertEquals(
            routes.sumOf { it.segments().duration(AccrualCondition.ONE_PERSON_PASSENGER) },
            routes.getOnePersonOperationTimePassengerTrain(january, context),
        )
    }
}
