package com.example.data_local.route.type_converters

import androidx.room.TypeConverter
import com.example.domain.entities.route.LocoType

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