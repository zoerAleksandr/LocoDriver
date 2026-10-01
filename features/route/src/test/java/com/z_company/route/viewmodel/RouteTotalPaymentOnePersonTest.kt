package com.z_company.route.viewmodel

import com.z_company.domain.entities.Day
import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.entities.TagForDay
import com.z_company.domain.entities.route.BasicData
import com.z_company.domain.entities.route.Route
import com.z_company.domain.entities.route.Station
import com.z_company.domain.entities.route.Train
import com.z_company.domain.entities.setting.SalarySetting
import com.z_company.domain.entities.setting.UserSettings
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * «Расчёт за смену» в развёрнутой карточке AllRoutes ([computeRouteTotalPayment]):
 * доплата за одно лицо — за всю смену, по пассажирской ставке для пассажирских
 * и пригородных поездов, по грузовой — для остальных.
 */
class RouteTotalPaymentOnePersonTest {
    private val hour = 3_600_000L
    private val userSettings = UserSettings(
        selectMonthOfYear = MonthOfYear(
            year = 1970,
            month = 0,
            tariffRate = 100.0,
            days = (1..31).map { Day(dayOfMonth = it, tag = TagForDay.WORKING_DAY) },
        ),
    )
    private val salarySetting = SalarySetting(
        nightTimePercent = 0.0,
        harmfulnessPercent = 0.0,
        zonalSurcharge = 0.0,
        onePersonOperationPercent = 40.0,
        onePersonOperationPassengerTrainPercent = 50.0,
        surchargeHeavyTrainsList = emptyList(),
        surchargeLongTrainsList = emptyList(),
    )

    // Смена 7 ч (UTC 00:00–07:00 → МСК 03:00–10:00, ночи нет), поезд в пути 3 ч.
    private fun route(trainNumber: String, isOnePerson: Boolean = true) = Route(
        basicData = BasicData(
            isOnePersonOperation = isOnePerson,
            timeStartWork = 0L,
            timeEndWork = 7 * hour,
        ),
        trains = mutableListOf(
            Train(
                number = trainNumber,
                stations = mutableListOf(
                    Station(timeDeparture = 2 * hour),
                    Station(timeArrival = 5 * hour),
                ),
            ),
        ),
    )

    private suspend fun total(route: Route): Double = assertNotNull(
        computeRouteTotalPayment(route, userSettings, salarySetting, candidateRoutes = listOf(route))
    )

    @Test
    fun onePersonSurchargeCoversWholeShiftByTrainCategory() = runTest {
        val base = total(route("2503", isOnePerson = false))

        // Грузовой: 7 ч × 100 × 40% = 280.
        assertEquals(280.0, total(route("2503")) - base, 0.01)
        // Пригородный 6123 и пассажирский 101: 7 ч × 100 × 50% = 350.
        assertEquals(350.0, total(route("6123")) - total(route("6123", isOnePerson = false)), 0.01)
        assertEquals(350.0, total(route("101")) - total(route("101", isOnePerson = false)), 0.01)
    }
}
