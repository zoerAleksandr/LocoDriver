package com.z_company.domain.entities.route

import com.z_company.domain.entities.setting.ServicePhase
import kotlinx.serialization.Serializable
import com.google.gson.annotations.SerializedName
import java.util.UUID

@Serializable
data class Train(
    var trainId: String = UUID.randomUUID().toString(),
    var basicId: String = "",
    var remoteObjectId: String? = null,
    var number: String? = null,
    var distance: String? = null,
    var weight: String? = null,
    var axle: String? = null,
    var conditionalLength: String? = null,
    var isHeavyLongDistance: Boolean = false,
    var stations: MutableList<Station> = mutableListOf(),
    var servicePhase: ServicePhase? = null
): java.io.Serializable

@Serializable
data class Station(
    var stationId: String = UUID.randomUUID().toString(),
    var trainId: String = "",
    @SerializedName("name")
    var stationName: String? = null,
    var timeArrival: Long? = null,
    var timeDeparture: Long? = null
): java.io.Serializable
