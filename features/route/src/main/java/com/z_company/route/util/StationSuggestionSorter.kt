package com.z_company.route.util

import android.content.Context
import com.z_company.domain.use_cases.StationDirectoryUseCase
import com.z_company.domain.util.GeoFix
import com.z_company.domain.util.StationDirectory
import com.z_company.domain.util.StationSuggestionOrder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Порядок выпадающих списков станций по положению: ближайшая станция (в радиусе подбора) идёт первой, остальные - как были.
 * Положение запрашивается один раз при открытии формы и ждётся не дольше [WAIT_MS]; после этого порядок не меняется (список
 * не «прыгает» при уточнении GPS). Нет положения или разрешения - список остаётся как есть.
 */
class StationSuggestionSorter(
    private val context: Context,
    private val directory: StationDirectoryUseCase,
) {
    @Volatile
    private var fix: GeoFix? = null
    private var started = false

    fun start(scope: CoroutineScope) {
        if (started) return
        started = true
        scope.launch { fix = DeviceLocation.currentFix(context, timeoutMs = WAIT_MS) }
    }

    fun sort(names: List<String>): List<String> {
        val f = fix ?: return names
        return StationSuggestionOrder.sort(names, emptyMap(), StationDirectory.points(directory.stations()), f)
    }

    private companion object {
        const val WAIT_MS = 1_000L
    }
}
