package com.z_company.data_local.route.type_converters

import androidx.room.TypeConverter
import com.z_company.domain.entities.route.LocoType

internal object TypeLocoConverter {
    @TypeConverter
    fun fromMediaType(value: LocoType): Int {
        return value.let { value.ordinal }
    }

    @TypeConverter
    fun toMediaType(value: Int): LocoType {
        return LocoType.values()[value]
    }
}