package com.z_company.data_local.route.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class SearchResponse(
    @PrimaryKey
    val responseText: String
)
