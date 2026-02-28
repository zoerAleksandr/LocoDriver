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
     * @param normPercent      percent of monthly norm (e.g. "85%")
     * @param monthYear        month and year label (e.g. "Февраль 2026")
     * @param hasCurrentRoute  whether there is an active route right now
     * @param trainNumber      current train number (or empty)
     * @param workTime         current route work time formatted (e.g. "03:15")
     * @param isDepartureNext  true if next tap will record departure
     * @param routeCount       total route count for the month
     */
    suspend fun update(
        totalTimeText: String,
        normPercent: String,
        monthYear: String,
        hasCurrentRoute: Boolean,
        trainNumber: String,
        workTime: String,
        isDepartureNext: Boolean,
        routeCount: String
    )
}
