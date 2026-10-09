package com.z_company.route.ui.settings

import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.z_company.core.ui.theme.MonoFont
import com.z_company.core.ui.theme.Shapes
import com.z_company.route.R
import com.z_company.route.component.AppAlertDialog
import com.z_company.route.component.SwipeToRevealDelete
import com.z_company.route.component.servicephase.TrainMapScreen
import com.z_company.route.viewmodel.ServicePhaseAutoFillViewModel
import com.z_company.route.viewmodel.TrackHistoryItem
import com.z_company.route.viewmodel.TrackHistoryStatus
import com.z_company.route.viewmodel.TrackHistoryViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.cos
import kotlin.math.max

/**
 * «Мои треки» (SCREEN_SPECS §17.8): история записанных треков «АВТО» карточками — мини-карта, название
 * (поезд и плечо), дата, длина, длительность, состояние отправки, звезда «Избранное», удаление (свайп или
 * корзина, с подтверждением). Тап открывает карту трека ([TrainMapScreen] в режиме просмотра).
 */
@Composable
fun SettingsTrackHistoryContent(viewModel: TrackHistoryViewModel) {
    val ui by viewModel.state.collectAsState()
    val context = LocalContext.current
    var toDelete by remember { mutableStateOf<TrackHistoryItem?>(null) }
    var closeSwipes by remember { mutableStateOf(0) }
    var confirmDeleteAll by remember { mutableStateOf(false) }

    LaunchedEffect(ui.message) {
        ui.message?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.messageShown()
        }
    }

    toDelete?.let { item ->
        AppAlertDialog(
            onDismissRequest = {
                toDelete = null
                closeSwipes++
            },
            title = "Удалить трек?",
            text = if (item.isLocal) {
                "Трек будет удалён с этого устройства и с сервера. Это действие нельзя отменить."
            } else {
                "Трек будет удалён с сервера. Это действие нельзя отменить."
            },
            confirmText = "Удалить",
            isDestructive = true,
            onConfirm = {
                viewModel.delete(item)
                toDelete = null
            },
            dismissText = "Отмена",
        )
    }
    if (confirmDeleteAll) {
        AppAlertDialog(
            onDismissRequest = { confirmDeleteAll = false },
            title = "Удалить всю историю?",
            text = "Все треки будут удалены с этого устройства и с сервера. Это действие нельзя отменить.",
            confirmText = "Удалить всё",
            isDestructive = true,
            onConfirm = {
                confirmDeleteAll = false
                viewModel.deleteAll()
            },
            dismissText = "Отмена",
        )
    }

    ui.opened?.let { opened ->
        TrainMapScreen(
            missingStation = { null },
            stops = emptyList(),
            loadRailLine = { ServicePhaseAutoFillViewModel.RailLineResult.NoPath },
            currentFix = { null },
            loadTrack = { viewModel.mapTrack(opened) },
            onDetermine = { false },
            onDismiss = viewModel::closeOpened,
            title = opened.title,
            viewOnly = true,
        )
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (!ui.consent) {
                item {
                    SettingsCard {
                        Text(
                            text = "Треки не сохраняются",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp),
                        )
                        Text(
                            text = "Новые поездки не записываются в историю. Включить сохранение можно в Профиле → «Данные и конфиденциальность».",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 14.dp),
                        )
                    }
                }
            }
            if (ui.loading) {
                item { Centered { CircularProgressIndicator(modifier = Modifier.size(28.dp)) } }
            } else if (ui.items.isEmpty()) {
                item {
                    Centered {
                        Text(
                            text = "Пока нет записанных треков",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            items(ui.items, key = { it.id }) { item ->
                SwipeToRevealDelete(
                    onDeleteClick = { toDelete = item },
                    closeSignal = closeSwipes,
                    onContentClick = { viewModel.open(item) },
                    backgroundVerticalPadding = 0.dp,
                    itemKey = item.id,
                ) {
                    TrackCard(
                        item = item,
                        zone = ui.zone,
                        onFavorite = { viewModel.toggleFavorite(item) },
                        onDelete = { toDelete = item },
                    )
                }
            }
            if (ui.canLoadMore || ui.remoteLoading) {
                item {
                    Centered {
                        if (ui.remoteLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        } else {
                            TextButton(onClick = viewModel::loadMore) { Text("Показать ещё") }
                        }
                    }
                }
            }
            if (ui.remoteError) {
                item {
                    SettingsSectionNote("Треки с сервера не загрузились. Показаны треки этого устройства.")
                }
            }
            if (ui.items.isNotEmpty()) {
                item {
                    Centered {
                        TextButton(onClick = { confirmDeleteAll = true }) {
                            Text("Удалить всю историю", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
        if (ui.opening) {
            Box(
                Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }
        }
    }
}

@Composable
private fun Centered(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) { content() }
}

@Composable
private fun TrackCard(
    item: TrackHistoryItem,
    zone: String,
    onFavorite: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 2.dp, shape = Shapes.medium)
            .background(color = MaterialTheme.colorScheme.secondary, shape = Shapes.medium)
            .padding(start = 12.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        MiniMap(item.preview, Modifier.size(72.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.primary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = TrackHistoryFormat.dateTime(item.startedAt, zone) + " · " + TrackHistoryFormat.duration(item.durationMs),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            item.distanceKm?.let { km ->
                Text(
                    text = TrackHistoryFormat.distance(km) + (item.avgSpeedKmh?.takeIf { it > 0 }?.let { " · ср. ${it.toInt()} км/ч" } ?: ""),
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = MonoFont),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
            Text(
                text = TrackHistoryFormat.status(item.status),
                style = MaterialTheme.typography.labelSmall,
                color = if (item.status == TrackHistoryStatus.REJECTED) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            IconButton(onClick = onFavorite, modifier = Modifier.size(40.dp)) {
                Icon(
                    painter = painterResource(com.z_company.core.R.drawable.ic_star),
                    contentDescription = if (item.isFavorite) "Убрать из избранного" else "В избранное",
                    tint = if (item.isFavorite) Color(0xFFF5B301) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                    modifier = Modifier.size(24.dp),
                )
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(40.dp)) {
                Icon(
                    painter = painterResource(R.drawable.delete_24px),
                    contentDescription = "Удалить трек",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}

/** Мини-карта: ломаная превью в рамке, без тайлов (карточка не ходит в сеть). */
@Composable
private fun MiniMap(points: List<Pair<Double, Double>>, modifier: Modifier) {
    val line = MaterialTheme.colorScheme.tertiary
    val bg = MaterialTheme.colorScheme.surfaceBright
    Canvas(
        modifier = modifier
            .clip(Shapes.small)
            .background(bg)
            .semantics { contentDescription = "Мини-карта маршрута" },
    ) {
        if (points.size < 2) return@Canvas
        val lat0 = points.minOf { it.first }
        val lat1 = points.maxOf { it.first }
        val lon0 = points.minOf { it.second }
        val lon1 = points.maxOf { it.second }
        val k = cos(Math.toRadians((lat0 + lat1) / 2))
        val w = max((lon1 - lon0) * k, 1e-9)
        val h = max(lat1 - lat0, 1e-9)
        val pad = 8.dp.toPx()
        val scale = minOf((size.width - 2 * pad) / w.toFloat(), (size.height - 2 * pad) / h.toFloat())
        val ox = (size.width - w.toFloat() * scale) / 2
        val oy = (size.height - h.toFloat() * scale) / 2
        fun map(p: Pair<Double, Double>) = Offset(
            ox + ((p.second - lon0) * k).toFloat() * scale,
            size.height - oy - (p.first - lat0).toFloat() * scale,
        )
        for (i in 1 until points.size) {
            drawLine(line, map(points[i - 1]), map(points[i]), strokeWidth = 2.5.dp.toPx(), cap = StrokeCap.Round)
        }
        drawCircle(line, 3.5.dp.toPx(), map(points.first()))
        drawCircle(Color.White, 1.5.dp.toPx(), map(points.first()))
        drawCircle(line, 3.5.dp.toPx(), map(points.last()), style = Stroke(width = 2.dp.toPx()))
    }
}

/** Форматы карточки (отдельно от UI: проверяются юнит-тестом). */
internal object TrackHistoryFormat {
    fun dateTime(ms: Long, zone: String): String {
        val f = SimpleDateFormat("d MMM yyyy, HH:mm", Locale("ru"))
        f.timeZone = TimeZone.getTimeZone(zone)
        return f.format(Date(ms))
    }

    fun duration(ms: Long): String {
        val minutes = ms / 60_000
        val h = minutes / 60
        val m = minutes % 60
        return when {
            h > 0 && m > 0 -> "$h ч $m мин"
            h > 0 -> "$h ч"
            else -> "${max(minutes, 1)} мин"
        }
    }

    fun distance(km: Double): String =
        if (km < 10) String.format(Locale("ru"), "%.1f км", km) else String.format(Locale("ru"), "%d км", km.toInt())

    fun status(s: TrackHistoryStatus): String = when (s) {
        TrackHistoryStatus.DEVICE_ONLY -> "Только на устройстве"
        TrackHistoryStatus.PENDING -> "Ожидает отправки"
        TrackHistoryStatus.SENT -> "Отправлен на сервер"
        TrackHistoryStatus.REJECTED -> "Не отправлен: лимит хранилища на сервере"
        TrackHistoryStatus.SERVER_ONLY -> "Только на сервере"
    }
}
