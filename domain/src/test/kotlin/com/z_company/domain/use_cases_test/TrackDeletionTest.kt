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
import com.z_company.domain.entities.route.PhysicalDeletionReason
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

import com.z_company.domain.track.InMemoryTrackRepository
import com.z_company.domain.track.TrackPoint
import com.z_company.domain.use_cases.TrainUseCase
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TrackDeletionTest {

    private val train = Train(trainId = "train-1")
    private val route = Route(basicData = BasicData(id = "route-1"), trains = mutableListOf(train))

    private fun repoWithTrack(): InMemoryTrackRepository {
        val repo = InMemoryTrackRepository()
        repo.append("track-1", "train-1", TrackPoint(1000, 55.0, 37.0, 10.0, 5f))
        repo.append("track-1", "train-1", TrackPoint(2000, 55.0, 37.0, 10.0, 5f))
        repo.append("other", "train-2", TrackPoint(1000, 55.0, 37.0, 10.0, 5f))
        return repo
    }

    @Test
    fun removingTrainDeletesItsTracksAndQueuesRemoteDelete() = runTest {
        val tracks = repoWithTrack()
        TrainUseCase(FakeRouteRepository(route), tracks).removeTrain(train).collect {}
        assertNull(tracks.track("track-1"))
        assertNotNull(tracks.track("other"))
        assertEquals(listOf("track-1"), tracks.pendingRemoteDeletes())
    }

    @Test
    fun recordingToDeletedTrainDoesNotResurrectTheTrack() = runTest {
        val tracks = repoWithTrack()
        TrainUseCase(FakeRouteRepository(route), tracks).removeTrain(train).collect {}
        // «АВТО» продолжает писать в запомненную цель — удалённый поезд.
        tracks.append("track-1", "train-1", TrackPoint(3000, 55.0, 37.0, 10.0, 5f))
        assertNull(tracks.track("track-1"))
        assertTrue(tracks.tracksForTrain("train-1").isEmpty())
    }

    @Test
    fun movingRouteToTrashDeletesTracksOfItsTrains() = runTest {
        val tracks = repoWithTrack()
        RouteUseCase(FakeRouteRepository(route), tracks).markAsRemoved(route).collect {}
        assertNull(tracks.track("track-1"))
        assertNotNull(tracks.track("other"))
        assertEquals(listOf("track-1"), tracks.pendingRemoteDeletes())
    }

    @Test
    fun removeAndPurgeRouteDeleteTracksToo() = runTest {
        val a = repoWithTrack()
        RouteUseCase(FakeRouteRepository(route), a).removeRoute(route).collect {}
        assertNull(a.track("track-1"))
        val b = repoWithTrack()
        RouteUseCase(FakeRouteRepository(route), b)
            .purgeRoute(route, PhysicalDeletionReason.TRASH_RETENTION_EXPIRED).collect {}
        assertNull(b.track("track-1"))
    }

    @Test
    fun restoringFromTrashDoesNotBringTracksBackButAllowsRecordingAgain() = runTest {
        val tracks = repoWithTrack()
        val useCase = RouteUseCase(FakeRouteRepository(route, trash = listOf(route)), tracks)
        useCase.markAsRemoved(route).collect {}
        useCase.restoreFromTrash("route-1").collect {}
        assertNull(tracks.track("track-1"))
        tracks.append("track-2", "train-1", TrackPoint(5000, 55.0, 37.0, 10.0, 5f))
        assertNotNull(tracks.track("track-2"))
    }

    @Test
    fun routeUseCaseWithoutTrackRepositoryStillWorks() = runTest {
        RouteUseCase(FakeRouteRepository(route)).markAsRemoved(route).collect {}
    }

    private class FakeRouteRepository(private val route: Route, private val trash: List<Route> = emptyList()) : RouteRepository {
        override fun loadRoutesByPeriod(startPeriod: Long, endPeriod: Long) =
            flowOf<ResultState<List<Route>>>(ResultState.Success(listOf(route)))

        override fun loadRouteByPeriodFlow(startPeriod: Long, endPeriod: Long) = flowOf(listOf(route))
        override fun loadRoutesAsStateFlow() = flowOf<ResultState<List<Route>>>(ResultState.Success(listOf(route)))
        override fun loadRoutesAsFlow() = flowOf(listOf(route))
        override fun loadRoutes() = listOf(route)
        override fun loadRoutesWithDeleting() = emptyList<Route>()
        override fun loadTrash() = trash
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
        override fun purgeRoute(route: Route, reason: PhysicalDeletionReason) = success(Unit)
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
        override fun markAsPendingRemoteDeletion(route: Route) = success(Unit)
        override fun restoreFromTrash(routeId: String) = success(Unit)
        override fun acknowledgeRemoteDeletion(routeId: String, deletedAt: Long) = success(Unit)
        override fun setSynchronizedRoute(basicId: String) = success(Unit)
        override fun markUnsynchronized(basicId: String) = success(Unit)
        override fun setFavoriteRoute(basicId: String, isFavorite: Boolean) = success(isFavorite)

        private fun <T> success(value: T): Flow<ResultState<T>> = flowOf(ResultState.Success(value))
    }
}
