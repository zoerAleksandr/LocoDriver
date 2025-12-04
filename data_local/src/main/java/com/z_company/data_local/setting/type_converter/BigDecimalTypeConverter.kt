package com.z_company.data_local.setting.type_converter

import androidx.room.TypeConverter
import java.math.BigDecimal

object BigDecimalTypeConverter {
    @TypeConverter
    fun fromBigDecimal(value: BigDecimal): String {
        return value.toString()
    }

    @TypeConverter
    fun stringToBigDecimal(value: String): BigDecimal {
        return BigDecimal(value)
    }
}