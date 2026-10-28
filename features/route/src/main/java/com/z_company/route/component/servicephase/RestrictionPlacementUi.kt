package com.z_company.route.component.servicephase

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.z_company.domain.track.Placement
import com.z_company.domain.track.RestrictionPass
import com.z_company.domain.track.RestrictionPlacement
import com.z_company.domain.util.TrainMapStop
import com.z_company.repository.remote_rest.tracks.SectionProfileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject

/**
 * Нанесётся ли каждое ограничение (по id записи) на карту: считается без сети (список участков и сохранённые на устройстве участки),
 * поэтому лимиты загрузок сервера не тратятся и пометка появляется сразу после добавления.
 */
@Composable
internal fun rememberPlacements(entries: List<RestrictionPass>, stops: List<TrainMapStop>): State<Map<String, Placement>> {
    val repo: SectionProfileRepository = koinInject()
    val key = entries.map { Triple(it.warningId, it.sectionId, it.place) to it.startAbsKm } to stops.map { it.name }
    return produceState(emptyMap(), key) {
        value = withContext(Dispatchers.Default) {
            val summaries = runCatching { repo.summaries(refresh = false) }.getOrDefault(emptyList())
            val untracked = entries.filter { !it.tracked && !it.isDeleted }
            val ids = untracked.flatMap { RestrictionPlacement.candidates(it, stops, summaries).orEmpty() }.map { it.id }.distinct()
            val cached = ids.mapNotNull { id -> runCatching { repo.cached(id) }.getOrNull()?.let { id to it } }.toMap()
            entries.filter { !it.isDeleted }.associate { it.warningId to RestrictionPlacement.classify(it, stops, summaries, cached) }
        }
    }
}

/** Пометка у ограничения, которое не на карте (null - на карте или неизвестно); [warn] - не нанесётся совсем, выделяется. */
internal fun placementNote(p: Placement?): Pair<String, Boolean>? = when (p) {
    Placement.NO_PROFILE -> "Нет профиля участка — на карту не нанесётся" to true
    Placement.GRAY_ZONE -> "Километраж здесь неизвестен — на карту не нанесётся" to true
    Placement.NO_KM -> "Без километров — на карту не нанесётся" to true
    Placement.PENDING -> "Не отображено на карте, карта пути этого участка в разработке" to false
    else -> null
}

@Composable
internal fun PlacementTag(note: Pair<String, Boolean>) {
    val (text, warn) = note
    Text(
        text,
        style = Pst.sans.copy(fontSize = 12.sp, fontWeight = if (warn) FontWeight.SemiBold else FontWeight.Normal),
        color = if (warn) Pst.danger else Pst.textMuted,
        modifier = if (warn) Modifier.background(Pst.danger.copy(alpha = 0.10f), RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 4.dp) else Modifier,
    )
}
