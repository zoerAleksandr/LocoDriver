package com.z_company.domain.util

import java.math.BigDecimal

operator fun BigDecimal?.minus(other: BigDecimal?): BigDecimal? =
    if (this != null && other != null) {
        this.subtract(other)
    } else {
        null
    }

operator fun BigDecimal?.plus(other: BigDecimal?): BigDecimal? =
    if (this != null && other != null) {
        this.add(other)
    } else if (this == null && other != null) {
        other
    } else if (this != null && other == null) {
        this
    } else {
        null
    }

fun BigDecimal?.plusNullableValue(other: BigDecimal?): BigDecimal? {
    return if (this == null) {
        null
    } else {
        this + other
    }
}