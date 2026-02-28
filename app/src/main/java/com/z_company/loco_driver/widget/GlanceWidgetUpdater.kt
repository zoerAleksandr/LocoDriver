package com.z_company.loco_driver.widget

import android.content.Context
import com.z_company.core.widget.WidgetUpdater

/**
 * Glance-based implementation of [WidgetUpdater].
 * Pushes data into Glance widget state and triggers recomposition.
 */
class GlanceWidgetUpdater(
    private val context: Context
) : WidgetUpdater {

    override suspend fun update(
        totalTimeText: String,
        normPercent: String,
        monthYear: String,
        hasCurrentRoute: Boolean,
        trainNumber: String,
        workTime: String,
        isDepartureNext: Boolean,
        routeCount: String
    ) {
        try {
            LocoDriverWidget.updateAllWidgets(
                context = context,
                totalTimeText = totalTimeText,
                normPercent = normPercent,
                monthYear = monthYear,
                hasCurrentRoute = hasCurrentRoute,
                currentTrainNumber = trainNumber,
                currentWorkTime = workTime,
                isDepartureNext = isDepartureNext,
                routeCount = routeCount
            )
        } catch (e: Exception) {
            // Widget update should never crash the app
            android.util.Log.w("GlanceWidgetUpdater", "Widget update failed", e)
        }
    }
}
