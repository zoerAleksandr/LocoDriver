package com.z_company.data_local.route.entity_converters

import com.z_company.domain.entities.route.Route
import com.z_company.data_local.route.entity.Route as RouteEntity

internal object RouteConverter {
    fun fromData(route: Route) = RouteEntity(
        basicData = BasicDataConverter.fromData(route.basicData),
        locomotives = LocomotiveConverter.fromDataList(route.locomotives),
        trains = TrainConverter.fromDataList(route.trains),
        passengers = PassengerConverter.fromDataList(route.passengers),
        photos = PhotoConverter.fromDataList(route.photos)
    )

    fun toData(entity: RouteEntity) = Route(
        basicData = BasicDataConverter.toData(entity.basicData),
        locomotives = LocomotiveConverter.toDataList(entity.locomotives),
        trains = TrainConverter.toDataList(entity.trains),
        passengers = PassengerConverter.toDataList(entity.passengers),
        photos = PhotoConverter.toDataList(entity.photos)
    )
}