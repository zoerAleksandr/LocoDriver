package com.example.domain.entities

import java.util.Calendar

data class MonthOfYear(
    var id: Int = 0,
    var year: Int = Calendar.getInstance().get(Calendar.YEAR),
    var month: Int = Calendar.getInstance().get(Calendar.MONTH),
    var normaHours: Int = 0
)
