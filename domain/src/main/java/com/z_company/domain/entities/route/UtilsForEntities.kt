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

    fun Train.timeFollowingSingleLocomotive(): Long {
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
            val timeStartFollowing: Long? = this.stations.firstOrNull()?.timeDeparture
            val timeEndFollowing: Long? = this.stations.lastOrNull()?.timeArrival
            (timeEndFollowing - timeStartFollowing) ?: 0L
        } else {
            0L
        }
    }

    fun Route.getTimeInServicePhase(listDistance: List<Int>, index: Int): Long {
        val startInterval = listDistance[index]
        val endInterval =
            if (index + 1 < listDistance.size) listDistance[index + 1] else Int.MAX_VALUE
        val searchIntervalDistance = startInterval until endInterval
        var distance = 0
        this.trains.forEach {
            distance += it.distance?.toIntOrNull() ?: 0
        }
        return if (searchIntervalDistance.contains(distance)) {
            this.getWorkTime() ?: 0L
        } else {
            0L
        }
    }

    fun Route.isHeavyLongDistanceTrain(): Boolean {
        this.trains.forEach {
            if (it.isHeavyLongDistance) return true
        }
        return false
    }

    fun Train.getTimeInHeavyLongDistance(): Long {
        return if (this.isHeavyLongDistance) {
            val timeStartFollowing: Long? = this.stations.firstOrNull()?.timeDeparture
            val timeEndFollowing: Long? = this.stations.lastOrNull()?.timeArrival
            (timeEndFollowing - timeStartFollowing) ?: 0L
        } else {
            0L
        }
    }
}