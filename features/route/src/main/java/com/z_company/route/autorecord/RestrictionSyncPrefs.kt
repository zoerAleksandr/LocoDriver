package com.z_company.route.autorecord

import android.content.Context
import com.z_company.repository.remote_rest.tracks.RestrictionSyncCursor

/** Метка последней догруженной записи журнала проследования (SharedPreferences, без данных пользователя). */
class RestrictionSyncPrefs(context: Context) : RestrictionSyncCursor {
    private val prefs = context.applicationContext.getSharedPreferences("restriction_sync", Context.MODE_PRIVATE)

    override fun lastPulledMs(): Long = runCatching { prefs.getLong(KEY, 0L) }.getOrDefault(0L)

    override fun setLastPulledMs(value: Long) {
        runCatching { prefs.edit().putLong(KEY, value).apply() }
    }

    private companion object {
        const val KEY = "last_pulled_ms"
    }
}
