package com.z_company.loco_driver.widget

import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.AndroidRemoteViews
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.layout.wrapContentHeight
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import com.z_company.core.ResultState
import com.z_company.core.util.ConverterLongToTime
import com.z_company.core.util.DateAndTimeConverter
import com.z_company.core.util.TimeManager
import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.entities.UtilForMonthOfYear.getPersonalNormaHours
import com.z_company.domain.entities.route.Route
import com.z_company.domain.entities.route.Station
import com.z_company.domain.entities.route.Train
import com.z_company.domain.entities.route.UtilsForEntities.findCurrentRoute
import com.z_company.domain.entities.route.UtilsForEntities.getWorkTime
import com.z_company.domain.entities.route.UtilsForEntities.findNextFutureRoute
import com.z_company.domain.use_cases.CalendarUseCase
import com.z_company.domain.use_cases.RouteUseCase
import com.z_company.domain.use_cases.SettingsUseCase
import com.z_company.domain.use_cases.TrainUseCase
import com.z_company.loco_driver.MainActivity
import com.z_company.loco_driver.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.Calendar
import java.util.TimeZone
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Развёрнутый виджет (4×4): блок нормы за месяц + текущий маршрут с быстрой
 * записью прибытия/отправления, либо (без активного маршрута) — состояние отдыха
 * и кнопка добавления маршрута, как на главном экране.
 * Дизайн — design/src/widgets.jsx. Тема адаптивная (values / values-night).
 */
class LocoDriverWidget : GlanceAppWidget() {

    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        try {
            withContext(Dispatchers.IO) {
                WidgetDataLoader.loadAndPush(context)
            }
        } catch (e: Exception) {
            Log.w("LocoDriverWidget", "Initial data load failed", e)
        }

        provideContent {
            GlanceTheme {
                WidgetContent()
            }
        }
    }

    @Composable
    private fun WidgetContent() {
        val prefs = currentState<Preferences>()
        val context = LocalContext.current

        val norm = NormData(
            workedHm = prefs[WKeys.WORKED_HM] ?: "--:--",
            normHm = prefs[WKeys.NORM_HM] ?: "",
            deltaText = prefs[WKeys.DELTA_TEXT] ?: "",
            isOvertime = prefs[WKeys.IS_OVERTIME] ?: false,
            hasNorm = prefs[WKeys.HAS_NORM] ?: false,
            barMax = prefs[WKeys.BAR_MAX] ?: 100,
            barProgress = prefs[WKeys.BAR_PROGRESS] ?: 0,
            barSecondary = prefs[WKeys.BAR_SECONDARY] ?: 0,
        )
        val state = prefs[WKeys.STATE] ?: "none"

        val normRv = RemoteViews(context.packageName, R.layout.widget_norm_block).also {
            WidgetRender.populateNorm(context, it, norm, metricSizeSp = 44f)
        }

        // Карточка обёртывает контент по высоте (как в референсе — компактная
        // карточка на обоях), а не заливает всю выделенную область виджета.
        // Свободное место виджета остаётся прозрачным.
        Box(
            modifier = GlanceModifier
                .fillMaxWidth()
                .wrapContentHeight()
                .cornerRadius(24.dp)
                .background(ImageProvider(R.drawable.widget_card_bg))
                .clickable(actionRunCallback<OpenAppActionCallback>())
                .padding(18.dp)
        ) {
            Column(modifier = GlanceModifier.fillMaxWidth().wrapContentHeight()) {
                AndroidRemoteViews(
                    remoteViews = normRv,
                    modifier = GlanceModifier.fillMaxWidth().wrapContentHeight()
                )

                if (state != "none") {
                    // Разделитель под блоком нормы
                    Spacer(modifier = GlanceModifier.height(14.dp))
                    Box(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(ImageProvider(R.drawable.widget_divider))
                    ) {}
                    Spacer(modifier = GlanceModifier.height(14.dp))

                    when (state) {
                        "current" -> RouteSection(context, prefs)
                        "upcoming", "upcoming_unknown" -> UpcomingSection(context, prefs)
                        "rest" -> RestPoSection(context, prefs)
                        "home_rest" -> HomeRestSection(context, prefs)
                    }
                }
            }
        }
    }

    /** Нижняя секция с активным маршрутом: хедер + две кнопки записи. */
    @Composable
    private fun RouteSection(context: Context, prefs: Preferences) {
        val headerRv = RemoteViews(context.packageName, R.layout.widget_route_header).also {
            WidgetRender.populateRouteHeader(
                rv = it,
                routeNumber = prefs[WKeys.ROUTE_NUMBER] ?: "",
                from = prefs[WKeys.STATION_FROM] ?: "",
                to = prefs[WKeys.STATION_TO] ?: "",
                trainLine = prefs[WKeys.TRAIN_LINE] ?: "",
            )
        }
        val isDepartureNext = prefs[WKeys.IS_DEPARTURE_NEXT] ?: true
        val doneTime = prefs[WKeys.STAMP_DONE_TIME] ?: ""

        AndroidRemoteViews(
            remoteViews = headerRv,
            modifier = GlanceModifier.fillMaxWidth().wrapContentHeight()
        )

        Spacer(modifier = GlanceModifier.height(12.dp))

        Row(modifier = GlanceModifier.fillMaxWidth()) {
            // Прибытие — активна, если следующее действие = прибытие
            val arrivalActive = !isDepartureNext
            val arrivalRv = WidgetRender.stampButton(
                context, isActive = arrivalActive, isDeparture = false, doneTime = doneTime
            )
            val arrivalMod = GlanceModifier.defaultWeight().let {
                if (arrivalActive) it.clickable(actionRunCallback<GoActionCallback>()) else it
            }
            Box(modifier = arrivalMod) {
                AndroidRemoteViews(arrivalRv, GlanceModifier.fillMaxWidth().wrapContentHeight())
            }

            Spacer(modifier = GlanceModifier.width(8.dp))

            // Отправление — активна, если следующее действие = отправление
            val departureActive = isDepartureNext
            val departureRv = WidgetRender.stampButton(
                context, isActive = departureActive, isDeparture = true, doneTime = doneTime
            )
            val departureMod = GlanceModifier.defaultWeight().let {
                if (departureActive) it.clickable(actionRunCallback<GoActionCallback>()) else it
            }
            Box(modifier = departureMod) {
                AndroidRemoteViews(departureRv, GlanceModifier.fillMaxWidth().wrapContentHeight())
            }
        }
    }

    /** Состояние «Следующий маршрут». */
    @Composable
    private fun UpcomingSection(context: Context, prefs: Preferences) {
        val rv = WidgetRender.buildUpcoming(
            context = context,
            number = prefs[WKeys.UP_NUMBER] ?: "",
            time = prefs[WKeys.UP_TIME] ?: "",
            date = prefs[WKeys.UP_DATE] ?: "",
            from = prefs[WKeys.UP_FROM] ?: "",
            to = prefs[WKeys.UP_TO] ?: "",
            known = (prefs[WKeys.STATE] ?: "") == "upcoming",
        )
        AndroidRemoteViews(rv, GlanceModifier.fillMaxWidth().wrapContentHeight())
    }

    /** Состояние «Отдых в ПО». */
    @Composable
    private fun RestPoSection(context: Context, prefs: Preferences) {
        val rv = WidgetRender.buildRestPo(
            context = context,
            shortDur = prefs[WKeys.REST_SHORT_DUR] ?: "",
            shortEnd = prefs[WKeys.REST_SHORT_END] ?: "",
            fullDur = prefs[WKeys.REST_FULL_DUR] ?: "",
            fullEnd = prefs[WKeys.REST_FULL_END] ?: "",
        )
        AndroidRemoteViews(rv, GlanceModifier.fillMaxWidth().wrapContentHeight())
    }

    /** Состояние «Домашний отдых» + кнопка «Добавить маршрут». */
    @Composable
    private fun HomeRestSection(context: Context, prefs: Preferences) {
        val rv = WidgetRender.buildHomeRest(
            context = context,
            dur = prefs[WKeys.HR_DUR] ?: "",
            end = prefs[WKeys.HR_END] ?: "",
        )
        AndroidRemoteViews(rv, GlanceModifier.fillMaxWidth().wrapContentHeight())

        Spacer(modifier = GlanceModifier.height(10.dp))

        val addBtnRv = RemoteViews(context.packageName, R.layout.widget_add_button_wide)
        Box(
            modifier = GlanceModifier
                .fillMaxWidth()
                .cornerRadius(14.dp)
                .background(ImageProvider(R.drawable.widget_accent_btn_bg))
                .clickable(actionRunCallback<AddRouteActionCallback>())
        ) {
            AndroidRemoteViews(addBtnRv, GlanceModifier.fillMaxWidth().wrapContentHeight())
        }
    }

    companion object {
        suspend fun updateAllWidgets(
            context: Context,
            norm: NormData,
            state: String,
            routeNumber: String,
            stationFrom: String,
            stationTo: String,
            trainLine: String,
            isDepartureNext: Boolean,
            stampDoneTime: String,
            upTime: String, upDate: String, upNumber: String,
            upFrom: String, upTo: String,
            restShortDur: String, restShortEnd: String,
            restFullDur: String, restFullEnd: String,
            hrDur: String, hrEnd: String,
        ) {
            val manager = GlanceAppWidgetManager(context)
            val glanceIds = manager.getGlanceIds(LocoDriverWidget::class.java)
            glanceIds.forEach { glanceId ->
                updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
                    prefs.toMutablePreferences().apply {
                        this[WKeys.WORKED_HM] = norm.workedHm
                        this[WKeys.NORM_HM] = norm.normHm
                        this[WKeys.DELTA_TEXT] = norm.deltaText
                        this[WKeys.IS_OVERTIME] = norm.isOvertime
                        this[WKeys.HAS_NORM] = norm.hasNorm
                        this[WKeys.BAR_MAX] = norm.barMax
                        this[WKeys.BAR_PROGRESS] = norm.barProgress
                        this[WKeys.BAR_SECONDARY] = norm.barSecondary
                        this[WKeys.STATE] = state
                        this[WKeys.ROUTE_NUMBER] = routeNumber
                        this[WKeys.STATION_FROM] = stationFrom
                        this[WKeys.STATION_TO] = stationTo
                        this[WKeys.TRAIN_LINE] = trainLine
                        this[WKeys.IS_DEPARTURE_NEXT] = isDepartureNext
                        this[WKeys.STAMP_DONE_TIME] = stampDoneTime
                        this[WKeys.UP_TIME] = upTime
                        this[WKeys.UP_DATE] = upDate
                        this[WKeys.UP_NUMBER] = upNumber
                        this[WKeys.UP_FROM] = upFrom
                        this[WKeys.UP_TO] = upTo
                        this[WKeys.REST_SHORT_DUR] = restShortDur
                        this[WKeys.REST_SHORT_END] = restShortEnd
                        this[WKeys.REST_FULL_DUR] = restFullDur
                        this[WKeys.REST_FULL_END] = restFullEnd
                        this[WKeys.HR_DUR] = hrDur
                        this[WKeys.HR_END] = hrEnd
                    }
                }
                LocoDriverWidget().update(context, glanceId)
            }
        }
    }
}

/**
 * Loads widget data directly from DB via Koin.
 * Called from provideGlance (widget first placed / system update)
 * and after GoActionCallback to refresh all data.
 */
object WidgetDataLoader : KoinComponent {

    private val routeUseCase: RouteUseCase by inject()
    private val settingsUseCase: SettingsUseCase by inject()
    private val calendarUseCase: CalendarUseCase by inject()

    suspend fun loadAndPush(context: Context) {
        val userSettings = settingsUseCase.getUserSetting()
        val currentTimeInMillis = Calendar.getInstance(
            TimeZone.getTimeZone("GMT+3")
        ).timeInMillis

        // Determine current month from system clock (Moscow time, UTC+3).
        val cal = Calendar.getInstance(TimeZone.getTimeZone("GMT+3"))
        val currentMonth = cal.get(Calendar.MONTH) // 0-based, как MonthOfYear.month
        val currentYear = cal.get(Calendar.YEAR)
        val monthOfYear: MonthOfYear? = userSettings.selectMonthOfYear.let {
            if (it.month == currentMonth && it.year == currentYear) it else {
                calendarUseCase.loadMonthOfYearList().find { m ->
                    m.month == currentMonth && m.year == currentYear
                }
            }
        }

        // Load all routes
        val allRoutes = routeUseCase.getListRoutes()

        // Filter routes for current month (include transitional routes)
        val routesForMonth = if (monthOfYear != null) {
            allRoutes.filter { route ->
                val startWork = route.basicData.timeStartWork ?: return@filter false
                val startCal = Calendar.getInstance(TimeZone.getTimeZone("GMT+3")).apply {
                    timeInMillis = startWork
                }
                val startInMonth = startCal.get(Calendar.MONTH) == monthOfYear.month
                    && startCal.get(Calendar.YEAR) == monthOfYear.year

                val endWork = route.basicData.timeEndWork
                val endInMonth = if (endWork != null) {
                    val endCal = Calendar.getInstance(TimeZone.getTimeZone("GMT+3")).apply {
                        timeInMillis = endWork
                    }
                    endCal.get(Calendar.MONTH) == monthOfYear.month
                        && endCal.get(Calendar.YEAR) == monthOfYear.year
                } else false

                startInMonth || endInMonth
            }
        } else allRoutes

        val filteredRouteList = if (userSettings.isConsiderFutureRoute) {
            routesForMonth
        } else {
            routesForMonth.filter { (it.basicData.timeStartWork ?: 0L) < currentTimeInMillis }
        }

        // Total work time
        val totalTimeMillis = if (monthOfYear != null) {
            filteredRouteList.getWorkTime(monthOfYear, userSettings.timeZone)
        } else 0L

        // Norm
        val normHoursInt = monthOfYear?.getPersonalNormaHours() ?: 0
        val normMillis = normHoursInt.toLong() * 3_600_000L
        val hasNorm = normHoursInt > 0
        val diff = totalTimeMillis - normMillis
        val isOvertime = diff >= 0
        // Переработка — со знаком «+»; остаток до нормы — без знака.
        val deltaText = (if (isOvertime) "+" else "") +
            ConverterLongToTime.getTimeInStringFormat(abs(diff))

        val workedMin = (totalTimeMillis / 60_000L).toInt()
        val normMin = (normMillis / 60_000L).toInt()
        val norm = NormData(
            workedHm = ConverterLongToTime.getTimeInStringFormat(totalTimeMillis),
            normHm = ConverterLongToTime.getTimeInStringFormat(normMillis),
            deltaText = deltaText,
            isOvertime = isOvertime,
            hasNorm = hasNorm,
            barMax = if (hasNorm) max(workedMin, normMin) else 0,
            barProgress = if (hasNorm) min(workedMin, normMin) else 0,
            barSecondary = if (hasNorm) workedMin else 0,
        )

        // Current route
        val currentRoute = allRoutes.findCurrentRoute(
            currentTimeInMillis = currentTimeInMillis,
            userSettings = userSettings
        )
        val hasCurrentRoute = currentRoute != null
        val lastTrain = currentRoute?.trains?.lastOrNull()

        val isDepartureNext = if (hasCurrentRoute) nextIsDeparture(lastTrain) else true

        val dateAndTimeConverter = DateAndTimeConverter(userSettings)
        val buttonInfo = computeButtonInfo(currentRoute, dateAndTimeConverter)

        // Route header fields
        val routeNumber = lastTrain?.number?.takeIf { it.isNotBlank() }?.let { "№$it" } ?: ""
        val stationFrom = lastTrain?.stations?.firstOrNull()?.stationName?.trim().orEmpty()
        val stationTo = if ((lastTrain?.stations?.size ?: 0) > 1) {
            lastTrain?.stations?.lastOrNull()?.stationName?.trim().orEmpty()
        } else ""
        // Подпись над кнопками: статус текущего поезда, капсом.
        val trainLine = listOf(buttonInfo.statusText, buttonInfo.rawTime)
            .filter { it.isNotEmpty() }
            .joinToString(" ")
            .uppercase()

        // ─── Состояние нижнего блока (как на главном экране) ───
        val state: String
        var upTime = ""; var upDate = ""; var upNumber = ""
        var upFrom = ""; var upTo = ""
        var restShortDur = ""; var restShortEnd = ""
        var restFullDur = ""; var restFullEnd = ""
        var hrDur = ""; var hrEnd = ""

        if (hasCurrentRoute) {
            state = "current"
        } else {
            val rh = computeRestHome(
                allRoutes, currentTimeInMillis, userSettings, dateAndTimeConverter
            )
            val futureRoute = allRoutes.findNextFutureRoute(currentTimeInMillis)
            when {
                // Отдых в ПО приоритетнее «Следующего маршрута».
                rh != null && rh.isPO -> {
                    state = "rest"
                    restShortDur = rh.shortDur; restShortEnd = rh.shortEnd
                    restFullDur = rh.fullDur; restFullEnd = rh.fullEnd
                }
                // Иначе (в т.ч. при домашнем отдыхе) — следующий маршрут, если он есть.
                futureRoute != null -> {
                    upTime = dateAndTimeConverter.getTime(futureRoute.basicData.timeStartWork)
                    upDate = dateAndTimeConverter.getDate(futureRoute.basicData.timeStartWork)
                    upNumber = futureRoute.basicData.number?.takeIf { it.isNotBlank() }
                        ?.let { "№$it" } ?: ""
                    upFrom = futureRoute.trains.firstOrNull()?.stations?.firstOrNull()
                        ?.stationName?.trim().orEmpty()
                    upTo = futureRoute.trains.lastOrNull()?.stations?.lastOrNull()
                        ?.stationName?.trim().orEmpty()
                    state = if (upFrom.isNotEmpty() || upTo.isNotEmpty()) "upcoming"
                        else "upcoming_unknown"
                }
                // Домашний отдых — когда следующего маршрута нет.
                rh != null -> {
                    state = "home_rest"
                    hrDur = rh.hrDur; hrEnd = rh.hrEnd
                }
                else -> state = "none"
            }
        }

        LocoDriverWidget.updateAllWidgets(
            context = context,
            norm = norm,
            state = state,
            routeNumber = routeNumber,
            stationFrom = stationFrom,
            stationTo = stationTo,
            trainLine = trainLine,
            isDepartureNext = isDepartureNext,
            stampDoneTime = buttonInfo.rawTime,
            upTime = upTime, upDate = upDate, upNumber = upNumber,
            upFrom = upFrom, upTo = upTo,
            restShortDur = restShortDur, restShortEnd = restShortEnd,
            restFullDur = restFullDur, restFullEnd = restFullEnd,
            hrDur = hrDur, hrEnd = hrEnd,
        )

        // Update small widget (только блок нормы)
        LocoDriverSmallWidget.updateAllSmallWidgets(context = context, data = norm)
    }

    /** Button info: train number, status text, status time (raw HH:MM). */
    data class ButtonInfo(
        val trainNumber: String,  // "п. №3" or ""
        val statusText: String,   // "В пути" / "Стоянка" or ""
        val rawTime: String,      // "13:45" or ""
    )

    /** Compute button info from last train: status + time of last recorded event. */
    fun computeButtonInfo(
        currentRoute: Route?,
        dateAndTimeConverter: DateAndTimeConverter
    ): ButtonInfo {
        val lastTrain = currentRoute?.trains?.lastOrNull()
            ?: return ButtonInfo("", "", "")
        val stations = lastTrain.stations
        if (stations.isEmpty()) return ButtonInfo("", "", "")

        val trainNumber = lastTrain.number?.let { "п. №$it" } ?: ""

        val hasServicePhase = lastTrain.servicePhase != null
        val endIdx = if (hasServicePhase && stations.size >= 2)
            stations.lastIndex - 1 else stations.lastIndex

        for (i in endIdx downTo 0) {
            val s = stations[i]
            if (s.timeDeparture != null) {
                return ButtonInfo(
                    trainNumber = trainNumber,
                    statusText = "В пути с",
                    rawTime = dateAndTimeConverter.getTime(s.timeDeparture)
                )
            }
            if (s.timeArrival != null) {
                return ButtonInfo(
                    trainNumber = trainNumber,
                    statusText = "Стоянка с",
                    rawTime = dateAndTimeConverter.getTime(s.timeArrival)
                )
            }
        }
        return ButtonInfo(trainNumber, "", "")
    }

    private data class RestHome(
        val isPO: Boolean,
        val shortDur: String = "", val shortEnd: String = "",
        val fullDur: String = "", val fullEnd: String = "",
        val hrDur: String = "", val hrEnd: String = "",
    )

    /**
     * Рассчитывает состояние отдыха по предыдущему завершённому маршруту:
     * отдых в ПО (короткий/полный) либо домашний отдых. null — нет данных.
     */
    private fun computeRestHome(
        allRoutes: List<Route>,
        currentTimeInMillis: Long,
        userSettings: com.z_company.domain.entities.setting.UserSettings,
        dtc: DateAndTimeConverter
    ): RestHome? {
        val previousRoute = allRoutes
            .filter {
                it.basicData.timeEndWork != null &&
                    it.basicData.timeEndWork!! < currentTimeInMillis &&
                    it.basicData.timeStartWork != null
            }
            .maxByOrNull { it.basicData.timeEndWork ?: 0L }
            ?: return null

        val startWork = previousRoute.basicData.timeStartWork!!
        val endWork = previousRoute.basicData.timeEndWork!!
        val workTime = endWork - startWork

        if (previousRoute.basicData.restPointOfTurnover) {
            val minTime = userSettings.minTimeRestPointOfTurnover
            val shortRest = maxOf(workTime / 2, minTime)
            val fullRest = maxOf(workTime, minTime)
            return RestHome(
                isPO = true,
                shortDur = ConverterLongToTime.getTimeInStringFormat(shortRest),
                shortEnd = "до ${dtc.getDateMiniAndTime(endWork + shortRest)}",
                fullDur = ConverterLongToTime.getTimeInStringFormat(fullRest),
                fullEnd = "до ${dtc.getDateMiniAndTime(endWork + fullRest)}",
            )
        } else {
            val sorted = allRoutes
                .filter { it.basicData.timeStartWork != null && it.basicData.timeEndWork != null }
                .sortedByDescending { it.basicData.timeStartWork!! }

            val idx = sorted.indexOfFirst { it.basicData.id == previousRoute.basicData.id }
            val chain = mutableListOf(previousRoute)

            if (idx != -1) {
                var nextIdx = idx + 1
                while (nextIdx < sorted.size) {
                    if (sorted[nextIdx].basicData.restPointOfTurnover == true) {
                        chain.add(sorted[nextIdx])
                        nextIdx++
                    } else break
                }
            }

            val sumWork = chain.sumOf { it.basicData.timeEndWork!! - it.basicData.timeStartWork!! }
            var sumRest = 0L
            for (i in 1 until chain.size) {
                sumRest += chain[i - 1].basicData.timeStartWork!! - chain[i].basicData.timeEndWork!!
            }

            val rawDuration = (sumWork.toDouble() * 2.6).toLong()
            val duration = maxOf(rawDuration - sumRest, userSettings.minTimeHomeRest)
            val endRestTime = endWork + duration
            return RestHome(
                isPO = false,
                hrDur = ConverterLongToTime.getTimeInStringFormat(duration),
                hrEnd = dtc.getDateMiniAndTime(endRestTime),
            )
        }
    }

    /** Determine if next action is departure (same logic as HomeViewModel). */
    fun nextIsDeparture(train: Train?): Boolean {
        if (train == null) return true
        val stations = train.stations
        if (stations.isEmpty()) return true

        val hasServicePhase = train.servicePhase != null
        val endIdx = if (hasServicePhase && stations.size >= 2)
            stations.lastIndex - 1 else stations.lastIndex

        for (i in endIdx downTo 0) {
            val s = stations[i]
            if (s.timeDeparture != null) return false
            if (s.timeArrival != null) return true
        }
        return true
    }
}

/**
 * ActionCallback for the active quick-stamp button — records the next event
 * (departure or arrival) directly from the widget, then refreshes.
 */
class GoActionCallback : ActionCallback, KoinComponent {

    private val trainUseCase: TrainUseCase by inject()
    private val routeUseCase: RouteUseCase by inject()
    private val settingsUseCase: SettingsUseCase by inject()

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        try {
            withContext(Dispatchers.IO) {
                executeGoClicked(context)
            }
        } catch (e: Exception) {
            Log.w("GoActionCallback", "Widget go clicked failed", e)
        }
    }

    private suspend fun executeGoClicked(context: Context) {
        val userSettings = settingsUseCase.getUserSetting()
        val now = TimeManager().nowExact()

        val allRoutes = routeUseCase.getListRoutes()
        val currentTimeInMillis = Calendar.getInstance(
            TimeZone.getTimeZone("GMT+3")
        ).timeInMillis
        val currentRoute = allRoutes.findCurrentRoute(
            currentTimeInMillis = currentTimeInMillis,
            userSettings = userSettings
        ) ?: return

        val current = currentRoute.trains.lastOrNull()

        if (current == null) {
            val newTrain = Train(
                basicId = currentRoute.basicData.id,
                stations = mutableListOf(Station(timeDeparture = now))
            )
            trainUseCase.saveTrain(newTrain).first { it is ResultState.Success }
        } else {
            val stations = current.stations.toMutableList()
            if (stations.isEmpty()) {
                stations.add(Station(timeDeparture = now))
            } else {
                val hasServicePhase = current.servicePhase != null
                val endIdx = if (hasServicePhase && stations.size >= 2)
                    stations.lastIndex - 1 else stations.lastIndex

                var filled = false
                for (i in endIdx downTo 0) {
                    val s = stations[i]
                    if (s.timeDeparture != null) {
                        val nextIdx = i + 1
                        val isServicePhaseArrival =
                            hasServicePhase && nextIdx == stations.lastIndex
                        if (nextIdx <= stations.lastIndex && !isServicePhaseArrival) {
                            stations[nextIdx] = stations[nextIdx].copy(timeArrival = now)
                        } else {
                            if (hasServicePhase && stations.size >= 2) {
                                stations.add(stations.lastIndex, Station(timeArrival = now))
                            } else {
                                stations.add(Station(timeArrival = now))
                            }
                        }
                        filled = true
                        break
                    }
                    if (s.timeArrival != null) {
                        stations[i] = s.copy(timeDeparture = now)
                        filled = true
                        break
                    }
                }
                if (!filled) {
                    stations[0] = stations[0].copy(timeDeparture = now)
                }
            }
            val updatedTrain = current.copy(stations = stations)
            trainUseCase.updateTrain(updatedTrain).first { it is ResultState.Success }
        }

        // Refresh all widget data (norm остаётся прежней — пересчёт быстрый и без гонки,
        // т.к. запись коснулась только станций текущего рейса).
        WidgetDataLoader.loadAndPush(context)
    }
}

/**
 * ActionCallback for "Add route" button — opens MainActivity with FormScreen.
 */
class AddRouteActionCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra("widget_add_route", true)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        context.startActivity(intent)
    }
}

/**
 * ActionCallback for tapping widget body — opens MainActivity at HomeScreen.
 */
class OpenAppActionCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra("widget_open_home", true)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        context.startActivity(intent)
    }
}
