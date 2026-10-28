package com.z_company.route.component.servicephase

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.z_company.domain.entities.setting.ServicePhase
import com.z_company.domain.entities.setting.ServicePhasePoint
import com.z_company.domain.entities.setting.ServicePhaseStation
import com.z_company.domain.entities.setting.ServicePhaseStationKind
import com.z_company.domain.entities.setting.ServicePhaseStationSource
import com.z_company.domain.use_cases.StationDirectoryUseCase
import com.z_company.domain.util.ServicePhaseStationsMerge
import org.koin.compose.koinInject
import com.z_company.route.viewmodel.AutoFillFailure
import com.z_company.route.viewmodel.AutoFillState
import com.z_company.route.viewmodel.ServicePhaseAutoFillViewModel
import com.z_company.route.viewmodel.message

/**
 * Плечо поезда для экранов ДУ-61 (ограничения поезда, добавление, профиль пути, карта): одно и то же везде. [phase] - полное плечо из справочника
 * (со станциями); [onUpdate] сохраняет его тем же путём, что редактор плеча; [onOpenEditor] открывает справочник (когда автоопределение не смогло).
 */
internal class LegContext(
    val title: String?,
    val phase: ServicePhase?,
    val country: String,
    val onSelect: () -> Unit,
    val onUpdate: (ServicePhase) -> Unit,
    val onAddNames: (List<String>) -> Unit,
    val onOpenEditor: () -> Unit,
    /** Справочник плеч можно открыть с этого экрана (на карте с Главного его нет). */
    val editorAvailable: Boolean = true,
)

/** Строка плеча + пометка «проверено на карте» или кнопка «Определить» (расстояние, координаты и промежуточные станции одной кнопкой). */
@Composable
internal fun LegBlock(ctx: LegContext, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        LegRow(title = ctx.title, onSelectLeg = ctx.onSelect)
        val phase = ctx.phase
        if (ctx.title != null && phase != null) LegDetermineRow(ctx, phase)
    }
}

@Composable
private fun LegDetermineRow(ctx: LegContext, phase: ServicePhase) {
    if (phase.isMapVerified) {
        Row(
            Modifier.background(Pst.success.copy(alpha = 0.12f), RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            PstIcon(PstIcons.Check, 14.dp, Pst.success)
            Text("Плечо проверено и найдено на карте", style = Pst.sans.copy(fontSize = 13.sp, fontWeight = FontWeight.Medium), color = Pst.text)
        }
        return
    }
    val vm: ServicePhaseAutoFillViewModel = viewModel(key = "leg-determine")
    val state by vm.state.collectAsState()
    var running by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var refine by remember { mutableStateOf<String?>(null) }
    // Сохранение плеча идёт в фоне, список плеч обновляется чуть позже: до этого показываем ход работы, а не «не определено».
    var applying by remember { mutableStateOf(false) }
    LaunchedEffect(applying, phase) {
        if (applying) {
            kotlinx.coroutines.delay(5_000)
            applying = false
        }
    }
    val directory: StationDirectoryUseCase = koinInject()
    LaunchedEffect(state, running) {
        if (!running) return@LaunchedEffect
        when (val st = state) {
            is AutoFillState.Ready -> {
                val found = st.stations.filter { it.kind != "halt" }.map { it.toPhaseStation() }
                ctx.onAddNames(found.map { it.name })
                // Координаты станций берутся из справочника (единый источник): подпись «измерено по карте» считается по плечу уже в таком
                // виде, иначе при следующем показе справочник подменит координаты и пометка пропадёт.
                ctx.onUpdate(withSignature(directory.resolve(determinedPhase(phase, st))))
                vm.cancel()
                applying = true
                running = false
            }
            // Нужны уточнения: пользователю объясняем, каких именно, и переходим в справочник только с его согласия.
            is AutoFillState.ChooseRoute -> { vm.cancel(); running = false; refine = "Между станциями плеча несколько путей: нужно выбрать нужный маршрут." }
            is AutoFillState.Choose -> { vm.cancel(); running = false; refine = "Название станции «${st.query}» подходит нескольким станциям на карте: нужно выбрать нужную." }
            is AutoFillState.Failed -> {
                vm.cancel()
                running = false
                when (st.failure) {
                    AutoFillFailure.STATION_NOT_FOUND ->
                        refine = "Станция ${st.name?.let { "«$it» " }.orEmpty()}не найдена на карте: у неё нет координат, их нужно указать."
                    AutoFillFailure.NO_PATH ->
                        refine = "Между станциями плеча не удалось проложить путь по рельсам: станция может стоять не на той ветке, а маршрут быть слишком сложным."
                    // Сеть, лимит, занятость сервера: уточнять нечего - просто повторить позже.
                    else -> message = st.failure.message()
                }
            }
            else -> Unit
        }
    }
    refine?.let { reason ->
        com.z_company.route.component.AppAlertDialog(
            onDismissRequest = { refine = null },
            title = "Плечо требует уточнений",
            text = if (ctx.editorAvailable) "$reason\n\nОткрыть справочник плеч, чтобы уточнить?" else "$reason\n\nУточните плечо в разделе «Поезд» (справочник плеч).",
            confirmText = if (ctx.editorAvailable) "Открыть справочник" else "Понятно",
            dismissText = if (ctx.editorAvailable) "Не сейчас" else null,
            onConfirm = { refine = null; if (ctx.editorAvailable) ctx.onOpenEditor() },
        )
    }
    Column(
        Modifier.fillMaxWidth().background(Pst.surface, RoundedCornerShape(12.dp)).padding(start = 14.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "Плечо не проверено на карте: расстояние, координаты и промежуточные станции не определены.",
                style = Pst.sans.copy(fontSize = 12.sp, lineHeight = 16.sp), color = Pst.textMuted, modifier = Modifier.weight(1f),
            )
            if (running || applying) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = Pst.accent)
            } else {
                PstButton(
                    "Определить",
                    {
                        message = null
                        running = true
                        vm.start(
                            departure = phase.departureStation,
                            arrival = phase.arrivalStation,
                            country = ctx.country,
                            departureOsmId = phase.departurePoint?.osmId,
                            arrivalOsmId = phase.arrivalPoint?.osmId,
                            departurePoint = phase.departurePoint?.let { it.latitude to it.longitude },
                            arrivalPoint = phase.arrivalPoint?.let { it.latitude to it.longitude },
                            // Как «Подобрать станции» в справочнике: уже есть станции с координатами - путь строго через них (по выбранному
                            // маршруту); нет - сперва выбор маршрута, если путей несколько (это делает справочник).
                            viaPoints = phase.intermediateStations.filter { it.hasCoordinates }.map { it.latitude!! to it.longitude!! },
                            pickRoute = phase.intermediateStations.none { it.hasCoordinates },
                        )
                    },
                    kind = PstButtonKind.FILLED, height = 40.dp, fontSize = 14,
                )
            }
        }
        message?.let { Text(it, style = Pst.sans.copy(fontSize = 12.sp, lineHeight = 16.sp), color = Pst.danger) }
    }
}

/** Результат автоподбора, применённый целиком (все найденные станции, расстояние по карте, координаты крайних станций): «проверено». */
private fun determinedPhase(phase: ServicePhase, ready: AutoFillState.Ready): ServicePhase {
    // Только станции: остановочные пункты (о.п.) автоопределение не добавляет.
    val found = ready.stations.filter { it.kind != "halt" }.map { it.toPhaseStation() }
    val (merged, _) = ServicePhaseStationsMerge.merge(phase.intermediateStations, found, found.map { it.id }.toSet())
    val km = kotlin.math.round(ready.lengthKm).toInt()
    val updated = phase.copy(
        distance = km,
        intermediateStations = merged,
        departurePoint = phase.departurePoint ?: ServicePhasePoint(ready.departure.latitude, ready.departure.longitude, ready.departure.osmId, ready.departure.esr?.takeIf(String::isNotBlank)),
        arrivalPoint = phase.arrivalPoint ?: ServicePhasePoint(ready.arrival.latitude, ready.arrival.longitude, ready.arrival.osmId, ready.arrival.esr?.takeIf(String::isNotBlank)),
    )
    return updated
}

/** Подпись «измерено по карте» по координатам плеча в том виде, в каком оно будет показано. */
private fun withSignature(phase: ServicePhase): ServicePhase = phase.copy(measuredSignature = phase.measuredSignatureFor(phase.distance))


private fun com.z_company.repository.remote_rest.response.OsmPathStation.toPhaseStation() = ServicePhaseStation(
    name = name,
    latitude = latitude,
    longitude = longitude,
    kmFromStart = kmFromStart,
    kind = if (kind == "halt") ServicePhaseStationKind.HALT else ServicePhaseStationKind.STATION,
    esr = esr?.takeIf(String::isNotBlank),
    osmId = osmId,
    source = ServicePhaseStationSource.OSM,
)
