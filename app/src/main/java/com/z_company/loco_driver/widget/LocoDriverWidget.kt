package com.z_company.loco_driver.widget

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.z_company.core.ResultState
import com.z_company.core.util.ConverterLongToTime
import com.z_company.core.util.DateAndTimeConverter
import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.entities.UtilForMonthOfYear.getPersonalNormaHours
import com.z_company.domain.entities.route.Route
import com.z_company.domain.entities.route.Station
import com.z_company.domain.entities.route.Train
import com.z_company.domain.entities.route.UtilsForEntities.findCurrentRoute
import com.z_company.domain.entities.route.UtilsForEntities.getWorkTime
import com.z_company.domain.entities.route.UtilsForEntities.isFuture
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

class LocoDriverWidget : GlanceAppWidget() {

    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // Load fresh data from DB before providing content
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
        val totalTimeText = prefs[Keys.TOTAL_TIME_TEXT] ?: "--:--"
        val normHours = prefs[Keys.NORM_HOURS] ?: ""
        val monthYear = prefs[Keys.MONTH_YEAR] ?: ""
        val hasCurrentRoute = prefs[Keys.HAS_CURRENT_ROUTE] ?: false
        val trainNumber = prefs[Keys.CURRENT_TRAIN_NUMBER] ?: ""
        val reportTime = prefs[Keys.REPORT_TIME] ?: ""
        val isDepartureNext = prefs[Keys.IS_DEPARTURE_NEXT] ?: true
        val routeCount = prefs[Keys.ROUTE_COUNT] ?: "0"
        val hasFutureRoute = prefs[Keys.HAS_FUTURE_ROUTE] ?: false
        val futureReportTime = prefs[Keys.FUTURE_REPORT_TIME] ?: ""
        val futureTrainNumber = prefs[Keys.FUTURE_TRAIN_NUMBER] ?: ""

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .cornerRadius(16.dp)
                .background(WidgetColors.background)
                .clickable(actionStartActivity<MainActivity>())
                .padding(12.dp)
        ) {
            Column(
                modifier = GlanceModifier.fillMaxSize()
            ) {
                // Header: app name + month/year
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        provider = ImageProvider(R.mipmap.ic_launcher),
                        contentDescription = null,
                        modifier = GlanceModifier.size(20.dp)
                    )
                    Spacer(modifier = GlanceModifier.width(6.dp))
                    Text(
                        text = "Машинист",
                        style = TextStyle(
                            color = ColorProvider(WidgetColors.textSecondary),
                            fontSize = 12.sp
                        )
                    )
                    Spacer(modifier = GlanceModifier.defaultWeight())
                    Text(
                        text = monthYear,
                        style = TextStyle(
                            color = ColorProvider(WidgetColors.textSecondary),
                            fontSize = 11.sp
                        )
                    )
                }

                Spacer(modifier = GlanceModifier.height(8.dp))

                // Main: total work time + norm hours
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = GlanceModifier.defaultWeight()
                    ) {
                        Text(
                            text = totalTimeText,
                            style = TextStyle(
                                color = ColorProvider(WidgetColors.textPrimary),
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = "Отработано",
                            style = TextStyle(
                                color = ColorProvider(WidgetColors.textSecondary),
                                fontSize = 11.sp
                            )
                        )
                    }
                    Column(
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = normHours,
                            style = TextStyle(
                                color = ColorProvider(WidgetColors.accent),
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = "норма",
                            style = TextStyle(
                                color = ColorProvider(WidgetColors.textSecondary),
                                fontSize = 11.sp
                            )
                        )
                    }
                }

                Spacer(modifier = GlanceModifier.height(8.dp))

                // Bottom: 3 states
                when {
                    hasCurrentRoute -> CurrentRouteRow(
                        trainNumber = trainNumber,
                        reportTime = reportTime,
                        isDepartureNext = isDepartureNext
                    )
                    hasFutureRoute -> FutureRouteRow(
                        futureReportTime = futureReportTime,
                        futureTrainNumber = futureTrainNumber
                    )
                    else -> Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Маршрутов: $routeCount",
                            style = TextStyle(
                                color = ColorProvider(WidgetColors.textSecondary),
                                fontSize = 12.sp
                            )
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun CurrentRouteRow(
        trainNumber: String,
        reportTime: String,
        isDepartureNext: Boolean
    ) {
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .cornerRadius(10.dp)
                .background(WidgetColors.routeBackground)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                provider = ImageProvider(
                    if (isDepartureNext) R.drawable.widget_play else R.drawable.widget_stop
                ),
                contentDescription = if (isDepartureNext) "Отправление" else "Прибытие",
                modifier = GlanceModifier
                    .size(24.dp)
                    .clickable(actionRunCallback<GoActionCallback>())
            )
            Spacer(modifier = GlanceModifier.width(6.dp))
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    text = if (trainNumber.isNotEmpty()) "Поезд $trainNumber" else "Текущий маршрут",
                    style = TextStyle(
                        color = ColorProvider(WidgetColors.textPrimary),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
                if (reportTime.isNotEmpty()) {
                    Text(
                        text = "Явка: $reportTime",
                        style = TextStyle(
                            color = ColorProvider(WidgetColors.textSecondary),
                            fontSize = 10.sp
                        )
                    )
                }
            }
        }
    }

    @Composable
    private fun FutureRouteRow(
        futureReportTime: String,
        futureTrainNumber: String
    ) {
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .cornerRadius(10.dp)
                .background(WidgetColors.routeBackground)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    text = "Ближайшая явка: $futureReportTime",
                    style = TextStyle(
                        color = ColorProvider(WidgetColors.accent),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
                if (futureTrainNumber.isNotEmpty()) {
                    Text(
                        text = "Поезд $futureTrainNumber",
                        style = TextStyle(
                            color = ColorProvider(WidgetColors.textSecondary),
                            fontSize = 10.sp
                        )
                    )
                }
            }
        }
    }

    /** Widget color palette */
    private object WidgetColors {
        val background = Color(0xE61C1B1F)
        val routeBackground = Color(0x332C2C30)
        val textPrimary = Color(0xFFFFFFFF)
        val textSecondary = Color(0xB3FFFFFF)
        val accent = Color(0xFF4FC3F7)
    }

    /** Preference keys */
    object Keys {
        val TOTAL_TIME_TEXT = stringPreferencesKey("total_time_text")
        val NORM_HOURS = stringPreferencesKey("norm_hours")
        val MONTH_YEAR = stringPreferencesKey("month_year")
        val HAS_CURRENT_ROUTE = booleanPreferencesKey("has_current_route")
        val CURRENT_TRAIN_NUMBER = stringPreferencesKey("current_train_number")
        val REPORT_TIME = stringPreferencesKey("report_time")
        val IS_DEPARTURE_NEXT = booleanPreferencesKey("is_departure_next")
        val ROUTE_COUNT = stringPreferencesKey("route_count")
        val HAS_FUTURE_ROUTE = booleanPreferencesKey("has_future_route")
        val FUTURE_REPORT_TIME = stringPreferencesKey("future_report_time")
        val FUTURE_TRAIN_NUMBER = stringPreferencesKey("future_train_number")
    }

    companion object {
        suspend fun updateAllWidgets(
            context: Context,
            totalTimeText: String,
            normHours: String,
            monthYear: String,
            hasCurrentRoute: Boolean,
            currentTrainNumber: String,
            reportTime: String,
            isDepartureNext: Boolean,
            routeCount: String,
            hasFutureRoute: Boolean,
            futureReportTime: String,
            futureTrainNumber: String
        ) {
            val manager = GlanceAppWidgetManager(context)
            val glanceIds = manager.getGlanceIds(LocoDriverWidget::class.java)
            glanceIds.forEach { glanceId ->
                updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
                    prefs.toMutablePreferences().apply {
                        this[Keys.TOTAL_TIME_TEXT] = totalTimeText
                        this[Keys.NORM_HOURS] = normHours
                        this[Keys.MONTH_YEAR] = monthYear
                        this[Keys.HAS_CURRENT_ROUTE] = hasCurrentRoute
                        this[Keys.CURRENT_TRAIN_NUMBER] = currentTrainNumber
                        this[Keys.REPORT_TIME] = reportTime
                        this[Keys.IS_DEPARTURE_NEXT] = isDepartureNext
                        this[Keys.ROUTE_COUNT] = routeCount
                        this[Keys.HAS_FUTURE_ROUTE] = hasFutureRoute
                        this[Keys.FUTURE_REPORT_TIME] = futureReportTime
                        this[Keys.FUTURE_TRAIN_NUMBER] = futureTrainNumber
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
        val timeZoneText = settingsUseCase.getTimeZone(userSettings.timeZone)
        val currentTimeInMillis = Calendar.getInstance(
            TimeZone.getTimeZone(timeZoneText)
        ).timeInMillis

        // Get current month
        val monthOfYear: MonthOfYear? = userSettings.selectMonthOfYear

        // Month label
        val monthYear = if (monthOfYear != null) {
            val monthNames = arrayOf(
                "Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
                "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"
            )
            "${monthNames.getOrElse(monthOfYear.month - 1) { "" }} ${monthOfYear.year}"
        } else ""

        // Load routes for current month
        val allRoutes = routeUseCase.getListRoutes()
        val routesForMonth = if (monthOfYear != null) {
            allRoutes.filter { route ->
                val startWork = route.basicData.timeStartWork ?: return@filter false
                val cal = Calendar.getInstance(TimeZone.getTimeZone(timeZoneText)).apply {
                    timeInMillis = startWork
                }
                val routeMonth = cal.get(Calendar.MONTH) // 0-based
                val routeYear = cal.get(Calendar.YEAR)
                routeMonth == monthOfYear.month - 1 && routeYear == monthOfYear.year
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
        val totalTimeText = ConverterLongToTime.getTimeInStringFormat(totalTimeMillis)

        // Individual norm hours
        val normHours = if (monthOfYear != null) {
            "${monthOfYear.getPersonalNormaHours()}ч"
        } else ""

        // Current route
        val currentRoute = allRoutes.findCurrentRoute(
            currentTimeInMillis = currentTimeInMillis,
            userSettings = userSettings
        )
        val hasCurrentRoute = currentRoute != null
        val trainNumber = currentRoute?.trains?.lastOrNull()?.number ?: ""

        // isDepartureNext
        val isDepartureNext = if (hasCurrentRoute) {
            nextIsDeparture(currentRoute?.trains?.lastOrNull())
        } else true

        // Report time
        val dateAndTimeConverter = DateAndTimeConverter(userSettings)
        val reportTime = if (hasCurrentRoute) {
            dateAndTimeConverter.getDateMiniAndTime(currentRoute?.basicData?.timeStartWork)
        } else ""

        // Future route
        val futureRoute = allRoutes
            .filter { it.isFuture(userSettings.timeZone) }
            .minByOrNull { it.basicData.timeStartWork ?: Long.MAX_VALUE }
        val hasFutureRoute = futureRoute != null
        val futureReportTime = if (hasFutureRoute) {
            dateAndTimeConverter.getDateMiniAndTime(futureRoute?.basicData?.timeStartWork)
        } else ""
        val futureTrainNumber = futureRoute?.trains?.lastOrNull()?.number ?: ""

        LocoDriverWidget.updateAllWidgets(
            context = context,
            totalTimeText = totalTimeText,
            normHours = normHours,
            monthYear = monthYear,
            hasCurrentRoute = hasCurrentRoute,
            currentTrainNumber = trainNumber,
            reportTime = reportTime,
            isDepartureNext = isDepartureNext,
            routeCount = filteredRouteList.size.toString(),
            hasFutureRoute = hasFutureRoute,
            futureReportTime = futureReportTime,
            futureTrainNumber = futureTrainNumber
        )
    }

    /** Determine if next action is departure (same logic as HomeViewModel) */
    private fun nextIsDeparture(train: Train?): Boolean {
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
 * ActionCallback for play/stop button — executes onGoClicked logic
 * directly from the widget without opening the Activity.
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
        // 1. Get user settings for timezone
        val userSettings = settingsUseCase.getUserSetting()
        val timeZoneText = settingsUseCase.getTimeZone(userSettings.timeZone)

        // 2. Get current time (truncated to minutes)
        val now = Calendar.getInstance(TimeZone.getTimeZone(timeZoneText)).apply {
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        // 3. Find current route
        val allRoutes = routeUseCase.getListRoutes()
        val currentTimeInMillis = Calendar.getInstance(
            TimeZone.getTimeZone(timeZoneText)
        ).timeInMillis
        val currentRoute = allRoutes.findCurrentRoute(
            currentTimeInMillis = currentTimeInMillis,
            userSettings = userSettings
        ) ?: return

        // 4. Get the last train
        val current = currentRoute.trains.lastOrNull()

        // 5. Build updated train (same logic as HomeViewModel.onGoClicked)
        val updatedTrain = if (current == null) {
            Train(
                basicId = currentRoute.basicData.id,
                stations = mutableListOf(Station(timeDeparture = now))
            )
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
            current.copy(stations = stations)
        }

        // 6. Save to DB
        trainUseCase.updateTrain(updatedTrain).first { it is ResultState.Success }

        // 7. Reload all widget data (recalculates isDepartureNext etc.)
        WidgetDataLoader.loadAndPush(context)
    }
}
