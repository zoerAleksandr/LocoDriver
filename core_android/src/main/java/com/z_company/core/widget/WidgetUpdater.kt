package com.z_company.core.widget

/**
 * Interface for pushing data to the home screen widget.
 * Implemented in the app module (Glance), injected via Koin in HomeViewModel.
 */
interface WidgetUpdater {

    /**
     * Update all widget instances with the latest data.
     *
     * @param totalTimeText    formatted total work time (e.g. "164:30")
     * @param normHours        individual monthly norm in hours (e.g. "176ч")
     * @param monthYear        month and year label (e.g. "Март 2026")
     * @param hasCurrentRoute  whether there is an active route right now
     * @param reportTime       report time of the current route (e.g. "28.02 08:00")
     * @param isDepartureNext  true if next tap will record departure
     * @param lastActionText   last recorded action text (e.g. "В пути с 16:20" or "Стоянка с 15:45")
     * @param stateInfoLine1   state info line 1 (e.g. "Явка 28.02 08:00" or rest duration)
     * @param stateInfoLine2   state info line 2 (e.g. rest end time or turnaround short rest)
     * @param stateInfoLine3   state info line 3 (turnaround full rest, or empty)
     * @param nextReportText   next report text (e.g. "Следующая явка 12.03 17:30")
     */
    suspend fun update(
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
    )
}
