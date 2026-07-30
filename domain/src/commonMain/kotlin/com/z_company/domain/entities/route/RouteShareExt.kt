package com.z_company.domain.entities.route

import com.z_company.domain.util.generateId

/**
 * Готовит маршрут, полученный по публичной ссылке, к сохранению
 * в локальную базу получателя.
 *
 * Переприсваивает все идентификаторы (Route/BasicData/Locomotive/Section/Train/Station/Passenger)
 * и сбрасывает флаги/ссылки облачной синхронизации. Так маршрут становится
 * «свежим» для получателя: при следующей синхронизации он будет создан
 * как новый объект в его облаке и не затронет объекты отправителя.
 *
 * Флаги:
 *  - `isSynchronizedRoute = false`, `remoteRouteId = null`
 *  - `isSynchronized = false`, `remoteObjectId = null`
 *  - `isDeleted = false`, `isFavorite = false`
 */
fun Route.reidentifyForImport(): Route {
    val newBasicId = generateId()
    val newBasicData = basicData.copy(
        id = newBasicId,
//        isSynchronizedRoute = false,
        remoteRouteId = null,
        isSynchronized = false,
        remoteObjectId = null,
        isDeleted = false,
        isFavorite = false
    )

    val newLocomotives = locomotives.map { loco ->
        val newLocoId = generateId()
        loco.copy(
            locoId = newLocoId,
            basicId = newBasicId,
            remoteObjectId = null,
            electricSectionList = loco.electricSectionList.map { section ->
                section.copy(sectionId = generateId(), locoId = newLocoId)
            }.toMutableList(),
            dieselSectionList = loco.dieselSectionList.map { section ->
                section.copy(sectionId = generateId(), locoId = newLocoId)
            }.toMutableList()
        )
    }.toMutableList()

    val newTrains = trains.map { train ->
        val newTrainId = generateId()
        train.copy(
            trainId = newTrainId,
            basicId = newBasicId,
            stations = train.stations.map { station ->
                station.copy(stationId = generateId(), trainId = newTrainId)
            }.toMutableList()
        )
    }.toMutableList()

    val newPassengers = passengers.map { passenger ->
        passenger.copy(
            passengerId = generateId(),
            basicId = newBasicId,
            remoteObjectId = null
        )
    }.toMutableList()

    val newOtherWorks = otherWorks.map { otherWork ->
        otherWork.copy(
            otherWorkId = generateId(),
            basicId = newBasicId,
            remoteObjectId = null
        )
    }.toMutableList()

    return Route(
        basicData = newBasicData,
        locomotives = newLocomotives,
        trains = newTrains,
        passengers = newPassengers,
        otherWorks = newOtherWorks,
        photos = mutableListOf()
    )
}
