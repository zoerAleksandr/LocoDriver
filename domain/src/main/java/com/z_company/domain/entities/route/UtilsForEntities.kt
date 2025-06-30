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
import com.z_company.domain.util.toIntOrZero
import org.koin.core.component.KoinComponent
import java.util.Calendar
import java.util.TimeZone

object UtilsForEntities : KoinComponent {
    fun Route.getWorkTime(): Long? {
        val timeEnd = this.basicData.timeEndWork
        val timeStart = this.basicData.timeStartWork
        return if (timeEnd != null && timeStart != null) {
            val timeWork = timeEnd - timeStart
            timeWork
//            val passengerTimeNotIncluded = this.notIncludedPassengerTime()
//            timeWork.plus(passengerTimeNotIncluded)
        } else {
            null
        }
    }

    private fun Route.notIncludedPassengerTime(): Long {
        var resultTime = 0L
        val startWork = this.basicData.timeStartWork
        val endWork = this.basicData.timeEndWork
        if (startWork == null || endWork == null) {
            return 0L
        }
        this.passengers.forEach { passenger ->
            passenger.timeArrival?.let { arrival ->
                if (endWork < arrival) {
                    val time = arrival.minus(endWork)
                    resultTime = resultTime.plus(time)
                }
            }
        }
        return resultTime
    }

    fun Route.isTimeWorkValid(): Boolean {
        val startTime = this.basicData.timeStartWork
        val endTime = this.basicData.timeEndWork

        return !startTime.moreThan(endTime)
    }

    fun Route.shortRest(minTime: Long?): Long? {
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

    fun Route.fullRest(minTime: Long?): Long? {
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
        val thisInRoute = parentList.find { it.basicData.id == this.basicData.id }
        var indexRoute = parentList.indexOf(thisInRoute)
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

    fun Route.isTransition(offsetInMoscow: Long): Boolean {
        if (this.basicData.timeStartWork == null || this.basicData.timeEndWork == null) {
            return false
        } else {
            val startCalendar = Calendar.getInstance(TimeZone.getDefault()).also {
                it.timeInMillis = this.basicData.timeStartWork!! + offsetInMoscow
            }
            val yearStart = startCalendar.get(Calendar.YEAR)
            val monthStart = startCalendar.get(Calendar.MONTH)

            val endCalendar = Calendar.getInstance().also {
                it.timeInMillis = this.basicData.timeEndWork!! + offsetInMoscow
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

    fun Passenger.isTransition(offsetInMoscow: Long): Boolean {
        if (this.timeDeparture == null || this.timeArrival == null) {
            return false
        } else {
            val startCalendar = Calendar.getInstance().also {
                it.timeInMillis = this.timeDeparture!! + offsetInMoscow
            }
            val yearStart = startCalendar.get(Calendar.YEAR)
            val monthStart = startCalendar.get(Calendar.MONTH)
            val endCalendar = Calendar.getInstance().also {
                it.timeInMillis = this.timeArrival!! + offsetInMoscow
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

    fun List<Route>.getNewRoutesToDayRange(
        days: IntRange,
        monthOfYear: MonthOfYear,
        offsetInMoscow: Long
    ): List<Route> {
        val firstData = Calendar.getInstance().also {
            it.set(Calendar.YEAR, monthOfYear.year)
            it.set(Calendar.MONTH, monthOfYear.month)
            it.set(Calendar.DAY_OF_MONTH, days.first)
            it.set(Calendar.HOUR_OF_DAY, 0)
            it.set(Calendar.MINUTE, 0)
            it.set(Calendar.SECOND, 0)
            it.set(Calendar.MILLISECOND, 0)
        }.timeInMillis + offsetInMoscow

        val secondData = Calendar.getInstance().also {
            it.set(Calendar.YEAR, monthOfYear.year)
            it.set(Calendar.MONTH, monthOfYear.month)
            it.set(Calendar.DAY_OF_MONTH, days.last)
            it.set(Calendar.HOUR_OF_DAY, 0)
            it.set(Calendar.MINUTE, 0)
            it.set(Calendar.SECOND, 0)
            it.set(Calendar.MILLISECOND, 0)
        }.timeInMillis + offsetInMoscow

        val newRouteList = mutableListOf<Route>()

        this.forEach { route ->
            var newRoute = Route()
            route.basicData.timeStartWork?.let { timeStart ->
                route.basicData.timeEndWork?.let { timeEnd ->
                    if (timeEnd < firstData || timeStart > secondData) return@forEach
                    if (firstData > timeStart) {
                        if (secondData > timeEnd) {
                            newRoute = route.copy(
                                basicData = route.basicData.copy(
                                    timeStartWork = firstData
                                )
                            )
                        } else {
                            newRoute = route.copy(
                                basicData = route.basicData.copy(
                                    timeStartWork = firstData,
                                    timeEndWork = secondData
                                )
                            )
                        }
                    } else {
                        if (secondData > timeEnd) {
                            newRoute = route

                        } else {
                            newRoute = route.copy(
                                basicData = route.basicData.copy(
                                    timeEndWork = secondData
                                )
                            )
                        }
                    }
                    newRouteList.add(newRoute)
                }
            }
        }
        return newRouteList
    }

    fun List<Route>.getWorkTime(monthOfYear: MonthOfYear, offsetInMoscow: Long): Long {
        var totalTime = 0L
        this.forEach { route ->
            if (route.isTransition(offsetInMoscow)) {
                val time = monthOfYear.getTimeInCurrentMonth(
                    route.basicData.timeStartWork!! + offsetInMoscow,
                    route.basicData.timeEndWork!! + offsetInMoscow
                )
                totalTime += time
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
            if (route.isTransition(userSettings.timeZone)) {
                val nightTimeInRoute =
                    CalculateNightTime.getNightTimeTransitionRoute(
                        month = userSettings.selectMonthOfYear.month,
                        startMillis = route.basicData.timeStartWork,
                        endMillis = route.basicData.timeEndWork,
                        hourStart = startNightHour,
                        minuteStart = startNightMinute,
                        hourEnd = endNightHour,
                        minuteEnd = endNightMinute,
                        offsetInMoscow = userSettings.timeZone
                    )
                nightTime = nightTime.plus(nightTimeInRoute) ?: 0L
            } else {
                val nightTimeInRoute = CalculateNightTime.getNightTime(
                    startMillis = route.basicData.timeStartWork,
                    endMillis = route.basicData.timeEndWork,
                    hourStart = startNightHour,
                    minuteStart = startNightMinute,
                    hourEnd = endNightHour,
                    minuteEnd = endNightMinute,
                    offsetInMoscow = userSettings.timeZone
                )
                nightTime = nightTime.plus(nightTimeInRoute) ?: 0L
            }
        }
        return nightTime
    }

    fun List<Route>.getPassengerTime(monthOfYear: MonthOfYear, offsetInMoscow: Long): Long {
        var passengerTime = 0L
        this.forEach { route ->
            route.passengers.forEach { passenger ->
                val time = passenger.getTimeFollowing(
                    startWork = route.basicData.timeStartWork,
                    endWork = route.basicData.timeEndWork,
                    offsetInMoscow = offsetInMoscow,
                    monthOfYear = monthOfYear
                )
                passengerTime += time
            }
        }
        return passengerTime
    }

    fun List<Route>.getSingleLocomotiveTime(): Long {
        var singleLocoTimeFollowing = 0L
        this.forEach { route ->
            route.trains.forEach { train ->
                singleLocoTimeFollowing += train.timeFollowingSingleLocomotive(
                    startWork = route.basicData.timeStartWork,
                    endWork = route.basicData.timeEndWork
                )
            }
        }
        return singleLocoTimeFollowing
    }

    fun List<Route>.getWorkingTimeOnAHoliday(monthOfYear: MonthOfYear, offsetInMoscow: Long): Long {
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
                        startDate = startHolidayInLong - offsetInMoscow,
                        endDate = endHolidayInLong - offsetInMoscow
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

    fun List<Route>.getWorkTimeWithoutHoliday(
        monthOfYear: MonthOfYear,
        offsetInMoscow: Long
    ): Long {
        val totalWorkTime = this.getWorkTime(monthOfYear, offsetInMoscow)
        val holidayWorkTime = this.getWorkingTimeOnAHoliday(monthOfYear, offsetInMoscow)
        return totalWorkTime - holidayWorkTime
    }

    // согласно распоряжению ОАО "РЖД" от 5 апреля 2014 г. N 859р
    fun Train.trainCategory(): String {
        return when (this.number?.toIntOrNull()) {
            in 1..150 -> "Скорые круглогодичные"
            in 151..298 -> "Скорые сезонного и разового обращения"
            in 301..450 -> "Пассажирские круглогодичные"
            in 601..698 -> "Пассажирские круглогодичные"
            in 451..598 -> "Пассажирские сезонные, разового назначения и детские"
            in 701..750 -> "Скоростные"
            in 751..788 -> "Высокоскоростные"
            in 801..898 -> "Обслуживаемые моторвагонным подвижным составовом (кроме скоростных и высокоскоростных)"
            in 901..920 -> "Служебного (специального) назначения"
            in 921..940 -> "Туристические (коммерческие)"
            in 941..960 -> "Людские"
            in 961..970 -> "Грузопассажирские"
            in 971..998 -> "Почтово-багажные"
            in 6001..6998 -> "Пригородные, приграничные пригородные (региональные) поезда"
            in 7001..7098 -> "Скоростные пригородные поезда"
            in 7101..7498 -> "Скорые пригородные и городские поезда"
            in 7501..7598 -> "Скоростные пригородные поезда"
            in 7601..7628 -> "Поезда служебного (специального) назначения"
            in 5001..5998 -> "Состоящие из цельнометаллических вагонов без пассажиров"
            in 7631..7998 -> "Состоящие из моторвагонного подвижного состава без пассажиров, в т.ч. от скоростных и высокоскоростных"
            in 1001..1020 -> "Рефрижераторные"
            in 1021..1420 -> "Контейнерные поезда"
            in 1421..1440 -> "Для перевозок груза в контрейлерах"
            in 1441..1450 -> "Специализированные для перевозки грузов в универсальном подвижном составе"
            in 1451..1460 -> "Для перевозки живности"
            in 1461..1810 -> "Для перевозки угля, руды, удобрений в кольцевых маршрутах"
            in 1811..1998 -> "Для перевозки наливных грузов в кольцевых и технологических маршрутах"
            in 9201..9298 -> "Соединенные поезда, следующие на один и более диспетчерских участков"
            in 9301..9498 -> "Для составов из порожних вагонов в количестве 350-520 осей с одним локомотивом в голове, в т.ч. на удлиненных плечах обслуживания"
            in 9501..9698 -> "Тяжеловесные поезда"
            in 9701..9750 -> "Тяжеловесные поезда, в том числе весом 8000 т"
            in 9751..9798 -> "Тяжеловесные поезда, в том числе весом 9000 т"
            in 2001..2998 -> "Сквозные, в т.ч. на удлиненных плечах обслуживания"
            in 3001..3398 -> "Участковые"
            in 3401..3468 -> "Сборные"
            in 3471..3498 -> "Сборно-участковые"
            in 3501..3598 -> "Вывозные — для уборки и подачи вагонов на отдельные промежуточные станции участка и подъездные пути"
            in 3601..3798 -> "Передаточные — для передачи вагонов с одной станции на другую"
            in 3801..3898 -> "Диспетчерские локомотивы — для уборки и подачи вагонов на промежуточные станции"
            in 3901..3978 -> "Подача вагонов рабочего парка на перегон для выгрузки в «окно» при производстве путевых работ"
            in 3981..3998 -> "Подача вагонов по перевозочным документам под погрузку или выгрузку на примыкание к главным путям на перегоне, внутристанционные передачи, подача вагонов по договорам на пути (подъездные пути) станций, закрытых для грузовых операций"
            in 4001..4148 -> "Толкачи-резервные локомотивы, следующие для подталкивания или после подталкивания грузовых поездов"
            in 4151..4188 -> "Толкачи-резервные локомотивы, следующие для подталкивания или после подталкивания пассажирских поездов"
            in 4191..4198 -> "Толкачи-резервные локомотивы, следующие для подталкивания или после подталкивания хозяйственных поездов"
            in 4201..4228 -> "Резервные локомотивы, следующие без вагонов от подталкивания грузовых поездов"
            in 4231..4258 -> "Резервные локомотивы, следующие без вагонов от подталкивания вывозных и передаточных поездов"
            in 4261..4298 -> "Резервные локомотивы, следующие без вагонов от подталкивания хозяйственных поездов"
            in 4301..4398 -> "Резервные локомотивы, следующие без вагонов от (к) пассажирских, людских, пригородных, почтово-багажных и грузопассажирских поездов"
            in 4401..4698 -> "Резервные локомотивы, следующие без вагонов от (к) поездов: ускоренных, соединенных, сквозных, участковых, сборных, сборно-участковых"
            in 4701..4778 -> "Резервные локомотивы, следующие без вагонов от (на) хозяйственных работ"
            in 4779..4798 -> "Рельсосмазыватели"
            in 4801..4898 -> "Резервные локомотивы, следующие без вагонов от (к) вывозных, маневровых и передаточных поездов"
            in 4901..4960 -> "Сплотки резервных грузовых локомотивов, находящиеся в эксплуатации"
            in 4961..4990 -> "Сплотки резервных пассажирских локомотивов, находящиеся в эксплуатации"
            in 4991..4998 -> "Сплотки резервных хозяйственных локомотивов, находящиеся в эксплуатации"
            in 8001..8048 -> "Восстановительные поезда"
            in 8051..8098 -> "Пожарные поезда"
            in 8101..8198 -> "Снегоочистители и снегоуборочная техника всех наименований"
            in 8201..8248 -> "Хозяйственные поезда, щебнеочистительные машины"
            in 8251..8298 -> "Хозяйственные поезда, выправочно-подбивочно-отделочные и рихтовочные машины"
            in 8301..8348 -> "Хозяйственные поезда, путеукладочные и путеразборочные машины"
            in 8351..8398 -> "Хозяйственные поезда, хоппер-дозаторные"
            in 8401..8448 -> "Хозяйственные поезда рельсовозные"
            in 8451..8498 -> "Хозяйственные поезда рельсошлифовальные"
            in 8501..8548 -> "Хозяйственные поезда, остальные машины и агрегаты"
            in 8551..8598 -> "Путеизмерители, дефектоскопы и вагоны-лаборатории"
            in 8601..8698 -> "Автодрезины, мотовозы и специальный самоходный подвижной состав"
            in 8701..8748 -> "Для перевозки воды по хозяйственным документам"
            in 8751..8798 -> "Для перевозки работников пути, контактной сети и т.д. к месту работы и обратно в моторвагонном или специальном самоходном подвижном составе"
            in 8801..8848 -> "Для перевозки работников пути, контактной сети и т.д. к месту работы и обратно в вагонах с локомотивной тягой"
            in 8851..8868 -> "Работа маломощных диспетчерских локомотивов на перегоне"
            in 8871..8898 -> "Работа с поездами по договорам с транспортными организациями железных дорог"
            in 8901..8928 -> "Локомотивы и сплотки локомотивов, моторвагонный подвижной состав и вагоны в ремонт и из ремонта"
            in 8931..8948 -> "Обкатка локомотивов и вагонов"
            in 8951..8988 -> "Обкатка составов из порожних пассажирских вагонов и моторвагонного подвижного состава"
            in 8991..8998 -> "Для проведения опытных поездок"
            in 9001..9098 -> "Из порожних вагонов, негодных под погрузку, следующих на заводы и в депо для ремонта и модернизации по специально оформленным документам"
            else -> "Номер не найден"
        }
    }

    fun Passenger.getTimeFollowing(
        startWork: Long?,
        endWork: Long?,
        offsetInMoscow: Long,
        monthOfYear: MonthOfYear
    ): Long {
        var timeStartFollowing: Long? = this.timeDeparture
        var timeEndFollowing: Long? = this.timeArrival
        timeStartFollowing?.let { timeStart ->
            timeEndFollowing?.let { timeEnd ->
                if (startWork != null) {
                    if (endWork != null) {
                        if (endWork < timeStart) return 0
                        if (startWork > timeEnd) return 0
                        if (startWork > timeStart) {
                            timeStartFollowing = startWork
                        }
                        if (endWork < timeEnd) {
                            timeEndFollowing = endWork
                        }
                        if (this.isTransition(offsetInMoscow)) {
                            return monthOfYear.getTimeInCurrentMonth(
                                timeStart + offsetInMoscow,
                                timeEnd + offsetInMoscow
                            )
                        }
                    }
                }
            }
        }
        return ((timeEndFollowing + offsetInMoscow) - (timeStartFollowing + offsetInMoscow)) ?: 0L
    }

    fun Train.timeFollowingSingleLocomotive(startWork: Long?, endWork: Long?): Long {
        return if (
            this.number?.toIntOrNull() in 4001..4148 ||
            this.number?.toIntOrNull() in 4151..4188 ||
            this.number?.toIntOrNull() in 4191..4198 ||
            this.number?.toIntOrNull() in 4201..4228 ||
            this.number?.toIntOrNull() in 4231..4258 ||
            this.number?.toIntOrNull() in 4261..4298 ||
            this.number?.toIntOrNull() in 4301..4398 ||
            this.number?.toIntOrNull() in 4401..4698 ||
            this.number?.toIntOrNull() in 4701..4778 ||
            this.number?.toIntOrNull() in 4801..4898
        ) {
            var timeStartFollowing: Long? = this.stations.firstOrNull()?.timeDeparture
            var timeEndFollowing: Long? = this.stations.lastOrNull()?.timeArrival
            timeStartFollowing?.let { timeStart ->
                timeEndFollowing?.let { timeEnd ->
                    if (startWork != null) {
                        if (endWork != null) {
                            if (endWork < timeStart) return 0
                            if (startWork > timeEnd) return 0
                            if (startWork > timeStart) {
                                timeStartFollowing = startWork
                            }
                            if (endWork < timeEnd) {
                                timeEndFollowing = endWork
                            }
                        }
                    }
                }
            }
            (timeEndFollowing - timeStartFollowing) ?: 0L
        } else {
            0L
        }
    }

    fun Route.getTimeInServicePhase(listDistance: List<Int>, index: Int): Long {
        val startWork = this.basicData.timeStartWork
        val endWork = this.basicData.timeEndWork
        if (startWork == null || endWork == null) {
            return 0L
        } else {
            val startInterval = listDistance[index]
            val endInterval =
                if (index + 1 < listDistance.size) listDistance[index + 1] else Int.MAX_VALUE

            val searchIntervalDistance = startInterval until endInterval
            val workInterval = startWork until endWork
            val result = this.trains.getTimeInServicePhase(
                distanceInterval = searchIntervalDistance,
                workInterval = workInterval
            )
            return result
        }
    }

    fun List<Train>.getTimeInServicePhase(
        distanceInterval: IntRange,
        workInterval: LongRange
    ): Long {
        var summaryDistance = 0
        var summaryTimeFollowing = 0L
        val trainsWithDistance = mutableListOf<Train>()
        this.forEach { train ->
            if (train.distance.toIntOrZero() > 0) {
                trainsWithDistance.add(train)
                summaryDistance += train.distance.toIntOrZero()
            }
        }
        if (distanceInterval.contains(summaryDistance)) {
            trainsWithDistance.forEach { train ->
                var timeDeparture = train.stations.firstOrNull()?.timeDeparture
                var timeArrival = train.stations.lastOrNull()?.timeArrival
                if (timeDeparture == null || timeArrival == null || timeDeparture > timeArrival) {
                    return@forEach
                } else {
                    val startWork = workInterval.first
                    val endWork = workInterval.last
                    if (endWork < timeDeparture) return@forEach
                    if (startWork > timeArrival) return@forEach
                    if (startWork > timeDeparture) {
                        timeDeparture = startWork
                    }
                    if (endWork < timeArrival) {
                        timeArrival = endWork
                    }
                    summaryTimeFollowing += timeArrival - timeDeparture
                }
            }
        }
        return summaryTimeFollowing
    }

    fun Route.getTimeInHeavyTrain(listWeight: List<Int>, index: Int): Long {
        val startInterval = listWeight[index]
        val endInterval =
            if (index + 1 < listWeight.size) listWeight[index + 1] else Int.MAX_VALUE
        val searchIntervalWeight = startInterval until endInterval
        var resultTime = 0L
        this.trains.forEach { train ->
            val weight = train.weight.toIntOrZero()
            if (searchIntervalWeight.contains(weight)) {
                var timeDeparture: Long? = train.stations.firstOrNull()?.timeDeparture
                var timeArrival: Long? = train.stations.lastOrNull()?.timeArrival
                val startWork = this.basicData.timeStartWork
                val endWork = this.basicData.timeEndWork
                if (startWork == null || endWork == null) {
                    return@forEach
                }
                if (timeDeparture == null || timeArrival == null || timeDeparture > timeArrival) {
                    return@forEach
                } else {

                    if (endWork < timeDeparture) 0L
                    if (startWork > timeArrival) 0L
                    if (startWork > timeDeparture) {
                        timeDeparture = startWork
                    }
                    if (endWork < timeArrival) {
                        timeArrival = endWork
                    }
                    resultTime += (timeArrival - timeDeparture)
                }
            }
        }

        return resultTime
    }

    private fun Train.getTimeInLongDistance(
        lengthIsLongDistance: Int,
        workInterval: LongRange
    ): Long {
        val time = if (this.conditionalLength.toIntOrZero() > lengthIsLongDistance) {
            var timeDeparture = this.stations.firstOrNull()?.timeDeparture
            var timeArrival = this.stations.lastOrNull()?.timeArrival
            if (timeDeparture == null || timeArrival == null || timeDeparture > timeArrival) {
                0L
            } else {
                val startWork = workInterval.first
                val endWork = workInterval.last
                if (endWork < timeDeparture) 0L
                if (startWork > timeArrival) 0L
                if (startWork > timeDeparture) {
                    timeDeparture = startWork
                }
                if (endWork < timeArrival) {
                    timeArrival = endWork
                }
                timeArrival - timeDeparture
            }
        } else {
            0L
        }
        return time
    }

    fun List<Route>.getOnePersonOperationTime(
        monthOfYear: MonthOfYear,
        offsetInMoscow: Long
    ): Long {
        var resultTime = 0L
        this.forEach { route ->
            if (route.basicData.isOnePersonOperation) {
                resultTime += if (route.isTransition(offsetInMoscow)) {
                    monthOfYear.getTimeInCurrentMonth(
                        route.basicData.timeStartWork!!,
                        route.basicData.timeEndWork!!
                    )
                } else {
                    route.getWorkTime() ?: 0L
                }
            }
        }
        return resultTime
    }

    fun List<Route>.getLongDistanceTime(lengthIsLongDistance: Int): Long {
        var resultTime = 0L
        this.forEach { route ->
            route.trains.forEach { train ->
                val startWork = route.basicData.timeStartWork
                val endWork = route.basicData.timeEndWork
                if (startWork != null && endWork != null) {
                    val workInterval = startWork until endWork
                    resultTime += train.getTimeInLongDistance(
                        lengthIsLongDistance = lengthIsLongDistance,
                        workInterval = workInterval
                    )
                }
            }
        }
        return resultTime
    }
}