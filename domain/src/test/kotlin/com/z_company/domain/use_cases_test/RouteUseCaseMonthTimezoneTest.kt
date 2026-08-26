package com.z_company.domain.use_cases_test

import com.z_company.core.ResultState
import com.z_company.domain.entities.Day
import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.entities.TagForDay
import com.z_company.domain.entities.route.BasicData
import com.z_company.domain.entities.route.Locomotive
import com.z_company.domain.entities.route.OtherWork
import com.z_company.domain.entities.route.Passenger
import com.z_company.domain.entities.route.Photo
import com.z_company.domain.entities.route.Route
import com.z_company.domain.entities.route.RoutePartner
import com.z_company.domain.entities.route.Train
import com.z_company.domain.repositories.RouteRepository
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

class RouteUseCaseMonthTimezoneTest {

    private val moscow = TimeZone.of("GMT+3")
    private val local = TimeZone.of("GMT+8")
    private val august = MonthOfYear(
        year = 2026,
        month = 7,
        days = (1..31).map { Day(it, TagForDay.WORKING_DAY) }
    )

    private val routeEndingInAugustOnlyLocally = Route(
        basicData = BasicData(
            timeStartWork = millis(moscow, 2026, 7, 31, 13, 45),
            timeEndWork = millis(moscow, 2026, 7, 31, 23, 20)
        )
    )

    @Test
    fun listRoutesByMonth_localBoundary_includesRouteThatEndedBeforeAugustInMoscow() = runTest {
        val result = RouteUseCase(FakeRouteRepository(routeEndingInAugustOnlyLocally))
            .listRoutesByMonth(
                august,
                TimeCalculationContext(localTZ = local, crossMonthTZ = local)
            )
            .first { it is ResultState.Success }

        assertEquals(listOf(routeEndingInAugustOnlyLocally), (result as ResultState.Success).data)
    }

    @Test
    fun listRoutesByMonth_moscowBoundary_excludesRouteThatEndedInJulyMoscow() = runTest {
        val result = RouteUseCase(FakeRouteRepository(routeEndingInAugustOnlyLocally))
            .listRoutesByMonth(
                august,
                TimeCalculationContext(localTZ = local, crossMonthTZ = moscow)
            )
            .first { it is ResultState.Success }

        assertEquals(emptyList(), (result as ResultState.Success).data)
    }

    private fun millis(
        timeZone: TimeZone,
        year: Int,
        month: Int,
        day: Int,
        hour: Int,
        minute: Int
    ): Long = LocalDateTime(year, month, day, hour, minute)
        .toInstant(timeZone)
        .toEpochMilliseconds()

    private class FakeRouteRepository(private val route: Route) : RouteRepository {
        override fun loadRoutesByPeriod(startPeriod: Long, endPeriod: Long) =
            flowOf<ResultState<List<Route>>>(ResultState.Success(listOf(route)))

        override fun loadRouteByPeriodFlow(startPeriod: Long, endPeriod: Long) = flowOf(listOf(route))
        override fun loadRoutesAsStateFlow() = flowOf<ResultState<List<Route>>>(ResultState.Success(listOf(route)))
        override fun loadRoutesAsFlow() = flowOf(listOf(route))
        override fun loadRoutes() = listOf(route)
        override fun loadRoutesWithDeleting() = emptyList<Route>()
        override fun loadRoute(routeId: String) = success<Route?>(null)
        override fun loadLoco(locoId: String) = success<Locomotive?>(null)
        override fun loadLocoListByBasicId(basicId: String) = emptyList<Locomotive>()
        override fun loadTrain(trainId: String) = success<Train?>(null)
        override fun loadTrainListByBasicId(basicId: String) = emptyList<Train>()
        override fun loadPassenger(passengerId: String) = success<Passenger?>(null)
        override fun loadPassengerListByBasicId(basicId: String) = emptyList<Passenger>()
        override fun loadOtherWork(otherWorkId: String) = success<OtherWork?>(null)
        override fun loadOtherWorkListByBasicId(basicId: String) = emptyList<OtherWork>()
        override fun loadPartner(routePartnerId: String) = success<RoutePartner?>(null)
        override fun loadPartnerListByBasicId(basicId: String) = emptyList<RoutePartner>()
        override fun loadPhoto(photoId: String) = success<Photo?>(null)
        override fun loadPhotosByRoute(basicId: String) = success(emptyList<Photo>())
        override fun remove(route: Route) = success(Unit)
        override fun removeLoco(locomotive: Locomotive) = success(Unit)
        override fun removeTrain(train: Train) = success(Unit)
        override fun removePassenger(passenger: Passenger) = success(Unit)
        override fun removeOtherWork(otherWork: OtherWork) = success(Unit)
        override fun removePartner(partner: RoutePartner) = success(Unit)
        override fun removePhoto(photo: Photo) = success(Unit)
        override fun saveRoute(route: Route) = success(Unit)
        override fun setRemoteObjectIdRoute(basicId: String, remoteRouteId: String?) = success(Unit)
        override fun setRemoteObjectIdBasicData(basicId: String, remoteObjectId: String?) = success(Unit)
        override fun setRemoteObjectIdLocomotive(locoId: String, remoteObjectId: String) = success(Unit)
        override fun setRemoteObjectIdPassenger(passengerId: String, objectId: String) = success(Unit)
        override fun setRemoteObjectIdOtherWork(otherWorkId: String, objectId: String) = success(Unit)
        override fun setRemoteObjectIdPartner(routePartnerId: String, objectId: String) = success(Unit)
        override fun setRemoteObjectIdPhoto(photoId: String, objectId: String) = success(Unit)
        override fun saveLocomotive(locomotive: Locomotive) = success(Unit)
        override fun saveTrain(train: Train) = success(Unit)
        override fun updateTrain(train: Train) = success(Unit)
        override fun savePassenger(passenger: Passenger) = success(Unit)
        override fun saveOtherWork(otherWork: OtherWork) = success(Unit)
        override fun savePartner(partner: RoutePartner) = success(Unit)
        override fun savePhoto(photo: Photo) = success(Unit)
        override fun markAsRemoved(route: Route) = success(Unit)
        override fun setSynchronizedRoute(basicId: String) = success(Unit)
        override fun markUnsynchronized(basicId: String) = success(Unit)
        override fun clearRepository() = success(Unit)
        override fun setFavoriteRoute(basicId: String, isFavorite: Boolean) = success(isFavorite)

        private fun <T> success(value: T): Flow<ResultState<T>> = flowOf(ResultState.Success(value))
    }
}
