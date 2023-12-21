package com.example.domain.entities.route

import java.util.UUID

data class Route(
    var id: String = UUID.randomUUID().toString(),
    var number: String? = null,
    var timeStartWork: Long? = null,
    var timeEndWork: Long? = null,
    var locoList: MutableList<Locomotive> = mutableListOf(),
    var trainList: MutableList<Train> = mutableListOf(),
    var passengerList: MutableList<Passenger> = mutableListOf(),
    var notes: Notes = Notes()
)
