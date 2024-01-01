package com.example.domain.entities.route

data class Route(
    var basicData: BasicData = BasicData(),
    var locomotives: MutableList<Locomotive> = mutableListOf(Locomotive()),
    var trains: MutableList<Train> = mutableListOf(Train()),
    var passengers: MutableList<Passenger> = mutableListOf(Passenger()),
    var notes: Notes? = Notes()
)
