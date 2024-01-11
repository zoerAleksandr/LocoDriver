package com.example.data_local.setting.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class MonthOfYear(
    @PrimaryKey
    val id: Int,
    val year: Int,
    val month: Int,
    val normaHours: Int
)
