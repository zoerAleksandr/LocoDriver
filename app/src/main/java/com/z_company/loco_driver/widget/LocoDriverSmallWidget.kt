package com.z_company.loco_driver.widget

import android.content.Context
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.util.Log
import android.widget.RemoteViews
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
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
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import com.z_company.loco_driver.R
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

        val context = LocalContext.current
        val remoteViews = RemoteViews(context.packageName, R.layout.widget_small_autosize).apply {
            setTextViewText(R.id.time_norm_text, buildTimeNormSpan(totalTimeText, normHours))
        }

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .cornerRadius(16.dp)
                .background(ImageProvider(R.drawable.widget_background_gradient))
                .clickable(actionRunCallback<OpenAppActionCallback>())
                .padding(horizontal = 12.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            AndroidRemoteViews(
                remoteViews = remoteViews,
                modifier = GlanceModifier.fillMaxSize()
            )
        }
    }

    /** Build SpannableString: "164:30" (white, bold, 32sp) + " / 176ч" (accent, 16sp) */
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
