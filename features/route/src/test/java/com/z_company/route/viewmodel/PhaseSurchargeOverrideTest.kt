@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.z_company.route.viewmodel

import com.z_company.domain.entities.DateSetTariffRate
import com.z_company.domain.entities.Day
import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.entities.TagForDay
import com.z_company.domain.entities.route.BasicData
import com.z_company.domain.entities.route.Passenger
import com.z_company.domain.entities.route.Route
import com.z_company.domain.entities.route.Station
import com.z_company.domain.entities.route.Train
import com.z_company.domain.entities.route.UtilsForEntities
import com.z_company.domain.entities.setting.SalarySetting
import com.z_company.domain.entities.setting.ServicePhase
import com.z_company.domain.entities.setting.SurchargeHeavyTrains
import com.z_company.domain.entities.setting.SurchargeLongTrains
import com.z_company.domain.entities.setting.UserSettings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Доплаты за тяжёлый/длинный состав, заданные на плече, перекрывают общие из настроек ЗП. */
class PhaseSurchargeOverrideTest {
    private val hour = 3_600_000L
    private val moscow = TimeZone.of("GMT+3")

    private fun instant(day: Int, hour: Int, minute: Int = 0): Long =
        LocalDateTime(2025, 1, day, hour, minute).toInstant(moscow).toEpochMilliseconds()

    private val forward = ServicePhase(
        id = "forward",
        departureStation = "A",
        arrivalStation = "B",
        distance = 100,
        surchargeHeavyTrainsList = listOf(SurchargeHeavyTrains(weight = "3500", percentSurcharge = "20")),
        surchargeLongTrainsList = listOf(SurchargeLongTrains(conditionalLength = "50", percentSurcharge = "30")),
    )
    private val backward = ServicePhase(id = "backward", departureStation = "B", arrivalStation = "A", distance = 100)

    private val globalHeavy = listOf(SurchargeHeavyTrains(weight = "6000", percentSurcharge = "10"))
    private val globalLong = listOf(SurchargeLongTrains(conditionalLength = "80", percentSurcharge = "20"))

    private fun trainOn(phase: ServicePhase?, weight: String = "4000", length: String = "60") = Train(
        weight = weight,
        conditionalLength = length,
        servicePhase = phase?.forTrain(),
        stations = mutableListOf(
            Station(timeDeparture = instant(day = 10, hour = 22, minute = 30)),
            Station(timeArrival = instant(day = 11, hour = 2, minute = 30)),
        ),
    )

    private fun route(train: Train) = Route(
        basicData = BasicData(
            timeStartWork = instant(day = 10, hour = 22),
            timeEndWork = instant(day = 11, hour = 3),
            timeStartBreak = instant(day = 10, hour = 23),
            timeEndBreak = instant(day = 11, hour = 0),
        ),
        passengers = mutableListOf(
            Passenger(timeDeparture = instant(day = 11, hour = 0), timeArrival = instant(day = 11, hour = 1)),
        ),
        trains = mutableListOf(train),
    )

    private val salarySetting = SalarySetting(
        surchargeHeavyTrainsList = globalHeavy,
        surchargeLongTrainsList = globalLong,
    )

    private fun helper(train: Train, setting: SalarySetting = salarySetting): SalaryCalculationHelper {
        val month = MonthOfYear(
            year = 2025,
            month = 0,
            tariffRate = 200.0,
            dateSetTariffRate = DateSetTariffRate(dateNewRate = 11, oldRate = 100.0),
            days = (1..31).map { Day(it, TagForDay.WORKING_DAY) },
        )
        return SalaryCalculationHelper(
            userSettings = UserSettings(
                selectMonthOfYear = month,
                timeZone = 0L,
                servicePhases = listOf(forward, backward),
            ),
            salarySetting = setting,
            allRoutes = listOf(route(train)),
        )
    }

    @Test
    fun heavyTrainOnPhaseWithOwnTiersGetsPhasePercent() = runTest {
        val helper = helper(trainOn(forward))

        // 4000 т не тяжёлый по общим настройкам (порог 6000), но тяжёлый на этом плече (порог 3500).
        assertEquals(listOf("10", "20"), helper.getPercentListSurchargeExtendedHeavyTrainsFlow().first())
        assertEquals(listOf(0L, 3 * hour), helper.getTimeListSurchargeHeavyTrainsFlow().first())
        // База смены 500 (1 ч по 100 + 2 ч по 200), 20% = 100.
        assertEquals(listOf(0.0, 100.0), helper.getMoneyListSurchargeExtendedHeavyTrainsFlow().first())
    }

    @Test
    fun sameWeightInOppositeDirectionWithoutOwnTiersIsNotHeavy() = runTest {
        val helper = helper(trainOn(backward))

        assertEquals(listOf("10"), helper.getPercentListSurchargeExtendedHeavyTrainsFlow().first())
        assertEquals(listOf(0.0), helper.getMoneyListSurchargeExtendedHeavyTrainsFlow().first())
    }

    @Test
    fun routeWithoutPhaseUsesGlobalTiers() = runTest {
        val helper = helper(trainOn(phase = null, weight = "6001", length = "81"))

        assertEquals(listOf(50.0), helper.getMoneyListSurchargeExtendedHeavyTrainsFlow().first())
        assertEquals(listOf(100.0), helper.getMoneyListSurchargeLongTrainsFlow().first())
    }

    @Test
    fun longTrainUsesPhaseTiersIndependentlyOfHeavy() = runTest {
        // На плече заданы и тяжёлые, и длинные, у поезда 60 ваг. > 50 → 30% от базы 500.
        val helper = helper(trainOn(forward, weight = "1000"))

        assertEquals(listOf("20", "30"), helper.getPercentListSurchargeLongTrainsFlow().first().let {
            // «20» - общая ступень, «30» - ступень плеча.
            it
        })
        assertEquals(listOf(0.0, 150.0), helper.getMoneyListSurchargeLongTrainsFlow().first())
    }

    @Test
    fun phaseWithOnlyHeavyTiersFallsBackToGlobalLong() = runTest {
        val onlyHeavy = forward.copy(surchargeLongTrainsList = emptyList())
        val month = MonthOfYear(
            year = 2025, month = 0, tariffRate = 200.0,
            dateSetTariffRate = DateSetTariffRate(dateNewRate = 11, oldRate = 100.0),
            days = (1..31).map { Day(it, TagForDay.WORKING_DAY) },
        )
        val helper = SalaryCalculationHelper(
            userSettings = UserSettings(selectMonthOfYear = month, timeZone = 0L, servicePhases = listOf(onlyHeavy)),
            salarySetting = salarySetting,
            allRoutes = listOf(route(trainOn(onlyHeavy, length = "81"))),
        )

        assertEquals(listOf(100.0), helper.getMoneyListSurchargeLongTrainsFlow().first())
    }

    @Test
    fun phasePercentEqualToGlobalMergesIntoGlobalRow() = runTest {
        val sameAsGlobal = forward.copy(
            surchargeHeavyTrainsList = listOf(SurchargeHeavyTrains(weight = "3500", percentSurcharge = "10")),
        )
        val month = MonthOfYear(
            year = 2025, month = 0, tariffRate = 200.0,
            dateSetTariffRate = DateSetTariffRate(dateNewRate = 11, oldRate = 100.0),
            days = (1..31).map { Day(it, TagForDay.WORKING_DAY) },
        )
        val helper = SalaryCalculationHelper(
            userSettings = UserSettings(selectMonthOfYear = month, timeZone = 0L, servicePhases = listOf(sameAsGlobal)),
            salarySetting = salarySetting,
            allRoutes = listOf(route(trainOn(sameAsGlobal))),
        )

        assertEquals(listOf("10"), helper.getPercentListSurchargeExtendedHeavyTrainsFlow().first())
        assertEquals(listOf(50.0), helper.getMoneyListSurchargeExtendedHeavyTrainsFlow().first())
    }

    @Test
    fun editingPhaseTiersAppliesToAlreadySavedTrains() = runTest {
        // Рейс хранит копию плеча без доплат; расчёт берёт актуальные ступени из настроек по id.
        val train = trainOn(forward)
        assertTrue(train.servicePhase!!.surchargeHeavyTrainsList.isEmpty())
        assertEquals(listOf(0.0, 100.0), helper(train).getMoneyListSurchargeExtendedHeavyTrainsFlow().first())
    }

    @Test
    fun routeBadgesRespectPhaseTiers() {
        val phases = listOf(forward, backward)
        assertTrue(UtilsForEntities.isHeavyTrains(salarySetting, route(trainOn(forward)), phases))
        assertFalse(UtilsForEntities.isHeavyTrains(salarySetting, route(trainOn(backward)), phases))
        assertTrue(UtilsForEntities.isLongCompositionTrain(salarySetting, route(trainOn(forward)), phases))
        assertFalse(UtilsForEntities.isLongCompositionTrain(salarySetting, route(trainOn(backward)), phases))
    }

    // ---- Несколько поездов, границы, невалидные строки, сверхурочные и итог месяца ----

    private fun month() = MonthOfYear(
        year = 2025,
        month = 0,
        tariffRate = 200.0,
        dateSetTariffRate = DateSetTariffRate(dateNewRate = 15, oldRate = 100.0),
        days = emptyList(),
    )

    /** Смена 8-12 (4 ч) в день [day]; поезда как заданы. После 15 января ставка 200, база смены 800. */
    private fun shift(day: Int, vararg trains: Train) = Route(
        basicData = BasicData(timeStartWork = instant(day, 8), timeEndWork = instant(day, 12)),
        trains = trains.toMutableList(),
    )

    private fun plainTrain(phase: ServicePhase?, weight: String = "4000", length: String = "0") = Train(
        weight = weight,
        conditionalLength = length,
        servicePhase = phase?.forTrain(),
    )

    private fun helperFor(
        routes: List<Route>,
        phases: List<ServicePhase> = listOf(forward, backward),
        setting: SalarySetting = salarySetting.copy(
            zonalSurcharge = 0.0,
            harmfulnessPercent = 0.0,
            nightTimePercent = 0.0,
        ),
    ) = SalaryCalculationHelper(
        userSettings = UserSettings(selectMonthOfYear = month(), timeZone = 0L, servicePhases = phases),
        salarySetting = setting,
        allRoutes = routes,
    )

    @Test
    fun mixedTrainsOnPhaseAndGlobalPickHigherPercent() = runTest {
        // Поезд 1 на плече: 4000 т > 3500 т → 20 %. Поезд 2 без плеча: 6500 т > 6000 т → 10 %. Вся смена - по 20 %.
        val helper = helperFor(listOf(shift(20, plainTrain(forward), plainTrain(null, weight = "6500"))))

        assertEquals(listOf("10", "20"), helper.getPercentListSurchargeExtendedHeavyTrainsFlow().first())
        assertEquals(listOf(0.0, 160.0), helper.getMoneyListSurchargeExtendedHeavyTrainsFlow().first())
    }

    @Test
    fun mixedTrainsGlobalWinsWhenItsPercentIsHigher() = runTest {
        val lowPhase = forward.copy(
            surchargeHeavyTrainsList = listOf(SurchargeHeavyTrains(weight = "3500", percentSurcharge = "5")),
        )
        val helper = helperFor(
            routes = listOf(shift(20, plainTrain(lowPhase), plainTrain(null, weight = "6500"))),
            phases = listOf(lowPhase),
        )

        // Ступень плеча не применилась - отдельной строки с её процентом нет.
        assertEquals(listOf("10"), helper.getPercentListSurchargeExtendedHeavyTrainsFlow().first())
        assertEquals(listOf(80.0), helper.getMoneyListSurchargeExtendedHeavyTrainsFlow().first())
    }

    @Test
    fun phaseTierBoundaryIsStrict() = runTest {
        assertEquals(
            listOf(0.0),
            helperFor(listOf(shift(20, plainTrain(forward, weight = "3500"))))
                .getMoneyListSurchargeExtendedHeavyTrainsFlow().first(),
        )
        assertEquals(
            listOf(0.0, 160.0),
            helperFor(listOf(shift(20, plainTrain(forward, weight = "3501"))))
                .getMoneyListSurchargeExtendedHeavyTrainsFlow().first(),
        )
        // То же для длины: ступень плеча 50 ваг.
        assertEquals(
            listOf(0.0),
            helperFor(listOf(shift(20, plainTrain(forward, weight = "0", length = "50"))))
                .getMoneyListSurchargeLongTrainsFlow().first(),
        )
        assertEquals(
            listOf(0.0, 240.0),
            helperFor(listOf(shift(20, plainTrain(forward, weight = "0", length = "51"))))
                .getMoneyListSurchargeLongTrainsFlow().first(),
        )
    }

    @Test
    fun invalidPhaseRowsAreIgnoredAndDuplicateThresholdTakesLast() = runTest {
        val messy = forward.copy(
            surchargeHeavyTrainsList = listOf(
                SurchargeHeavyTrains(id = "1", weight = "", percentSurcharge = "50"),
                SurchargeHeavyTrains(id = "2", weight = "0", percentSurcharge = "50"),
                SurchargeHeavyTrains(id = "3", weight = "3000", percentSurcharge = "abc"),
                SurchargeHeavyTrains(id = "4", weight = "3500", percentSurcharge = "20"),
                SurchargeHeavyTrains(id = "5", weight = "3500", percentSurcharge = "30"),
            ),
        )
        val helper = helperFor(listOf(shift(20, plainTrain(messy))), phases = listOf(messy))

        assertEquals(listOf("10", "30"), helper.getPercentListSurchargeExtendedHeavyTrainsFlow().first())
        assertEquals(listOf(0.0, 240.0), helper.getMoneyListSurchargeExtendedHeavyTrainsFlow().first())
    }

    @Test
    fun phaseWithOnlyInvalidRowsFallsBackToGlobal() = runTest {
        val broken = forward.copy(
            surchargeHeavyTrainsList = listOf(SurchargeHeavyTrains(weight = "", percentSurcharge = "")),
        )
        val helper = helperFor(
            routes = listOf(shift(20, plainTrain(broken, weight = "6001"))),
            phases = listOf(broken),
        )

        assertEquals(listOf("10"), helper.getPercentListSurchargeExtendedHeavyTrainsFlow().first())
        assertEquals(listOf(80.0), helper.getMoneyListSurchargeExtendedHeavyTrainsFlow().first())
    }

    @Test
    fun monthWithDifferentPhasesAccruesEachRouteByItsOwnTiers() = runTest {
        val helper = helperFor(
            listOf(
                shift(20, plainTrain(forward)),                      // свои 20 % → 160
                shift(21, plainTrain(backward)),                     // тот же вес без своих ступеней → 0
                shift(22, plainTrain(null, weight = "6001")),        // общие 10 % → 80
            ),
        )

        assertEquals(listOf("10", "20"), helper.getPercentListSurchargeExtendedHeavyTrainsFlow().first())
        assertEquals(listOf(80.0, 160.0), helper.getMoneyListSurchargeExtendedHeavyTrainsFlow().first())
        assertEquals(listOf(4 * hour, 4 * hour), helper.getTimeListSurchargeHeavyTrainsFlow().first())
    }

    @Test
    fun overtimeAndTotalMatchGlobalTierWithSamePercent() = runTest {
        // Доплата плеча (3500 т → 20 %) должна вести себя в сверхурочных и итоге так же, как такая же общая ступень.
        fun routes() = listOf(shift(5, plainTrain(forward)), shift(20, plainTrain(forward)))
        val phased = helperFor(routes())
        val global = helperFor(
            routes = listOf(shift(5, plainTrain(null)), shift(20, plainTrain(null))),
            phases = emptyList(),
            setting = salarySetting.copy(
                zonalSurcharge = 0.0,
                harmfulnessPercent = 0.0,
                nightTimePercent = 0.0,
                surchargeHeavyTrainsList = listOf(SurchargeHeavyTrains(weight = "3500", percentSurcharge = "20")),
            ),
        )
        val withoutOverride = helperFor(routes(), phases = listOf(backward))

        assertEquals(
            global.getMoneyListSurchargeExtendedHeavyTrainsFlow().first().sum(),
            phased.getMoneyListSurchargeExtendedHeavyTrainsFlow().first().sum(),
            0.001,
        )
        assertEquals(
            global.getMoneySurchargeOvertimeFlow().first(),
            phased.getMoneySurchargeOvertimeFlow().first(),
            0.001,
        )
        assertEquals(
            global.getMoneyTotalChargedFlow().first(),
            phased.getMoneyTotalChargedFlow().first(),
            0.001,
        )
        // Тест осмысленный: без ступеней плеча (4000 т < 6000 т) итог меньше.
        assertTrue(phased.getMoneyTotalChargedFlow().first() > withoutOverride.getMoneyTotalChargedFlow().first())
    }
}
