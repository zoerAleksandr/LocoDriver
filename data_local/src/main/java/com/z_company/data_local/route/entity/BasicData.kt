package com.z_company.data_local.route.entity

import androidx.room.ColumnInfo
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
    @ColumnInfo(defaultValue = "0")
    var isSynchronizedRoute: Boolean = false,
    @ColumnInfo(defaultValue = "NULL")
    var remoteRouteId: String? = null,
    @ColumnInfo(defaultValue = "0")
    var isOnePersonOperation: Boolean = false,
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