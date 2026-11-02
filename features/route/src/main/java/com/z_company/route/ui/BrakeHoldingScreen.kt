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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import com.z_company.domain.brake.BrakeHoldingRadio
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
                Text(
                    "Вагонов с ручным тормозом: ${result.manualWagons}",
                    fontFamily = VuMono, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = t.text,
                )
                VuHint(
                    "Пересчёт на вагоны: остаток ${fmtHold(result.remainingMass, 0)} т = ${result.manualAxles} осей; ручной тормоз вагона действует на 4 оси, " +
                        "то есть ${result.manualAxles} / 4 → ${result.manualWagons} вагонов.",
                )
                VuWarnVerdict(
                    "У вагонов с раздельным торможением (два тормозных цилиндра) ДВА ручных тормоза, каждый на 2 оси: на таких вагонах " +
                        "затормозите оба - всего ${result.manualBrakesSplit} ручных тормозов на те же ${result.manualWagons} вагонов.",
                    size = 14,
                )
            }
        }
        result.warnings.forEach { VuVerdict(false, it, size = 14) }
        VuHint("Башмаки укладывают со стороны уклона. Нельзя: между колёсными парами одной тележки, перед стыком, перед крестовиной, на рамный рельс стрелки, на наружный рельс кривой.")
    }
}

/** Пункт шага: [who] - исполнитель по технологической карте ЦТ-162/р, если он отличается от исполнителя шага. */
private class HBullet(val text: String, val who: String? = null)

/**
 * Шаг порядка действий (технологическая карта ЦТ-162/р). [phase] - заголовок этапа над первым шагом этапа; [badge] - срок (красный чип);
 * [forbidden] - перечень запретов (красная плашка). Нумерация шагов сохраняется: 8 - закрепление, 9 - устранение неисправности.
 */
private class HStep(
    val title: String,
    val who: String,
    val bullets: List<HBullet>,
    val phase: String? = null,
    val badge: String? = null,
    val forbiddenTitle: String? = null,
    val forbidden: List<String> = emptyList(),
)

private const val MASH = "Машинист"
private const val POM = "Помощник"

private val HOLDING_STEPS = listOf(
    HStep(
        "Остановка и торможение", MASH, phase = "Сразу после остановки",
        bullets = listOf(
            HBullet("Если не нужна экстренная остановка - остановить поезд по возможности на площадке и прямом участке (по режимной карте)."),
            HBullet("Полное служебное торможение: разрядка УР на 0,15-0,17 МПа (1,5-1,7 кгс/см²) за один приём - кран № 395: V положение, затем III."),
        ),
    ),
    HStep(
        "Автотормоз и вспомогательный тормоз", MASH,
        bullets = listOf(
            HBullet("Убедиться, что кран № 395 в III положении."),
            HBullet("Вспомогательный кран № 254 - в крайнее VI положение, ручку зафиксировать на защёлку."),
        ),
    ),
    HStep(
        "Место остановки", MASH,
        bullets = listOf(HBullet("Определить по ближайшим километровому и пикетному столбикам (или кнопкой GPS ниже).")),
    ),
    HStep(
        "Радиосвязь об остановке", MASH,
        bullets = listOf(
            HBullet("Объявить остановку и причину машинистам поездов, следующих по перегону, и ДСП, ограничивающим перегон (текст - ниже, «Сообщения по радио»)."),
            HBullet("Получить подтверждение от встречных и вслед идущих поездов, ДСП, ДНЦ."),
            HBullet("Зафиксировать время передачи сообщения, а также время и фамилии подтвердивших - на обороте бланка ДУ-61.", POM),
            HBullet("Контролировать отсчёт времени от остановки и докладывать машинисту.", POM),
        ),
    ),
    HStep(
        "Подготовка к устранению неисправности", POM, phase = "Подготовка и вызов помощи",
        bullets = listOf(
            HBullet("Включить красные огни буферных фонарей на ведущей секции."),
            HBullet("Набрать воздух в запасный резервуар токоприёмника."),
            HBullet("Убедиться, что поезд заторможен, а ручка вспомогательного крана в крайнем тормозном положении и зафиксирована на защёлку."),
        ),
    ),
    HStep(
        "Вспомогательный локомотив", MASH, badge = "не позднее 10 мин после остановки",
        bullets = listOf(
            HBullet("Затребовать вспомогательный локомотив по радио у ДНЦ, ДСП (при диспетчерской централизации - у ДНЦ); текст - ниже."),
            HBullet("Указать помощнику определить число башмаков для удержания поезда с учётом веса."),
        ),
    ),
    HStep(
        "Расчёт закрепления", POM,
        bullets = listOf(
            HBullet("По натурному листу определить массу поезда, число осей и вагоны с нагрузкой на ось более 10 т (их номера с головы)."),
            HBullet("Рассчитать башмаки и ручные тормоза - вкладка «Расчёт»."),
            HBullet("Затормозить ручной тормоз локомотива: штурвал по часовой стрелке до упора."),
        ),
    ),
    HStep(
        "Закрепление", POM, phase = "Закрепление и устранение неисправности",
        bullets = listOf(
            HBullet("Башмаки укладывать под колёса вагонов со стороны уклона; носок полоза башмака должен касаться обода колеса."),
            HBullet("Разнородный состав (груженые и порожние вагоны): по натурному листу определить груженые вагоны и их номера."),
            HBullet("Сначала класть башмаки под груженые вагоны: так их нужно меньше (на груженый состав - 0,6 башмака на 100 тс при уклоне 0,018, на порожний - 1,6)."),
            HBullet("Если башмаки под вагоны с нагрузкой на ось более 10 т уложить нельзя - число башмаков считать по вагонам с нагрузкой менее 10 т."),
            HBullet("Поезд на переломном профиле: закреплять со стороны предполагаемого ухода, пропорционально весу частей поезда и величине уклона."),
            HBullet("Если башмаков на локомотиве не хватает - дополнительно затормозить ручные тормоза вагонов (число - во вкладке «Расчёт»)."),
            HBullet("Доложить ДСП, ограничивающему перегон (ДНЦ): сколько башмаков уложено и на скольких вагонах заторможены ручные тормоза.", MASH),
            HBullet("Записать в журнал формы ТУ-152: число башмаков и вагонов с ручными тормозами."),
        ),
        forbiddenTitle = "Башмаки нельзя укладывать",
        forbidden = listOf(
            "между колёсными парами одной тележки;",
            "перед рельсовым стыком;",
            "перед крестовиной стрелочного перевода;",
            "на рамный рельс стрелочного перевода;",
            "на наружный рельс кривой.",
        ),
    ),
    HStep(
        "Устранение неисправности", "$MASH, ${POM.lowercase()}",
        bullets = listOf(
            HBullet("Устранить неисправность по аварийной схеме, установленной заводом-изготовителем (перемычки, предохранители и т.п.).", MASH),
            HBullet("Не отвлекаться на радиопереговоры; при необходимости переговоры ведёт помощник, называя должность и фамилию.", MASH),
            HBullet("Только если для устранения нужен заход в высоковольтную камеру электровоза: визуально убедиться, что токоприёмники опущены, и по вольтметру - что напряжения нет.", POM),
            HBullet("Отсчитывать время от остановки и докладывать машинисту.", POM),
        ),
    ),
)

@Composable
private fun HoldingStepsTab(state: BrakeHoldingUiState, vm: BrakeHoldingViewModel) {
    val t = vuColors()
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
                VuHint("Нажмите после остановки: пойдёт отсчёт, а время подставится в сообщение о вспомогательном локомотиве. Время от остановки контролирует помощник машиниста и докладывает машинисту.")
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
            s.phase?.let { HoldingPhaseHeader(it) }
            HoldingStepRow(i + 1, s, done, last = i == HOLDING_STEPS.lastIndex, onToggle = { vm.toggleStep(i) })
        }
        VuVerdict(false, "Отпуск тормозов поезда до устранения неисправности или прицепки вспомогательного локомотива ЗАПРЕЩЕНО.")
        VuHint("Расчёт закрепления бригада делает заранее - перед отправлением с начальной станции (пункта смены бригад) по самому крутому уклону участка - и фиксирует на бланке ДУ-61.")

        VuBlock("СООБЩЕНИЯ ПО РАДИО") {
            HoldingGpsPlace(state, vm)
        }
        HoldingMessageCard("Об остановке", BrakeHoldingRadio.stopMessage(radio))
        HoldingMessageCard("Требование вспомогательного локомотива", BrakeHoldingRadio.helpLocoMessage(radio))
        VuHint("Тексты - по технологической карте ЦТ-162/р (операции 4.1 и 7.2): в них подставлены известные данные (поезд, локомотив, время, место по GPS), остальное - «…» - назовите сами. Время записать на обороте бланка ДУ-61 и в журнал формы ТУ-152.")
    }
}

/** Заголовок этапа: акцентная метка и линия. */
@Composable
private fun HoldingPhaseHeader(title: String) {
    val t = vuColors()
    Column(Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        VuTag(title, color = t.accent)
        Box(Modifier.fillMaxWidth().height(2.dp).background(t.accent.copy(alpha = 0.25f), CircleShape))
    }
}

/** Шаг: слева номер в кружке (тап - «выполнено») с вертикальной линией, справа карточка: исполнитель, срок, заголовок, пункты, запреты. */
@Composable
private fun HoldingStepRow(number: Int, step: HStep, done: Boolean, last: Boolean, onToggle: () -> Unit) {
    val t = vuColors()
    Row(Modifier.fillMaxWidth().height(androidx.compose.foundation.layout.IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.size(32.dp).background(if (done) t.success else t.accent, CircleShape).clickable(onClick = onToggle),
                contentAlignment = Alignment.Center,
            ) {
                Text(if (done) "✓" else number.toString(), fontFamily = VuMono, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = t.bg)
            }
            if (!last) Box(Modifier.padding(top = 4.dp).width(2.dp).weight(1f).background(t.border))
        }
        Box(Modifier.weight(1f).padding(bottom = if (last) 0.dp else 12.dp)) {
            VuCard(Modifier.clickable(onClick = onToggle)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    VuTag(step.who)
                    Text(step.title, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = if (done) t.muted else t.text, lineHeight = 22.sp)
                    step.badge?.let {
                        Text(
                            it.uppercase(), fontFamily = VuMono, fontSize = 11.sp, letterSpacing = 0.5.sp, fontWeight = FontWeight.Bold, color = t.bg,
                            modifier = Modifier.background(t.danger, RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        step.bullets.forEach { b ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("•", fontSize = 14.sp, color = t.accent, fontWeight = FontWeight.Bold)
                                Text(
                                    androidx.compose.ui.text.buildAnnotatedString {
                                        b.who?.let { w ->
                                            pushStyle(androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.SemiBold, color = t.accent))
                                            append("$w: ")
                                            pop()
                                        }
                                        append(b.text)
                                    },
                                    fontSize = 14.sp, lineHeight = 20.sp, color = if (done) t.muted else t.text,
                                )
                            }
                        }
                    }
                    if (step.forbidden.isNotEmpty()) {
                        Column(
                            Modifier.fillMaxWidth().background(t.danger.copy(alpha = 0.10f), RoundedCornerShape(12.dp)).padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(step.forbiddenTitle.orEmpty().uppercase(), fontFamily = VuMono, fontSize = 11.sp, letterSpacing = 0.5.sp, fontWeight = FontWeight.Bold, color = t.danger)
                            step.forbidden.forEach { Text("✕  $it", fontSize = 14.sp, lineHeight = 20.sp, color = t.text) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HoldingMessageCard(title: String, text: String) {
    val t = vuColors()
    VuCard {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            VuTag(title)
            Text(text, fontSize = 15.sp, lineHeight = 22.sp, color = t.text)
        }
    }
}

/** Кнопка «Определить место по GPS»: километр и пикет для сообщений (положение + участок с отметками, как на профиле пути; нужен вход и участок рядом). */
@Composable
private fun HoldingGpsPlace(state: BrakeHoldingUiState, vm: BrakeHoldingViewModel) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var requested by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf<String?>(null) }
    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()) { granted ->
        if (granted.values.any { it }) { requested = true; busy = true } else note = "Нет доступа к геолокации: разрешите его в настройках телефона или назовите место по столбикам."
    }
    val place = state.radio.km.takeIf { it.isNotBlank() }?.let { "${state.radio.km} км ${state.radio.picket} пк" }
    VuBtn(
        if (busy) "Определяем место…" else "Определить место по GPS",
        {
            note = null
            if (com.z_company.route.util.DeviceLocation.hasPermission(context)) { requested = true; busy = true }
            else launcher.launch(arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION))
        },
        Modifier.fillMaxWidth(), kind = VuBtnKind.Tonal, enabled = !busy,
    )
    VuHint(
        note ?: place?.let { "Место по GPS: $it (подставлено в сообщения)." }
            ?: "Подставит километр и пикет в сообщения, если есть GPS, вход в аккаунт и участок пути рядом. Иначе назовите их по столбикам.",
    )
    if (requested) {
        com.z_company.route.component.servicephase.HoldingPositionResolver { km, pk, message ->
            requested = false; busy = false
            if (km != null && pk != null) vm.setPlace(km, pk) else note = message
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
