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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.clip
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
        HoldingFillFromRoute(state, vm)
        if (state.fromTrain) {
            VuHint("Данные поезда подставлены из раздела «Поезд»: проверьте и при необходимости поправьте.")
        }
        VuBlock("ПОЕЗД") {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                VuInput("Вес состава, т", state.weightText, Modifier.weight(1f), onChange = vm::setWeight, unit = "т", sunk = true)
                VuInput("Вес локомотива, т", state.locoWeightText, Modifier.weight(1f), onChange = vm::setLocoWeight, unit = "т", sunk = true)
            }
            VuInput("Всего осей состава", state.axlesText, onChange = vm::setAxles, sunk = true, keyboard = KeyboardType.Number, maxLen = 4)
            VuHint("Масса локомотива учитывается в общей массе расчёта. Оси локомотива отдельно не вводятся: ручной тормоз локомотива затормаживается отдельным действием, а норма ручных осей ниже относится к вагонам.")
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
            "Требуется башмаков: ${result.shoesRequired}",
            fontFamily = VuMono, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = t.text,
        )
        val fastening = if (result.shoesUsed >= result.shoesRequired) {
            "Как закрепить: ${result.shoesRequired} башмаков."
        } else {
            val shoesPart = if (result.shoesUsed == 0) "без башмаков" else "${result.shoesUsed} башмаков"
            val manualPart = result.manualAxles?.let { axes ->
                "$axes ручных осей (${result.manualWagons ?: 0} ${wagonWord(result.manualWagons ?: 0)})"
            } ?: "ручными тормозами не закрепляется"
            "Как закрепить: $shoesPart и $manualPart."
        }
        Text(fastening, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = t.accent, lineHeight = 24.sp)
        if (result.shoesUsed == 0 && result.shoesRequired > 0) {
            VuHint("Башмаки не учтены: поле «Башмаков на локомотиве» пустое или указано 0. Потребность по норме — ${result.shoesRequired}.")
        }
        VuHint(
            "Масса с локомотивом ${fmtHold(result.totalMass, 0)} т; уклон ${fmtSlope(result.slopeStepPermille)}" +
                (if (result.slopeRoundedUp) " (округлён вверх от введённого)" else "") +
                ": норма на 100 тс веса - ${fmtHold(result.shoesPer100, 1)} башмака" +
                (result.manualAxlesPer100?.let { " или ${fmtHold(it, 1)} ручной оси" } ?: ", ручными тормозами не закрепляется"),
        )
        if (result.shoesUsed >= result.shoesRequired) {
            VuVerdict(true, "Имеющихся башмаков (${result.shoesUsed}) достаточно")
        } else {
            val missing = result.shoesRequired - result.shoesUsed
            VuWarnVerdict("Башмаков не хватает: $missing. Закрепляется ${fmtHold(result.coveredMass, 0)} т из ${fmtHold(result.totalMass, 0)} т")
            if (result.manualAxles != null) {
                VuHint(
                    "Остаток веса для ручных тормозов: ${fmtHold(result.remainingMass, 0)} т = ${result.manualAxles} осей; ручной тормоз вагона действует на 4 оси, " +
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

@Composable
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
private fun HoldingFillFromRoute(state: BrakeHoldingUiState, vm: BrakeHoldingViewModel) {
    val t = vuColors()
    state.fillPick?.let { pick ->
        androidx.compose.material3.ModalBottomSheet(
            onDismissRequest = vm::dismissFillPick,
            sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = androidx.compose.material3.MaterialTheme.colorScheme.background,
        ) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Поезда · ${pick.trains.size}", style = androidx.compose.material3.MaterialTheme.typography.headlineMedium, color = t.accent, modifier = Modifier.padding(start = 4.dp, bottom = 12.dp))
                pick.trains.forEach { train ->
                    val routeName = train.number?.takeIf { it.isNotBlank() }?.let { "Поезд №$it" } ?: "Поезд без номера"
                    VuCard(Modifier.fillMaxWidth().clickable { vm.pickTrain(train) }) {
                        Text(routeName, Modifier.padding(16.dp), fontSize = 17.sp, color = t.text)
                    }
                }
            }
        }
    }
    state.fillDialog?.let { msg ->
        com.z_company.route.component.AppAlertDialog(onDismissRequest = vm::dismissFillDialog, title = "Заполнить из маршрута", text = msg, confirmText = "Понятно", onConfirm = vm::dismissFillDialog)
    }
    Box(
        Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(if (state.filling) t.accentSoft.copy(alpha = 0.4f) else t.accentSoft, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = !state.filling, onClick = vm::onFillClicked),
        contentAlignment = Alignment.Center,
    ) {
        Text(if (state.filling) "Заполняю…" else "Заполнить из маршрута", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = if (state.filling) t.accent.copy(alpha = 0.5f) else t.accent)
    }
}

/** Исполнитель пункта по нормативному порядку, если он отличается от исполнителя шага. */
private class HBullet(val text: String, val who: String? = null)

/**
 * Шаг порядка действий. [phase] - заголовок этапа над первым шагом этапа; [badge] - срок (красный чип);
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
            HBullet("Передать сообщение об остановке по форме инструкции (текст — ниже, «Сообщения по радио»)."),
            HBullet("Передавать по порядку: УКВ — машинистам встречных и вслед идущих поездов; при пассажирском поезде — также начальнику поезда; затем КВ — ДСП, ограничивающим перегон, и ДНЦ. Если поездная радиосвязь недоступна — доложить по сотовой связи или иным способом дежурному основного депо либо командно-инструкторскому составу, затем передать место и причину остановки ДСП/ДНЦ."),
            HBullet("Получить подтверждение от встречных и вслед идущих поездов, ДСП, ДНЦ."),
        ),
    ),
    HStep(
        "Радиосвязь об остановке: фиксация", POM,
        bullets = listOf(
            HBullet("Зафиксировать время передачи сообщения, а также время и фамилии подтвердивших - на обороте бланка ДУ-61."),
            HBullet("Контролировать отсчёт времени от остановки и докладывать машинисту."),
        ),
    ),
    HStep(
        "Подготовка к устранению неисправности", POM, phase = "Подготовка и вызов помощи",
        bullets = listOf(
            HBullet("Включить красные огни буферных фонарей на ведущей секции."),
            HBullet("Набрать воздух в запасный резервуар токоприёмника, резервуар цепей управления и резервуар главного выключателя, если это предусмотрено конструкцией локомотива."),
            HBullet("Убедиться, что поезд заторможен, а ручка вспомогательного крана в крайнем тормозном положении и зафиксирована на защёлку."),
        ),
    ),
    HStep(
        "Вспомогательный локомотив", MASH,
        bullets = listOf(
            HBullet("Если неисправность не устранена в установленный срок, через ДСП (ДНЦ) затребовать вспомогательный локомотив по шаблону ниже; точное место сообщить после личной проверки."),
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
        "Закрепление поезда", "По указанию машиниста", phase = "Закрепление и устранение неисправности",
        bullets = listOf(
            HBullet("Исполнитель: грузовой поезд — помощник машиниста; пассажирский — начальник поезда совместно с поездной бригадой; МВПС/ССПС — помощник машиниста; ВСМВПС с локомотивной тягой — локомотивная бригада ВСМВПС."),
            HBullet("Башмаки укладывать под колёса вагонов со стороны уклона; носок полоза башмака должен касаться обода колеса."),
            HBullet("Разнородный состав (груженые и порожние вагоны): по натурному листу определить груженые вагоны и их номера."),
            HBullet("Сначала класть башмаки под груженые вагоны: так их нужно меньше (на груженый состав - 0,6 башмака на 100 тс при уклоне 0,018, на порожний - 1,6)."),
            HBullet("Если башмаки под вагоны с нагрузкой на ось более 10 т уложить нельзя - число башмаков считать по вагонам с нагрузкой менее 10 т."),
            HBullet("Поезд на переломном профиле: закреплять со стороны предполагаемого ухода, пропорционально весу частей поезда и величине уклона."),
            HBullet("Если башмаков на локомотиве не хватает - дополнительно затормозить ручные тормоза вагонов (число - во вкладке «Расчёт»)."),
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
        "Доклад и запись о закреплении", POM,
        bullets = listOf(
            HBullet("Доложить ДСП, ограничивающему перегон (ДНЦ): сколько башмаков уложено и на скольких вагонах заторможены ручные тормоза."),
            HBullet("Записать число башмаков и приведённых в действие ручных тормозов вагонов в ТУ-152; время сообщения и подтверждения — на обороте ДУ-61 или в ТУ-152."),
        ),
    ),
    HStep(
        "Устранение неисправности", "Для машиниста и помощника",
        bullets = listOf(
            HBullet("Устранить неисправность по аварийной схеме, установленной заводом-изготовителем (перемычки, предохранители и т.п.)."),
            HBullet("После устранения неисправности доложить ДСП ближайшей станции (ДНЦ) и согласовать дальнейшие действия. Не возобновлять движение до прибытия вспомогательного локомотива или разрешения ДНЦ."),
            HBullet("Не отвлекаться на радиопереговоры; при необходимости переговоры ведёт помощник, называя должность и фамилию."),
            HBullet("Если для устранения нужен заход в высоковольтную камеру электровоза: визуально убедиться, что токоприёмники опущены, и по вольтметру - что напряжения нет."),
            HBullet("Отсчитывать время от остановки и докладывать машинисту."),
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
    val stopTime = state.stopStartMs?.let { java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(it)) }
    val radio = state.radio.copy(time = stopTime.orEmpty())

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        VuBlock("ВРЕМЯ ОТ ОСТАНОВКИ") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                VuChip("10 минут", state.helpLocoDeadlineMin == 10, { vm.setHelpLocoDeadline(10) }, Modifier.weight(1f))
                VuChip("5 минут", state.helpLocoDeadlineMin == 5, { vm.setHelpLocoDeadline(5) }, Modifier.weight(1f))
            }
            VuHint("5 минут — для машинистов МВПС на участках с интенсивным пригородным движением, включённых в перечень распорядительных документов ОАО «РЖД».")
            VuHint("В выбранный срок ДСП/ДНЦ не вызывает локомотивную бригаду по радио.")
            if (elapsedSec == null) {
                VuBtn("Остановились", { vm.startStop() }, Modifier.fillMaxWidth())
                VuHint("Время от остановки контролирует помощник машиниста и докладывает машинисту.")
            } else {
                Text(fmtClock(elapsedSec), fontFamily = VuMono, fontSize = 34.sp, fontWeight = FontWeight.Bold, color = t.text)
                val deadlineSec = state.helpLocoDeadlineMin * 60L
                val left = deadlineSec - elapsedSec
                if (left >= 0) {
                    VuVerdict(true, "Вспомогательный локомотив — затребовать не позднее ${state.helpLocoDeadlineMin} мин после остановки: осталось ${fmtClock(left)}")
                } else {
                    VuVerdict(false, "Срок вызова вспомогательного локомотива (${state.helpLocoDeadlineMin} мин) прошёл на ${fmtClock(-left)}: затребуйте немедленно")
                }
                VuHint("Остановка в $stopTime.")
                VuBtn("Сбросить", { vm.resetStop() }, Modifier.fillMaxWidth(), kind = VuBtnKind.Tonal)
            }
        }
        HOLDING_STEPS.forEachIndexed { i, s ->
            val done = i in state.doneSteps
            s.phase?.let { HoldingPhaseHeader(it) }
            HoldingStepRow(i + 1, s, done, last = i == HOLDING_STEPS.lastIndex, deadlineMinutes = state.helpLocoDeadlineMin, onToggle = { vm.toggleStep(i) })
        }
        VuVerdict(false, "Отпуск тормозов поезда до устранения неисправности или прицепки вспомогательного локомотива ЗАПРЕЩЕНО.")
        VuHint("Расчёт закрепления бригада делает заранее - перед отправлением с начальной станции (пункта смены бригад) по самому крутому уклону участка - и фиксирует на бланке ДУ-61.")

        HoldingExpandableCard(
            title = "Ограждение поезда",
            instruction = "Действия выполняют после приказа ДНЦ о закрытии перегона и получения информации о порядке помощи; по указанию машиниста (непосредственно или по радиосвязи).",
            items = listOf(
                "ГРУЗОВОЙ ПОЕЗД. Помощник машиниста ограждает поезд с головы: укладывает петарды в 800 м от головы, отходит обратно к поезду на 20–25 м и подаёт сигнал остановки. Если помощь оказывается с хвоста, укладывает петарды в 800 м от хвостового вагона, отходит на 20–25 м к поезду и подаёт сигнал остановки (п. 13.7).",
                "ПАССАЖИРСКИЙ ПОЕЗД. Поездная бригада действует по указанию машиниста. В случаях, предусмотренных ИСИ п. 3.13 (затребован восстановительный/пожарный поезд или помощь с хвоста; либо поезд отправлен при перерыве всех средств сигнализации и связи в указанных ИСИ условиях), проводник последнего вагона приводит в действие ручной тормоз, укладывает петарды в 800 м от хвоста, отходит на 20 м к поезду и показывает ручной красный сигнал в сторону перегона. Возвращается после остановки прибывшей помощи или передачи ограждения другому работнику (пп. 3.13–3.14).",
                "ПАССАЖИРСКИЙ ПОЕЗД НА АВТОБЛОКИРОВКЕ. Проводник последнего вагона проверяет видимость сигналов, наблюдает за перегоном и при появлении следующего поезда принимает меры к его остановке. При ограждении по п. 3.15 помощник машиниста ограждает голову, а проводник последнего вагона — хвост; петарды укладывают в 1000 м от головы и хвоста соответственно.",
            ),
        )

        VuBlock("СООБЩЕНИЯ ПО РАДИО") {
            HoldingGpsPlace(state, vm)
        }
        HoldingMessageCard("Сообщение об остановке", BrakeHoldingRadio.stopMessage(radio))
        HoldingMessageCard("Требование помощи", BrakeHoldingRadio.helpLocoMessage(radio))
        HoldingMessageCard("После личной проверки места", BrakeHoldingRadio.verifiedPlaceMessage(radio))
        VuHint("Передавайте требование помощи с формулировкой о том, что точное место сообщите после личной проверки. Затем лично проверьте километр и пикет головы поезда и передайте уточнённый доклад по третьему шаблону. Незаполненные поля («…») назовите сами. Время сообщения и подтверждения фиксируйте на обороте ДУ-61 или в журнале ТУ-152.")
        VuHint("Нормативные источники: инструкция о взаимодействии работников при аварийных и нестандартных ситуациях, раздел 13; Инструкция по сигнализации на железнодорожном транспорте, пп. 3.13–3.15.")
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
private fun HoldingStepRow(number: Int, step: HStep, done: Boolean, last: Boolean, deadlineMinutes: Int, onToggle: () -> Unit) {
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
                    (step.badge ?: if (step.title == "Вспомогательный локомотив") "не позднее $deadlineMinutes мин после остановки (10 мин общее правило, 5 мин для МВПС по перечню)" else null)?.let {
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
private fun HoldingExpandableCard(title: String, instruction: String, items: List<String>) {
    val t = vuColors()
    var expanded by remember { mutableStateOf(false) }
    VuCard(Modifier.fillMaxWidth().clickable { expanded = !expanded }) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = t.text)
                androidx.compose.foundation.Canvas(
                    Modifier.size(24.dp).rotate(if (expanded) 180f else 0f),
                ) {
                    val stroke = 2.dp.toPx()
                    val middleX = size.width / 2f
                    val middleY = size.height / 2f
                    drawLine(t.accent, androidx.compose.ui.geometry.Offset(middleX - 5.dp.toPx(), middleY - 2.dp.toPx()), androidx.compose.ui.geometry.Offset(middleX, middleY + 3.dp.toPx()), stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                    drawLine(t.accent, androidx.compose.ui.geometry.Offset(middleX, middleY + 3.dp.toPx()), androidx.compose.ui.geometry.Offset(middleX + 5.dp.toPx(), middleY - 2.dp.toPx()), stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                }
            }
            if (expanded) {
                Text(instruction, fontSize = 13.sp, lineHeight = 19.sp, color = t.muted)
                items.forEach { item ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("•", fontSize = 14.sp, color = t.accent, fontWeight = FontWeight.Bold)
                        Text(item, fontSize = 14.sp, lineHeight = 20.sp, color = t.text)
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

/** Кнопка «Определить место по GPS»: километр/пикет по профилю пути и ближайшая станция по OSM. */
@Composable
private fun HoldingGpsPlace(state: BrakeHoldingUiState, vm: BrakeHoldingViewModel) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var requested by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf<String?>(null) }
    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()) { granted ->
        if (granted.values.any { it }) { requested = true; busy = true } else note = "Нет доступа к геолокации: разрешите его в настройках телефона или назовите место по столбикам."
    }
    val place = state.radio.km.takeIf { it.isNotBlank() }?.let {
        "${state.radio.km} км ${state.radio.picket} пк" + state.radio.stretch.takeIf(String::isNotBlank)?.let { station -> ", станция $station" }.orEmpty()
    }
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
            ?: "Подставит километр и пикет по профилю пути, а название ближайшей станции — по OSM, если найдётся; иначе назовите место по столбикам.",
    )
    if (requested) {
        com.z_company.route.component.servicephase.HoldingPositionResolver { km, pk, stretch, message ->
            requested = false; busy = false
            if (km != null && pk != null) vm.setPlace(km, pk, stretch) else note = message
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

private fun wagonWord(count: Int): String = when {
    count % 10 == 1 && count % 100 != 11 -> "вагон"
    count % 10 in 2..4 && count % 100 !in 12..14 -> "вагона"
    else -> "вагонов"
}
