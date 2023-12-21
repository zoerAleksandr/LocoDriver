package com.example.data_local.route.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.domain.entities.route.Locomotive
import com.example.domain.entities.route.Notes
import com.example.domain.entities.route.Passenger
import com.example.domain.entities.route.Train

@Entity
internal data class Route(
    @PrimaryKey
    val id: String,
    var number: String?,
    var timeStartWork: Long?,
    var timeEndWork: Long?,
    @Embedded(prefix = "locoList_")
    val locoList: MutableList<Locomotive> = mutableListOf(),
    @Embedded(prefix = "trainList_")
    val trainList: MutableList<Train> = mutableListOf(),
    @Embedded(prefix = "passengerList_")
    val passengerList: MutableList<Passenger> = mutableListOf(),
    @Embedded(prefix = "notes_")
    var notes: Notes = Notes()
)
