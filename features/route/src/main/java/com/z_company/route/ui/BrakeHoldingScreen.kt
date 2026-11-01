package com.z_company.route.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.ui.Alignment
import com.z_company.domain.brake.BrakeHoldingRadio
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.z_company.domain.brake.BrakeHolding
import com.z_company.domain.brake.HoldingLoad
import com.z_company.domain.brake.HoldingResult
import com.z_company.route.component.HeaderBackTile
import com.z_company.route.component.ScreenHeader
import com.z_company.route.viewmodel.BrakeHoldingUiState
import com.z_company.route.viewmodel.BrakeHoldingViewModel
import com.z_company.route.viewmodel.autoLoad
import com.z_company.route.viewmodel.load
import com.z_company.route.viewmodel.toInput
import com.z_company.domain.util.toFiniteDoubleOrNull

private val HOLDING_TABS = listOf("Расчёт", "Порядок действий")
private val QUICK_SLOPES = listOf(6, 8, 10, 12, 14, 16, 18, 20)

/** Инструмент «Закрепление поезда» (SCREEN_SPECS §27): башмаки и ручные тормоза при вынужденной остановке на перегоне. */
@Composable
fun BrakeHoldingScreen(
    state: BrakeHoldingUiState,
    onBack: () -> Unit,
    viewModel: BrakeHoldingViewModel,
) {
    val t = vuColors()
    var tab by remember { mutableIntStateOf(0) }
    Scaffold(
        containerColor = t.bg,
        topBar = { ScreenHeader(title = "Закрепление поезда", navigation = { HeaderBackTile(onBack) }) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).imePadding()) {
            VuTabs(HOLDING_TABS, tab, { tab = it }, modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 8.dp))
            when (tab) {
                0 -> HoldingCalcTab(state, viewModel)
                else -> HoldingStepsTab(state, viewModel)
            }
        }
    }
}

@Composable
private fun HoldingCalcTab(state: BrakeHoldingUiState, vm: BrakeHoldingViewModel) {
    val t = vuColors()
    val input = remember(state) { state.toInput() }
    val result = remember(input) { input?.let(BrakeHolding::calculate) }
    val auto = state.autoLoad()
    val load = state.load()
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (state.fromTrain) {
            VuHint("Данные поезда подставлены из раздела «Поезд»: проверьте и при необходимости поправьте.")
        }
        VuBlock("ПОЕЗД") {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                VuInput("Вес состава, т", state.weightText, Modifier.weight(1f), onChange = vm::setWeight, unit = "т", sunk = true)
                VuInput("Вес локомотива, т", state.locoWeightText, Modifier.weight(1f), onChange = vm::setLocoWeight, unit = "т", sunk = true)
            }
            VuInput("Всего осей состава", state.axlesText, onChange = vm::setAxles, sunk = true, keyboard = KeyboardType.Number, maxLen = 4)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Состав", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = t.muted)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    VuChip("Груженый", load == HoldingLoad.LOADED, { vm.setLoad(HoldingLoad.LOADED) }, Modifier.widthIn(min = 96.dp))
                    VuChip("Порожний", load == HoldingLoad.EMPTY, { vm.setLoad(HoldingLoad.EMPTY) }, Modifier.widthIn(min = 96.dp))
                    if (state.loadManual != null && auto != null) VuChip("По нагрузке на ось", false, { vm.setLoad(null) })
                }
                val axleLoad = state.weightText.toFiniteDoubleOrNull()?.let { w -> state.axlesText.trim().toIntOrNull()?.takeIf { it > 0 }?.let { w / it } }
                VuHint(
                    when {
                        state.loadManual != null -> "Выбрано вручную."
                        axleLoad != null -> "Определено по нагрузке: ${fmtHold(axleLoad, 1)} т/ось (10 т и более - груженый, менее - порожний)."
                        else -> "Укажите вес и число осей или выберите вручную. Расчёт - для однородного состава (целиком груженого или порожнего)."
                    },
                )
            }
        }
        VuBlock("УЧАСТОК И СРЕДСТВА") {
            VuInput("Уклон (спуск), ‰", state.slopeText, onChange = vm::setSlope, unit = "‰", sunk = true)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                QUICK_SLOPES.forEach { s ->
                    VuChip(s.toString(), state.slopeText.trim().replace(',', '.') == s.toString(), { vm.setSlope(s.toString()) }, Modifier.widthIn(min = 48.dp))
                }
            }
            VuHint("Уклон в тысячных: 0,018 = 18 ‰. Берите самый крутой спуск участка (режимная карта). Значения между ступенями таблицы округляются вверх.")
            VuInput("Башмаков на локомотиве, шт", state.shoesText, onChange = vm::setShoes, sunk = true, keyboard = KeyboardType.Number, maxLen = 3)
        }
        HoldingResultCard(result, state)
        VuHint(
            "Расчёт по Порядку определения потребного количества ручных тормозов и тормозных башмаков для закрепления поезда на перегоне; " +
                "нормы на 100 тс веса - " + com.z_company.domain.brake.InstructionRef.table("4") + " (в Правилах 2014 г. - табл. III.4). " +
                "Масса локомотива учитывается. Уклон, которого нет в таблице, берётся ближайший больший.",
        )
        VuHint("Справочный инструмент: решение о закреплении принимает машинист по инструкциям владельца инфраструктуры.")
    }
}

@Composable
private fun HoldingResultCard(result: HoldingResult?, state: BrakeHoldingUiState) {
    val t = vuColors()
    VuBlock("РЕЗУЛЬТАТ") {
        if (result == null) {
            val slope = state.slopeText.toFiniteDoubleOrNull()
            VuHint(
                when {
                    state.weightText.toFiniteDoubleOrNull()?.let { it > 0.0 } != true -> "Введите вес состава."
                    state.load() == null -> "Выберите, груженый состав или порожний."
                    slope == null -> "Введите уклон."
                    slope > 40.0 -> "Уклон круче 0,040: таблицы нет, закрепление определяет владелец инфраструктуры."
                    else -> "Введите данные."
                },
            )
            return@VuBlock
        }
        Text(
            "Башмаков: ${result.shoesRequired}",
            fontFamily = VuMono, fontSize = 30.sp, fontWeight = FontWeight.Bold, color = t.text,
        )
        VuHint(
            "Масса с локомотивом ${fmtHold(result.totalMass, 0)} т; уклон ${fmtSlope(result.slopeStepPermille)}" +
                (if (result.slopeRoundedUp) " (округлён вверх от введённого)" else "") +
                ": на 100 тс веса ${fmtHold(result.shoesPer100, 1)} башмака" +
                (result.manualAxlesPer100?.let { " или ${fmtHold(it, 1)} ручной оси" } ?: ", ручными тормозами не закрепляется"),
        )
        if (result.shoesUsed >= result.shoesRequired) {
            VuVerdict(true, "Имеющихся башмаков (${result.shoesUsed}) достаточно")
        } else {
            val missing = result.shoesRequired - result.shoesUsed
            VuWarnVerdict("Башмаков не хватает: $missing. Закрепляется ${fmtHold(result.coveredMass, 0)} т из ${fmtHold(result.totalMass, 0)} т")
            if (result.manualAxles != null) {
                Text(
                    "Ручных тормозных осей: ${result.manualAxles}",
                    fontFamily = VuMono, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = t.text,
                )
                VuHint(
                    "На остаток ${fmtHold(result.remainingMass, 0)} т. Вагонов с ручным тормозом: ${result.manualWagons4} (по 4 оси); " +
                        "при раздельном торможении (два тормозных цилиндра): ${result.manualWagons2} (по 2 оси).",
                )
            }
        }
        result.warnings.forEach { VuVerdict(false, it, size = 14) }
        VuHint("Башмаки укладывают со стороны уклона. Нельзя: между колёсными парами одной тележки, перед стыком, перед крестовиной, на рамный рельс стрелки, на наружный рельс кривой.")
    }
}

private class HoldingStep(val time: String, val title: String, val who: String, val text: String)

private val HOLDING_STEPS = listOf(
    HoldingStep("30 с", "Остановка на площадке и прямом", "Машинист", "Если не нужна экстренная остановка - остановить поезд по возможности на площадке и прямом участке (по режимной карте). Полное служебное торможение: разрядка УР на 1,5-1,7 кгс/см² за один приём, кран № 395: V, затем III положение."),
    HoldingStep("2 с", "Автотормоз и вспомогательный", "Машинист", "Кран № 395 в III положении; вспомогательный кран № 254 - в крайнее VI положение, ручку зафиксировать на защёлку."),
    HoldingStep("30 с", "Место остановки", "Машинист", "Определить по ближайшим километровому и пикетному столбикам."),
    HoldingStep("1-2 мин", "Радиосвязь об остановке", "Машинист", "«Внимание все! Я, машинист (фамилия) поезда № … остановился на … километре … пикете … пути перегона … вследствие … Будьте бдительны!» Получить подтверждения от встречных и вслед идущих, ДСП, ДНЦ. Время и фамилии - на обороте бланка ДУ-61."),
    HoldingStep("1,5 мин", "Подготовка", "Помощник", "Красные огни буферных фонарей; набрать воздух в запасный резервуар токоприёмника; убедиться, что поезд заторможен и вспомогательный кран зафиксирован."),
    HoldingStep("до 10 мин", "Вспомогательный локомотив", "Машинист", "Затребовать НЕ ПОЗДНЕЕ 10 минут после остановки: «Я машинист … поезда № …, на … км … пикете перегона … требую вспомогательный локомотив по причине неисправности локомотива секции …, серии …, № …, из-за … Время … час … мин»."),
    HoldingStep("2-3 мин", "Расчёт закрепления", "Помощник", "По натурному листу определить массу и груженые вагоны; рассчитать башмаки и ручные тормоза (вкладка «Расчёт»). Ручной тормоз локомотива - по часовой стрелке до упора."),
    HoldingStep("5-90 мин", "Закрепление", "Помощник", "Башмаки - под колёса со стороны уклона (носок полоза касается обода). Недостаток - ручные тормоза вагонов. Доложить ДСП/ДНЦ: число башмаков и вагонов с ручным тормозом. Запись в журнал формы ТУ-152."),
    HoldingStep("-", "Устранение неисправности", "Машинист", "Не отвлекаться на радиопереговоры (ведёт помощник). Захода в высоковольтную камеру - только после опускания токоприёмников и проверки отсутствия напряжения по вольтметру. Помощник отсчитывает время от остановки."),
)

@Composable
private fun HoldingStepsTab(state: BrakeHoldingUiState, vm: BrakeHoldingViewModel) {
    val t = vuColors()
    val context = androidx.compose.ui.platform.LocalContext.current
    val view = androidx.compose.ui.platform.LocalView.current
    val running = state.stopStartMs != null
    // таймер: тик раз в секунду, пока идёт остановка; экран не гаснет
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(running) {
        while (running) { now = System.currentTimeMillis(); kotlinx.coroutines.delay(1000) }
    }
    DisposableEffect(running) {
        view.keepScreenOn = running
        onDispose { view.keepScreenOn = false }
    }
    val elapsedSec = state.stopStartMs?.let { ((now - it) / 1000).coerceAtLeast(0) }
    val deadlineSec = BrakeHoldingRadio.HELP_LOCO_DEADLINE_MIN * 60L
    val stopTime = state.stopStartMs?.let { java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(it)) }
    val radio = state.radio.copy(time = stopTime.orEmpty())

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        VuHint("Порядок действий при вынужденной остановке поезда на перегоне из-за неисправности локомотива и невозможности удержать поезд (распоряжение ОАО «РЖД» № ЦТ-162/р от 28.04.2025).")
        VuBlock("ВРЕМЯ ОТ ОСТАНОВКИ") {
            if (elapsedSec == null) {
                VuBtn("Остановились", { vm.startStop() }, Modifier.fillMaxWidth())
                VuHint("Нажмите после остановки: пойдёт отсчёт, а время подставится в сообщение о вспомогательном локомотиве.")
            } else {
                Text(fmtClock(elapsedSec), fontFamily = VuMono, fontSize = 34.sp, fontWeight = FontWeight.Bold, color = t.text)
                val left = deadlineSec - elapsedSec
                if (left >= 0) {
                    VuVerdict(true, "Вспомогательный локомотив - затребовать не позднее ${BrakeHoldingRadio.HELP_LOCO_DEADLINE_MIN} мин после остановки: осталось ${fmtClock(left)}")
                } else {
                    VuVerdict(false, "Срок вызова вспомогательного локомотива (${BrakeHoldingRadio.HELP_LOCO_DEADLINE_MIN} мин) прошёл на ${fmtClock(-left)}: затребуйте немедленно")
                }
                VuHint("Остановка в $stopTime.")
                VuBtn("Сбросить", { vm.resetStop() }, Modifier.fillMaxWidth(), kind = VuBtnKind.Tonal)
            }
        }
        HOLDING_STEPS.forEachIndexed { i, s ->
            val done = i in state.doneSteps
            VuCard(Modifier.clickable { vm.toggleStep(i) }) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        VuTag("${i + 1}. ${s.who}")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            VuTag(s.time, color = t.accent)
                            Box(
                                Modifier.size(22.dp).background(if (done) t.success else t.bgSubtle, CircleShape),
                                contentAlignment = Alignment.Center,
                            ) { if (done) Text("✓", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = t.bg) }
                        }
                    }
                    Text(s.title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = if (done) t.muted else t.text)
                    Text(s.text, fontSize = 14.sp, lineHeight = 20.sp, color = if (done) t.muted else t.text)
                }
            }
        }
        VuVerdict(false, "Отпуск тормозов поезда до устранения неисправности или прицепки вспомогательного локомотива ЗАПРЕЩЕНО.")
        VuHint("Расчёт закрепления бригада делает заранее - перед отправлением с начальной станции (пункта смены бригад) по самому крутому уклону участка - и фиксирует на бланке ДУ-61.")

        VuBlock("СООБЩЕНИЯ ПО РАДИО") {
            VuInput("Фамилия машиниста", state.radio.surname, onChange = { v -> vm.setRadio { it.copy(surname = v) } }, sunk = true, text = true, maxLen = 40)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                VuInput("Поезд №", state.radio.trainNumber, Modifier.weight(1f), onChange = { v -> vm.setRadio { it.copy(trainNumber = v) } }, sunk = true, text = true)
                VuInput("Километр", state.radio.km, Modifier.weight(1f), onChange = { v -> vm.setRadio { it.copy(km = v) } }, sunk = true, keyboard = KeyboardType.Number, maxLen = 5)
                VuInput("Пикет", state.radio.picket, Modifier.weight(1f), onChange = { v -> vm.setRadio { it.copy(picket = v) } }, sunk = true, keyboard = KeyboardType.Number, maxLen = 2)
            }
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                VuChip("Чётный путь", state.radio.evenTrack == true, { vm.setRadio { it.copy(evenTrack = if (it.evenTrack == true) null else true) } }, Modifier.widthIn(min = 110.dp))
                VuChip("Нечётный путь", state.radio.evenTrack == false, { vm.setRadio { it.copy(evenTrack = if (it.evenTrack == false) null else false) } }, Modifier.widthIn(min = 110.dp))
            }
            VuInput("Перегон", state.radio.stretch, onChange = { v -> vm.setRadio { it.copy(stretch = v) } }, sunk = true, text = true, maxLen = 60)
            VuInput("Причина", state.radio.cause, onChange = { v -> vm.setRadio { it.copy(cause = v) } }, sunk = true, text = true, maxLen = 80)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                VuInput("Секция", state.radio.locoSection, Modifier.weight(0.7f), onChange = { v -> vm.setRadio { it.copy(locoSection = v) } }, sunk = true, text = true, maxLen = 3)
                VuInput("Серия", state.radio.locoSeries, Modifier.weight(1f), onChange = { v -> vm.setRadio { it.copy(locoSeries = v) } }, sunk = true, text = true, maxLen = 12)
                VuInput("Номер", state.radio.locoNumber, Modifier.weight(1f), onChange = { v -> vm.setRadio { it.copy(locoNumber = v) } }, sunk = true, text = true, maxLen = 8)
            }
        }
        HoldingMessageCard("Об остановке", BrakeHoldingRadio.stopMessage(radio), context)
        HoldingMessageCard("Требование вспомогательного локомотива", BrakeHoldingRadio.helpLocoMessage(radio), context)
        VuHint("Тексты - по технологической карте ЦТ-162/р (операции 4.1 и 7.2). Время записать на обороте бланка ДУ-61 и в журнал формы ТУ-152.")
    }
}

@Composable
private fun HoldingMessageCard(title: String, text: String, context: android.content.Context) {
    val t = vuColors()
    VuCard {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            VuTag(title)
            Text(text, fontSize = 15.sp, lineHeight = 22.sp, color = t.text)
            VuBtn("Копировать", {
                val cm = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                cm.setPrimaryClip(android.content.ClipData.newPlainText(title, text))
            }, Modifier.fillMaxWidth(), kind = VuBtnKind.Tonal, h = 44.dp)
        }
    }
}

private fun fmtClock(totalSec: Long): String = "%02d:%02d".format(totalSec / 60, totalSec % 60)

private fun fmtHold(v: Double, digits: Int): String {
    val f = when (digits) { 0 -> 1.0; 1 -> 10.0; else -> 100.0 }
    val r = kotlin.math.round(v * f) / f
    return (if (r % 1.0 == 0.0) r.toLong().toString() else r.toString()).replace('.', ',')
}

private fun fmtSlope(permille: Int): String = "0,${permille.toString().padStart(3, '0')}"
