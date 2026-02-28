package com.z_company.loco_driver.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
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
import com.z_company.loco_driver.MainActivity
import com.z_company.loco_driver.R

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
        val currentWorkTime = prefs[Keys.CURRENT_WORK_TIME] ?: "00:00"
        val isDepartureNext = prefs[Keys.IS_DEPARTURE_NEXT] ?: true
        val routeCount = prefs[Keys.ROUTE_COUNT] ?: "0"

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(R.drawable.widget_background)
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
                            color = ColorProvider(R.color.widget_text_secondary),
                            fontSize = 12.sp
                        )
                    )
                    Spacer(modifier = GlanceModifier.defaultWeight())
                    Text(
                        text = monthYear,
                        style = TextStyle(
                            color = ColorProvider(R.color.widget_text_secondary),
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
                                color = ColorProvider(R.color.widget_text_primary),
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = "Отработано",
                            style = TextStyle(
                                color = ColorProvider(R.color.widget_text_secondary),
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
                                color = ColorProvider(R.color.widget_accent),
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = "нормы",
                            style = TextStyle(
                                color = ColorProvider(R.color.widget_text_secondary),
                                fontSize = 11.sp
                            )
                        )
                    }
                }

                Spacer(modifier = GlanceModifier.height(8.dp))

                // Bottom: current route or route count
                if (hasCurrentRoute) {
                    CurrentRouteRow(
                        trainNumber = trainNumber,
                        workTime = currentWorkTime,
                        isDepartureNext = isDepartureNext
                    )
                } else {
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Маршрутов: $routeCount",
                            style = TextStyle(
                                color = ColorProvider(R.color.widget_text_secondary),
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
        workTime: String,
        isDepartureNext: Boolean
    ) {
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(R.drawable.widget_route_background)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                provider = ImageProvider(
                    if (isDepartureNext) R.drawable.widget_play else R.drawable.widget_stop
                ),
                contentDescription = if (isDepartureNext) "Отправление" else "Прибытие",
                modifier = GlanceModifier
                    .size(18.dp)
                    .clickable(actionRunCallback<GoActionCallback>())
            )
            Spacer(modifier = GlanceModifier.width(6.dp))
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    text = if (trainNumber.isNotEmpty()) "Поезд $trainNumber" else "Текущий маршрут",
                    style = TextStyle(
                        color = ColorProvider(R.color.widget_text_primary),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
            Text(
                text = workTime,
                style = TextStyle(
                    color = ColorProvider(R.color.widget_accent),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }

    /** Preference keys used by the widget state (via updateAppWidgetState). */
    object Keys {
        val TOTAL_TIME_TEXT = stringPreferencesKey("total_time_text")
        val NORM_PERCENT = stringPreferencesKey("norm_percent")
        val MONTH_YEAR = stringPreferencesKey("month_year")
        val HAS_CURRENT_ROUTE = booleanPreferencesKey("has_current_route")
        val CURRENT_TRAIN_NUMBER = stringPreferencesKey("current_train_number")
        val CURRENT_WORK_TIME = stringPreferencesKey("current_work_time")
        val IS_DEPARTURE_NEXT = booleanPreferencesKey("is_departure_next")
        val ROUTE_COUNT = stringPreferencesKey("route_count")
    }

    companion object {
        /**
         * Update all widget instances with new data from HomeViewModel.
         */
        suspend fun updateAllWidgets(
            context: Context,
            totalTimeText: String,
            normPercent: String,
            monthYear: String,
            hasCurrentRoute: Boolean,
            currentTrainNumber: String,
            currentWorkTime: String,
            isDepartureNext: Boolean,
            routeCount: String
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
                        this[Keys.CURRENT_WORK_TIME] = currentWorkTime
                        this[Keys.IS_DEPARTURE_NEXT] = isDepartureNext
                        this[Keys.ROUTE_COUNT] = routeCount
                    }
                }
                LocoDriverWidget().update(context, glanceId)
            }
        }
    }
}

/**
 * ActionCallback for play/stop button in widget.
 * Opens the app so the user can use the full play/stop functionality.
 */
class GoActionCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val intent = Intent(context, LocoDriverWidgetReceiver::class.java).apply {
            action = LocoDriverWidgetReceiver.ACTION_GO_CLICKED
        }
        context.sendBroadcast(intent)
    }
}
