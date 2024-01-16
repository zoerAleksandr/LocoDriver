package com.example.domain.entities.route

data class Route(
    var basicData: BasicData = BasicData(),
    var locomotives: MutableList<Locomotive> = mutableListOf(),
    var trains: MutableList<Train> = mutableListOf(),
    var passengers: MutableList<Passenger> = mutableListOf(),
    var notes: Notes? = null
)
