package com.example.data_local.route

import java.util.UUID
import com.example.domain.entities.route.Route
import com.example.data_local.route.entity.Route as RouteEntity

internal object RouteConverter {
    fun fromData(route: Route) = RouteEntity(
        route.id.ifBlank { UUID.randomUUID().toString() },
        route.number,
        route.timeStartWork,
        route.timeEndWork,
        route.locoList,
        route.trainList,
        route.passengerList,
        route.notes
    )

    fun toData(entity: RouteEntity) = Route().apply {
        id = entity.id
        number = entity.number
        timeStartWork = entity.timeStartWork
        timeEndWork = entity.timeEndWork
        locoList = entity.locoList
        trainList = entity.trainList
        passengerList = entity.passengerList
        notes = entity.notes
    }
}