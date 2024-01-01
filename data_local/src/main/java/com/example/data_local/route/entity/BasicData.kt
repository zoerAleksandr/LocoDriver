package com.example.data_local.route.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.jetbrains.annotations.NotNull

@Entity
internal data class BasicData(
    @PrimaryKey
    val id: String,
    var number: String?,
    var timeStartWork: Long?,
    var timeEndWork: Long?,
)