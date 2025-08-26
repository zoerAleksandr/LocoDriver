package com.z_company.entity

import com.z_company.domain.entities.route.BasicData
import com.z_company.domain.entities.route.Locomotive
import com.z_company.domain.entities.route.Passenger
import com.z_company.domain.entities.route.Photo
import com.z_company.domain.entities.route.Train

internal data class Route(
    var remoteObjectId: String? = null,
    var basicData: BasicData = BasicData(),
    var locomotives: MutableList<Locomotive> = mutableListOf(),
    var trains: MutableList<Train> = mutableListOf(),
    var passengers: MutableList<Passenger> = mutableListOf(),
    var photos: MutableList<Photo> = mutableListOf()
)
