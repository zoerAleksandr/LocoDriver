@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.z_company.iosapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z_company.core.ResultState
import com.z_company.domain.entities.route.LocoType
import com.z_company.domain.entities.route.Route
import com.z_company.domain.entities.route.Train
import com.z_company.domain.entities.route.UtilsForEntities.getBreakDuration
import com.z_company.domain.entities.route.UtilsForEntities.getWorkTime
import com.z_company.domain.entities.route.UtilsForEntities.isExtendedServicePhaseTrains
import com.z_company.domain.entities.route.UtilsForEntities.isHeavyTrains
import com.z_company.domain.entities.route.UtilsForEntities.isLongCompositionTrain
import com.z_company.domain.entities.setting.SalarySetting
import com.z_company.domain.entities.setting.UserSettings
import com.z_company.domain.use_cases.RouteUseCase
import com.z_company.domain.use_cases.SalarySettingUseCase
import com.z_company.domain.use_cases.SettingsUseCase
import com.z_company.domain.util.CalculationEnergy
import com.z_company.domain.util.TimeCalculationContext
import com.z_company.domain.util.currencySymbol
import com.z_company.domain.util.displayTimeZone
import com.z_company.domain.util.toMoneyString
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.TimeZone
import kotlin.math.abs
import kotlin.math.roundToLong
import kotlin.time.Clock

// ═════════════════════════════════════════════════════════════════════════════
// Модели быстрого просмотра (§9.1) — только готовые строки для Swift.
// ═════════════════════════════════════════════════════════════════════════════

data class QuickLabelValueUi(
    val label: String,
    val value: String,
)

/** Секция отдыха: «Домашний отдых» / «Отдых в пункте оборота · {станция}». */
data class QuickRestSectionUi(
    val kind: String, // "home" | "turnover"
    val title: String,
    val rows: List<QuickLabelValueUi>,
)

/** «Приёмка» / «Сдача»: плашки сохранённых времён. */
data class QuickTimeStopsUi(
    val title: String,
    val stops: List<QuickLabelValueUi>,
)

/** Блок расход/рекуперация секции локомотива. */
data class QuickConsumptionUi(
    val title: String,
    val totalText: String,
    /** Рекуперация — «успех» цвет. */
    val isRecovery: Boolean,
    val acceptedText: String,
    val deliveryText: String,
    val acceptedSupportingText: String,
    val deliverySupportingText: String,
)

data class QuickLocoSectionUi(
    val title: String,
    val consumptions: List<QuickConsumptionUi>,
    /** «Экипировка: …» или пусто. */
    val supplyText: String,
)

data class QuickLocoUi(
    /** «серия №номер». */
    val title: String,
    val acceptStops: QuickTimeStopsUi?,
    val deliveryStops: QuickTimeStopsUi?,
    val sections: List<QuickLocoSectionUi>,
)

data class QuickStationUi(
    /** Название; для конечной — суффикс «· конечная». */
    val nameText: String,
    val isPassing: Boolean,
    /** Проходная: «просл HH:MM»; иначе «приб – отпр». */
    val timeText: String,
    /** "none" | "warning" (>5 мин) | "danger" (>30 мин). */
    val badge: String,
    /** «перегон · путь N · примечание» перед этой станцией или пусто. */
    val segmentLine: String,
)

data class QuickTrainUi(
    /** «Поезд №N». */
    val title: String,
    /** «вес т · оси ваг · у.д.» (у.д. без дробной части, если целое). */
    val characteristicsText: String,
    /** «Вагонник: ФИО · таб. N · прицепка HH:MM» или пусто. */
    val carInspectorText: String,
    val stations: List<QuickStationUi>,
    /** Вспомогательная тяга («Толкач: …» и т.п.). */
    val assistLines: List<String>,
)

data class QuickPassengerUi(
    val title: String,
    val routeText: String,
    val timeText: String,
    val notesText: String,
)

data class RouteQuickViewUi(
    val isReady: Boolean = false,
    val routeNumberText: String = "№б/н",
    /** «первая → последняя» из графика поездов; пусто — скрыто. */
    val directionText: String = "",
    /** «Время работы» — плашка шапки. */
    val workTimeText: String = "",
    /** «Заработано» — «—» если не посчитано. */
    val paymentText: String = "—",
    val isSynchronized: Boolean = false,
    /** Явка / Перерыв / Сдача. */
    val workRows: List<QuickLabelValueUi> = emptyList(),
    val restSections: List<QuickRestSectionUi> = emptyList(),
    val locomotives: List<QuickLocoUi> = emptyList(),
    val trains: List<QuickTrainUi> = emptyList(),
    val passengers: List<QuickPassengerUi> = emptyList(),
    /** Доплаты: повышенная длина/масса/свыше 12ч/удлинённое плечо. */
    val surcharges: List<String> = emptyList(),
    val notesText: String = "",
)

// ═════════════════════════════════════════════════════════════════════════════
// ViewModel
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Быстрый просмотр маршрута (§9.1) — read-only данные для `RouteQuickViewSheet`.
 * Действия (избранное/удалить/шарить/копировать/синхронизировать) — в
 * `AllRoutesScreenIosViewModel`, этот VM только считает и форматирует.
 *
 * `homeRest` считается семантикой `calculationHomeRest` c `.collect` (не `.first`,
 * иначе берётся эмит `Loading`) — здесь suspend-расчёт публикует терминальное
 * значение (BUG-паритет §9.1).
 */
class RouteQuickViewIosViewModel(
    private val routeUseCase: RouteUseCase,
    private val settingsUseCase: SettingsUseCase,
    private val salarySettingUseCase: SalarySettingUseCase,
) : ViewModel() {

    private val _ui = MutableStateFlow(RouteQuickViewUi())

    fun watchQuickView(callback: (RouteQuickViewUi) -> Unit) {
        viewModelScope.launch { _ui.collect { callback(it) } }
    }

    /** Собирает данные быстрого просмотра по id маршрута. */
    fun show(routeId: String) {
        viewModelScope.launch {
            _ui.value = RouteQuickViewUi()
            try {
                val routeState = routeUseCase.routeDetails(routeId)
                    .first { it is ResultState.Success || it is ResultState.Error }
                val route = (routeState as? ResultState.Success)?.data ?: return@launch
                val us = settingsUseCase.getUserSettingFlow().first()
                val ss = salarySettingUseCase.salarySettingFlow().first()
                val built = withContext(Dispatchers.Default) { build(route, us, ss) }
                _ui.value = built
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _ui.value = RouteQuickViewUi()
            }
        }
    }

    /** Пересобрать для того же маршрута (после смены настроек). */
    fun refresh() {
        // Показываем то, что уже собрано; полный пересчёт — через show().
        _ui.value = _ui.value
    }

    fun dispose() {
        // Загрузка одноразовая — отменять нечего, оставлено для симметрии API.
    }

    // ═══════════════════════════════════════════════════════════════════════

    private suspend fun build(
        route: Route,
        us: UserSettings,
        ss: SalarySetting,
    ): RouteQuickViewUi = coroutineScope {
        val tz = displayTz(us)
        val isDecimal = us.isDecimalTime
        val start = route.basicData.timeStartWork
        val end = route.basicData.timeEndWork

        // ── Шапка ──
        val numberText = route.basicData.number?.takeIf { it.isNotBlank() }
            ?.let { "№$it" } ?: "№б/н"
        val sortedTrains = sortedTrains(route)
        val directionText = run {
            val first = sortedTrains.firstOrNull()?.stations
                ?.firstOrNull()?.stationName?.takeIf { it.isNotBlank() }
            val last = sortedTrains.lastOrNull()?.stations
                ?.lastOrNull()?.stationName?.takeIf { it.isNotBlank() }
            if (first != null && last != null && first != last) "$first → $last" else ""
        }
        val workTimeText = Fmt.duration(route.getWorkTime(), isDecimal)

        // ── Время работы ──
        val workRows = listOf(
            QuickLabelValueUi(
                "Явка",
                start?.let { Fmt.dateMiniTime(it, tz) } ?: "",
            ),
            QuickLabelValueUi(
                "Перерыв",
                if (route.getBreakDuration() > 0L) {
                    Fmt.duration(route.getBreakDuration(), isDecimal)
                } else "",
            ),
            QuickLabelValueUi(
                "Сдача",
                end?.let { Fmt.dateMiniTime(it, tz) } ?: "",
            ),
        )

        // ── Отдых (§9.1) ──
        val restSections = buildRestSections(route, us, tz, isDecimal)

        // ── Локомотивы (§9.1): по времени приёмки, без времени — в конце ──
        val locomotives = route.locomotives
            .sortedWith(compareBy(nullsLast<Long>()) { it.timeStartOfAcceptance })
            .map { loco ->
                val acceptStops = buildList {
                    loco.timeStartOfAcceptance?.let {
                        add(QuickLabelValueUi("начало", Fmt.dateMiniTime(it, tz)))
                    }
                    loco.timeEndOfAcceptance?.let {
                        add(QuickLabelValueUi("конец", Fmt.dateMiniTime(it, tz)))
                    }
                    loco.timeBarrierOut?.let {
                        add(QuickLabelValueUi("КП", Fmt.dateMiniTime(it, tz)))
                    }
                }
                val deliveryStops = buildList {
                    loco.timeBarrierIn?.let {
                        add(QuickLabelValueUi("КП", Fmt.dateMiniTime(it, tz)))
                    }
                    loco.timeStartOfDelivery?.let {
                        add(QuickLabelValueUi("начало", Fmt.dateMiniTime(it, tz)))
                    }
                    loco.timeEndOfDelivery?.let {
                        add(QuickLabelValueUi("конец", Fmt.dateMiniTime(it, tz)))
                    }
                }
                val sections = when (loco.type) {
                    LocoType.ELECTRIC -> loco.electricSectionList.mapIndexedNotNull { index, s ->
                        val consEnergy = CalculationEnergy.getTotalEnergyConsumption(
                            s.acceptedEnergy, s.deliveryEnergy,
                        )
                        val consRec = CalculationEnergy.getTotalEnergyConsumption(
                            s.acceptedRecovery, s.deliveryRecovery,
                        )
                        val hasEnergy = consEnergy != null ||
                            s.acceptedEnergy != null || s.deliveryEnergy != null
                        val hasRecovery = consRec != null ||
                            s.acceptedRecovery != null || s.deliveryRecovery != null
                        if (!hasEnergy && !hasRecovery) return@mapIndexedNotNull null
                        QuickLocoSectionUi(
                            title = "Секция ${index + 1}",
                            consumptions = buildList {
                                if (hasEnergy) {
                                    add(
                                        QuickConsumptionUi(
                                            title = "Расход",
                                            totalText = consEnergy?.let { "${grouped(it)} кВт·ч" } ?: "",
                                            isRecovery = false,
                                            acceptedText = s.acceptedEnergy?.let { grouped(it) } ?: "",
                                            deliveryText = s.deliveryEnergy?.let { grouped(it) } ?: "",
                                            acceptedSupportingText = "",
                                            deliverySupportingText = "",
                                        )
                                    )
                                }
                                if (hasRecovery) {
                                    add(
                                        QuickConsumptionUi(
                                            title = "Рекуперация",
                                            totalText = consRec?.let {
                                                "−" + grouped(abs(it)) + " кВт·ч"
                                            } ?: "",
                                            isRecovery = true,
                                            acceptedText = s.acceptedRecovery?.let { grouped(it) } ?: "",
                                            deliveryText = s.deliveryRecovery?.let { grouped(it) } ?: "",
                                            acceptedSupportingText = "",
                                            deliverySupportingText = "",
                                        )
                                    )
                                }
                            },
                            supplyText = "",
                        )
                    }

                    LocoType.DIESEL -> loco.dieselSectionList.mapIndexedNotNull { index, s ->
                        val accFuelKilo = s.acceptedFuel?.let { f -> s.coefficient?.let { f * it } }
                        val delFuelKilo = s.deliveryFuel?.let { f -> s.coefficient?.let { f * it } }
                        val consFuel = CalculationEnergy.getTotalFuelConsumption(
                            s.acceptedFuel, s.deliveryFuel, s.fuelSupply,
                        )
                        val consFuelKilo = CalculationEnergy.getTotalFuelInKiloConsumption(
                            acceptedInKilo = accFuelKilo,
                            deliveryInKilo = delFuelKilo,
                            refuelInKilo = s.fuelSupplyInKilo,
                        )
                        val hasFuel = consFuel != null ||
                            s.acceptedFuel != null || s.deliveryFuel != null
                        val hasRefuel = s.fuelSupply != null || s.fuelSupplyInKilo != null
                        if (!hasFuel && !hasRefuel) return@mapIndexedNotNull null
                        QuickLocoSectionUi(
                            title = "Секция ${index + 1}",
                            consumptions = if (hasFuel) {
                                listOf(
                                    QuickConsumptionUi(
                                        title = "Расход",
                                        totalText = consFuel?.let {
                                            grouped(it) + " л" +
                                                (consFuelKilo?.let { kg -> " · ${grouped(kg)} кг" } ?: "")
                                        } ?: "",
                                        isRecovery = false,
                                        acceptedText = s.acceptedFuel?.let { grouped(it) } ?: "",
                                        deliveryText = s.deliveryFuel?.let { grouped(it) } ?: "",
                                        acceptedSupportingText = accFuelKilo?.let { "${grouped(it)} кг" } ?: "",
                                        deliverySupportingText = delFuelKilo?.let { "${grouped(it)} кг" } ?: "",
                                    )
                                )
                            } else emptyList(),
                            supplyText = if (hasRefuel) {
                                val parts = buildList {
                                    s.fuelSupply?.let { add("${grouped(it)} л") }
                                    s.fuelSupplyInKilo?.let { add("${grouped(it)} кг") }
                                }
                                if (parts.isEmpty()) "" else "Экипировка: ${parts.joinToString(" · ")}"
                            } else "",
                        )
                    }
                }
                QuickLocoUi(
                    title = listOfNotNull(
                        loco.series?.takeIf { it.isNotBlank() },
                        loco.number?.takeIf { it.isNotBlank() }?.let { "№$it" },
                    ).joinToString(" "),
                    acceptStops = acceptStops.takeIf { it.isNotEmpty() }
                        ?.let { QuickTimeStopsUi("Приёмка", it) },
                    deliveryStops = deliveryStops.takeIf { it.isNotEmpty() }
                        ?.let { QuickTimeStopsUi("Сдача", it) },
                    sections = sections,
                )
            }

        // ── Поезда (§9.1) ──
        val trains = sortedTrains.map { train ->
            val weight = train.weight?.takeIf { it.isNotBlank() }?.let { "$it т" }
            val axle = train.axle?.takeIf { it.isNotBlank() }?.let { "$it ваг" }
            val ud = train.conditionalLength?.takeIf { it.isNotBlank() }?.let { "${trimZero(it)} у.д." }
            val characteristicsText = listOfNotNull(weight, axle, ud).joinToString(" · ")
            val ci = train.carInspector?.let { inspector ->
                val parts = buildList {
                    inspector.fullName?.takeIf { it.isNotBlank() }?.let { add(it) }
                    inspector.tabNumber?.takeIf { it.isNotBlank() }?.let { add("таб. $it") }
                    inspector.couplingTime?.let {
                        add("прицепка ${Fmt.time(it, tz)}")
                    }
                }
                if (parts.isEmpty()) "" else "Вагонник: ${parts.joinToString(" · ")}"
            } ?: ""
            val stations = train.stations.mapIndexed { index, st ->
                val stopMs = if (st.isPassingStation) null else {
                    val a = st.timeArrival
                    val d = st.timeDeparture
                    if (a != null && d != null && d > a) d - a else null
                }
                val badge = when {
                    stopMs == null -> "none"
                    stopMs > 30 * Fmt.HOUR / 60 -> "danger"
                    stopMs > 5 * Fmt.HOUR / 60 -> "warning"
                    else -> "none"
                }
                val nameText = buildString {
                    append(st.stationName.orEmpty())
                    if (st.isFinalStation) append(" · конечная")
                }
                val timeText = when {
                    st.isPassingStation -> st.timeArrival?.let {
                        "просл ${Fmt.time(it, tz)}"
                    } ?: ""
                    else -> {
                        val a = st.timeArrival?.let { Fmt.time(it, tz) }
                        val d = st.timeDeparture?.let { Fmt.time(it, tz) }
                        when {
                            a != null && d != null -> "$a – $d"
                            a != null -> a
                            d != null -> d
                            else -> ""
                        }
                    }
                }
                val segmentLine = if (index > 0 &&
                    (st.segmentTrackNumber?.isNotBlank() == true || st.segmentNotes?.isNotBlank() == true)
                ) {
                    buildList {
                        add("перегон")
                        st.segmentTrackNumber?.takeIf { it.isNotBlank() }?.let { add("путь $it") }
                        st.segmentNotes?.takeIf { it.isNotBlank() }?.let { add(it) }
                    }.joinToString(" · ")
                } else ""
                QuickStationUi(
                    nameText = nameText,
                    isPassing = st.isPassingStation,
                    timeText = timeText,
                    badge = badge,
                    segmentLine = segmentLine,
                )
            }
            val assistLines = buildList {
                train.pusher?.let { add(assistLine("Толкач", it.locomotiveSeries, it.locomotiveNumber)) }
                train.doubleTraction?.let {
                    add(assistLine("Двойная тяга", it.locomotiveSeries, it.locomotiveNumber))
                }
                train.doubledTrain?.let {
                    add(assistLine("Сдвоенный", it.locomotiveSeries, it.locomotiveNumber))
                }
            }
            QuickTrainUi(
                title = if (train.number.isNullOrBlank()) "Поезд" else "Поезд №${train.number}",
                characteristicsText = characteristicsText,
                carInspectorText = ci,
                stations = stations,
                assistLines = assistLines,
            )
        }

        // ── Пассажиром (§9.1) ──
        val passengers = route.passengers.map { p ->
            val routeText = listOfNotNull(
                p.stationDeparture?.takeIf { it.isNotBlank() },
                p.stationArrival?.takeIf { it.isNotBlank() },
            ).joinToString(" — ")
            val timeText = buildList {
                p.timeDeparture?.let { add(Fmt.dateMiniTime(it, tz)) }
                p.timeArrival?.let { add(Fmt.dateMiniTime(it, tz)) }
            }.joinToString(" – ")
            QuickPassengerUi(
                title = if (p.trainNumber.isNullOrBlank()) {
                    "Следование пассажиром"
                } else {
                    "Следование пассажиром · №${p.trainNumber}"
                },
                routeText = routeText,
                timeText = timeText,
                notesText = p.notes.orEmpty(),
            )
        }

        // ── Доплаты (§9.1) ──
        val surcharges = buildList {
            if (isLongCompositionTrain(ss, route, us.servicePhases)) {
                add("Поезда повышенной длины")
            }
            if (isHeavyTrains(ss, route, us.servicePhases)) {
                add("Поезда повышенной массы")
            }
            if (start != null && end != null && end > start && (end - start) > 12 * Fmt.HOUR) {
                add("Работа свыше 12-ти часов")
            }
            if (isExtendedServicePhaseTrains(ss, route)) {
                add("Удлинённое плечо обслуживания")
            }
        }

        // ── Заработок (§9.1): computeRouteTotalPayment, «—» если не посчитано ──
        val payment = async {
            try {
                val previous = routeUseCase.previousRouteForOverRestFlow(
                    route, TimeCalculationContext.from(us)
                ).first()
                computeRouteTotalPayment(route, us, ss, listOfNotNull(previous))
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                null
            }
        }

        RouteQuickViewUi(
            isReady = true,
            routeNumberText = numberText,
            directionText = directionText,
            workTimeText = workTimeText,
            paymentText = payment.await()?.let {
                it.toMoneyString(currencySymbol(us.country))
            } ?: "—",
            isSynchronized = route.basicData.isSynchronized,
            workRows = workRows,
            restSections = restSections,
            locomotives = locomotives,
            trains = trains,
            passengers = passengers,
            surcharges = surcharges,
            notesText = route.basicData.notes.orEmpty(),
        )
    }

    /**
     * Секции отдыха (§9.1): домашний / в ПО + «Фактический отдых» при следующей
     * явке. Расчёт — как `RouteActionsHelper.calculationHomeRest` /
     * `calculationActualRest` (терминальные значения, без `Loading`).
     */
    private suspend fun buildRestSections(
        route: Route,
        us: UserSettings,
        tz: TimeZone,
        isDecimal: Boolean,
    ): List<QuickRestSectionUi> {
        val endWork = route.basicData.timeEndWork ?: return emptyList()
        if (route.basicData.timeStartWork == null) return emptyList()
        val sections = mutableListOf<QuickRestSectionUi>()

        if (route.basicData.restPointOfTurnover) {
            // Отдых в ПО: короткий/полный (как calculateShortRest/calculateFullRest).
            val allRoutes = runCatching { routeUseCase.getListRoutes() }.getOrDefault(emptyList())
            val minimum = effectiveTurnaroundMinimum(route, allRoutes, us)
            val timeResult = route.getWorkTime() ?: (endWork - route.basicData.timeStartWork!!)
            var halfRest = timeResult / 2
            if (halfRest % 60_000L != 0L) halfRest += 60_000L
            val shortEffective = if (halfRest > minimum) halfRest else minimum
            val fullEffective = if (timeResult > minimum) timeResult else minimum
            val station = routeEndStation(route)
            val title = if (station.isNullOrBlank()) {
                "Отдых в пункте оборота"
            } else {
                "Отдых в пункте оборота · $station"
            }
            val rows = mutableListOf(
                QuickLabelValueUi(
                    "Короткий отдых",
                    "${Fmt.duration(shortEffective, isDecimal)} · до ${Fmt.dateMiniTime(endWork + shortEffective, tz)}",
                ),
                QuickLabelValueUi(
                    "Полный отдых",
                    "${Fmt.duration(fullEffective, isDecimal)} · до ${Fmt.dateMiniTime(endWork + fullEffective, tz)}",
                ),
            )
            actualRestRow(route, us)?.let { rows.add(it) }
            sections.add(QuickRestSectionUi("turnover", title, rows))
        } else {
            // Домашний отдых — calculationHomeRest (BUG-14-паритет: цепочка из
            // месяцев выбранного в настройках месяца и предыдущего).
            val home = calculationHomeRest(route, us) ?: return emptyList()
            val rows = mutableListOf(
                QuickLabelValueUi(
                    "Отдых",
                    "${Fmt.duration(home.first, isDecimal)} · до ${Fmt.dateMiniTime(endWork + home.first, tz)}",
                ),
                QuickLabelValueUi(
                    "Минимальный",
                    Fmt.duration(us.minTimeHomeRest, isDecimal),
                ),
            )
            actualRestRow(route, us)?.let { rows.add(it) }
            sections.add(QuickRestSectionUi("home", "Домашний отдых", rows))
        }
        return sections
    }

    private suspend fun actualRestRow(route: Route, us: UserSettings): QuickLabelValueUi? {
        val endWork = route.basicData.timeEndWork ?: return null
        val startWork = route.basicData.timeStartWork ?: return null
        val current = us.selectMonthOfYear
        val nextMonth = if (current.month < 11) {
            current.copy(month = current.month + 1, days = emptyList())
        } else {
            current.copy(year = current.year + 1, month = 0, days = emptyList())
        }
        val tzLong = us.timeZone
        val currentList = runCatching {
            routeUseCase.listRoutesByMonth(current, tzLong)
                .first { it is ResultState.Success || it is ResultState.Error }
        }.getOrNull()
        val nextList = runCatching {
            routeUseCase.listRoutesByMonth(nextMonth, tzLong)
                .first { it is ResultState.Success || it is ResultState.Error }
        }.getOrNull()
        val currentRoutes = (currentList as? ResultState.Success)?.data ?: emptyList()
        val nextRoutes = (nextList as? ResultState.Success)?.data ?: emptyList()
        val nextStartWork = (currentRoutes + nextRoutes)
            .asSequence()
            .filter { it.basicData.id != route.basicData.id }
            .mapNotNull { it.basicData.timeStartWork }
            .filter { it > startWork }
            .minOrNull() ?: return null
        if (nextStartWork <= endWork) return null
        val displayTz = runCatching { TimeZone.of(us.displayTimeZone()) }
            .getOrElse { TimeZone.of("GMT+3") }
        return QuickLabelValueUi(
            "Фактический отдых",
            "до ${Fmt.dateMiniTime(nextStartWork, displayTz)} · " +
                Fmt.duration(nextStartWork - endWork, us.isDecimalTime),
        )
    }

    /**
     * Домашний отдых — как `RouteActionsHelper.calculationHomeRest`:
     * (окончание полного, окончание минимального). BUG-14 (паритет с Android):
     * цепочка берётся из маршрутов ВЫБРАННОГО месяца и предыдущего.
     */
    private suspend fun calculationHomeRest(
        route: Route,
        us: UserSettings,
    ): Pair<Long, Long>? {
        val routeEnd = route.basicData.timeEndWork ?: return null
        if (route.basicData.timeStartWork == null) return null
        val current = us.selectMonthOfYear
        val previousMonth = if (current.month > 0) {
            current.copy(month = current.month - 1, days = emptyList())
        } else {
            current.copy(year = current.year - 1, month = 11, days = emptyList())
        }
        val tz = us.timeZone
        val (currentResult, prevResult) = coroutineScope {
            val c = async(Dispatchers.Default) {
                routeUseCase.listRoutesByMonth(current, tz)
                    .first { it is ResultState.Success || it is ResultState.Error }
            }
            val p = async(Dispatchers.Default) {
                routeUseCase.listRoutesByMonth(previousMonth, tz)
                    .first { it is ResultState.Success || it is ResultState.Error }
            }
            c.await() to p.await()
        }
        val currentList = (currentResult as? ResultState.Success)?.data ?: return null
        val prevList = (prevResult as? ResultState.Success)?.data ?: return null
        val sorted = (currentList + prevList)
            .sortedByDescending { it.basicData.timeStartWork ?: 0L }
            .toMutableList()
        val existingIndex = sorted.indexOfFirst { it.basicData.id == route.basicData.id }
        if (existingIndex != -1) {
            sorted[existingIndex] = route
        } else {
            val insertIndex = sorted.indexOfFirst {
                (it.basicData.timeStartWork ?: 0L) < (route.basicData.timeStartWork ?: 0L)
            }
            if (insertIndex == -1) sorted.add(route) else sorted.add(insertIndex, route)
        }
        val index = sorted.indexOfFirst { it.basicData.id == route.basicData.id }
        if (index == -1) return null
        val chain = mutableListOf(sorted[index])
        var nextIdx = index + 1
        while (nextIdx < sorted.size && sorted[nextIdx].basicData.restPointOfTurnover) {
            chain.add(sorted[nextIdx])
            nextIdx++
        }
        if (chain.any { it.basicData.timeStartWork == null || it.basicData.timeEndWork == null }) {
            return null
        }
        val sumWork = chain.sumOf {
            (it.basicData.timeEndWork ?: 0L) - (it.basicData.timeStartWork ?: 0L)
        }
        var sumRest = 0L
        for (i in 1 until chain.size) {
            sumRest += (chain[i - 1].basicData.timeStartWork ?: 0L) -
                (chain[i].basicData.timeEndWork ?: 0L)
        }
        val rawDuration = (sumWork.toDouble() * 2.6).toLong()
        val duration = maxOf(rawDuration - sumRest, us.minTimeHomeRest)
        return (routeEnd + duration) to (routeEnd + us.minTimeHomeRest)
    }

    /** Второй отдых в ПО подряд — минимум `minTimeRestPointOfTurnoverSecond`. */
    private fun effectiveTurnaroundMinimum(
        route: Route,
        routes: List<Route>,
        us: UserSettings,
    ): Long {
        val start = route.basicData.timeStartWork
        val isSecond = route.basicData.restPointOfTurnover && start != null && routes.asSequence()
            .filter { it.basicData.id != route.basicData.id }
            .filter { !it.basicData.isDeleted }
            .filter { (it.basicData.timeStartWork ?: Long.MAX_VALUE) < start }
            .maxByOrNull { it.basicData.timeStartWork ?: Long.MIN_VALUE }
            ?.basicData?.restPointOfTurnover == true
        return if (isSecond) us.minTimeRestPointOfTurnoverSecond else us.minTimeRestPointOfTurnover
    }

    /** Поезда в порядке §9.1: по времени отправления первой станции, без времени — в конце. */
    private fun sortedTrains(route: Route): List<Train> =
        route.trains.sortedWith(
            compareBy(nullsLast<Long>()) { it.stations.firstOrNull()?.timeDeparture }
        )

    private fun assistLine(label: String, series: String?, number: String?): String {
        val info = buildString {
            series?.takeIf { it.isNotBlank() }?.let { append(it) }
            number?.takeIf { it.isNotBlank() }?.let { append(" - $it") }
        }.trim(' ', '-')
        return if (info.isBlank()) "$label:" else "$label: $info"
    }

    private fun displayTz(us: UserSettings): TimeZone =
        runCatching { TimeZone.of(us.displayTimeZone()) }.getOrElse { TimeZone.of("GMT+3") }

    /** У.д. без дробной части, если целое (§9.1). */
    private fun trimZero(value: String): String =
        if (value.endsWith(".0")) value.dropLast(2) else value

    /** Число с группировкой разрядов тонким пробелом, без дробной части (как Android `grouped`). */
    private fun grouped(value: Double): String {
        val rounded = value
        val whole = abs(rounded).roundToLong()
        val sb = StringBuilder()
        val s = whole.toString()
        for ((i, c) in s.withIndex()) {
            if (i > 0 && (s.length - i) % 3 == 0) sb.append(' ')
            sb.append(c)
        }
        return (if (rounded < 0) "−" else "") + sb.toString()
    }
}
