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
import com.z_company.domain.entities.route.Route
import com.z_company.domain.entities.route.Station
import com.z_company.domain.entities.route.Train
import com.z_company.domain.entities.route.UtilsForEntities.findCurrentRoute
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
        val normPercent = prefs[Keys.NORM_PERCENT] ?: "0%"
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

                // Main: total work time + norm percent
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
                            text = normPercent,
                            style = TextStyle(
                                color = ColorProvider(WidgetColors.accent),
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = "нормы",
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
        val NORM_PERCENT = stringPreferencesKey("norm_percent")
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
            normPercent: String,
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
                        this[Keys.NORM_PERCENT] = normPercent
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
                executeGoClicked(context, glanceId)
            }
        } catch (e: Exception) {
            Log.w("GoActionCallback", "Widget go clicked failed", e)
        }
    }

    private suspend fun executeGoClicked(context: Context, glanceId: GlanceId) {
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

        // 7. Refresh widget
        LocoDriverWidget().update(context, glanceId)
    }
}
