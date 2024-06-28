package com.z_company.domain.entities

object UtilForMonthOfYear {
    fun MonthOfYear.getNormaHours(): Int {
        var normaOfMonth = 0
        this.days.forEach { day ->
            if (!day.isReleaseDay) {
                normaOfMonth += when (day.tag) {
                    TagForDay.WORKING_DAY -> 8
                    TagForDay.SHORTENED_DAY -> 7
                    TagForDay.NON_WORKING_DAY -> 0
                }
            }
        }
        return normaOfMonth
    }
}