package com.z_company.domain.entities

import java.util.Calendar

object UtilForMonthOfYear {
    fun MonthOfYear.getPersonalNormaHours(): Int {
        var normaOfMonth = 0
        this.days.forEach { day ->
            if (!day.isReleaseDay) {
                normaOfMonth += when (day.tag) {
                    TagForDay.WORKING_DAY -> 8
                    TagForDay.SHORTENED_DAY -> 7
                    TagForDay.NON_WORKING_DAY -> 0
                    TagForDay.HOLIDAY -> 0
                }
            }
        }
        return normaOfMonth
    }

    fun MonthOfYear.getDayoffHours(): Int {
        var totalRelease = 0
        this.days.forEach { day ->
            if (day.isReleaseDay) {
                totalRelease += when (day.tag) {
                    TagForDay.WORKING_DAY -> 8
                    TagForDay.SHORTENED_DAY -> 7
                    TagForDay.NON_WORKING_DAY -> 0
                    TagForDay.HOLIDAY -> 0
                }
            }
        }
        return totalRelease
    }

    fun MonthOfYear.getStandardNormaHours(): Int {
        var normaOfMonth = 0
        this.days.forEach { day ->
            normaOfMonth += when (day.tag) {
                TagForDay.WORKING_DAY -> 8
                TagForDay.SHORTENED_DAY -> 7
                TagForDay.NON_WORKING_DAY -> 0
                TagForDay.HOLIDAY -> 0
            }

        }
        return normaOfMonth
    }

    fun MonthOfYear.getTodayNormaHours(): Int {
        val currentDate = Calendar.getInstance()
        var normaOfMonth = 0
        if (currentDate.get(Calendar.MONTH) == this.month) {
            this.days.forEach { day ->
                if (currentDate.get(Calendar.DAY_OF_MONTH) >= day.dayOfMonth) {
                    normaOfMonth += when (day.tag) {
                        TagForDay.WORKING_DAY -> 8
                        TagForDay.SHORTENED_DAY -> 7
                        TagForDay.NON_WORKING_DAY -> 0
                        TagForDay.HOLIDAY -> 0
                    }
                }
            }
        }
        return normaOfMonth
    }

    fun MonthOfYear.getTimeInCurrentMonth(
        startTime: Long,
        endTime: Long,
    ): Long {
        val startCalendar = Calendar.getInstance().also {
            it.timeInMillis = startTime
        }

        if (startCalendar.get(Calendar.MONTH) == this.month) {
            val endCurrentDay = Calendar.getInstance().also {
                it.timeInMillis = startTime
                it.set(Calendar.DAY_OF_MONTH, it.get(Calendar.DAY_OF_MONTH) + 1)
                it.set(Calendar.HOUR_OF_DAY, 0)
                it.set(Calendar.MINUTE, 0)
                it.set(Calendar.MILLISECOND, 0)
            }
            val endCurrentDayInMillis = endCurrentDay.timeInMillis
            return endCurrentDayInMillis - startTime
        } else {
            val startCurrentDay = Calendar.getInstance().also {
                it.timeInMillis = endTime
                it.set(Calendar.HOUR_OF_DAY, 0)
                it.set(Calendar.MINUTE, 0)
                it.set(Calendar.MILLISECOND, 0)
            }
            val startCurrentDayInMillis = startCurrentDay.timeInMillis
            return endTime - startCurrentDayInMillis
        }
    }
}