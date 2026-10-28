package com.z_company.route.autorecord

import android.content.Context
import com.z_company.domain.track.RestrictionJournalRecorder
import com.z_company.domain.track.RestrictionJournalStore
import com.z_company.repository.remote_rest.tracks.SectionProfileRepository
import com.z_company.route.component.servicephase.ProfileCalibrationStore

/**
 * Фоновая запись журнала проследования ограничений для «АВТО»: участок берётся только из кэша (без сети и квот),
 * поправка оси - та же, что на профиле пути (ручная привязка), поэтому зоны совпадают с тем, что видит машинист.
 */
fun createRestrictionRecorder(
    context: Context,
    store: RestrictionJournalStore,
    sections: SectionProfileRepository,
): RestrictionJournalRecorder {
    val calibrations = ProfileCalibrationStore(context)
    return RestrictionJournalRecorder(
        store = store,
        sections = { id -> sections.cached(id) },
        calibrationKm = { s -> calibrations.get(s.id, s.axisHash) },
        nowMs = { System.currentTimeMillis() },
    )
}
