package com.z_company.data_local.route.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.z_company.data_local.route.type_converters.DateTypeConverter
import java.util.Date

@Entity
@TypeConverters(
    DateTypeConverter::class
)
internal data class BasicData(
    @PrimaryKey
    val id: String,
    var isSynchronized: Boolean = false,
    var remoteObjectId: String? = null,
    var isDeleted: Boolean,
    var updatedAt: Date,
    var number: String?,
    var timeStartWork: Long?,
    var timeEndWork: Long?,
    var restPointOfTurnover: Boolean,
    var notes: String?
)