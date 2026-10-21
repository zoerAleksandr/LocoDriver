package com.z_company.route.component.servicephase

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import android.content.pm.ActivityInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.z_company.domain.track.KmMarks
import com.z_company.domain.track.LiveProfile
import com.z_company.domain.track.ProfileElement
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.roundToInt

/*
 * Профиль пути на карте поезда: полоса внизу карты и полноэкранный экран (SCREEN_SPECS §6.9, «Профиль пути»).
 * График — продольный профиль выбранного участка в направлении движения, маркер — живое положение поезда.
 */

/** Окна показа полноэкранного профиля: сколько км позади и впереди; `ahead = 0` - весь участок. */
internal enum class ProfileScale(val label: String, val behindKm: Double, val aheadKm: Double) {
    S3("3 км", 0.5, 2.5),
    S6("6 км", 1.0, 5.0),
    S12("12 км", 2.0, 10.0),
    ALL("Весь участок", 0.0, 0.0),
}

/** Окно мини-профиля в полосе: позади 1 км, впереди 5 км. */
private const val STRIP_BEHIND_KM = 1.0
private const val STRIP_AHEAD_KM = 5.0

private fun fmt1(v: Double): String = ((v * 10).roundToInt() / 10.0).toString().replace('.', ',')

/** «↑ 4,0 ‰» (подъём в направлении движения), «↓ 2,5 ‰», «площадка». */
internal fun gradeText(live: LiveProfile, e: ProfileElement?): String? {
    e ?: return null
    val g = live.gradeInView(e)
    return when {
        abs(g) < 0.05 -> "площадка"
        g > 0 -> "↑ ${fmt1(g)} ‰"
        else -> "↓ ${fmt1(-g)} ‰"
    }
}

private fun emptyText(model: ProfileModel): String = when (model.emptyReason) {
    ProfileEmpty.LOADING -> "Загружаем профили участков…"
    ProfileEmpty.NO_LOCATION -> "Определяем местоположение…"
    ProfileEmpty.NO_SECTIONS -> "Профили участков не загружены. Проверьте интернет — после загрузки профиль появится."
    ProfileEmpty.NO_NEARBY -> "Для этого места профиль пока не собран. Данные профиля собираются — скоро он появится."
    ProfileEmpty.NO_DATA -> "Для участка «${model.section?.name.orEmpty()}» данные профиля собираются. Скоро профиль появится."
    null -> ""
}

/**
 * Полоса «Профиль пути»: слева над графиком километр и уклон, мини-профиль впереди по ходу; справа кнопки по вертикали:
 * «Развернуть на весь экран» вверху, «Скрыть» внизу. Нет данных - пояснение вместо графика.
 */
@Composable
internal fun ProfileStrip(
    model: ProfileModel,
    height: Dp,
    onHide: () -> Unit,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .background(Pst.surface)
            .padding(start = 16.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        val live = model.live
        val reason = model.emptyReason
        Column(Modifier.weight(1f).fillMaxHeight().padding(vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            if (reason == null && live != null) {
                val pos = model.position
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (pos?.offRoute == true) "вне участка" else pos?.kmText ?: "ПРОФИЛЬ ПУТИ",
                        style = Pst.mono.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                        color = if (pos?.offRoute == true) Pst.danger else Pst.textMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    gradeText(live, pos?.element)?.let {
                        Text(it, style = Pst.mono.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold), color = Pst.text, maxLines = 1)
                    }
                }
                ProfileChart(model, STRIP_BEHIND_KM, STRIP_AHEAD_KM, detailed = false, modifier = Modifier.weight(1f).fillMaxWidth().clickable(onClickLabel = "Профиль пути на весь экран", onClick = onExpand))
            } else {
                Text("ПРОФИЛЬ ПУТИ", style = Pst.mono.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold), color = Pst.textMuted)
                Text(emptyText(model), style = Pst.sans.copy(fontSize = 12.sp, lineHeight = 15.sp), color = Pst.textMuted, maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
        }
        StripButtons("Профиль пути на весь экран", onExpand, "Скрыть профиль пути", onHide, height)
    }
}

/**
 * График: заливка под линией высот, живой маркер, станции и километры (при [detailed]). Окно — вокруг положения
 * ([behindKm] позади, [aheadKm] впереди); [aheadKm] = 0 — весь участок.
 */
@Composable
internal fun ProfileChart(model: ProfileModel, behindKm: Double, aheadKm: Double, detailed: Boolean, modifier: Modifier = Modifier) {
    val live = model.live
    val line = model.elevation()
    if (live == null || line == null || line.points.size < 2) return
    val pos = model.position
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current.density
    val ink = Pst.text
    val muted = Pst.textMuted
    val grid = Pst.border
    val accent = Pst.accent
    val labelStyle: TextStyle = Pst.mono.copy(fontSize = if (detailed) 10.sp else 9.sp, color = muted)
    val stationStyle: TextStyle = Pst.sans.copy(fontSize = 10.sp, color = muted)
    val marks = live.section.kmMarks
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val labelH = if (detailed) 16f * density else 0f
        val topPad = if (detailed) 16f * density else 4f * density
        val plotH = h - labelH - topPad - 2f * density
        if (w <= 20f || plotH <= 10f) return@Canvas
        val window = if (aheadKm <= 0.0) com.z_company.domain.track.ProfileWindow(0.0, live.lengthKm) else live.window(behindKm, aheadKm)
        val span = (window.toX - window.fromX).coerceAtLeast(1e-6)
        fun px(x: Double) = ((x - window.fromX) / span * w).toFloat()
        val n = (w / (3f * density)).toInt().coerceIn(20, 400)
        val xs = DoubleArray(n + 1) { window.fromX + span * it / n }
        val hs = DoubleArray(n + 1) { line.heightAt(live.toAxis(xs[it])) }
        val lo = hs.min()
        val hi = hs.max()
        val range = max(hi - lo, 20.0)
        val mid = (hi + lo) / 2
        val yMin = mid - range * 0.6
        val yMax = mid + range * 0.6
        fun py(v: Double) = topPad + plotH - ((v - yMin) / (yMax - yMin) * plotH).toFloat()

        // Сетка: нижняя и верхняя линии.
        drawLine(grid, Offset(0f, topPad), Offset(w, topPad), strokeWidth = 1f)
        drawLine(grid, Offset(0f, topPad + plotH), Offset(w, topPad + plotH), strokeWidth = 1f)

        val fill = Path().apply {
            moveTo(px(xs[0]), topPad + plotH)
            for (i in 0..n) lineTo(px(xs[i]), py(hs[i]))
            lineTo(px(xs[n]), topPad + plotH)
            close()
        }
        drawPath(fill, accent.copy(alpha = 0.16f))
        val stroke = Path().apply {
            moveTo(px(xs[0]), py(hs[0]))
            for (i in 1..n) lineTo(px(xs[i]), py(hs[i]))
        }
        drawPath(stroke, ink, style = Stroke(width = 2f * density, cap = StrokeCap.Round))

        if (detailed) {
            // Станции: пунктир и название сверху, без наложений.
            var lastEnd = -1f
            live.section.stations.map { live.toView(it.km) to it.name }
                .filter { it.first in window.fromX..window.toX }
                .sortedBy { it.first }
                .forEach { (x, name) ->
                    val xx = px(x)
                    drawLine(muted.copy(alpha = 0.5f), Offset(xx, topPad), Offset(xx, topPad + plotH), strokeWidth = 1f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f)))
                    val t = measurer.measure(name, stationStyle, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    val tx = (xx + 3f * density).coerceAtMost(w - t.size.width)
                    if (tx > lastEnd) {
                        drawText(t, topLeft = Offset(tx.coerceAtLeast(0f), 0f))
                        lastEnd = tx + t.size.width + 4f * density
                    }
                }
            // Километры: подпись у каждого границы целого км.
            var prevAbs: Double? = null
            var lastLabel = -1000f
            val step = max(span / 600.0, 0.01)
            var x = window.fromX
            while (x <= window.toX) {
                val abs = KmMarks.toAbs(marks, live.toAxis(x)) ?: live.toAxis(x)
                val pa = prevAbs
                if (pa != null && floor(pa + 1e-9) != floor(abs + 1e-9)) {
                    val label = max(floor(pa + 1e-9), floor(abs + 1e-9)).toInt().toString()
                    val xx = px(x)
                    if (xx - lastLabel > 44f * density) {
                        drawLine(muted.copy(alpha = 0.6f), Offset(xx, topPad + plotH), Offset(xx, topPad + plotH + 4f * density), strokeWidth = 1f)
                        val t = measurer.measure(label, labelStyle)
                        drawText(t, topLeft = Offset((xx - t.size.width / 2f).coerceIn(0f, w - t.size.width), topPad + plotH + 3f * density))
                        lastLabel = xx
                    }
                }
                prevAbs = abs
                x += step
            }
        }

        // Живой маркер.
        if (pos != null) {
            val vx = live.toView(pos.kmAxis)
            if (vx in window.fromX..window.toX) {
                val xx = px(vx)
                val yy = py(line.heightAt(pos.kmAxis))
                val c = if (pos.offRoute || !pos.locked) muted else accent
                drawLine(c, Offset(xx, topPad), Offset(xx, topPad + plotH), strokeWidth = 2f * density)
                drawCircle(c, radius = 5f * density, center = Offset(xx, yy))
            }
        }
    }
}

/**
 * Экран «Профиль пути» на весь экран. Только горизонтально, как график скорости: на время показа активность переводится
 * в landscape, по выходу возвращается прежняя ориентация (пересоздания нет, configChanges в манифесте).
 * Сверху одна строка: назад, участок (нажатие - список), смена направления, масштаб; затем график и строка «Сейчас».
 */
@Composable
internal fun ProfileFullScreen(model: ProfileModel, onDismiss: () -> Unit) {
    var showPicker by rememberSaveable { mutableStateOf(false) }
    var scaleIdx by rememberSaveable { mutableStateOf(ProfileScale.S6.ordinal) }
    val scale = ProfileScale.values()[scaleIdx]
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        val activity = LocalContext.current.findActivity()
        DisposableEffect(activity) {
            val previous = activity?.requestedOrientation
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            onDispose { previous?.let { activity.requestedOrientation = it } }
        }
        val live = model.live
        val reason = model.emptyReason
        Column(modifier = Modifier.fillMaxSize().background(Pst.bg).statusBarsPadding().navigationBarsPadding()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MapButton(onClick = onDismiss, description = "Назад") { PstIcon(PstIcons.ChevronLeft, 22.dp, Pst.text) }
                SectionHeader(model, Modifier.weight(1f)) { showPicker = true }
                if (reason == null && live != null) {
                    ProfileScale.values().forEach { s ->
                        val on = s == scale
                        Text(
                            text = s.label,
                            style = Pst.sans.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
                            color = if (on) Pst.onAccent else Pst.text,
                            modifier = Modifier
                                .background(if (on) Pst.accent else Pst.bgSubtle, RoundedCornerShape(12.dp))
                                .clickable { scaleIdx = s.ordinal }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            maxLines = 1,
                        )
                    }
                    StripButton("Сменить направление", { model.toggleDirection() }) { PstIcon(PstIcons.Swap, 20.dp, Pst.text) }
                }
            }
            if (model.fitsLeg == false) {
                Text(
                    text = "Этот участок не совпадает с плечом поезда. Выберите другой или оставьте, если едете не по плечу.",
                    style = Pst.sans.copy(fontSize = 12.sp, lineHeight = 15.sp),
                    color = Pst.text,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp).fillMaxWidth()
                        .background(Pst.warning, RoundedCornerShape(12.dp)).padding(8.dp),
                )
            }
            if (reason == null && live != null) {
                ProfileChart(
                    model, scale.behindKm, scale.aheadKm, detailed = true,
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                )
                NowRow(model, live)
            } else {
                Column(
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = emptyText(model),
                        style = Pst.sans.copy(fontSize = 15.sp, lineHeight = 21.sp),
                        color = Pst.textMuted,
                        textAlign = TextAlign.Center,
                    )
                    if (model.sections.isNotEmpty()) {
                        PstButton(text = "Выбрать участок", onClick = { showPicker = true }, kind = PstButtonKind.TONAL)
                    }
                }
            }
        }
    }
    if (showPicker) SectionPickerDialog(model, onDismiss = { showPicker = false })
}

/** Участок и направление одной карточкой (нажатие - список участков): название, «от А к Б», способ выбора. */
@Composable
private fun SectionHeader(model: ProfileModel, modifier: Modifier = Modifier, onPick: () -> Unit) {
    val live = model.live
    val s = live?.section
    Row(
        modifier = modifier
            .background(Pst.surface, RoundedCornerShape(14.dp))
            .clickable(onClickLabel = "Выбрать участок", onClick = onPick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = s?.name ?: "Выбрать участок",
                style = Pst.sans.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
                color = Pst.text, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            if (s != null && live != null) {
                val from = if (live.reversed) s.toStation else s.fromStation
                val to = if (live.reversed) s.fromStation else s.toStation
                Text(
                    text = "$from → $to · " + if (model.manualId != null) "выбран вручную" else "по местоположению",
                    style = Pst.sans.copy(fontSize = 11.sp), color = Pst.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Text("Сменить", style = Pst.sans.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold), color = Pst.accent, maxLines = 1)
    }
}

/** Строка «Сейчас» в одну линию: километр и пикет, уклон, ближайшая станция впереди; вне участка - предупреждение. */
@Composable
private fun NowRow(model: ProfileModel, live: LiveProfile) {
    val pos = model.position
    val ahead = pos?.let { p ->
        val vx = live.toView(p.kmAxis)
        live.section.stations.map { live.toView(it.km) to it.name }.filter { it.first > vx + 0.05 }.minByOrNull { it.first }
            ?.let { (x, name) -> name to (x - vx) }
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)
            .background(Pst.surface, RoundedCornerShape(14.dp)).padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        when {
            pos == null -> Text("Положение на участке определяется…", style = Pst.sans.copy(fontSize = 13.sp), color = Pst.textMuted)
            pos.offRoute -> Text(
                "Вы вне этого участка. Выберите другой или дождитесь автоподбора.",
                style = Pst.sans.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold), color = Pst.danger, maxLines = 2,
            )
            else -> {
                Text(
                    text = pos.kmText ?: "${fmt1(live.toView(pos.kmAxis))} км от начала",
                    style = Pst.mono.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold), color = Pst.text, maxLines = 1,
                )
                gradeText(live, pos.element)?.let {
                    Text(it, style = Pst.mono.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold), color = Pst.text, maxLines = 1)
                }
                Text(
                    text = ahead?.let { "До «${it.first}» — ${pstKm(it.second)}" }
                        ?: if (pos.kmText == null) "Нет километровой привязки — показано расстояние от начала." else "",
                    style = Pst.sans.copy(fontSize = 13.sp), color = Pst.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/** Список участков: ближайшие по положению сверху (плечо поезда — выше всех), поиск по названию и станциям. */
@Composable
private fun SectionPickerDialog(model: ProfileModel, onDismiss: () -> Unit) {
    var query by remember { mutableStateOf(TextFieldValue("")) }
    val rows = remember(query.text, model.sections, model.manualId) { model.rows(query.text) }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Column(modifier = Modifier.fillMaxSize().background(Pst.bg).statusBarsPadding().navigationBarsPadding()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MapButton(onClick = onDismiss, description = "Назад") { PstIcon(PstIcons.ChevronLeft, 22.dp, Pst.text) }
                Text("Участок профиля", style = Pst.sans.copy(fontSize = 18.sp, fontWeight = FontWeight.SemiBold), color = Pst.text)
            }
            PstTextField(
                value = query,
                onValueChange = { query = it },
                label = "Поиск",
                placeholder = "Название или станция",
                leadingIcon = PstIcons.Search,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            )
            if (model.manualId != null) {
                Text(
                    text = "Подбирать по местоположению",
                    style = Pst.sans.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
                    color = Pst.accent,
                    modifier = Modifier.fillMaxWidth().clickable { model.useAuto(); onDismiss() }.padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }
            if (rows.isEmpty()) {
                Box(Modifier.weight(1f).fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (model.sections.isEmpty()) "Участки не загружены. Проверьте интернет." else "Ничего не найдено. Данные профилей собираются — список пополняется.",
                        style = Pst.sans.copy(fontSize = 14.sp),
                        color = Pst.textMuted,
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
                    items(rows, key = { it.section.id }) { row ->
                        val s = row.section
                        val chosen = s.id == model.section?.id
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .clickable { model.choose(s.id); onDismiss() }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(s.name, style = Pst.sans.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold), color = Pst.text, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                val tags = buildList {
                                    add(pstKm(s.lengthKm))
                                    row.distanceM?.let { add(if (it < 300) "рядом с вами" else "в ${pstKm(it / 1000.0)} от вас") }
                                    if (row.fitsLeg == true) add("совпадает с плечом")
                                    if (!s.hasProfile) add("профиль собирается")
                                }
                                Text(tags.joinToString(" · "), style = Pst.sans.copy(fontSize = 12.sp), color = Pst.textMuted, maxLines = 2)
                            }
                            if (chosen) PstIcon(PstIcons.Check, 20.dp, Pst.accent)
                        }
                        Box(Modifier.fillMaxWidth().height(1.dp).background(Pst.border))
                    }
                }
            }
        }
    }
}
