package com.z_company.domain.entities.route

import com.z_company.domain.entities.TimePeriod
import com.z_company.domain.util.div
import com.z_company.domain.util.lessThan
import com.z_company.domain.util.minus
import com.z_company.domain.util.moreThan
import com.z_company.domain.util.plus
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

object UtilsForEntities {
    fun Route.getWorkTime(): Long? {
        val timeEnd = this.basicData.timeEndWork
        val timeStart = this.basicData.timeStartWork
        return if (timeEnd != null && timeStart != null) {
            timeEnd - timeStart
        } else {
            null
        }
    }

    fun Route.isTimeWorkValid(): Boolean {
        val startTime = this.basicData.timeStartWork
        val endTime = this.basicData.timeEndWork

        return !startTime.moreThan(endTime)
    }

    fun Route.shortRest(minTime: Long): Long? {
        return if (this.isTimeWorkValid()) {
            val startTime = this.basicData.timeStartWork
            val endTime = this.basicData.timeEndWork
            val timeResult = endTime - startTime
            var halfRest = timeResult / 2
            halfRest?.let { half ->
                val minute = half / 60_000L
                if (minute % 2L != 0L) {
                    halfRest += minute
                }
                if (halfRest.moreThan(minTime)) {
                    endTime + halfRest
                } else {
                    endTime + minTime
                }
            }
        } else {
            null
        }
    }

    fun Route.fullRest(minTime: Long): Long? {
        return if (this.isTimeWorkValid()) {
            val startTime = this.basicData.timeStartWork
            val endTime = this.basicData.timeEndWork
            val timeResult = endTime - startTime
            if (minTime.moreThan(timeResult)) {
                endTime + minTime
            } else {
                endTime + timeResult
            }
        } else {
            null
        }
    }

    fun Passenger.getFollowingTime(): Long? {
        val timeEnd = this.timeArrival
        val timeStart = this.timeDeparture
        return if (timeEnd != null && timeStart != null) {
            timeEnd - timeStart
        } else {
            null
        }
    }

    fun Route.getHomeRest(parentList: List<Route>, minTimeHomeRest: Long?): Long? {
        val routeChain = mutableListOf<Route>()

        var indexRoute = parentList.indexOf(this)
        routeChain.add(parentList[indexRoute])
        if (indexRoute > 0) {
            indexRoute -= 1
            while (parentList[indexRoute].basicData.restPointOfTurnover) {
                routeChain.add(parentList[indexRoute])
                if (indexRoute == 0) {
                    break
                } else {
                    indexRoute -= 1
                }
            }
        }

        routeChain.sortBy {
            it.basicData.timeStartWork
        }
        var totalWorkTime = 0L
        var totalRestTime = 0L
        routeChain.forEachIndexed { index, routeInChain ->
            totalWorkTime += routeInChain.getWorkTime() ?: 0L
            if (index != routeChain.lastIndex) {
                val startRest = routeInChain.basicData.timeEndWork
                val endRest = routeChain[index + 1].basicData.timeStartWork
                val restTime = endRest - startRest
                totalRestTime += restTime ?: 0L
            }
        }
        var homeRest = (totalWorkTime * 2.6 - totalRestTime).toLong()
        if (homeRest.lessThan(minTimeHomeRest)) {
            homeRest = minTimeHomeRest ?: 0L
        }
        return routeChain.last().basicData.timeEndWork + homeRest
    }

    fun Route.inTimePeriod(period: TimePeriod): Boolean {
//        val startLocalDateTime: LocalDateTime? =
//            this.basicData.timeStartWork?.let {
//                LocalDateTime.ofInstant(Instant.ofEpochMilli(it), ZoneId.systemDefault())
//            }
//
//        val startLocalDate: LocalDate? =
//            startLocalDateTime?.let { dateTime ->
//                LocalDate.of(dateTime.year, dateTime.month, dateTime.dayOfMonth)
//            }
//
//        val endLocalDateTime: LocalDateTime? =
//            this.basicData.timeEndWork?.let {
//                LocalDateTime.ofInstant(Instant.ofEpochMilli(it), ZoneId.systemDefault())
//            }
//
//        val endLocalDate: LocalDate? =
//            endLocalDateTime?.let { dateTime ->
//                LocalDate.of(dateTime.year, dateTime.month, dateTime.dayOfMonth)
//            }


        period.startDate?.let { startDateInFilter ->
            this.basicData.timeStartWork?.let { currentDate ->
                if (currentDate < startDateInFilter) {
                    return false
                }
            }
        }

        period.endDate?.let { endDateInFilter ->
            this.basicData.timeEndWork?.let { currentDate ->
                if (currentDate > endDateInFilter) {
                    return false
                }
            }
        }
        return true
    }
}