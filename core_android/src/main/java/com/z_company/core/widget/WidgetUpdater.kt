package com.z_company.core.widget

/**
 * Interface for pushing data to the home screen widget.
 * Implemented in the app module (Glance), injected via Koin in HomeViewModel.
 */
interface WidgetUpdater {

    /**
     * Update all widget instances with the latest home screen data.
     *
     * @param totalTimeText    formatted total work time (e.g. "164:30")
     * @param normHours        individual monthly norm in hours (e.g. "176ч")
     * @param monthYear        month and year label (e.g. "Февраль 2026")
     * @param hasCurrentRoute  whether there is an active route right now
     * @param trainNumber      current train number (or empty)
     * @param reportTime       report time of the current route (e.g. "28.02 08:00")
     * @param isDepartureNext  true if next tap will record departure
     * @param routeCount       total route count for the month
     * @param hasFutureRoute   whether there is a future route
     * @param futureReportTime report time of the nearest future route
     * @param futureTrainNumber train number of the nearest future route
     */
    suspend fun update(
        totalTimeText: String,
        normHours: String,
        monthYear: String,
        hasCurrentRoute: Boolean,
        trainNumber: String,
        reportTime: String,
        isDepartureNext: Boolean,
        routeCount: String,
        hasFutureRoute: Boolean,
        futureReportTime: String,
        futureTrainNumber: String
    )
}
