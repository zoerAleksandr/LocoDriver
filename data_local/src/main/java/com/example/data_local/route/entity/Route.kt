package com.example.data_local.route.entity

import androidx.room.Embedded
import androidx.room.Relation

internal data class Route(
    @Embedded
    val basicData: BasicData,
    @Relation(parentColumn = "id", entityColumn = "baseId")
    val locomotives: List<Locomotive>
)
