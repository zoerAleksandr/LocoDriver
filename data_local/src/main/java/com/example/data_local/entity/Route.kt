package com.example.data_local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
internal data class Route(
    @PrimaryKey
    val id: String,
    var number: String?,
    var timeStartWork: Long?,
    var timeEndWork: Long?,

)
