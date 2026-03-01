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
        normHours: String,
        monthYear: String,
        hasCurrentRoute: Boolean,
        reportTime: String,
        isDepartureNext: Boolean,
        lastActionText: String,
        stateInfoLine1: String,
        stateInfoLine2: String,
        stateInfoLine3: String,
        nextReportText: String,
        normRemainingText: String,
        isOvertime: Boolean,
        trainNumberText: String,
        statusText: String,
        statusTimeText: String
    ) {
        try {
            LocoDriverWidget.updateAllWidgets(
                context = context,
                totalTimeText = totalTimeText,
                normHours = normHours,
                monthYear = monthYear,
                hasCurrentRoute = hasCurrentRoute,
                reportTime = reportTime,
                isDepartureNext = isDepartureNext,
                lastActionText = lastActionText,
                stateInfoLine1 = stateInfoLine1,
                stateInfoLine2 = stateInfoLine2,
                stateInfoLine3 = stateInfoLine3,
                nextReportText = nextReportText,
                normRemainingText = normRemainingText,
                isOvertime = isOvertime,
                trainNumberText = trainNumberText,
                statusText = statusText,
                statusTimeText = statusTimeText
            )
            LocoDriverSmallWidget.updateAllSmallWidgets(
                context = context,
                totalTimeText = totalTimeText,
                normHours = normHours
            )
        } catch (e: Exception) {
            android.util.Log.w("GlanceWidgetUpdater", "Widget update failed", e)
        }
    }
}
