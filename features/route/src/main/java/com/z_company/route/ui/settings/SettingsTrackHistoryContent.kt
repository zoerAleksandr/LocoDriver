package com.z_company.route.ui.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.Image
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.Composable
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
import com.z_company.route.component.AppBottomSheet
import com.z_company.route.component.BottomSheetAction
import com.z_company.route.component.SelectionMarker
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
 * «Мои треки» (SCREEN_SPECS §17.8): история записанных треков «АВТО» карточками в паттерне карточки маршрута
 * («Все маршруты», §9): мини-карта, название, дата, явка рейса, длина и время в пути, индикаторы (звезда, облако,
 * синхронизация). Свайп влево — удалить (с подтверждением), вправо — в избранное/из избранного, долгое нажатие —
 * контекстное меню. Тап открывает карту трека ([TrainMapScreen] в режиме просмотра).
 *
 * @param onRequestSync запрос принудительной отправки одного трека (экран проверяет согласие и сеть).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsTrackHistoryContent(viewModel: TrackHistoryViewModel, onRequestSync: (TrackHistoryItem) -> Unit = {}) {
    val ui by viewModel.state.collectAsState()
    var toDelete by remember { mutableStateOf<TrackHistoryItem?>(null) }
    var menuFor by remember { mutableStateOf<TrackHistoryItem?>(null) }
    var closeSwipes by remember { mutableStateOf(0) }
    val menuSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    menuFor?.let { item ->
        AppBottomSheet(
            onDismissRequest = { menuFor = null },
            sheetState = menuSheetState,
            title = item.title,
            actions = TrackHistoryActions.menu(item).map { action ->
                BottomSheetAction(text = TrackHistoryActions.label(action, item)) {
                    when (action) {
                        TrackHistoryActions.Action.FAVORITE -> viewModel.toggleFavorite(item)
                        TrackHistoryActions.Action.SYNC -> onRequestSync(item)
                        TrackHistoryActions.Action.DELETE -> toDelete = item
                    }
                }
            },
        )
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
                if (ui.isSelectionMode) {
                    // Режим выбора (как на «Все маршруты»): тап переключает отметку, свайп-удаление отключено.
                    val selected = item.id in ui.selectedIds
                    TrackCard(
                        item = item,
                        zone = ui.zone,
                        syncing = item.id in ui.syncingIds,
                        selectionMode = true,
                        isSelected = selected,
                        modifier = Modifier.clickable { viewModel.toggleSelection(item.id) },
                    )
                } else {
                    SwipeToRevealDelete(
                        onDeleteClick = { toDelete = item },
                        closeSignal = closeSwipes,
                        onContentClick = { viewModel.open(item) },
                        onContentLongClick = { menuFor = item },
                        onSwipeRight = { viewModel.toggleFavorite(item) },
                        rightReveal = true,
                        rightIconRes = com.z_company.core.R.drawable.ic_star,
                        rightIconTint = Color(0xFFF5B301),
                        rightBackground = Color(0xFFF5B301).copy(alpha = 0.16f),
                        rightContentDescription = if (item.isFavorite) "Убрать из избранного" else "В избранное",
                        backgroundVerticalPadding = 0.dp,
                        itemKey = item.id,
                    ) {
                        TrackCard(
                            item = item,
                            zone = ui.zone,
                            syncing = item.id in ui.syncingIds,
                        )
                    }
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
    syncing: Boolean,
    modifier: Modifier = Modifier,
    selectionMode: Boolean = false,
    isSelected: Boolean = false,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 2.dp, shape = Shapes.medium)
            .background(color = MaterialTheme.colorScheme.secondary, shape = Shapes.medium)
            .then(
                if (selectionMode && isSelected) {
                    Modifier.border(2.dp, MaterialTheme.colorScheme.surfaceContainerLow, Shapes.medium)
                } else {
                    Modifier
                },
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (selectionMode) SelectionMarker(isSelected = isSelected)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                MiniMap(item.preview, Modifier.size(72.dp))
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    item.workStartAt?.let { start ->
                        Text(
                            text = TrackHistoryFormat.workStart(start, zone),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Text(
                        text = TrackHistoryFormat.summary(item.distanceKm, item.durationMs),
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = MonoFont),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (item.status == TrackHistoryStatus.REJECTED) {
                        Text(
                            text = TrackHistoryFormat.status(item.status),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            // Индикаторы внизу карточки, справа — как на карточке маршрута: избранное, синхронизация.
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            ) {
                if (item.isFavorite) {
                    Icon(
                        painter = painterResource(com.z_company.core.R.drawable.ic_star),
                        contentDescription = "В избранном",
                        tint = Color(0xFFF5B301),
                        modifier = Modifier.size(20.dp),
                    )
                }
                SyncIndicator(item = item, syncing = syncing)
            }
        }
    }
}

/** Индикатор синхронизации (те же значки, что на карточке маршрута): отправлен, не отправлен, идёт отправка. */
@Composable
private fun SyncIndicator(item: TrackHistoryItem, syncing: Boolean) {
    val description = TrackHistoryFormat.syncDescription(item.status, syncing)
    when {
        syncing -> {
            val angle by rememberInfiniteTransition(label = "trackSync").animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing), RepeatMode.Restart),
                label = "trackSyncAngle",
            )
            Icon(
                painter = painterResource(R.drawable.sync_24px),
                contentDescription = description,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp).graphicsLayer { rotationZ = -angle },
            )
        }
        item.isSynced -> Image(
            painter = painterResource(R.drawable.sync_on_icon),
            contentDescription = description,
            modifier = Modifier.size(20.dp),
        )
        else -> Icon(
            painter = painterResource(R.drawable.sync_disabled_24px),
            contentDescription = description,
            tint = if (item.status == TrackHistoryStatus.REJECTED) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
        )
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

    /** «Явка: д МММ гггг, чч:мм» — явка рейса, к которому записан трек. */
    fun workStart(ms: Long, zone: String): String = "Явка: " + dateTime(ms, zone)

    /** «N км · в пути N ч M мин»; пока сводка не достроена (км нет) — только время в пути. */
    fun summary(distanceKm: Double?, durationMs: Long): String {
        val way = "в пути " + duration(durationMs)
        return if (distanceKm == null) way else distance(distanceKm) + " · " + way
    }

    /** Описание индикатора синхронизации (доступность); пометки «Только на сервере» больше нет. */
    fun syncDescription(s: TrackHistoryStatus, syncing: Boolean): String = when {
        syncing -> "Отправляется на сервер"
        s == TrackHistoryStatus.SENT || s == TrackHistoryStatus.SERVER_ONLY -> "Синхронизирован"
        s == TrackHistoryStatus.REJECTED -> status(s)
        s == TrackHistoryStatus.PENDING -> "Ожидает отправки"
        else -> "Не синхронизирован"
    }

    /** Красная строка на карточке — только для окончательного отказа сервера. */
    fun status(s: TrackHistoryStatus): String = when (s) {
        TrackHistoryStatus.REJECTED -> "Не отправлен: лимит хранилища на сервере"
        else -> ""
    }
}

/** Пункты контекстного меню карточки (долгое нажатие): чистая логика, проверяется юнит-тестом. */
internal object TrackHistoryActions {
    enum class Action { FAVORITE, SYNC, DELETE }

    /** «Синхронизировать» — только для локального трека, ещё не отправленного. */
    fun menu(item: TrackHistoryItem): List<Action> = buildList {
        add(Action.FAVORITE)
        if (item.isLocal && !item.isSynced && !item.noTrain) add(Action.SYNC)
        add(Action.DELETE)
    }

    fun label(a: Action, item: TrackHistoryItem): String = when (a) {
        Action.FAVORITE -> if (item.isFavorite) "Убрать из избранного" else "В избранное"
        Action.SYNC -> "Синхронизировать"
        Action.DELETE -> "Удалить"
    }
}
