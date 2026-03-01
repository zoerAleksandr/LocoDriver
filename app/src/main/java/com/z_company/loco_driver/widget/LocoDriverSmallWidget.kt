package com.z_company.loco_driver.widget

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
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
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Compact widget showing only total work time and norm hours.
 */
class LocoDriverSmallWidget : GlanceAppWidget() {

    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        try {
            withContext(Dispatchers.IO) {
                WidgetDataLoader.loadAndPush(context)
            }
        } catch (e: Exception) {
            Log.w("SmallWidget", "Initial data load failed", e)
        }

        provideContent {
            GlanceTheme {
                SmallWidgetContent()
            }
        }
    }

    @Composable
    private fun SmallWidgetContent() {
        val prefs = currentState<Preferences>()
        val totalTimeText = prefs[Keys.TOTAL_TIME_TEXT] ?: "--:--"
        val normHours = prefs[Keys.NORM_HOURS] ?: ""

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .cornerRadius(16.dp)
                .background(Colors.background)
                .clickable(actionRunCallback<OpenAppActionCallback>())
                .padding(horizontal = 12.dp, vertical = 8.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = totalTimeText,
                        style = TextStyle(
                            color = ColorProvider(Colors.textPrimary),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    if (normHours.isNotEmpty()) {
                        Text(
                            text = " / $normHours",
                            style = TextStyle(
                                color = ColorProvider(Colors.accent),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }
                Text(
                    text = "Отработано / норма",
                    style = TextStyle(
                        color = ColorProvider(Colors.textSecondary),
                        fontSize = 10.sp
                    )
                )
            }
        }
    }

    private object Colors {
        val background = Color(0xE6333333)
        val textPrimary = Color(0xFFf0f0f0)
        val textSecondary = Color(0xFFEBE8E8)
        val accent = Color(0xFF92b2e5)
    }

    object Keys {
        val TOTAL_TIME_TEXT = stringPreferencesKey("small_total_time_text")
        val NORM_HOURS = stringPreferencesKey("small_norm_hours")
    }

    companion object {
        suspend fun updateAllSmallWidgets(
            context: Context,
            totalTimeText: String,
            normHours: String
        ) {
            val manager = GlanceAppWidgetManager(context)
            val glanceIds = manager.getGlanceIds(LocoDriverSmallWidget::class.java)
            glanceIds.forEach { glanceId ->
                updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
                    prefs.toMutablePreferences().apply {
                        this[Keys.TOTAL_TIME_TEXT] = totalTimeText
                        this[Keys.NORM_HOURS] = normHours
                    }
                }
                LocoDriverSmallWidget().update(context, glanceId)
            }
        }
    }
}
