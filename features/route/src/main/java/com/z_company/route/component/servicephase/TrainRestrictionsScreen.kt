package com.z_company.route.component.servicephase

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import com.z_company.domain.track.RestrictionJournalStore
import com.z_company.domain.track.RestrictionPass
import com.z_company.domain.track.RestrictionReport
import com.z_company.domain.track.RestrictionStatus
import com.z_company.domain.util.TrainMapStop
import com.z_company.route.autorecord.TrackUploadTrigger
import com.z_company.route.component.AppAlertDialog
import com.z_company.route.component.PdfActionSheet
import com.z_company.route.util.RestrictionPdfGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject

/**
 * «Ограничения поезда» (раздел «Поезд»): журнал проследования (заход головой и выход всем составом, скорость, максимум,
 * превышение), добавление предупреждений (тот же экран «Предупреждения», что и в профиле пути; участок определяется по
 * плечу поезда - добавлять можно, только если плечо выбрано) и PDF-отчёт, когда все ограничения проследованы.
 */
@Composable
internal fun TrainRestrictionsScreen(
    trainId: String,
    trainTitle: String,
    /** Плечо поезда, например «Лужская — Гатчина»; null - не выбрано (добавлять ограничения нельзя, пока не выбрано). */
    legTitle: String?,
    /** Открыть существующий выбор плеча «Поезда» (результат сохраняется в поезд тем же путём и сразу виден в «Поезде», на карте и профиле). */
    onSelectLeg: () -> Unit,
    stops: List<TrainMapStop>,
    trains: List<ProfileTrain>,
    zone: String,
    onDismiss: () -> Unit,
) {
    val store: RestrictionJournalStore = koinInject()
    val upload: TrackUploadTrigger = koinInject()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var entries by remember { mutableStateOf(emptyList<RestrictionPass>()) }
    var showAdd by rememberSaveable { mutableStateOf(false) }
    var toDelete by remember { mutableStateOf<RestrictionPass?>(null) }
    var pdfUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var exporting by remember { mutableStateOf(false) }

    // Запись идёт в фоне (АВТО), поэтому список обновляется, пока экран открыт.
    LaunchedEffect(trainId, showAdd) {
        while (true) {
            entries = withContext(Dispatchers.Default) { runCatching { store.forTrain(trainId) }.getOrDefault(emptyList()) }
            delay(2_000)
        }
    }
    val legSelected = legTitle != null
    val rows = remember(entries, zone) { RestrictionReport.rows(entries, zone) }
    val ready = RestrictionReport.isReady(entries)
    val left = RestrictionReport.remaining(entries)

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Column(
            modifier = Modifier.fillMaxSize().background(Pst.bg).statusBarsPadding().navigationBarsPadding().imePadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MapButton(onClick = onDismiss, description = "Назад") { PstIcon(PstIcons.ChevronLeft, 22.dp, Pst.text) }
                Column(Modifier.weight(1f)) {
                    Text("Ограничения поезда", style = Pst.sans.copy(fontSize = 18.sp, fontWeight = FontWeight.SemiBold), color = Pst.text)
                    Text(trainTitle, style = Pst.sans.copy(fontSize = 12.sp), color = Pst.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            LegRow(title = legTitle, onSelectLeg = onSelectLeg)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                PstButton(
                    "Добавить", { showAdd = true }, kind = PstButtonKind.FILLED, enabled = legSelected, height = 44.dp, fontSize = 14,
                    modifier = Modifier.weight(1f),
                )
                PstButton(
                    if (exporting) "Формируем…" else "Отчёт PDF",
                    {
                        exporting = true
                        scope.launch {
                            try {
                                val file = withContext(Dispatchers.IO) { RestrictionPdfGenerator(context).generate(trainTitle, rows) }
                                pdfUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Не удалось сформировать PDF", Toast.LENGTH_LONG).show()
                            } finally {
                                exporting = false
                            }
                        }
                    },
                    kind = PstButtonKind.TONAL, enabled = ready && !exporting, height = 44.dp, fontSize = 14, modifier = Modifier.weight(1f),
                )
            }
            val hint = when {
                !legSelected -> "Чтобы добавлять ограничения, выберите плечо поезда: участок определяется по нему."
                rows.isEmpty() -> "Ограничений нет. «Добавить» — выбрать станции или перегоны, где действуют предупреждения; при включённом «АВТО» время и скорость проследования запишутся сами."
                !ready -> "Отчёт будет доступен, когда все ограничения проследованы (осталось: $left)."
                else -> "Все ограничения проследованы — можно сформировать отчёт."
            }
            Text(hint, style = Pst.sans.copy(fontSize = 12.sp, lineHeight = 16.sp), color = Pst.textMuted)
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(entries.filter { !it.isDeleted }.sortedWith(compareBy({ it.enterAt ?: Long.MAX_VALUE }, { it.updatedAt })), key = { it.warningId }) { e ->
                    val row = RestrictionReport.rows(listOf(e), zone).first()
                    RestrictionCard(row, e) { toDelete = e }
                }
            }
        }
    }

    if (showAdd) {
        RestrictionAddHost(trainId = trainId, stops = stops, trains = trains, onClose = { showAdd = false; upload.trigger() })
    }
    toDelete?.let { e ->
        AppAlertDialog(
            onDismissRequest = { toDelete = null },
            title = "Убрать ограничение из поезда?",
            text = if (e.hasData) "Записанные время и скорость проследования будут удалены." else "Предупреждение останется на участке, но этот поезд его больше не учитывает.",
            confirmText = "Убрать",
            isDestructive = true,
            dismissText = "Отмена",
            onConfirm = {
                toDelete = null
                scope.launch(Dispatchers.Default) {
                    runCatching { store.remove(e.trainId, e.warningId, System.currentTimeMillis()) }
                    entries = runCatching { store.forTrain(trainId) }.getOrDefault(emptyList())
                    upload.trigger()
                }
            },
        )
    }
    pdfUri?.let { PdfActionSheet(uri = it, onDismiss = { pdfUri = null }) }
}

@Composable
private fun RestrictionCard(row: RestrictionReport.Row, e: RestrictionPass, onRemove: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().background(Pst.surface, RoundedCornerShape(14.dp)).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(row.title, style = Pst.sans.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold), color = Pst.text, modifier = Modifier.weight(1f), maxLines = 2)
            if (row.exceeded) Text("ПРЕВЫШЕНИЕ", style = Pst.mono.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold), color = Pst.danger)
        }
        val where = listOfNotNull(row.place.ifBlank { null }, row.kmText).joinToString(" · ")
        if (where.isNotEmpty()) Text(where, style = Pst.sans.copy(fontSize = 11.sp), color = Pst.textMuted)
        Text("Заход головой: ${row.enter}", style = Pst.sans.copy(fontSize = 12.sp), color = Pst.text)
        if (e.kind == "speed") {
            Text("Выход составом: ${row.exit}", style = Pst.sans.copy(fontSize = 12.sp), color = Pst.text)
            Text(
                "Максимум: ${row.maxSpeed}" + (row.limit?.let { " при ограничении $it" } ?: ""),
                style = Pst.sans.copy(fontSize = 12.sp, fontWeight = if (row.exceeded) FontWeight.Bold else FontWeight.Normal),
                color = if (row.exceeded) Pst.danger else Pst.text,
            )
        }
        val status = when (e.status) {
            RestrictionStatus.PLANNED -> "Ожидает проследования"
            RestrictionStatus.IN_ZONE -> "Состав в зоне"
            RestrictionStatus.DONE -> "Проследовано"
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(status, style = Pst.mono.copy(fontSize = 10.sp), color = Pst.textMuted, modifier = Modifier.weight(1f))
            Text(
                "Убрать", style = Pst.sans.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold), color = Pst.danger,
                modifier = Modifier.clickable(onClick = onRemove).padding(8.dp),
            )
        }
    }
}

/**
 * Добавление предупреждений из раздела «Поезд»: тот же экран [WarningsDialog], что и в профиле пути. Участок определяется по
 * плечу поезда (один подходящий - берётся сам, несколько - выбор из списка). Добавленное сохраняется в хранилище предупреждений
 * участка (видно на профиле и на карте) и привязывается к поезду (журнал).
 */
@Composable
private fun RestrictionAddHost(trainId: String, stops: List<TrainMapStop>, trains: List<ProfileTrain>, onClose: () -> Unit) {
    val model = rememberProfileModel(null, null, 0L, stops, trains, trainId)
    var showVoice by rememberSaveable { mutableStateOf(false) }
    var picked by remember { mutableStateOf(false) }
    // Подходящие плечу участки; если по названиям не определились - все (машинист выберет сам).
    val candidates = remember(model.loaded, model.summaries, model.stopNames) {
        if (!model.loaded) emptyList() else model.rows("").let { all -> all.filter { it.fitsLeg == true }.ifEmpty { all } }
    }

    if (model.live != null) {
        WarningsDialog(model, onVoice = { showVoice = true }, onDismiss = onClose)
        if (showVoice) WarningVoiceDialog(model, onDismiss = { showVoice = false })
        WarningEditDialog(model)
        WarningOfferDialog(model)
        return
    }
    when {
        model.needLogin -> InfoDialog("Профиль участков доступен после входа в аккаунт.", onClose)
        !model.loaded -> Unit
        candidates.isEmpty() -> InfoDialog("Нет доступных участков с профилем пути. Проверьте интернет или добавьте ограничения в профиле пути на карте.", onClose)
        candidates.size == 1 -> LaunchedEffect(candidates[0].summary.id) {
            if (!picked) { picked = true; model.choose(candidates[0].summary.id) }
        }
        else -> Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Column(
                Modifier.fillMaxWidth(0.9f).background(Pst.bg, RoundedCornerShape(20.dp)).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("Участок плеча", style = Pst.sans.copy(fontSize = 18.sp, fontWeight = FontWeight.SemiBold), color = Pst.text)
                candidates.forEach { r ->
                    Text(
                        r.summary.name, style = Pst.sans.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold), color = Pst.text,
                        modifier = Modifier.fillMaxWidth().background(Pst.surface, RoundedCornerShape(12.dp))
                            .clickable { model.choose(r.summary.id) }.padding(14.dp),
                    )
                }
                PstButton("Отмена", onClose, kind = PstButtonKind.TEXT, height = 40.dp, fontSize = 14)
            }
        }
    }
    model.chooseError?.let { InfoDialog(it, onClose) }
}

@Composable
private fun InfoDialog(text: String, onClose: () -> Unit) {
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier.fillMaxWidth(0.85f).background(Pst.bg, RoundedCornerShape(20.dp)).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(text, style = Pst.sans.copy(fontSize = 14.sp, lineHeight = 20.sp), color = Pst.text)
            PstButton("Понятно", onClose, kind = PstButtonKind.FILLED, height = 40.dp, fontSize = 14)
        }
    }
}
