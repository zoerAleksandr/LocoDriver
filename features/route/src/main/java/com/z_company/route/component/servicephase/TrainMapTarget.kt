package com.z_company.route.component.servicephase

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.z_company.domain.util.ArrivalPlan
import com.z_company.domain.util.ArrivalPlanData
import com.z_company.domain.util.PlanTarget
import com.z_company.domain.util.TimeMask

/**
 * Экран «Цель» (SCREEN_SPECS §6.9, состояние 7): шаг 1 - станция впереди, шаг 2 - время прибытия, «на остановку»
 * (время запоминается), «показывать цель на экране Карты». Цель снимается только здесь.
 *
 * [preselect] - точка, выбранная тапом на карте (сразу шаг 2). [distanceM]/[note] - расстояние и пояснение для текущей цели.
 */
@Composable
internal fun TrainMapTargetScreen(
    ahead: List<ArrivalPlan.TargetStop>,
    plan: ArrivalPlanData?,
    preselect: PlanTarget?,
    distanceM: Double?,
    note: String?,
    nowMs: Long,
    zone: String,
    speedKmh: Double?,
    onSave: (ArrivalPlanData) -> Unit,
    onCancelTarget: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var picked by remember { mutableStateOf(preselect) }
    var changing by remember { mutableStateOf(false) }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Box(Modifier.fillMaxSize().background(Pst.bg)) {
            Column(Modifier.fillMaxSize().statusBarsPadding().imePadding()) {
                PstSheetHeader(
                    title = TrainMapPlanTexts.TARGET_TITLE,
                    onClose = onDismiss,
                )
                Column(
                    modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    val p = picked
                    when {
                        p != null -> TargetForm(context, p, plan, nowMs, zone, onBack = { picked = null }, onSave = {
                            picked = null
                            changing = false
                            onSave(it)
                        })
                        plan != null && !changing -> TargetSummary(
                            plan, distanceM, note, nowMs, zone, speedKmh,
                            onShowOnMap = { onSave(plan.copy(showOnMap = it)) },
                            onChange = { changing = true },
                            onCancelTarget = onCancelTarget,
                            onDone = onDismiss,
                        )
                        else -> TargetList(ahead, onPick = { picked = it })
                    }
                }
            }
        }
    }
}

@Composable
private fun TargetList(ahead: List<ArrivalPlan.TargetStop>, onPick: (PlanTarget) -> Unit) {
    PstGroupHead(TrainMapPlanTexts.STEP1)
    if (ahead.isEmpty()) {
        PstNote(TrainMapPlanTexts.NO_STOPS)
        return
    }
    Column(
        modifier = Modifier.fillMaxWidth().background(Pst.surface, RoundedCornerShape(16.dp)),
    ) {
        ahead.forEachIndexed { i, a ->
            Row(
                modifier = Modifier.fillMaxWidth().clickable { onPick(a.target) }.padding(horizontal = 14.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    a.target.name.orEmpty().ifBlank { TrainMapPlanTexts.DEFAULT_TITLE },
                    style = Pst.sans.copy(fontSize = 15.sp, fontWeight = FontWeight.Medium),
                    color = Pst.text,
                    maxLines = 2,
                    modifier = Modifier.weight(1f),
                )
                a.distanceM?.let { Text(TrainMapEta.distance(it), style = Pst.mono.copy(fontSize = 13.sp), color = Pst.textMuted, maxLines = 1) }
            }
            if (i < ahead.lastIndex) androidx.compose.material3.HorizontalDivider(color = Pst.border)
        }
    }
}

@Composable
private fun TargetSummary(
    plan: ArrivalPlanData,
    distanceM: Double?,
    note: String?,
    nowMs: Long,
    zone: String,
    speedKmh: Double?,
    onShowOnMap: (Boolean) -> Unit,
    onChange: () -> Unit,
    onCancelTarget: () -> Unit,
    onDone: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().background(Pst.surface, RoundedCornerShape(16.dp)).padding(14.dp),
    ) { ArrivalPlanBody(plan, distanceM, note, nowMs, zone, speedKmh) }
    CheckRow(TrainMapPlanTexts.SHOW_ON_MAP, plan.showOnMap) { onShowOnMap(!plan.showOnMap) }
    PstButton(TrainMapPlanTexts.DONE, onClick = onDone, kind = PstButtonKind.CTA, modifier = Modifier.fillMaxWidth())
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        PstButton(TrainMapPlanTexts.CHANGE, onClick = onChange, kind = PstButtonKind.OUTLINED, height = 48.dp)
        PstButton(TrainMapPlanTexts.CANCEL_TARGET, onClick = onCancelTarget, kind = PstButtonKind.DANGER, height = 48.dp)
    }
}

@Composable
private fun CheckRow(text: String, checked: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, style = Pst.sans.copy(fontSize = 14.sp), color = Pst.text, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = { onToggle() },
            colors = SwitchDefaults.colors(checkedTrackColor = Pst.accent),
        )
    }
}

@Composable
private fun TargetForm(
    context: Context,
    target: PlanTarget,
    plan: ArrivalPlanData?,
    nowMs: Long,
    zone: String,
    onBack: () -> Unit,
    onSave: (ArrivalPlanData) -> Unit,
) {
    var time by remember { mutableStateOf(TextFieldValue("")) }
    var timeError by remember { mutableStateOf(false) }
    var pendingNextDay by remember { mutableStateOf<Long?>(null) }
    var onStop by remember { mutableStateOf(TrainMapPlanStore.onStop(context)) }
    var stopText by remember { mutableStateOf(TextFieldValue(TrainMapPlanStore.stopMin(context).toString())) }
    var showOnMap by remember { mutableStateOf(plan?.showOnMap ?: true) }

    fun finish(ms: Long) {
        val min = stopText.text.toIntOrNull()?.coerceIn(0, 60) ?: ArrivalPlan.DEFAULT_STOP_MIN
        TrainMapPlanStore.saveStop(context, onStop, min)
        onSave(ArrivalPlanData(target, ms, onStop = onStop, stopMin = min, showOnMap = showOnMap))
    }

    PstGroupHead(TrainMapPlanTexts.STEP2)
    Row(
        modifier = Modifier.fillMaxWidth().background(Pst.surface, RoundedCornerShape(16.dp)).clickable(onClick = onBack).padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PstIcon(PstIcons.MapPin, 18.dp, Pst.accent)
        Text(
            target.name ?: TrainMapPlanTexts.DEFAULT_TITLE,
            style = Pst.sans.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
            color = Pst.text,
            maxLines = 2,
            modifier = Modifier.weight(1f),
        )
        Text(TrainMapPlanTexts.CHANGE.replace("цель", "станцию"), style = Pst.sans.copy(fontSize = 13.sp), color = Pst.accent)
    }
    PstTextField(
        value = time,
        onValueChange = { v ->
            val clean = TimeMask.format(v.text, previous = time.text)
            time = if (clean == v.text) v else TextFieldValue(clean, TextRange(clean.length))
            timeError = false
            pendingNextDay = null
        },
        label = TrainMapPlanTexts.FIELD_LABEL,
        placeholder = TrainMapPlanTexts.FIELD_PLACEHOLDER,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        mono = true,
        isError = timeError,
        modifier = Modifier.fillMaxWidth(),
    )
    if (timeError) {
        Text(
            TrainMapPlanTexts.INPUT_ERROR,
            style = Pst.sans.copy(fontSize = 13.sp, lineHeight = 17.sp, fontWeight = FontWeight.SemiBold),
            color = Pst.danger,
        )
    }
    CheckRow(TrainMapPlanTexts.ON_STOP, onStop) { onStop = !onStop }
    if (onStop) {
        PstTextField(
            value = stopText,
            onValueChange = { v -> stopText = v.copy(text = v.text.filter { it.isDigit() }.take(2)) },
            label = TrainMapPlanTexts.STOP_MIN_LABEL,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            mono = true,
            modifier = Modifier.fillMaxWidth(),
        )
    }
    CheckRow(TrainMapPlanTexts.SHOW_ON_MAP, showOnMap) { showOnMap = !showOnMap }
    val pending = pendingNextDay
    if (pending != null) {
        Text(TrainMapPlanTexts.NEXT_DAY_ASK, style = Pst.sans.copy(fontSize = 13.sp, lineHeight = 18.sp), color = Pst.text)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PstButton(TrainMapPlanTexts.NEXT_DAY_YES, onClick = { finish(pending) }, kind = PstButtonKind.CTA, height = 48.dp, fontSize = 14)
            PstButton(TrainMapPlanTexts.NEXT_DAY_EDIT, onClick = { pendingNextDay = null }, kind = PstButtonKind.TEXT, height = 48.dp, fontSize = 14)
        }
    } else {
        PstButton(
            text = TrainMapPlanTexts.CALC,
            kind = PstButtonKind.CTA,
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                val hm = ArrivalPlan.parseTime(time.text)
                if (hm == null) {
                    timeError = true
                } else {
                    val p = ArrivalPlan.planned(System.currentTimeMillis(), hm.first, hm.second, zone)
                    if (p.earlier) pendingNextDay = p.ms else finish(p.ms)
                }
            },
        )
    }
}
