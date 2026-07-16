package com.z_company.loco_driver.widget

import android.content.Context
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
import androidx.glance.action.clickable
import androidx.glance.appwidget.AndroidRemoteViews
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxSize
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import com.z_company.loco_driver.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Малый виджет (2×2): блок «Отработано / норма за месяц» —
 * mono-метрика, дельта и индикатор нормы. Тап открывает приложение.
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
        val context = LocalContext.current

        val data = NormData(
            workedHm = prefs[WKeys.WORKED_HM] ?: "--:--",
            normHm = prefs[WKeys.NORM_HM] ?: "",
            deltaText = prefs[WKeys.DELTA_TEXT] ?: "",
            isOvertime = prefs[WKeys.IS_OVERTIME] ?: false,
            hasNorm = prefs[WKeys.HAS_NORM] ?: false,
            barMax = prefs[WKeys.BAR_MAX] ?: 100,
            barProgress = prefs[WKeys.BAR_PROGRESS] ?: 0,
            barSecondary = prefs[WKeys.BAR_SECONDARY] ?: 0,
        )

        val rv = RemoteViews(context.packageName, R.layout.widget_small_norm).also {
            WidgetRender.populateNorm(context, it, data, metricSizeSp = 38f)
        }

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .cornerRadius(24.dp)
                .background(ImageProvider(R.drawable.widget_card_bg))
                .clickable(actionRunCallback<OpenAppActionCallback>())
        ) {
            AndroidRemoteViews(
                remoteViews = rv,
                modifier = GlanceModifier.fillMaxSize()
            )
        }
    }

    companion object {
        suspend fun updateAllSmallWidgets(context: Context, data: NormData) {
            val manager = GlanceAppWidgetManager(context)
            val glanceIds = manager.getGlanceIds(LocoDriverSmallWidget::class.java)
            glanceIds.forEach { glanceId ->
                updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
                    prefs.toMutablePreferences().apply {
                        this[WKeys.WORKED_HM] = data.workedHm
                        this[WKeys.NORM_HM] = data.normHm
                        this[WKeys.DELTA_TEXT] = data.deltaText
                        this[WKeys.IS_OVERTIME] = data.isOvertime
                        this[WKeys.HAS_NORM] = data.hasNorm
                        this[WKeys.BAR_MAX] = data.barMax
                        this[WKeys.BAR_PROGRESS] = data.barProgress
                        this[WKeys.BAR_SECONDARY] = data.barSecondary
                    }
                }
                LocoDriverSmallWidget().update(context, glanceId)
            }
        }
    }
}
