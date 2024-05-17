package com.z_company.domain.entities.route

import com.z_company.domain.util.div
import com.z_company.domain.util.minus
import com.z_company.domain.util.moreThan
import com.z_company.domain.util.plus

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
}