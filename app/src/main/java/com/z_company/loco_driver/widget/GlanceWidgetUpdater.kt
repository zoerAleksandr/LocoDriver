package com.z_company.loco_driver.widget

import android.content.Context
import com.z_company.core.widget.WidgetUpdater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Glance-based implementation of [WidgetUpdater].
 *
 * Приложение (HomeViewModel) вызывает [update] после изменения данных.
 * Единый источник данных виджета — [WidgetDataLoader.loadAndPush], который
 * заново читает актуальное состояние из БД и раскладывает его по обоим
 * виджетам (малый + развёрнутый). Поэтому переданные строки игнорируются —
 * это исключает расхождение форматов между приложением и виджетом.
 */
class GlanceWidgetUpdater(
    private val context: Context
) : WidgetUpdater {

    override suspend fun update(
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
        try {
            withContext(Dispatchers.IO) {
                WidgetDataLoader.loadAndPush(context)
            }
        } catch (e: Exception) {
            android.util.Log.w("GlanceWidgetUpdater", "Widget update failed", e)
        }
    }
}
