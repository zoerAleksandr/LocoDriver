package com.z_company.route.viewmodel

import com.z_company.domain.entities.Day
import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.entities.TagForDay
import com.z_company.domain.entities.route.BasicData
import com.z_company.domain.entities.route.Route
import com.z_company.domain.entities.route.Train
import com.z_company.domain.entities.setting.SalarySetting
import com.z_company.domain.entities.setting.UserSettings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Тесты удержаний «Благосостояние» и «Алименты» в расчёте зарплаты.
 *
 * Ключевое свойство, которое проверяют эти тесты: два удержания считаются
 * от РАЗНЫХ баз.
 *   - Благосостояние = % от «Всего начислено» (грязная сумма, как Профсоюз/
 *     Прочие удержания) — НЕ зависит от размера других удержаний.
 *   - Алименты = % от «чистой» суммы к выдаче БЕЗ учёта самих алиментов:
 *     Всего начислено − НДФЛ − Профсоюз − Прочие удержания − Благосостояние
 *     — то есть меняется вместе с каждым из перечисленных удержаний.
 *
 * Константы:
 *   ONE_HOUR_MS = 3_600_000 мс = 1 час
 *
 * SalarySetting в тестах всегда обнуляет zonalSurcharge и nightTimePercent
 * (в дефолтном SalarySetting() они 25% и 40% соответственно), чтобы
 * «Всего начислено» точно равнялось часы × тариф и не зависело от времени
 * суток и прочих надбавок — иначе величина «грязной» базы была бы
 * непредсказуема и тесты проверяли бы не то, что заявлено.
 */
class RetentionCalculationTest {

    private val oneHourMs = 3_600_000L

    private fun salarySetting(
        ndfl: Double = 0.0,
        unionistsRetention: Double = 0.0,
        otherRetention: Double = 0.0,
        welfarePercent: Double = 0.0,
        alimonyPercent: Double = 0.0
    ) = SalarySetting(
        zonalSurcharge = 0.0,
        nightTimePercent = 0.0,
        ndfl = ndfl,
        unionistsRetention = unionistsRetention,
        otherRetention = otherRetention,
        welfarePercent = welfarePercent,
        alimonyPercent = alimonyPercent
    )

    private fun createHelper(
        routes: List<Route>,
        tariffRate: Double,
        setting: SalarySetting
    ): SalaryCalculationHelper {
        val days = (1..30).map { Day(dayOfMonth = it, tag = TagForDay.WORKING_DAY) }
        val monthOfYear = MonthOfYear(tariffRate = tariffRate, days = days)
        val userSettings = UserSettings(selectMonthOfYear = monthOfYear)
        return SalaryCalculationHelper(
            userSettings = userSettings,
            salarySetting = setting,
            allRoutes = routes
        )
    }

    /** Простой маршрут без пассажиров/одиночки/праздников — только тарифное время. */
    private fun basicRoute(workDurationMs: Long): Route = Route(
        basicData = BasicData(
            isOnePersonOperation = false,
            timeStartWork = 0L,
            timeEndWork = workDurationMs
        ),
        trains = mutableListOf(Train(number = "2503")),
        passengers = mutableListOf()
    )

    // ─── Благосостояние: % от грязной суммы начисления ─────────────────────

    @Test
    fun welfare_isPercentOfGross() = runTest {
        // 10 часов * 1000 руб/час = 10000 гросс; 5% → 500
        val route = basicRoute(10 * oneHourMs)
        val setting = salarySetting(welfarePercent = 5.0)
        val helper = createHelper(listOf(route), tariffRate = 1000.0, setting = setting)

        val gross = helper.getMoneyTotalChargedFlow().first()
        val welfare = helper.getMoneyWelfareRetentionFlow().first()

        assertEquals(10000.0, gross, 0.01)
        assertEquals(500.0, welfare, 0.01)
    }

    @Test
    fun welfare_zeroPercent_returnsZero() = runTest {
        val route = basicRoute(10 * oneHourMs)
        val setting = salarySetting(welfarePercent = 0.0)
        val helper = createHelper(listOf(route), tariffRate = 1000.0, setting = setting)

        val welfare = helper.getMoneyWelfareRetentionFlow().first()
        assertEquals(0.0, welfare, 0.01)
    }

    @Test
    fun welfare_isUnaffectedByOtherRetentions() = runTest {
        // Один и тот же гросс (10000) и welfarePercent = 10%, но совсем разные
        // НДФЛ/Профсоюз/Прочие/Алименты — Благосостояние должно остаться 1000
        // в обоих случаях: оно считается ТОЛЬКО от гросс, база других удержаний
        // на него не влияет.
        val route = basicRoute(10 * oneHourMs)

        val settingLight = salarySetting(welfarePercent = 10.0)
        val helperLight = createHelper(listOf(route), tariffRate = 1000.0, setting = settingLight)
        val welfareLight = helperLight.getMoneyWelfareRetentionFlow().first()

        val settingHeavy = salarySetting(
            ndfl = 50.0,
            unionistsRetention = 30.0,
            otherRetention = 20.0,
            alimonyPercent = 90.0,
            welfarePercent = 10.0
        )
        val helperHeavy = createHelper(listOf(route), tariffRate = 1000.0, setting = settingHeavy)
        val welfareHeavy = helperHeavy.getMoneyWelfareRetentionFlow().first()

        assertEquals(1000.0, welfareLight, 0.01)
        assertEquals(1000.0, welfareHeavy, 0.01)
    }

    // ─── Алименты: % от чистой суммы (гросс минус НДФЛ/Профсоюз/Прочие/Благосостояние) ───

    @Test
    fun alimonyBase_subtractsNdflUnionistsOtherAndWelfare_fromGross() = runTest {
        // Гросс = 20 * 1000 = 20000.
        // НДФЛ 13% = 2600, Профсоюз 1% = 200, Прочие 2% = 400, Благосостояние 5% = 1000.
        // База алиментов = 20000 - 2600 - 200 - 400 - 1000 = 15800.
        val route = basicRoute(20 * oneHourMs)
        val setting = salarySetting(
            ndfl = 13.0,
            unionistsRetention = 1.0,
            otherRetention = 2.0,
            welfarePercent = 5.0
        )
        val helper = createHelper(listOf(route), tariffRate = 1000.0, setting = setting)

        val base = helper.getMoneyAlimonyBaseFlow().first()
        assertEquals(15800.0, base, 0.01)
    }

    @Test
    fun alimony_isPercentOfNetBase_notGross() = runTest {
        // База алиментов = 15800 (см. предыдущий тест), Алименты 25% → 3950.
        // Если бы алименты считались от гросс (20000 * 0.25 = 5000), тест бы упал —
        // это и есть проверка «из разных значений».
        val route = basicRoute(20 * oneHourMs)
        val setting = salarySetting(
            ndfl = 13.0,
            unionistsRetention = 1.0,
            otherRetention = 2.0,
            welfarePercent = 5.0,
            alimonyPercent = 25.0
        )
        val helper = createHelper(listOf(route), tariffRate = 1000.0, setting = setting)

        val alimony = helper.getMoneyAlimonyRetentionFlow().first()
        assertEquals(3950.0, alimony, 0.01)
    }

    @Test
    fun alimony_shrinksAsOtherRetentionsGrow_sameGross() = runTest {
        // Один и тот же гросс (10000), alimonyPercent = 100% (алименты == база),
        // чтобы напрямую видеть базу через сумму удержания.
        // Чем больше остальных удержаний — тем меньше база и тем меньше алименты,
        // хотя гросс (и, соответственно, Благосостояние) не меняется.
        val route = basicRoute(10 * oneHourMs)

        val noOtherDeductions = salarySetting(alimonyPercent = 100.0)
        val helperNo = createHelper(listOf(route), tariffRate = 1000.0, setting = noOtherDeductions)
        val alimonyNo = helperNo.getMoneyAlimonyRetentionFlow().first()

        val withNdflOnly = salarySetting(ndfl = 10.0, alimonyPercent = 100.0)
        val helperNdfl = createHelper(listOf(route), tariffRate = 1000.0, setting = withNdflOnly)
        val alimonyNdfl = helperNdfl.getMoneyAlimonyRetentionFlow().first()

        val withAllDeductions = salarySetting(
            ndfl = 10.0,
            unionistsRetention = 5.0,
            otherRetention = 5.0,
            welfarePercent = 10.0,
            alimonyPercent = 100.0
        )
        val helperAll = createHelper(listOf(route), tariffRate = 1000.0, setting = withAllDeductions)
        val alimonyAll = helperAll.getMoneyAlimonyRetentionFlow().first()

        assertEquals(10000.0, alimonyNo, 0.01)   // база = гросс целиком
        assertEquals(9000.0, alimonyNdfl, 0.01)  // база = 10000 - 1000 (10% НДФЛ)
        assertEquals(7000.0, alimonyAll, 0.01)   // база = 10000 -1000-500-500-1000
        assertTrue(alimonyNo > alimonyNdfl)
        assertTrue(alimonyNdfl > alimonyAll)
    }

    @Test
    fun alimony_zeroPercent_returnsZero_regardlessOfBase() = runTest {
        val route = basicRoute(10 * oneHourMs)
        val setting = salarySetting(ndfl = 13.0, unionistsRetention = 1.0, alimonyPercent = 0.0)
        val helper = createHelper(listOf(route), tariffRate = 1000.0, setting = setting)

        val alimony = helper.getMoneyAlimonyRetentionFlow().first()
        assertEquals(0.0, alimony, 0.01)
    }

    // ─── Всего удержано / К выдаче: сумма всех пяти видов удержаний ────────

    @Test
    fun totalRetention_sumsAllFiveDeductions() = runTest {
        // Гросс = 20000; НДФЛ 2600, Профсоюз 200, Прочие 400, Благосостояние 1000,
        // база алиментов 15800, Алименты 25% = 3950.
        // Всего удержано = 2600+200+400+1000+3950 = 8150.
        val route = basicRoute(20 * oneHourMs)
        val setting = salarySetting(
            ndfl = 13.0,
            unionistsRetention = 1.0,
            otherRetention = 2.0,
            welfarePercent = 5.0,
            alimonyPercent = 25.0
        )
        val helper = createHelper(listOf(route), tariffRate = 1000.0, setting = setting)

        val total = helper.getMoneyTotalRetentionFlow().first()
        assertEquals(8150.0, total, 0.01)
    }

    @Test
    fun toBeCredited_equalsGrossMinusTotalRetention() = runTest {
        // К выдаче = 20000 - 8150 = 11850.
        val route = basicRoute(20 * oneHourMs)
        val setting = salarySetting(
            ndfl = 13.0,
            unionistsRetention = 1.0,
            otherRetention = 2.0,
            welfarePercent = 5.0,
            alimonyPercent = 25.0
        )
        val helper = createHelper(listOf(route), tariffRate = 1000.0, setting = setting)

        val toBeCredited = helper.getMoneyToBeCredited().first()
        assertEquals(11850.0, toBeCredited, 0.01)
    }

    @Test
    fun noDeductionsAtAll_toBeCreditedEqualsGross() = runTest {
        val route = basicRoute(10 * oneHourMs)
        val setting = salarySetting()
        val helper = createHelper(listOf(route), tariffRate = 1000.0, setting = setting)

        val gross = helper.getMoneyTotalChargedFlow().first()
        val toBeCredited = helper.getMoneyToBeCredited().first()
        assertEquals(10000.0, gross, 0.01)
        assertEquals(gross, toBeCredited, 0.01)
    }
}
