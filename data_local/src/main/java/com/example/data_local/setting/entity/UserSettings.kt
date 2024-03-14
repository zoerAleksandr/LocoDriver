package com.example.data_local.setting.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
internal data class UserSettings(
    @PrimaryKey
    val key: String,
    val minTimeRest: Long,
    val lastEnteredDieselCoefficient: Double
)
