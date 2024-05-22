package com.z_company.core.util

import android.util.Log
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

object CalculateNightTime {
    @OptIn(ExperimentalStdlibApi::class)
    fun test(startMillis: Long?, endMillis: Long?) {
        if (startMillis == null || endMillis == null) {
            return
        }
//        val startCalendar = getInstance().also {
//            it.timeInMillis = startMillis
//        }
//        val endCalendar = getInstance().also {
//            it.timeInMillis = endMillis
//        }


        val hourStart = 0
        val minuteStart = 0

        val hourEnd = 6
        val minuteEnd = 0
        val startLocalDateTime = LocalDateTime.ofEpochSecond(
            startMillis / 1000,
            0,
            ZoneOffset.MIN
        )

        val endLocalDateTime = LocalDateTime.ofEpochSecond(
            endMillis / 1000,
            0,
            ZoneOffset.ofHours(3)
        )


        val dateList = mutableListOf<LocalDateTime>()
        var dayOfWork = startLocalDateTime
        dateList.add(dayOfWork)
        while (dayOfWork.isBefore(endLocalDateTime)) {
            dayOfWork = dayOfWork.plusDays(1L)
            dateList.add(dayOfWork)
        }
        var countNightTime = 0L
        dateList.forEach { localDateTime ->
            val startNight = localDateTime.withHour(hourStart).withMinute(minuteStart)
            if (hourStart <= hourEnd) {
                val endNight = localDateTime.withHour(hourEnd).withMinute(minuteEnd)
                Log.d("ZZZ", "startLocalDateTime $startLocalDateTime")
                Log.d("ZZZ", "endLocalDateTime $endLocalDateTime")
                Log.d("ZZZ", "endNight $endNight")

//                val duration = Duration.between(startNight, endNight)
//                Log.d("ZZZ", "duration $duration")
                if (startNight.rangeUntil(endNight).contains(startLocalDateTime)) {
                    val nightTime = Duration.between(startLocalDateTime, endNight).toMillis()
                    Log.d("ZZZ", "nightTime millis $nightTime")
                    val text = DateAndTimeConverter.getTimeInStringFormat(nightTime)
                    Log.d("ZZZ", "nightTime $text")
                }

//                countNightTime += duration
            } else {
                val endNight = localDateTime
                    .plusDays(1)
                    .withHour(hourEnd)
                    .withMinute(minuteEnd)
                val duration = Duration.between(startNight, endNight).toMillis()
                countNightTime += duration
            }
        }

        Log.d("ZZZ", "countNightTime $countNightTime")


//        val nightPeriodList = mutableListOf<OpenEndRange<Calendar>>()

//        val nightStart = getInstance().also {
//            it.timeInMillis = startMillis
//            it.set(HOUR_OF_DAY, hourStart)
//            it.set(MINUTE, minuteStart)
//        }
//        val endDayOfMonth = if (hourStart > hourEnd) {
//            startCalendar.get(DAY_OF_MONTH) + 1
//        } else {
//            startCalendar.get(DAY_OF_MONTH)
//        }
//
//        val nightEnd = getInstance().also {
//            it.timeInMillis = startMillis
//            it.set(DAY_OF_MONTH, endDayOfMonth)
//            it.set(HOUR_OF_DAY, hourEnd)
//            it.set(MINUTE, minuteEnd)
//        }
//
//        if (endCalendar.after(nightEnd)) {
//
//        }

//        Log.d("ZZZ", "nightStart - ${nightStart.time}")
//        Log.d("ZZZ", "nightEnd - ${nightEnd.time}")


//        val until = startCalendar.rangeUntil(endCalendar)
//        Log.d("ZZZ", until.toString())
    }
}