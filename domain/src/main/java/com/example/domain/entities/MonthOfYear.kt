package com.example.domain.entities

import java.util.Calendar

data class MonthOfYear(
    val month: Int = Calendar.getInstance().get(Calendar.MONTH),
    val year: Int = Calendar.getInstance().get(Calendar.YEAR)
)
