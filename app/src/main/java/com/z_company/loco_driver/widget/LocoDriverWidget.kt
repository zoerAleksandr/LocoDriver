package com.z_company.loco_driver.widget

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.util.Log
import android.view.View
import android.widget.RemoteViews
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
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.AndroidRemoteViews
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.LocalContext
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
        val hasCurrentRoute = prefs[Keys.HAS_CURRENT_ROUTE] ?: false
        val isDepartureNext = prefs[Keys.IS_DEPARTURE_NEXT] ?: true
        val stateInfoLine1 = prefs[Keys.STATE_INFO_LINE1] ?: ""
        val stateInfoLine2 = prefs[Keys.STATE_INFO_LINE2] ?: ""
        val stateInfoLine3 = prefs[Keys.STATE_INFO_LINE3] ?: ""
        val stateInfoLine4 = prefs[Keys.STATE_INFO_LINE4] ?: ""
        val stateInfoLine5 = prefs[Keys.STATE_INFO_LINE5] ?: ""
        val nextReportText = prefs[Keys.NEXT_REPORT_TEXT] ?: ""
        val normRemainingText = prefs[Keys.NORM_REMAINING_TEXT] ?: ""
        val isOvertime = prefs[Keys.IS_OVERTIME] ?: false
        val trainNumberText = prefs[Keys.TRAIN_NUMBER_TEXT] ?: ""
        val statusText = prefs[Keys.STATUS_TEXT] ?: ""
        val statusTimeText = prefs[Keys.STATUS_TIME_TEXT] ?: ""

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .cornerRadius(16.dp)
                .background(ImageProvider(R.drawable.widget_background_gradient))
                .clickable(actionRunCallback<OpenAppActionCallback>())
                .padding(12.dp)
        ) {
            Column(
                modifier = GlanceModifier.fillMaxSize()
            ) {
                // ─── Top: work time / norm (left) + remaining (right) ───
                AutoSizedTopRow(
                    totalTimeText = totalTimeText,
                    normHours = normHours,
                    normRemainingText = normRemainingText,
                    isOvertime = isOvertime
                )

                // ─── Bottom: different layout for current route vs rest ───
                if (hasCurrentRoute) {
                    // ─── Current route: action bar centered, report text at bottom ───

                    // Top spacer — pushes action bar to center
                    Spacer(modifier = GlanceModifier.defaultWeight())

                    // Wide action button (centered vertically)
                    Row(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .cornerRadius(12.dp)
                            .background(WidgetColors.addButtonBackground)
                            .clickable(actionRunCallback<GoActionCallback>())
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Play/stop icon on the left
                        Image(
                            provider = ImageProvider(
                                if (isDepartureNext) R.drawable.widget_play else R.drawable.widget_stop
                            ),
                            contentDescription = if (isDepartureNext) "Отправление" else "Прибытие",
                            modifier = GlanceModifier.size(36.dp)
                        )

                        // Action text centered in remaining space
                        Column(
                            modifier = GlanceModifier.defaultWeight(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            val actionText = if (isDepartureNext)
                                "Сохранить время отправления"
                            else
                                "Сохранить время прибытия"
                            Text(
                                text = actionText,
                                style = TextStyle(
                                    color = ColorProvider(WidgetColors.addButtonText),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                maxLines = 1
                            )

                            // Detail line: "п. №3390 стоянка с 00:47"
                            val detailParts = listOf(trainNumberText, statusText, statusTimeText)
                                .filter { it.isNotEmpty() }
                            val detailText = detailParts.joinToString(" ")
                            if (detailText.isNotEmpty()) {
                                Text(
                                    text = detailText,
                                    style = TextStyle(
                                        color = ColorProvider(WidgetColors.addButtonText),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Normal
                                    ),
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    // Bottom spacer — pushes report text to bottom
                    Spacer(modifier = GlanceModifier.defaultWeight())

                    // "Текущая явка 01.03 08:00" at the very bottom
                    if (stateInfoLine1.isNotEmpty()) {
                        Text(
                            text = stateInfoLine1,
                            style = TextStyle(
                                color = ColorProvider(WidgetColors.accent),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            maxLines = 1
                        )
                    }
                } else {
                    // ─── No current route: rest info (left) + add button (right) ───
                    Spacer(modifier = GlanceModifier.defaultWeight())
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        // Left: state info lines + next report
                        Column(
                            modifier = GlanceModifier.defaultWeight().padding(end = 8.dp)
                        ) {
                            if (stateInfoLine1.isNotEmpty()) {
                                Text(
                                    text = stateInfoLine1,
                                    style = TextStyle(
                                        color = ColorProvider(WidgetColors.accent),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    maxLines = 1
                                )
                            }
                            // Spacer between rest type and rest values
                            if (stateInfoLine1.isNotEmpty() && stateInfoLine2.isNotEmpty()) {
                                Spacer(modifier = GlanceModifier.height(6.dp))
                            }
                            if (stateInfoLine2.isNotEmpty()) {
                                Text(
                                    text = stateInfoLine2,
                                    style = TextStyle(
                                        color = ColorProvider(WidgetColors.textPrimary),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Normal
                                    ),
                                    maxLines = 1
                                )
                            }
                            if (stateInfoLine3.isNotEmpty()) {
                                Text(
                                    text = stateInfoLine3,
                                    style = TextStyle(
                                        color = ColorProvider(WidgetColors.textPrimary),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Normal
                                    ),
                                    maxLines = 1
                                )
                            }
                            // Spacer between short and full rest
                            if (stateInfoLine3.isNotEmpty() && stateInfoLine4.isNotEmpty()) {
                                Spacer(modifier = GlanceModifier.height(6.dp))
                            }
                            if (stateInfoLine4.isNotEmpty()) {
                                Text(
                                    text = stateInfoLine4,
                                    style = TextStyle(
                                        color = ColorProvider(WidgetColors.textPrimary),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Normal
                                    ),
                                    maxLines = 1
                                )
                            }
                            if (stateInfoLine5.isNotEmpty()) {
                                Text(
                                    text = stateInfoLine5,
                                    style = TextStyle(
                                        color = ColorProvider(WidgetColors.textPrimary),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Normal
                                    ),
                                    maxLines = 1
                                )
                            }
                            if (nextReportText.isNotEmpty()) {
                                Spacer(modifier = GlanceModifier.height(8.dp))
                                Text(
                                    text = nextReportText,
                                    style = TextStyle(
                                        color = ColorProvider(WidgetColors.accent),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    maxLines = 1
                                )
                            }
                        }

                        // Right: add route button
                        Box(
                            modifier = GlanceModifier
                                .width(90.dp)
                                .cornerRadius(12.dp)
                                .background(WidgetColors.addButtonBackground)
                                .clickable(actionRunCallback<AddRouteActionCallback>())
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Image(
                                    provider = ImageProvider(R.drawable.widget_add_dark),
                                    contentDescription = "Добавить маршрут",
                                    modifier = GlanceModifier.size(40.dp)
                                )
                                Spacer(modifier = GlanceModifier.height(2.dp))
                                Text(
                                    text = "Добавить",
                                    style = TextStyle(
                                        color = ColorProvider(WidgetColors.addButtonText),
                                        fontSize = 10.sp
                                    )
                                )
                                Text(
                                    text = "маршрут",
                                    style = TextStyle(
                                        color = ColorProvider(WidgetColors.addButtonText),
                                        fontSize = 10.sp
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    /** Auto-sized top row using AndroidRemoteViews with autoSizeTextType */
    @Composable
    private fun AutoSizedTopRow(
        totalTimeText: String,
        normHours: String,
        normRemainingText: String,
        isOvertime: Boolean
    ) {
        val context = LocalContext.current
        val remoteViews = RemoteViews(context.packageName, R.layout.widget_autosize_top_row).apply {
            // Build spannable for time + norm
            setTextViewText(R.id.time_norm_text, buildTimeNormSpan(totalTimeText, normHours))

            // Remaining / overtime
            if (normRemainingText.isNotEmpty()) {
                val remainingColor = if (isOvertime) 0xFFeb9e9e.toInt() else 0xFF92b2e5.toInt()
                setTextViewText(R.id.remaining_text, normRemainingText)
                setTextColor(R.id.remaining_text, remainingColor)

                val labelText = if (isOvertime) "переработка" else "до нормы"
                setTextViewText(R.id.remaining_label, labelText)

                setViewVisibility(R.id.remaining_container, View.VISIBLE)
            } else {
                setViewVisibility(R.id.remaining_container, View.GONE)
            }
        }

        AndroidRemoteViews(
            remoteViews = remoteViews,
            modifier = GlanceModifier.fillMaxWidth().height(78.dp)
        )
    }

    /** Build SpannableString: "164:30" (white, bold, 44sp) + " / 176ч" (accent, 22sp) */
    private fun buildTimeNormSpan(totalTimeText: String, normHours: String): CharSequence {
        val builder = SpannableStringBuilder()
        builder.append(totalTimeText)
        builder.setSpan(
            ForegroundColorSpan(0xFFf0f0f0.toInt()),
            0, totalTimeText.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        builder.setSpan(
            StyleSpan(Typeface.BOLD),
            0, totalTimeText.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        if (normHours.isNotEmpty()) {
            val normStart = builder.length
            builder.append(" / $normHours")
            builder.setSpan(
                ForegroundColorSpan(0xFF92b2e5.toInt()),
                normStart, builder.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            builder.setSpan(
                RelativeSizeSpan(0.5f),
                normStart, builder.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        return builder
    }

    /** Widget color palette — matches app dark theme (Color.kt) */
    private object WidgetColors {
        val background = Color(0xE6333333)          // DarkBackground with 90% alpha
        val buttonBackground = Color(0x33363636)    // DarkSecondaryContainer with 20% alpha
        val textPrimary = Color(0xFFf0f0f0)         // DarkPrimary
        val textSecondary = Color(0xFFEBE8E8)       // DarkOnSurface
        val accent = Color(0xFF92b2e5)              // DarkTertiary
        val overtimeColor = Color(0xFFeb9e9e)       // DarkError — for overtime indicator
        val addButtonBackground = Color(0xDDf0f0f0) // Light background for "Добавить" button
        val addButtonText = Color(0xFF333333)        // Dark text for "Добавить" button
    }

    /** Preference keys */
    object Keys {
        val TOTAL_TIME_TEXT = stringPreferencesKey("total_time_text")
        val NORM_HOURS = stringPreferencesKey("norm_hours")
        val MONTH_YEAR = stringPreferencesKey("month_year")
        val HAS_CURRENT_ROUTE = booleanPreferencesKey("has_current_route")
        val REPORT_TIME = stringPreferencesKey("report_time")
        val IS_DEPARTURE_NEXT = booleanPreferencesKey("is_departure_next")
        val LAST_ACTION_TEXT = stringPreferencesKey("last_action_text")
        val STATE_INFO_LINE1 = stringPreferencesKey("state_info_line1")
        val STATE_INFO_LINE2 = stringPreferencesKey("state_info_line2")
        val STATE_INFO_LINE3 = stringPreferencesKey("state_info_line3")
        val STATE_INFO_LINE4 = stringPreferencesKey("state_info_line4")
        val STATE_INFO_LINE5 = stringPreferencesKey("state_info_line5")
        val NEXT_REPORT_TEXT = stringPreferencesKey("next_report_text")
        val NORM_REMAINING_TEXT = stringPreferencesKey("norm_remaining_text")
        val IS_OVERTIME = booleanPreferencesKey("is_overtime")
        val TRAIN_NUMBER_TEXT = stringPreferencesKey("train_number_text")
        val STATUS_TEXT = stringPreferencesKey("status_text")
        val STATUS_TIME_TEXT = stringPreferencesKey("status_time_text")
    }

    companion object {
        suspend fun updateAllWidgets(
            context: Context,
            totalTimeText: String,
            normHours: String,
            monthYear: String,
            hasCurrentRoute: Boolean,
            reportTime: String,
            isDepartureNext: Boolean,
            lastActionText: String,
            stateInfoLine1: String,
            stateInfoLine2: String,
            stateInfoLine3: String,
            stateInfoLine4: String,
            stateInfoLine5: String,
            nextReportText: String,
            normRemainingText: String,
            isOvertime: Boolean,
            trainNumberText: String,
            statusText: String,
            statusTimeText: String
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
                        this[Keys.REPORT_TIME] = reportTime
                        this[Keys.IS_DEPARTURE_NEXT] = isDepartureNext
                        this[Keys.LAST_ACTION_TEXT] = lastActionText
                        this[Keys.STATE_INFO_LINE1] = stateInfoLine1
                        this[Keys.STATE_INFO_LINE2] = stateInfoLine2
                        this[Keys.STATE_INFO_LINE3] = stateInfoLine3
                        this[Keys.STATE_INFO_LINE4] = stateInfoLine4
                        this[Keys.STATE_INFO_LINE5] = stateInfoLine5
                        this[Keys.NEXT_REPORT_TEXT] = nextReportText
                        this[Keys.NORM_REMAINING_TEXT] = normRemainingText
                        this[Keys.IS_OVERTIME] = isOvertime
                        this[Keys.TRAIN_NUMBER_TEXT] = trainNumberText
                        this[Keys.STATUS_TEXT] = statusText
                        this[Keys.STATUS_TIME_TEXT] = statusTimeText
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

        // Determine current month from system clock (not from persisted selectMonthOfYear)
        val cal = Calendar.getInstance(TimeZone.getTimeZone(timeZoneText))
        val currentMonth = cal.get(Calendar.MONTH) + 1 // 1-based
        val currentYear = cal.get(Calendar.YEAR)
        val allMonths = calendarUseCase.loadMonthOfYearList()
        val monthOfYear: MonthOfYear? = allMonths.find {
            it.month == currentMonth && it.year == currentYear
        }

        // Month label
        val monthYear = if (monthOfYear != null) {
            val monthNames = arrayOf(
                "Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
                "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"
            )
            "${monthNames.getOrElse(monthOfYear.month - 1) { "" }} ${monthOfYear.year}"
        } else ""

        // Load all routes
        val allRoutes = routeUseCase.getListRoutes()

        // Filter routes for current month (include transitional routes)
        val routesForMonth = if (monthOfYear != null) {
            allRoutes.filter { route ->
                val startWork = route.basicData.timeStartWork ?: return@filter false
                val startCal = Calendar.getInstance(TimeZone.getTimeZone(timeZoneText)).apply {
                    timeInMillis = startWork
                }
                val startInMonth = startCal.get(Calendar.MONTH) == monthOfYear.month - 1
                    && startCal.get(Calendar.YEAR) == monthOfYear.year

                val endWork = route.basicData.timeEndWork
                val endInMonth = if (endWork != null) {
                    val endCal = Calendar.getInstance(TimeZone.getTimeZone(timeZoneText)).apply {
                        timeInMillis = endWork
                    }
                    endCal.get(Calendar.MONTH) == monthOfYear.month - 1
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

        // isDepartureNext
        val isDepartureNext = if (hasCurrentRoute) {
            nextIsDeparture(currentRoute?.trains?.lastOrNull())
        } else true

        // Report time
        val dateAndTimeConverter = DateAndTimeConverter(userSettings)
        val reportTime = if (hasCurrentRoute) {
            dateAndTimeConverter.getDateMiniAndTime(currentRoute?.basicData?.timeStartWork)
        } else ""

        // Button info (train number, status, time — under play/stop button)
        val buttonInfo = computeButtonInfo(currentRoute, dateAndTimeConverter)

        // Norm remaining / overtime
        val normHoursInt = monthOfYear?.getPersonalNormaHours() ?: 0
        val normMillis = normHoursInt.toLong() * 3_600_000L
        val diff = totalTimeMillis - normMillis
        val isOvertime = diff >= 0
        val remainingMillis = if (isOvertime) diff else -diff
        val normRemainingText = if (normHoursInt > 0) {
            ConverterLongToTime.getTimeInStringFormat(remainingMillis)
        } else ""

        // State info lines (middle section)
        val stateInfo = computeStateInfo(
            hasCurrentRoute = hasCurrentRoute,
            currentRoute = currentRoute,
            allRoutes = allRoutes,
            currentTimeInMillis = currentTimeInMillis,
            userSettings = userSettings,
            dateAndTimeConverter = dateAndTimeConverter
        )

        // Next report text
        val futureRoute = allRoutes.findNextFutureRoute(currentTimeInMillis)
        val nextReportText = if (futureRoute != null) {
            "След. явка ${dateAndTimeConverter.getDateMiniAndTime(futureRoute.basicData.timeStartWork)}"
        } else "След. явка неизвестна"

        LocoDriverWidget.updateAllWidgets(
            context = context,
            totalTimeText = totalTimeText,
            normHours = normHours,
            monthYear = monthYear,
            hasCurrentRoute = hasCurrentRoute,
            reportTime = reportTime,
            isDepartureNext = isDepartureNext,
            lastActionText = "",
            stateInfoLine1 = stateInfo.line1,
            stateInfoLine2 = stateInfo.line2,
            stateInfoLine3 = stateInfo.line3,
            stateInfoLine4 = stateInfo.line4,
            stateInfoLine5 = stateInfo.line5,
            nextReportText = nextReportText,
            normRemainingText = normRemainingText,
            isOvertime = isOvertime,
            trainNumberText = buttonInfo.trainNumber,
            statusText = buttonInfo.statusText,
            statusTimeText = buttonInfo.statusTime
        )

        // Update small widget
        LocoDriverSmallWidget.updateAllSmallWidgets(
            context = context,
            totalTimeText = totalTimeText,
            normHours = normHours
        )
    }

    /** Button info: train number, status text, status time */
    data class ButtonInfo(
        val trainNumber: String,  // "п. №3" or ""
        val statusText: String,   // "В пути" / "Стоянка" or ""
        val statusTime: String    // "с 13:45" or ""
    )

    /** Compute button info from last train: train number, status, time */
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
                    statusText = "В пути",
                    statusTime = "с ${dateAndTimeConverter.getTime(s.timeDeparture)}"
                )
            }
            if (s.timeArrival != null) {
                return ButtonInfo(
                    trainNumber = trainNumber,
                    statusText = "Стоянка",
                    statusTime = "с ${dateAndTimeConverter.getTime(s.timeArrival)}"
                )
            }
        }
        return ButtonInfo(trainNumber, "", "")
    }

    private data class StateInfo(
        val line1: String,
        val line2: String,
        val line3: String,
        val line4: String = "",
        val line5: String = ""
    )

    /** Compute state info: report time / rest info / empty */
    private fun computeStateInfo(
        hasCurrentRoute: Boolean,
        currentRoute: Route?,
        allRoutes: List<Route>,
        currentTimeInMillis: Long,
        userSettings: com.z_company.domain.entities.setting.UserSettings,
        dateAndTimeConverter: DateAndTimeConverter
    ): StateInfo {
        // State 1: Current route — show report time
        if (hasCurrentRoute && currentRoute != null) {
            val reportText =
                "Текущая явка ${dateAndTimeConverter.getDateMiniAndTime(currentRoute.basicData.timeStartWork)}"
            return StateInfo(reportText, "", "")
        }

        // State 2: No current route — find previous completed route
        val previousRoute = allRoutes
            .filter {
                it.basicData.timeEndWork != null &&
                    it.basicData.timeEndWork!! < currentTimeInMillis &&
                    it.basicData.timeStartWork != null
            }
            .maxByOrNull { it.basicData.timeEndWork ?: 0L }

        if (previousRoute != null) {
            val startWork = previousRoute.basicData.timeStartWork!!
            val endWork = previousRoute.basicData.timeEndWork!!
            val workTime = endWork - startWork

            if (previousRoute.basicData.restPointOfTurnover) {
                // Turnaround rest
                val minTime = userSettings.minTimeRestPointOfTurnover
                val shortRest = maxOf(workTime / 2, minTime)
                val fullRest = maxOf(workTime, minTime)
                val shortDuration =
                    "Короткий ${ConverterLongToTime.formatDurationFromMillis(shortRest)}"
                val shortEnd =
                    "до ${dateAndTimeConverter.getDateMiniAndTime(endWork + shortRest)}"
                val fullDuration =
                    "Полный ${ConverterLongToTime.formatDurationFromMillis(fullRest)}"
                val fullEnd =
                    "до ${dateAndTimeConverter.getDateMiniAndTime(endWork + fullRest)}"
                return StateInfo("Отдых в ПО", shortDuration, shortEnd, fullDuration, fullEnd)
            } else {
                // Home rest (simplified — single route, no chain)
                val rawDuration = (workTime.toDouble() * 2.6).toLong()
                val duration = maxOf(rawDuration, userSettings.minTimeHomeRest)
                val endRestTime = endWork + duration
                val durationLine =
                    "Продлится ${ConverterLongToTime.formatDurationFromMillis(duration)}"
                val endLine = "До ${dateAndTimeConverter.getDateMiniAndTime(endRestTime)}"
                return StateInfo("Домашний отдых", durationLine, endLine)
            }
        }

        // State 3: No routes at all
        return StateInfo("", "", "")
    }

    /** Determine if next action is departure (same logic as HomeViewModel) */
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

        if (current == null) {
            // 5a. No train exists — create a new one
            val newTrain = Train(
                basicId = currentRoute.basicData.id,
                stations = mutableListOf(Station(timeDeparture = now))
            )
            trainUseCase.saveTrain(newTrain).first { it is ResultState.Success }
        } else {
            // 5b. Build updated train (same logic as HomeViewModel.onGoClicked)
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

        // 6. Update only button-related fields (isDepartureNext, trainNumber, status, statusTime).
        //    Do NOT recalculate norm/totalTime to avoid race condition with HomeViewModel
        //    which will push the full recalculated data shortly after.
        val updatedRoutes = routeUseCase.getListRoutes()
        val updatedRoute = updatedRoutes.findCurrentRoute(
            currentTimeInMillis = currentTimeInMillis,
            userSettings = userSettings
        )
        val updatedTrain = updatedRoute?.trains?.lastOrNull()
        val newIsDeparture = WidgetDataLoader.nextIsDeparture(updatedTrain)

        val dateAndTimeConverter = DateAndTimeConverter(userSettings)
        val buttonInfo = WidgetDataLoader.computeButtonInfo(updatedRoute, dateAndTimeConverter)

        // Update only button prefs
        val manager = GlanceAppWidgetManager(context)
        val glanceIds = manager.getGlanceIds(LocoDriverWidget::class.java)
        glanceIds.forEach { id ->
            updateAppWidgetState(context, PreferencesGlanceStateDefinition, id) { prefs ->
                prefs.toMutablePreferences().apply {
                    this[LocoDriverWidget.Keys.IS_DEPARTURE_NEXT] = newIsDeparture
                    this[LocoDriverWidget.Keys.TRAIN_NUMBER_TEXT] = buttonInfo.trainNumber
                    this[LocoDriverWidget.Keys.STATUS_TEXT] = buttonInfo.statusText
                    this[LocoDriverWidget.Keys.STATUS_TIME_TEXT] = buttonInfo.statusTime
                }
            }
            LocoDriverWidget().update(context, id)
        }
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
 * Ensures navigation always goes to Home, even if FormScreen was previously open.
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
