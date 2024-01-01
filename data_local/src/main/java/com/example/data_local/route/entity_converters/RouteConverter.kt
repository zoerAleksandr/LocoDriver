package com.example.data_local.route.entity_converters

import com.example.data_local.route.entity_converters.PassengerConverter
import com.example.domain.entities.route.Route
import com.example.data_local.route.entity.Route as RouteEntity

internal object RouteConverter {
    fun fromData(route: Route) = RouteEntity(
        basicData = BasicDataConverter.fromData(route.basicData),
        locomotives = LocomotiveConverter.fromDataList(route.locomotives),
//        trains = TrainConverter.fromDataList(route.trains),
//        passengers = PassengerConverter.fromDataList(route.passengers),
//        notes = route.notes?.let { NotesConverter.fromData(it) }
    )

    fun toData(entity: RouteEntity) = Route().apply {
        basicData = BasicDataConverter.toData(entity.basicData)
        locomotives = LocomotiveConverter.toDataList(entity.locomotives)
//        trains = TrainConverter.toDataList(entity.trains)
//        passengers = PassengerConverter.toDataList(entity.passengers)
//        notes = entity.notes?.let { NotesConverter.toData(it) }
    }
}