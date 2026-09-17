@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.z_company.route.viewmodel

import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.entities.route.BasicData
import com.z_company.domain.entities.route.Locomotive
import com.z_company.domain.entities.route.OtherWork
import com.z_company.domain.entities.route.OverRestRoutes.overRestPayment
import com.z_company.domain.entities.route.Passenger
import com.z_company.domain.entities.route.Photo
import com.z_company.domain.entities.route.PhysicalDeletionReason
import com.z_company.domain.entities.route.Route
import com.z_company.domain.entities.route.RoutePartner
import com.z_company.domain.entities.route.Train
import com.z_company.domain.entities.setting.SalarySetting
import com.z_company.domain.entities.setting.UserSettings
import com.z_company.domain.repositories.RouteRepository
import com.z_company.domain.salary.SalaryCalculationHelper
import com.z_company.domain.use_cases.RouteUseCase
import com.z_company.domain.util.TimeCalculationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Переотдых в `FormViewModel.calculateSalary`: предыдущий маршрут берётся через
 * `RouteUseCase.previousRouteForOverRestFlow` (ближайшая явка раньше явки
 * маршрута среди месяца явки и предыдущего месяца), деньги — через
 * `Route.overRestPayment`. Сама FormViewModel требует Koin/Application, поэтому
 * проверяется ровно та цепочка, из которой собран блок `deferredOverRestMoney`.
 */
class FormViewModelOverRestTest {
    private val hour = 3_600_000L
    private val zone = TimeZone.of("GMT+3")

    private fun at(month: Int, day: Int, hour: Int) =
        LocalDateTime(2025, month, day, hour, 0).toInstant(zone).toEpochMilliseconds()

    /** Настройки: выбран май, тариф 100, норма отдыха в ПО 3 ч. */
    private val settings = UserSettings(
        selectMonthOfYear = MonthOfYear(year = 2025, month = 4, tariffRate = 100.0, days = emptyList()),
        timeZone = 0L,
        minTimeRestPointOfTurnover = 3 * hour,
    )

    // Отдых в ПО 30 апреля 15:00–19:00 → оплачиваемый переотдых с 23:00.
    private val lastOfApril = Route(
        basicData = BasicData(
            id = "apr-last",
            timeStartWork = at(4, 30, 15),
            timeEndWork = at(4, 30, 19),
            restPointOfTurnover = true,
        ),
    )

    // Первый маршрут мая: явка 1 мая 03:00 → переотдых 4 ч.
    private val firstOfMay = Route(
        basicData = BasicData(id = "may-first", timeStartWork = at(5, 1, 3), timeEndWork = at(5, 1, 9)),
    )

    /** Репозиторий-заглушка: отдаёт маршруты, попавшие в запрошенный период по явке. */
    private fun repository(vararg routes: Route): RouteRepository = object : RouteRepository {
        override fun loadRouteByPeriodFlow(startPeriod: Long, endPeriod: Long): Flow<List<Route>> {
            return flowOf(routes.filter { route ->
                val start = route.basicData.timeStartWork ?: return@filter false
                start in startPeriod..endPeriod
            })
        }

        override fun loadRoutesByPeriod(startPeriod: Long, endPeriod: Long) = unused()
        override fun loadRoutesAsStateFlow() = unused()
        override fun loadRoutesAsFlow() = unused()
        override fun loadRoutes() = unused()
        override fun loadRoutesWithDeleting() = unused()
        override fun loadTrash() = unused()
        override fun loadRoute(routeId: String) = unused()
        override fun loadLoco(locoId: String) = unused()
        override fun loadLocoListByBasicId(basicId: String) = unused()
        override fun loadTrain(trainId: String) = unused()
        override fun loadTrainListByBasicId(basicId: String) = unused()
        override fun loadPassenger(passengerId: String) = unused()
        override fun loadPassengerListByBasicId(basicId: String) = unused()
        override fun loadOtherWork(otherWorkId: String) = unused()
        override fun loadOtherWorkListByBasicId(basicId: String) = unused()
        override fun loadPartner(routePartnerId: String) = unused()
        override fun loadPartnerListByBasicId(basicId: String) = unused()
        override fun loadPhoto(photoId: String) = unused()
        override fun loadPhotosByRoute(basicId: String) = unused()
        override fun remove(route: Route) = unused()
        override fun purgeRoute(route: Route, reason: PhysicalDeletionReason) = unused()
        override fun removeLoco(locomotive: Locomotive) = unused()
        override fun removeTrain(train: Train) = unused()
        override fun removePassenger(passenger: Passenger) = unused()
        override fun removeOtherWork(otherWork: OtherWork) = unused()
        override fun removePartner(partner: RoutePartner) = unused()
        override fun removePhoto(photo: Photo) = unused()
        override fun saveRoute(route: Route) = unused()
        override fun setRemoteObjectIdRoute(basicId: String, remoteRouteId: String?) = unused()
        override fun setRemoteObjectIdBasicData(basicId: String, remoteObjectId: String?) = unused()
        override fun setRemoteObjectIdLocomotive(locoId: String, remoteObjectId: String) = unused()
        override fun setRemoteObjectIdPassenger(passengerId: String, objectId: String) = unused()
        override fun setRemoteObjectIdOtherWork(otherWorkId: String, objectId: String) = unused()
        override fun setRemoteObjectIdPartner(routePartnerId: String, objectId: String) = unused()
        override fun setRemoteObjectIdPhoto(photoId: String, objectId: String) = unused()
        override fun saveLocomotive(locomotive: Locomotive) = unused()
        override fun saveTrain(train: Train) = unused()
        override fun updateTrain(train: Train) = unused()
        override fun savePassenger(passenger: Passenger) = unused()
        override fun saveOtherWork(otherWork: OtherWork) = unused()
        override fun savePartner(partner: RoutePartner) = unused()
        override fun savePhoto(photo: Photo) = unused()
        override fun markAsRemoved(route: Route) = unused()
        override fun markAsPendingRemoteDeletion(route: Route) = unused()
        override fun restoreFromTrash(routeId: String) = unused()
        override fun acknowledgeRemoteDeletion(routeId: String, deletedAt: Long) = unused()
        override fun setSynchronizedRoute(basicId: String) = unused()
        override fun markUnsynchronized(basicId: String) = unused()
        override fun setFavoriteRoute(basicId: String, isFavorite: Boolean) = unused()

        private fun unused(): Nothing = error("не используется в этом тесте")
    }

    /** Блок deferredOverRestMoney из FormViewModel.calculateSalary. */
    private suspend fun formOverRest(route: Route, useCase: RouteUseCase, setting: UserSettings): Pair<Double, Long> {
        val previous = useCase.previousRouteForOverRestFlow(route, TimeCalculationContext.from(setting)).first()
        val payment = route.overRestPayment(
            previous = previous,
            minTimeRest = setting.minTimeRestPointOfTurnover,
            tariffRate = setting.selectMonthOfYear.tariffRate,
        )
        return payment.money to payment.timeMillis
    }

    @Test
    fun firstRouteOfMonthSeesTurnoverRestFromPreviousMonth() = runTest {
        val useCase = RouteUseCase(repository(lastOfApril, firstOfMay))

        val (money, time) = formOverRest(firstOfMay, useCase, settings)

        assertEquals(4 * hour, time)
        assertEquals(4 * 100.0 * (2.0 / 3.0), money, 0.001)
        assertTrue(money > 0.0)
    }

    @Test
    fun routeOutsideSelectedMonthStillGetsOverRest() = runTest {
        // Выбран май, а редактируется маршрут июля: раньше список брался за
        // selectMonthOfYear, индекс маршрута был -1 и переотдых всегда 0.
        val julyRest = Route(
            basicData = BasicData(
                id = "jul-rest",
                timeStartWork = at(7, 10, 8),
                timeEndWork = at(7, 10, 12),
                restPointOfTurnover = true,
            ),
        )
        val julyNext = Route(
            basicData = BasicData(id = "jul-next", timeStartWork = at(7, 10, 18), timeEndWork = at(7, 10, 22)),
        )
        val useCase = RouteUseCase(repository(lastOfApril, firstOfMay, julyRest, julyNext))

        val (money, time) = formOverRest(julyNext, useCase, settings)

        // Норма 4 ч (работа) > 3 ч: переотдых 16:00–18:00 = 2 ч по тарифу выбранного месяца.
        assertEquals(2 * hour, time)
        assertEquals(2 * 100.0 * (2.0 / 3.0), money, 0.001)
    }

    @Test
    fun previousRouteIsNearestEarlierStartNotAnyRestRoute() = runTest {
        // Между отдыхом в ПО и текущим маршрутом есть ещё один — переотдых
        // считается от ближайшего (без отдыха в ПО) → 0.
        val between = Route(
            basicData = BasicData(id = "between", timeStartWork = at(4, 30, 21), timeEndWork = at(4, 30, 23)),
        )
        val useCase = RouteUseCase(repository(lastOfApril, between, firstOfMay))

        assertSame(
            between.basicData.id,
            useCase.previousRouteForOverRestFlow(firstOfMay, TimeCalculationContext.from(settings)).first()?.basicData?.id,
        )
        assertEquals(0.0 to 0L, formOverRest(firstOfMay, useCase, settings))
    }

    @Test
    fun routeWithoutStartHasNoPreviousRoute() = runTest {
        val useCase = RouteUseCase(repository(lastOfApril))
        val draft = Route(basicData = BasicData(id = "draft"))

        assertNull(useCase.previousRouteForOverRestFlow(draft, TimeCalculationContext.from(settings)).first())
    }

    @Test
    fun tripOverRestMatchesFormulaOfSharedCalculator() = runTest {
        // Полный переотдых поездки = сумме частей по месяцам в месячном расчёте.
        val april = SalaryCalculationHelper(
            userSettings = settings.copy(selectMonthOfYear = settings.selectMonthOfYear.copy(month = 3)),
            salarySetting = SalarySetting(),
            allRoutes = listOf(lastOfApril),
            adjacentRoutes = listOf(firstOfMay),
        )
        val may = SalaryCalculationHelper(
            userSettings = settings,
            salarySetting = SalarySetting(),
            allRoutes = listOf(firstOfMay),
            adjacentRoutes = listOf(lastOfApril),
        )
        val (money, time) = formOverRest(firstOfMay, RouteUseCase(repository(lastOfApril, firstOfMay)), settings)

        assertEquals(time, april.getOverRestTimeFlow().first() + may.getOverRestTimeFlow().first())
        assertEquals(money, april.getMoneyOverRestFlow().first() + may.getMoneyOverRestFlow().first(), 0.001)
    }
}
