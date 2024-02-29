package com.example.data_local.route.entity

import androidx.room.Embedded
import androidx.room.Relation

internal data class Route(
    @Embedded
    val basicData: BasicData,
    @Relation(parentColumn = "id", entityColumn = "basicId")
    val locomotives: List<Locomotive>,
    @Relation(parentColumn = "id", entityColumn = "basicId")
    val trains: List<Train>,
    @Relation(parentColumn = "id", entityColumn = "basicId")
    val passengers: List<Passenger>,
    @Relation(parentColumn = "id", entityColumn = "basicId")
    val notes: Notes?
)
