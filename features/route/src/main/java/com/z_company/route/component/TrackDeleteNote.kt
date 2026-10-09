package com.z_company.route.component

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.z_company.domain.track.TrackRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.compose.getKoin

/** Строка в диалогах удаления поезда/маршрута, если по удаляемым поездам записаны треки (SCREEN_SPECS §6.9). */
const val TRACKS_WILL_BE_DELETED_NOTE = "Будут удалены и записанные треки."

/** Есть ли в `Track.db` хотя бы один трек по любому из поездов [trainIds]. */
@Composable
fun rememberHasTracks(trainIds: List<String>): State<Boolean> {
    val repository = getKoin().getOrNull<TrackRepository>()
    return produceState(initialValue = false, trainIds, repository) {
        value = repository != null && trainIds.isNotEmpty() && withContext(Dispatchers.Default) {
            runCatching { trainIds.any { repository.tracksForTrain(it).isNotEmpty() } }.getOrDefault(false)
        }
    }
}

/**
 * Слот `contentAfterHeader` для [AppBottomSheet]: строка «Будут удалены и записанные треки.»,
 * только если по поездам [trainIds] есть треки; иначе `null` (ничего не показывается).
 */
@Composable
fun rememberTrackDeleteNote(trainIds: List<String>): (@Composable ColumnScope.() -> Unit)? {
    val has by rememberHasTracks(trainIds)
    if (!has) return null
    return {
        Text(
            text = TRACKS_WILL_BE_DELETED_NOTE,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
        )
    }
}
