package com.example.data_local.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.domain.entities.Locomotive
import com.example.domain.entities.Notes
import com.example.domain.entities.Passenger
import com.example.domain.entities.Train

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
