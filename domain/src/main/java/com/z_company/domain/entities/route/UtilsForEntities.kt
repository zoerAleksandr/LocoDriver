package com.z_company.domain.entities.route

import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.entities.TagForDay
import com.z_company.domain.entities.TimePeriod
import com.z_company.domain.entities.UserSettings
import com.z_company.domain.entities.UtilForMonthOfYear.getTimeInCurrentMonth
import com.z_company.domain.util.CalculateNightTime
import com.z_company.domain.util.div
import com.z_company.domain.util.lessThan
import com.z_company.domain.util.minus
import com.z_company.domain.util.moreThan
import com.z_company.domain.util.plus
import java.util.Calendar

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
                if (half % 60_000L != 0L) {
                    halfRest += 60_000L
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
        if (parentList.isNotEmpty()) {
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
        } else {
            return null
        }
    }

    fun Route.inTimePeriod(period: TimePeriod): Boolean {
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

    private fun Route.timeInLongInPeriod(startDate: Long, endDate: Long): Long? {
        this.basicData.timeStartWork?.let { startWork ->
            this.basicData.timeEndWork?.let { endWork ->
                if (startDate > startWork) {
                    return if (endDate > endWork) {
                        endWork - startDate
                    } else {
                        endDate - startDate
                    }
                } else {
                    return if (endDate > endWork) {
                        endWork - startWork
                    } else {
                        endDate - startWork
                    }
                }
            }
        }
        return null
    }


    fun Route.getPassengerTime(): Long? {
        var totalTime: Long? = 0L
        this.passengers.forEach { passenger ->
            totalTime += passenger.getFollowingTime()
        }
        return totalTime
    }

    fun Route.isTransition(): Boolean {
        if (this.basicData.timeStartWork == null || this.basicData.timeEndWork == null) {
            return false
        } else {
            val startCalendar = Calendar.getInstance().also {
                it.timeInMillis = this.basicData.timeStartWork!!
            }
            val yearStart = startCalendar.get(Calendar.YEAR)
            val monthStart = startCalendar.get(Calendar.MONTH)

            val endCalendar = Calendar.getInstance().also {
                it.timeInMillis = this.basicData.timeEndWork!!
            }
            val yearEnd = endCalendar.get(Calendar.YEAR)
            val monthEnd = endCalendar.get(Calendar.MONTH)
            return if (monthStart < monthEnd && yearStart == yearEnd) {
                true
            } else if (monthStart > monthEnd && yearStart < yearEnd) {
                true
            } else {
                false
            }
        }
    }

    fun List<Route>.getTotalWorkTime(monthOfYear: MonthOfYear): Long {
        var totalTime = 0L
        this.forEach { route ->
            if (route.isTransition()) {
                totalTime += monthOfYear.getTimeInCurrentMonth(
                    route.basicData.timeStartWork!!,
                    route.basicData.timeEndWork!!
                )
            } else {
                route.getWorkTime().let { time ->
                    totalTime += time ?: 0
                }
            }
        }
        return totalTime
    }

    fun List<Route>.getNightTime(userSettings: UserSettings): Long {
        var nightTime = 0L
        val startNightHour = userSettings.nightTime.startNightHour
        val startNightMinute = userSettings.nightTime.startNightMinute
        val endNightHour = userSettings.nightTime.endNightHour
        val endNightMinute = userSettings.nightTime.endNightMinute

        this.forEach { route ->
            if (route.isTransition()) {
                val nightTimeInRoute =
                    CalculateNightTime.getNightTimeTransitionRoute(
                        month = userSettings.selectMonthOfYear.month,
                        year = userSettings.selectMonthOfYear.year,
                        startMillis = route.basicData.timeStartWork,
                        endMillis = route.basicData.timeEndWork,
                        hourStart = startNightHour,
                        minuteStart = startNightMinute,
                        hourEnd = endNightHour,
                        minuteEnd = endNightMinute
                    )

                nightTime = nightTime.plus(nightTimeInRoute) ?: 0L
            } else {
                val nightTimeInRoute = CalculateNightTime.getNightTime(
                    startMillis = route.basicData.timeStartWork,
                    endMillis = route.basicData.timeEndWork,
                    hourStart = startNightHour,
                    minuteStart = startNightMinute,
                    hourEnd = endNightHour,
                    minuteEnd = endNightMinute
                )
                nightTime = nightTime.plus(nightTimeInRoute) ?: 0L
            }
        }
        return nightTime
    }

    fun List<Route>.getPassengerTime(monthOfYear: MonthOfYear): Long {
        var passengerTime = 0L
        this.forEach { route ->
            route.passengers.forEach { passenger ->
                passengerTime = if (route.isTransition()) {
                    passengerTime.plus(
                        monthOfYear.getTimeInCurrentMonth(
                            passenger.timeDeparture!!,
                            passenger.timeArrival!!,
                        )
                    )
                } else {
                    passengerTime.plus((passenger.timeArrival - passenger.timeDeparture) ?: 0L)
                }
            }
        }
        return passengerTime
    }

    fun List<Route>.getWorkingTimeOnAHoliday(monthOfYear: MonthOfYear): Long {
        var holidayTime = 0L

        val holidayList = monthOfYear.days.filter { it.tag == TagForDay.HOLIDAY }
        if (holidayList.isNotEmpty()) {
            holidayList.forEach { day ->
                val startHolidayInLong = Calendar.getInstance().also {
                    it.set(Calendar.YEAR, monthOfYear.year)
                    it.set(Calendar.MONTH, monthOfYear.month)
                    it.set(Calendar.DAY_OF_MONTH, day.dayOfMonth)
                    it.set(Calendar.HOUR_OF_DAY, 0)
                    it.set(Calendar.MINUTE, 0)
                    it.set(Calendar.SECOND, 0)
                    it.set(Calendar.MILLISECOND, 0)
                }.timeInMillis

                val endHoliday = Calendar.getInstance().also {
                    it.set(Calendar.YEAR, monthOfYear.year)
                    it.set(Calendar.MONTH, monthOfYear.month)
                    it.set(Calendar.DAY_OF_MONTH, day.dayOfMonth)
                    it.set(Calendar.HOUR_OF_DAY, 0)
                    it.set(Calendar.MINUTE, 0)
                    it.set(Calendar.SECOND, 0)
                    it.set(Calendar.MILLISECOND, 0)
                }
                endHoliday.add(Calendar.DATE, 1)

                val endHolidayInLong = endHoliday.timeInMillis

                this.forEach { route ->
                    route.timeInLongInPeriod(
                        startDate = startHolidayInLong,
                        endDate = endHolidayInLong
                    )?.let { timeInPeriod ->
                        if (timeInPeriod > 0) {
                            holidayTime += timeInPeriod
                        }
                    }

                }
            }
        }

        return holidayTime
    }

    fun List<Route>.getWorkTimeWithHoliday(monthOfYear: MonthOfYear): Long {
        val totalWorkTime = this.getTotalWorkTime(monthOfYear)
        val holidayWorkTime = this.getWorkingTimeOnAHoliday(monthOfYear)
        return totalWorkTime - holidayWorkTime
    }
}