package com.z_company.data_local.route

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOne
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.z_company.core.ErrorEntity
import com.z_company.core.ResultState
import com.z_company.core.ResultState.Companion.flowMap
import com.z_company.core.ResultState.Companion.flowRequest
import com.z_company.data_local.route.db.RouteDatabase
import com.z_company.data_local.route.mapping.BasicDataMapper
import com.z_company.data_local.route.mapping.LocomotiveMapper
import com.z_company.data_local.route.mapping.OtherWorkMapper
import com.z_company.data_local.route.mapping.PassengerMapper
import com.z_company.data_local.route.mapping.PhotoMapper
import com.z_company.data_local.route.mapping.RoutePartnerMapper
import com.z_company.data_local.route.mapping.TrainMapper
import com.z_company.domain.entities.route.BasicData
import com.z_company.domain.entities.route.Locomotive
import com.z_company.domain.entities.route.OtherWork
import com.z_company.domain.entities.route.Passenger
import com.z_company.domain.entities.route.Photo
import com.z_company.domain.entities.route.Route
import com.z_company.domain.entities.route.RoutePartner
import com.z_company.domain.entities.route.Train
import com.z_company.domain.repositories.RouteRepository
import com.z_company.domain.util.generateId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SqlDelightRouteRepository : RouteRepository, KoinComponent {
    private val db: RouteDatabase by inject()

    private fun assembleRoute(basicData: com.zcompany.datalocal.route.db.BasicData): Route {
        val locomotives = db.locomotiveQueries.getByBasicId(basicData.id).executeAsList()
        val trains = db.trainQueries.getByBasicId(basicData.id).executeAsList()
        val passengers = db.passengerQueries.getByBasicId(basicData.id).executeAsList()
        val otherWorks = db.otherWorkQueries.getByBasicId(basicData.id).executeAsList()
        val partners = db.routePartnerQueries.getByBasicId(basicData.id).executeAsList()
        val photos = db.photoQueries.getByBasicId(basicData.id).executeAsList()
        return Route(
            basicData = BasicDataMapper.toData(basicData),
            locomotives = locomotives.map { LocomotiveMapper.toData(it) }.toMutableList(),
            trains = trains.map { TrainMapper.toData(it) }.toMutableList(),
            passengers = passengers.map { PassengerMapper.toData(it) }.toMutableList(),
            otherWorks = otherWorks.map { OtherWorkMapper.toData(it) }.toMutableList(),
            partners = partners.map { RoutePartnerMapper.toData(it) }.toMutableList(),
            photos = photos.map { PhotoMapper.toData(it) }.toMutableList()
        )
    }

    private fun saveRouteInternal(route: Route) {
        val basicId = route.basicData.id.ifBlank { generateId() }
        val updatedBasic = if (route.basicData.id.isBlank()) route.basicData.copy(id = basicId) else route.basicData
        db.basicDataQueries.insertOrReplace(
            id = updatedBasic.id,
            remoteRouteId = updatedBasic.remoteRouteId,
            isOnePersonOperation = if (updatedBasic.isOnePersonOperation) 1L else 0L,
            isSynchronized = if (updatedBasic.isSynchronized) 1L else 0L,
            remoteObjectId = updatedBasic.remoteObjectId,
            isDeleted = if (updatedBasic.isDeleted) 1L else 0L,
            updatedAt = BasicDataMapper.encodeUpdatedAt(updatedBasic.updatedAt),
            number = updatedBasic.number,
            timeStartWork = updatedBasic.timeStartWork,
            timeEndWork = updatedBasic.timeEndWork,
            restPointOfTurnover = if (updatedBasic.restPointOfTurnover) 1L else 0L,
            notes = updatedBasic.notes,
            isFavorite = if (updatedBasic.isFavorite) 1L else 0L,
            timeStartBreak = updatedBasic.timeStartBreak,
            timeEndBreak = updatedBasic.timeEndBreak,
            timeStartWorkBeforeArrival = updatedBasic.timeStartWorkBeforeArrival
        )
        route.locomotives.forEach { loco ->
            val locoBasicId = loco.basicId.ifBlank { basicId }
            db.locomotiveQueries.insertOrReplace(
                locoId = loco.locoId,
                basicId = locoBasicId,
                series = loco.series,
                number = loco.number,
                type = loco.type.ordinal.toLong(),
                electricSectionList = LocomotiveMapper.encodeElectricSections(loco.electricSectionList),
                dieselSectionList = LocomotiveMapper.encodeDieselSections(loco.dieselSectionList),
                timeStartOfAcceptance = loco.timeStartOfAcceptance,
                timeEndOfAcceptance = loco.timeEndOfAcceptance,
                timeStartOfDelivery = loco.timeStartOfDelivery,
                timeEndOfDelivery = loco.timeEndOfDelivery,
                normaElectricCurrent1 = loco.normaElectricCurrent1,
                normaElectricCurrent2 = loco.normaElectricCurrent2,
                normaDiesel = loco.normaDiesel,
                heatingCounterAccepted = loco.heatingCounterAccepted?.toString(),
                heatingCounterDelivery = loco.heatingCounterDelivery?.toString(),
                auxiliaryCounterAccepted = loco.auxiliaryCounterAccepted?.toString(),
                auxiliaryCounterDelivery = loco.auxiliaryCounterDelivery?.toString(),
                timeBarrierOut = loco.timeBarrierOut,
                timeBarrierIn = loco.timeBarrierIn,
                acceptanceStationId = loco.acceptanceStationId,
                deliveryStationId = loco.deliveryStationId
            )
        }
        route.trains.forEach { train ->
            val trainBasicId = train.basicId.ifBlank { basicId }
            db.trainQueries.insertOrReplace(
                trainId = train.trainId,
                basicId = trainBasicId,
                number = train.number,
                additionalNumbers = TrainMapper.encodeAdditionalNumbers(train.additionalNumbers),
                distance = train.distance,
                weight = train.weight,
                axle = train.axle,
                conditionalLength = train.conditionalLength,
                stations = TrainMapper.encodeStations(train.stations),
                servicePhase = TrainMapper.encodeServicePhase(train.servicePhase),
                pusher = TrainMapper.encodeTrainAssist(train.pusher),
                doubleTraction = TrainMapper.encodeTrainAssist(train.doubleTraction),
                doubledTrain = TrainMapper.encodeTrainAssist(train.doubledTrain)
            )
        }
        route.passengers.forEach { passenger ->
            val pBasicId = passenger.basicId.ifBlank { basicId }
            db.passengerQueries.insertOrReplace(
                passengerId = passenger.passengerId,
                basicId = pBasicId,
                remoteObjectId = passenger.remoteObjectId,
                trainNumber = passenger.trainNumber,
                stationDeparture = passenger.stationDeparture,
                stationArrival = passenger.stationArrival,
                timeArrival = passenger.timeArrival,
                timeDeparture = passenger.timeDeparture,
                notes = passenger.notes,
                isWorkStartByArrival = if (passenger.isWorkStartByArrival) 1L else 0L
            )
        }
        route.otherWorks.forEach { otherWork ->
            val owBasicId = otherWork.basicId.ifBlank { basicId }
            db.otherWorkQueries.insertOrReplace(
                otherWorkId = otherWork.otherWorkId,
                basicId = owBasicId,
                remoteObjectId = otherWork.remoteObjectId,
                workType = otherWork.workType,
                timeStart = otherWork.timeStart,
                timeEnd = otherWork.timeEnd,
                station = otherWork.station,
                notes = otherWork.notes
            )
        }
        route.partners.forEach { partner ->
            val partnerBasicId = partner.basicId.ifBlank { basicId }
            db.routePartnerQueries.insertOrReplace(
                routePartnerId = partner.routePartnerId,
                basicId = partnerBasicId,
                remoteObjectId = partner.remoteObjectId,
                sourcePartnerId = partner.sourcePartnerId,
                fullName = partner.fullName,
                tabNumber = partner.tabNumber,
                notes = partner.notes
            )
        }
        route.photos.forEach { photo ->
            val photoBasicId = photo.basicId.ifBlank { basicId }
            db.photoQueries.insertOrReplace(
                photoId = photo.photoId,
                basicId = photoBasicId,
                remoteObjectId = photo.remoteObjectId,
                url = photo.url,
                dateOfCreate = photo.dateOfCreate
            )
        }
    }

    override fun loadRouteByPeriodFlow(startPeriod: Long, endPeriod: Long): Flow<List<Route>> {
        val basicDataFlow = db.basicDataQueries.getByPeriod(startPeriod, endPeriod)
            .asFlow()
            .mapToList(Dispatchers.Default)
        // Триггер: любое изменение в таблице Train вызывает переэмиссию
        val trainTrigger = db.trainQueries.countAll()
            .asFlow()
            .mapToOne(Dispatchers.Default)
        // Триггер: любое изменение в таблице Passenger (отвлечения) вызывает переэмиссию
        val passengerTrigger = db.passengerQueries.countAll()
            .asFlow()
            .mapToOne(Dispatchers.Default)
        // Триггер: любое изменение в таблице OtherWork (прочая работа) вызывает переэмиссию
        val otherWorkTrigger = db.otherWorkQueries.countAll()
            .asFlow()
            .mapToOne(Dispatchers.Default)
        // Триггер: любое изменение в таблице RoutePartner (напарники) вызывает переэмиссию
        val partnerTrigger = db.routePartnerQueries.countAll()
            .asFlow()
            .mapToOne(Dispatchers.Default)

        return combine(basicDataFlow, trainTrigger, passengerTrigger, otherWorkTrigger, partnerTrigger) { basicDataList, _, _, _, _ ->
            basicDataList.map { assembleRoute(it) }
        }
    }

    override fun loadRoutesByPeriod(startPeriod: Long, endPeriod: Long): Flow<ResultState<List<Route>>> {
        return loadRouteByPeriodFlow(startPeriod, endPeriod)
            .map<List<Route>, ResultState<List<Route>>> { ResultState.Success(it) }
    }

    override fun loadRoutesAsStateFlow(): Flow<ResultState<List<Route>>> {
        return flowMap {
            val basicDataFlow = db.basicDataQueries.getAll()
                .asFlow()
                .mapToList(Dispatchers.Default)
            val trainTrigger = db.trainQueries.countAll()
                .asFlow()
                .mapToOne(Dispatchers.Default)
            val passengerTrigger = db.passengerQueries.countAll()
                .asFlow()
                .mapToOne(Dispatchers.Default)
            val otherWorkTrigger = db.otherWorkQueries.countAll()
                .asFlow()
                .mapToOne(Dispatchers.Default)
            val partnerTrigger = db.routePartnerQueries.countAll()
                .asFlow()
                .mapToOne(Dispatchers.Default)

            combine(basicDataFlow, trainTrigger, passengerTrigger, otherWorkTrigger, partnerTrigger) { list, _, _, _, _ ->
                ResultState.Success(list.map { assembleRoute(it) })
            }
        }
    }

    override fun loadRoutesAsFlow(): Flow<List<Route>> {
        val basicDataFlow = db.basicDataQueries.getAll()
            .asFlow()
            .mapToList(Dispatchers.Default)
        val trainTrigger = db.trainQueries.countAll()
            .asFlow()
            .mapToOne(Dispatchers.Default)
        val passengerTrigger = db.passengerQueries.countAll()
            .asFlow()
            .mapToOne(Dispatchers.Default)
        val otherWorkTrigger = db.otherWorkQueries.countAll()
            .asFlow()
            .mapToOne(Dispatchers.Default)
        val partnerTrigger = db.routePartnerQueries.countAll()
            .asFlow()
            .mapToOne(Dispatchers.Default)

        return combine(basicDataFlow, trainTrigger, passengerTrigger, otherWorkTrigger, partnerTrigger) { list, _, _, _, _ ->
            list.map { assembleRoute(it) }
        }
    }

    override fun loadRoutes(): List<Route> {
        return db.basicDataQueries.getAll().executeAsList().map { assembleRoute(it) }
    }

    override fun loadRoutesWithDeleting(): List<Route> {
        return db.basicDataQueries.getAllWithDeleted().executeAsList().map { assembleRoute(it) }
    }

    override fun loadRoute(routeId: String): Flow<ResultState<Route?>> {
        return flow {
            emit(ResultState.Loading())
            emitAll(
                combine(
                    combine(
                        db.basicDataQueries.getById(routeId)
                            .asFlow().mapToOneOrNull(Dispatchers.Default),
                        db.locomotiveQueries.getByBasicId(routeId)
                            .asFlow().mapToList(Dispatchers.Default),
                        db.trainQueries.getByBasicId(routeId)
                            .asFlow().mapToList(Dispatchers.Default),
                        db.passengerQueries.getByBasicId(routeId)
                            .asFlow().mapToList(Dispatchers.Default),
                        db.photoQueries.getByBasicId(routeId)
                            .asFlow().mapToList(Dispatchers.Default)
                    ) { basicData, locos, trains, passengers, photos ->
                        basicData?.let { bd ->
                            Route(
                                basicData = BasicDataMapper.toData(bd),
                                locomotives = locos.map { LocomotiveMapper.toData(it) }.toMutableList(),
                                trains = trains.map { TrainMapper.toData(it) }.toMutableList(),
                                passengers = passengers.map { PassengerMapper.toData(it) }.toMutableList(),
                                photos = photos.map { PhotoMapper.toData(it) }.toMutableList()
                            )
                        }
                    },
                    db.otherWorkQueries.getByBasicId(routeId)
                        .asFlow().mapToList(Dispatchers.Default),
                    db.routePartnerQueries.getByBasicId(routeId)
                        .asFlow().mapToList(Dispatchers.Default)
                ) { route, otherWorks, partners ->
                    if (route == null) {
                        ResultState.Success(null) as ResultState<Route?>
                    } else {
                        ResultState.Success(
                            route.copy(
                                otherWorks = otherWorks.map { OtherWorkMapper.toData(it) }.toMutableList(),
                                partners = partners.map { RoutePartnerMapper.toData(it) }.toMutableList()
                            )
                        ) as ResultState<Route?>
                    }
                }.catch { e ->
                    emit(ResultState.Error(ErrorEntity(e)))
                }
            )
        }
    }

    override fun loadLoco(locoId: String): Flow<ResultState<Locomotive?>> {
        return flowMap {
            db.locomotiveQueries.getById(locoId)
                .asFlow()
                .mapToOneOrNull(Dispatchers.Default)
                .map { loco -> ResultState.Success(loco?.let { LocomotiveMapper.toData(it) }) }
        }
    }

    override fun loadLocoListByBasicId(basicId: String): List<Locomotive> {
        return db.locomotiveQueries.getByBasicId(basicId).executeAsList().map { LocomotiveMapper.toData(it) }
    }

    override fun loadTrain(trainId: String): Flow<ResultState<Train?>> {
        return flowMap {
            db.trainQueries.getById(trainId)
                .asFlow()
                .mapToOneOrNull(Dispatchers.Default)
                .map { train -> ResultState.Success(train?.let { TrainMapper.toData(it) }) }
        }
    }

    override fun loadTrainListByBasicId(basicId: String): List<Train> {
        return db.trainQueries.getByBasicId(basicId).executeAsList().map { TrainMapper.toData(it) }
    }

    override fun loadPassenger(passengerId: String): Flow<ResultState<Passenger?>> {
        return flowMap {
            db.passengerQueries.getById(passengerId)
                .asFlow()
                .mapToOneOrNull(Dispatchers.Default)
                .map { p -> ResultState.Success(p?.let { PassengerMapper.toData(it) }) }
        }
    }

    override fun loadPassengerListByBasicId(basicId: String): List<Passenger> {
        return db.passengerQueries.getByBasicId(basicId).executeAsList().map { PassengerMapper.toData(it) }
    }

    override fun loadOtherWork(otherWorkId: String): Flow<ResultState<OtherWork?>> {
        return flowMap {
            db.otherWorkQueries.getById(otherWorkId)
                .asFlow()
                .mapToOneOrNull(Dispatchers.Default)
                .map { ow -> ResultState.Success(ow?.let { OtherWorkMapper.toData(it) }) }
        }
    }

    override fun loadOtherWorkListByBasicId(basicId: String): List<OtherWork> {
        return db.otherWorkQueries.getByBasicId(basicId).executeAsList().map { OtherWorkMapper.toData(it) }
    }

    override fun loadPartner(routePartnerId: String): Flow<ResultState<RoutePartner?>> {
        return flowMap {
            db.routePartnerQueries.getById(routePartnerId)
                .asFlow()
                .mapToOneOrNull(Dispatchers.Default)
                .map { p -> ResultState.Success(p?.let { RoutePartnerMapper.toData(it) }) }
        }
    }

    override fun loadPartnerListByBasicId(basicId: String): List<RoutePartner> {
        return db.routePartnerQueries.getByBasicId(basicId).executeAsList().map { RoutePartnerMapper.toData(it) }
    }

    override fun loadPhoto(photoId: String): Flow<ResultState<Photo?>> {
        return flowMap {
            db.photoQueries.getById(photoId)
                .asFlow()
                .mapToOneOrNull(Dispatchers.Default)
                .map { p -> ResultState.Success(p?.let { PhotoMapper.toData(it) }) }
        }
    }

    override fun loadPhotosByRoute(basicId: String): Flow<ResultState<List<Photo>>> {
        return flowMap {
            db.photoQueries.getByBasicId(basicId)
                .asFlow()
                .mapToList(Dispatchers.Default)
                .map { photos -> ResultState.Success(photos.map { PhotoMapper.toData(it) }) }
        }
    }

    override fun saveRoute(route: Route): Flow<ResultState<Unit>> {
        return flowRequest { saveRouteInternal(route) }
    }

    override fun saveLocomotive(locomotive: Locomotive): Flow<ResultState<Unit>> {
        return flowRequest {
            val locoId = locomotive.locoId.ifBlank { generateId() }
            db.locomotiveQueries.insertOrReplace(
                locoId = locoId,
                basicId = locomotive.basicId,
                series = locomotive.series,
                number = locomotive.number,
                type = locomotive.type.ordinal.toLong(),
                electricSectionList = LocomotiveMapper.encodeElectricSections(locomotive.electricSectionList),
                dieselSectionList = LocomotiveMapper.encodeDieselSections(locomotive.dieselSectionList),
                timeStartOfAcceptance = locomotive.timeStartOfAcceptance,
                timeEndOfAcceptance = locomotive.timeEndOfAcceptance,
                timeStartOfDelivery = locomotive.timeStartOfDelivery,
                timeEndOfDelivery = locomotive.timeEndOfDelivery,
                normaElectricCurrent1 = locomotive.normaElectricCurrent1,
                normaElectricCurrent2 = locomotive.normaElectricCurrent2,
                normaDiesel = locomotive.normaDiesel,
                heatingCounterAccepted = locomotive.heatingCounterAccepted?.toString(),
                heatingCounterDelivery = locomotive.heatingCounterDelivery?.toString(),
                auxiliaryCounterAccepted = locomotive.auxiliaryCounterAccepted?.toString(),
                auxiliaryCounterDelivery = locomotive.auxiliaryCounterDelivery?.toString(),
                timeBarrierOut = locomotive.timeBarrierOut,
                timeBarrierIn = locomotive.timeBarrierIn,
                acceptanceStationId = locomotive.acceptanceStationId,
                deliveryStationId = locomotive.deliveryStationId
            )
            db.basicDataQueries.markUnsynchronized(locomotive.basicId)
        }
    }

    override fun saveTrain(train: Train): Flow<ResultState<Unit>> {
        return flowRequest {
            db.trainQueries.insertOrReplace(
                trainId = train.trainId.ifBlank { generateId() },
                basicId = train.basicId,
                number = train.number,
                additionalNumbers = TrainMapper.encodeAdditionalNumbers(train.additionalNumbers),
                distance = train.distance,
                weight = train.weight,
                axle = train.axle,
                conditionalLength = train.conditionalLength,
                stations = TrainMapper.encodeStations(train.stations),
                servicePhase = TrainMapper.encodeServicePhase(train.servicePhase),
                pusher = TrainMapper.encodeTrainAssist(train.pusher),
                doubleTraction = TrainMapper.encodeTrainAssist(train.doubleTraction),
                doubledTrain = TrainMapper.encodeTrainAssist(train.doubledTrain)
            )
            db.basicDataQueries.markUnsynchronized(train.basicId)
        }
    }

    override fun updateTrain(train: Train): Flow<ResultState<Unit>> = saveTrain(train)

    override fun savePassenger(passenger: Passenger): Flow<ResultState<Unit>> {
        return flowRequest {
            db.passengerQueries.insertOrReplace(
                passengerId = passenger.passengerId.ifBlank { generateId() },
                basicId = passenger.basicId,
                remoteObjectId = passenger.remoteObjectId,
                trainNumber = passenger.trainNumber,
                stationDeparture = passenger.stationDeparture,
                stationArrival = passenger.stationArrival,
                timeArrival = passenger.timeArrival,
                timeDeparture = passenger.timeDeparture,
                notes = passenger.notes,
                isWorkStartByArrival = if (passenger.isWorkStartByArrival) 1L else 0L
            )
            db.basicDataQueries.markUnsynchronized(passenger.basicId)
        }
    }

    override fun saveOtherWork(otherWork: OtherWork): Flow<ResultState<Unit>> {
        return flowRequest {
            db.otherWorkQueries.insertOrReplace(
                otherWorkId = otherWork.otherWorkId.ifBlank { generateId() },
                basicId = otherWork.basicId,
                remoteObjectId = otherWork.remoteObjectId,
                workType = otherWork.workType,
                timeStart = otherWork.timeStart,
                timeEnd = otherWork.timeEnd,
                station = otherWork.station,
                notes = otherWork.notes
            )
            db.basicDataQueries.markUnsynchronized(otherWork.basicId)
        }
    }

    override fun savePartner(partner: RoutePartner): Flow<ResultState<Unit>> {
        return flowRequest {
            db.routePartnerQueries.insertOrReplace(
                routePartnerId = partner.routePartnerId.ifBlank { generateId() },
                basicId = partner.basicId,
                remoteObjectId = partner.remoteObjectId,
                sourcePartnerId = partner.sourcePartnerId,
                fullName = partner.fullName,
                tabNumber = partner.tabNumber,
                notes = partner.notes
            )
            db.basicDataQueries.markUnsynchronized(partner.basicId)
        }
    }

    override fun savePhoto(photo: Photo): Flow<ResultState<Unit>> {
        return flowRequest {
            db.photoQueries.insertOrReplace(
                photoId = photo.photoId.ifBlank { generateId() },
                basicId = photo.basicId,
                remoteObjectId = photo.remoteObjectId,
                url = photo.url,
                dateOfCreate = photo.dateOfCreate
            )
        }
    }

    override fun setRemoteObjectIdRoute(basicId: String, remoteRouteId: String?): Flow<ResultState<Unit>> {
        return flowRequest { db.basicDataQueries.setRemoteRouteId(remoteRouteId, basicId) }
    }

    override fun setRemoteObjectIdBasicData(basicId: String, remoteObjectId: String?): Flow<ResultState<Unit>> {
        return flowRequest { db.basicDataQueries.setRemoteObjectId(remoteObjectId, basicId) }
    }

    override fun setRemoteObjectIdLocomotive(locoId: String, remoteObjectId: String): Flow<ResultState<Unit>> {
        // removeObjectId removed from Locomotive table — no-op
        return flowRequest { }
    }

    override fun setRemoteObjectIdPassenger(passengerId: String, objectId: String): Flow<ResultState<Unit>> {
        return flowRequest { db.passengerQueries.setRemoteObjectId(objectId, passengerId) }
    }

    override fun setRemoteObjectIdOtherWork(otherWorkId: String, objectId: String): Flow<ResultState<Unit>> {
        return flowRequest { db.otherWorkQueries.setRemoteObjectId(objectId, otherWorkId) }
    }

    override fun setRemoteObjectIdPartner(routePartnerId: String, objectId: String): Flow<ResultState<Unit>> {
        return flowRequest { db.routePartnerQueries.setRemoteObjectId(objectId, routePartnerId) }
    }

    override fun setRemoteObjectIdPhoto(photoId: String, objectId: String): Flow<ResultState<Unit>> {
        return flowRequest { db.photoQueries.setRemoteObjectId(objectId, photoId) }
    }

    override fun remove(route: Route): Flow<ResultState<Unit>> {
        return flow {
            emit(ResultState.Loading())
            db.basicDataQueries.delete(route.basicData.id)
            emit(ResultState.Success(Unit))
        }.catch { e ->
            emit(ResultState.Error(ErrorEntity(e)))
        }.flowOn(Dispatchers.Default)
    }

    override fun removeLoco(locomotive: Locomotive): Flow<ResultState<Unit>> {
        return flowRequest { db.locomotiveQueries.delete(locomotive.locoId) }
    }

    override fun removeTrain(train: Train): Flow<ResultState<Unit>> {
        return flowRequest { db.trainQueries.delete(train.trainId) }
    }

    override fun removePassenger(passenger: Passenger): Flow<ResultState<Unit>> {
        return flowRequest { db.passengerQueries.delete(passenger.passengerId) }
    }

    override fun removeOtherWork(otherWork: OtherWork): Flow<ResultState<Unit>> {
        return flowRequest { db.otherWorkQueries.delete(otherWork.otherWorkId) }
    }

    override fun removePartner(partner: RoutePartner): Flow<ResultState<Unit>> {
        return flowRequest { db.routePartnerQueries.delete(partner.routePartnerId) }
    }

    override fun removePhoto(photo: Photo): Flow<ResultState<Unit>> {
        return flowRequest { db.photoQueries.delete(photo.photoId) }
    }

    override fun markAsRemoved(route: Route): Flow<ResultState<Unit>> {
        val updated = route.copy(basicData = route.basicData.copy(isDeleted = true))
        return saveRoute(updated)
    }

    override fun setSynchronizedRoute(basicId: String): Flow<ResultState<Unit>> {
        return flowRequest { db.basicDataQueries.setSynchronized(basicId) }
    }

    override fun markUnsynchronized(basicId: String): Flow<ResultState<Unit>> {
        return flowRequest { db.basicDataQueries.markUnsynchronized(basicId) }
    }

    override fun clearRepository(): Flow<ResultState<Unit>> {
        return flowRequest { db.basicDataQueries.deleteAll() }
    }

    override fun setFavoriteRoute(basicId: String, isFavorite: Boolean): Flow<ResultState<Boolean>> {
        return flow {
            emit(ResultState.Loading())
            db.basicDataQueries.setFavorite(if (isFavorite) 1L else 0L, basicId)
            emit(ResultState.Success(isFavorite))
        }.catch {
            emit(ResultState.Error(ErrorEntity(it)))
        }.flowOn(Dispatchers.Default)
    }
}
