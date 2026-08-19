package com.z_company.route.component

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.DismissDirection
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.SwipeToDismiss
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material.DismissValue
import androidx.compose.material.FractionalThreshold
import androidx.compose.material.rememberDismissState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.entities.route.Route
import com.z_company.core.ui.component.AutoSizeText
import com.z_company.core.ui.theme.Shapes
import com.z_company.core.util.ConverterLongToTime
import com.z_company.core.util.DateAndTimeConverter
import com.z_company.domain.entities.route.UtilsForEntities.getBreakDuration
import com.z_company.domain.entities.route.UtilsForEntities.getPassengerTime
import com.z_company.domain.entities.route.UtilsForEntities.getWorkTime
import com.z_company.domain.entities.route.UtilsForEntities.getWorkTimeInMonth
import com.z_company.domain.util.TimeCalculationContext
import com.z_company.domain.util.getTimeZone
import com.z_company.route.R
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@SuppressLint("StateFlowValueCalledInComposition")
@OptIn(
    ExperimentalMaterialApi::class, ExperimentalFoundationApi::class,
    ExperimentalMaterial3Api::class
)
@Composable
fun ItemHomeScreen(
    modifier: Modifier = Modifier,
    route: Route,
    convertTimeToString: (Long?) -> String,
    isExpand: Boolean = false,
    onRequestDelete: (Route) -> Unit,
    // Дефолтный максимум: 18.sp — крупнее обычного текста (bodyLarge = 16),
    // но не такой большой как titleLarge (22). AutoSizeText сжимает до 10.sp
    // если не помещается. Если нужно крупнее — увеличить до 20-22.sp.
    requiredSizeText: TextUnit = 18.sp,
    changingTextSize: (TextUnit) -> Unit = {},
    onLongClick: () -> Unit = {},
    containerColor: Color,
    onClick: () -> Unit,
    dateAndTimeConverter: DateAndTimeConverter,
    isHeavyTrains: Boolean = false,
    isLongCompositionTrain: Boolean = false,
    isExtendedServicePhaseTrains: Boolean = false,
    isHolidayTimeInRoute: Boolean = false,
    number: Int? = null,
    monthOfYear: MonthOfYear? = null,
    offsetInMoscow: Long = 0L,
    timeCalculationContext: TimeCalculationContext? = null,
    // Итог оплаты за смену для блока «Расчёт за смену» в развёрнутой карточке.
    // null → блок не показывается (например, на Главном/в Календаре).
    shiftPaymentText: String? = null,
) {

    // --- мемоизируем тяжёлые вычисления по route ---
    val sortedTrains = remember(route) {
        route.trains
            .filter { it.stations.firstOrNull()?.timeDeparture != null }
            .sortedByDescending { it.stations.firstOrNull()?.timeDeparture } +
            route.trains.filter { it.stations.firstOrNull()?.timeDeparture == null }
    }
    val breakDuration = remember(route) { route.getBreakDuration() }
    val passengerTime = remember(route) { route.getPassengerTime() }
    val workTime = remember(route) { route.getWorkTime() }

    // --- memoized texts to avoid repeated computation on recomposition ---
    val (timeTextMemo, workTimeStringMemo) = remember(route, dateAndTimeConverter) {
        val startWork =
            dateAndTimeConverter.getDateMiniAndTime(value = route.basicData.timeStartWork)
        val isDifference = dateAndTimeConverter.isDifferenceDate(
            first = route.basicData.timeStartWork,
            second = route.basicData.timeEndWork
        )
        val endWork = if (isDifference) {
            dateAndTimeConverter.getDateMiniAndTime(value = route.basicData.timeEndWork)
        } else {
            dateAndTimeConverter.getTime(route.basicData.timeEndWork)
        }
        val timeText = "$startWork - $endWork"
        val workTimeValue = if (monthOfYear != null) {
            val ctx = timeCalculationContext ?: TimeCalculationContext(
                localTZ = kotlinx.datetime.TimeZone.of(getTimeZone(offsetInMoscow)),
                crossMonthTZ = kotlinx.datetime.TimeZone.of(getTimeZone(offsetInMoscow))
            )
            route.getWorkTimeInMonth(monthOfYear, ctx)
        } else {
            route.getWorkTime()
        }
        val workTimeString = convertTimeToString(workTimeValue)
        timeText to workTimeString
    }

    // Явка и сдача с датой ОБЕ — для крупного шрифта (столбец), даже если даты
    // совпадают: так столбец «явка над сдачей» ровный (в обычном формате дата сдачи
    // при совпадении опускается).
    val stackedStartEnd = remember(route, dateAndTimeConverter) {
        val s = dateAndTimeConverter.getDateMiniAndTime(value = route.basicData.timeStartWork)
        val e = route.basicData.timeEndWork?.let {
            dateAndTimeConverter.getDateMiniAndTime(value = it)
        } ?: ""
        s to e
    }

    val interactionSource = remember { MutableInteractionSource() }

    // Bottom-sheet «Обозначения» — открывается по нажатию на значки маршрута.
    val legendSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showLegend by remember { mutableStateOf(false) }
    if (showLegend) {
        RouteLegendSheet(
            onDismissRequest = { showLegend = false },
            sheetState = legendSheetState,
        )
    }

    // Свайп-удаление как у станции/секции локомотива: раскрывает красную кнопку
    // «УДАЛИТЬ», по нажатию → onRequestDelete (подтверждение показывает экран).
    SwipeToRevealDelete(
        // Уважаем переданный modifier (например, padding/animateItem с экрана «Все
        // маршруты»); раньше он игнорировался и внешние отступы не применялись.
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 65.dp),
        onDeleteClick = { onRequestDelete(route) },
        onContentClick = onClick,
        onContentLongClick = onLongClick,
        backgroundVerticalPadding = 0.dp,
        // Привязываем состояние свайпа к id маршрута: когда после удаления в тот же
        // слот попадает следующий маршрут, свайп сбрасывается (не «прилипает»).
        itemKey = route.basicData.id,
    ) { _ ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .defaultMinSize(minHeight = 65.dp),
                elevation = CardDefaults.elevatedCardElevation(
                    defaultElevation = 1.dp,
                ),
                colors = CardDefaults.cardColors(
                    containerColor = containerColor,
                    contentColor = MaterialTheme.colorScheme.primary
                ),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically)
                ) {
                    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    // Одной строкой «явка − сдача  время» — только если реально помещается
                    // при обычном размере (не ужимаясь). Иначе явка/сдача столбцом + чип.
                    // Решение по фактической ширине, а не по фиксированному порогу шрифта.
                    val measureStyle = MaterialTheme.typography.bodyLarge.copy(
                        fontFamily = com.z_company.core.ui.theme.MonoFont,
                        fontSize = requiredSizeText,
                    )
                    val headerGapPx = with(LocalDensity.current) { 8.dp.roundToPx() }
                    // Проверяем фактические значения маршрута: короткий диапазон
                    // остаётся в одну строку, перенос включается только когда именно
                    // этот текст не помещается без уменьшения шрифта.
                    val oneLineFits = textsFitInWidth(
                        availableWidthPx = constraints.maxWidth,
                        extraPx = headerGapPx,
                        timeTextMemo to measureStyle,
                        workTimeStringMemo to measureStyle.copy(fontWeight = FontWeight.Medium),
                    )
                    if (!oneLineFits) {
                        // Не помещается: явка и сдача друг над другом, продолжительность
                        // работы — справа тем же шрифтом и без отдельного фона.
                        val timeStyle = MaterialTheme.typography.bodyLarge.copy(
                            fontFamily = com.z_company.core.ui.theme.MonoFont,
                            fontWeight = FontWeight.Medium,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                Text(
                                    text = stackedStartEnd.first,
                                    style = timeStyle,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                if (stackedStartEnd.second.isNotBlank()) {
                                    Text(
                                        text = stackedStartEnd.second,
                                        style = timeStyle,
                                        color = MaterialTheme.colorScheme.primary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                            // Продолжительность — только если она есть (у маршрута без
                            // времени сдачи её нет, пустое значение не показываем).
                            if (workTimeStringMemo.isNotBlank()) {
                                Text(
                                    text = workTimeStringMemo,
                                    style = timeStyle,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    softWrap = false,
                                )
                            }
                        }
                    } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            // maxLines=1 + minTextSize=10.sp гарантируют что текст
                                // помещается в одну строку даже при больших системных шрифтах
                                // или узких экранах. Если уменьшать ниже 10sp — текст
                                // нечитабельный, тогда срабатывает Ellipsis.
                            AutoSizeText(
                                text = timeTextMemo,
                                maxTextSize = requiredSizeText,
                                minTextSize = 10.sp,
                                maxLines = 1,
                                fontWeight = FontWeight.Medium,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontFamily = com.z_company.core.ui.theme.MonoFont,
                                ),
                                onTextLayout = { textLayoutResult ->
                                    val size = textLayoutResult.layoutInput.style.fontSize
                                    changingTextSize(size)
                                }
                            )
                        }

                        AutoSizeText(
                            text = workTimeStringMemo,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontFamily = com.z_company.core.ui.theme.MonoFont,
                            ),
                            maxTextSize = requiredSizeText,
                            minTextSize = 10.sp,
                            maxLines = 1,
                            fontWeight = FontWeight.Medium,
                            overflow = TextOverflow.Ellipsis,
                            onTextLayout = { textLayoutResult ->
                                val size = textLayoutResult.layoutInput.style.fontSize
                                changingTextSize(size)
                            }
                        )
                    }
                    }
                    }

                    // Свёрнутый вид (как на Главном): основной поезд одной строкой,
                    // «+N» для остальных показывает футер ниже. В развёрнутом виде
                    // поезда выводятся отдельной группой с иконками (см. ниже).
                    // Если поездов нет, но есть прочая работа — на этом месте
                    // показываем её тип и станцию (та же строка под явкой/сдачей).
                    val otherWorksToShow = if (sortedTrains.isEmpty()) {
                        route.otherWorks.take(1)
                    } else {
                        emptyList()
                    }
                    AnimatedVisibility(
                        visible = !isExpand &&
                            (sortedTrains.isNotEmpty() || otherWorksToShow.isNotEmpty()),
                        // tween без пружины — без отскока и «подёргивания» на затухании.
                        enter = expandVertically(tween(220, easing = FastOutSlowInEasing)) +
                            fadeIn(tween(220)),
                        exit = shrinkVertically(tween(220, easing = FastOutSlowInEasing)) +
                            fadeOut(tween(220)),
                    ) {
                        val trainsToShow = sortedTrains.take(1)
                        Column(modifier = Modifier.fillMaxWidth()) {
                            trainsToShow.forEach { train ->
                                val tn = if (!train.number.isNullOrBlank()) "№${train.number} " else ""
                                // Черту « — » ставим только между реально заполненными
                                // названиями станций, иначе у поезда без станций на карточке
                                // появлялась «голая» черта без значений.
                                val first = train.stations.firstOrNull()?.stationName?.takeIf { it.isNotBlank() }
                                val last = if (train.stations.size > 1) {
                                    train.stations.last().stationName?.takeIf { it.isNotBlank() }
                                } else null
                                val path = when {
                                    first != null && last != null -> "$first — $last"
                                    else -> first ?: last ?: ""
                                }
                                val info = "$tn$path".trim()
                                if (info.isNotBlank()) {
                                    Text(
                                        modifier = Modifier.fillMaxWidth(),
                                        text = info,
                                        // Тот же шрифт что и время работы — JetBrains Mono
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontFamily = com.z_company.core.ui.theme.MonoFont,
                                            fontSize = 14.sp,
                                        ),
                                        color = MaterialTheme.colorScheme.primary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                            otherWorksToShow.forEach { otherWork ->
                                val type = otherWork.workType?.takeIf { it.isNotBlank() }
                                val station = otherWork.station?.takeIf { it.isNotBlank() }
                                val info = when {
                                    type != null && station != null -> "$type — $station"
                                    else -> type ?: station ?: ""
                                }
                                if (info.isNotBlank()) {
                                    Text(
                                        modifier = Modifier.fillMaxWidth(),
                                        text = info,
                                        // Тот же шрифт что и время работы — JetBrains Mono
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontFamily = com.z_company.core.ui.theme.MonoFont,
                                            fontSize = 14.sp,
                                        ),
                                        color = MaterialTheme.colorScheme.primary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    }

                    // Развёрнутый вид (по дизайну «Все маршруты»): группы единиц с
                    // иконками — Локомотивы / Поезда / Пассажиром — и итог оплаты.
                    // Плавное раскрытие/сворачивание по переключателю плотности.
                    // Если разворачивать нечего — не показываем даже разделитель.
                    val hasExpandedContent = route.locomotives.isNotEmpty() ||
                        sortedTrains.isNotEmpty() ||
                        route.passengers.isNotEmpty() ||
                        route.otherWorks.isNotEmpty() ||
                        !route.basicData.notes.isNullOrBlank() ||
                        shiftPaymentText != null
                    AnimatedVisibility(
                        visible = isExpand && hasExpandedContent,
                        // tween без пружины — без отскока и «подёргивания» на затухании.
                        enter = expandVertically(tween(220, easing = FastOutSlowInEasing)) +
                            fadeIn(tween(220)),
                        exit = shrinkVertically(tween(220, easing = FastOutSlowInEasing)) +
                            fadeOut(tween(220)),
                    ) {
                      Column(
                          modifier = Modifier.fillMaxWidth(),
                          verticalArrangement = Arrangement.spacedBy(4.dp),
                      ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .height(1.dp)
                                .background(MaterialTheme.colorScheme.outlineVariant)
                        )

                        if (route.locomotives.isNotEmpty()) {
                            RouteUnitGroup(label = "Локомотивы") {
                                route.locomotives.forEach { loco ->
                                    RouteUnitRow(
                                        iconRes = R.drawable.ic_card_locomotive_ref,
                                        title = loco.series?.takeIf { it.isNotBlank() } ?: "Локомотив",
                                        titleMono = loco.number?.takeIf { it.isNotBlank() },
                                    )
                                }
                            }
                        }

                        if (sortedTrains.isNotEmpty()) {
                            RouteUnitGroup(label = "Поезда") {
                                sortedTrains.forEach { train ->
                                    val first = train.stations.firstOrNull()
                                        ?.stationName?.takeIf { it.isNotBlank() }
                                    val last = if (train.stations.size > 1) {
                                        train.stations.last().stationName?.takeIf { it.isNotBlank() }
                                    } else null
                                    val path = when {
                                        first != null && last != null -> "$first — $last"
                                        else -> first ?: last
                                    }
                                    RouteUnitRow(
                                        iconRes = R.drawable.ic_card_train_ref,
                                        title = train.number?.takeIf { it.isNotBlank() }
                                            ?.let { "№$it" } ?: "Поезд",
                                        subtitle = path,
                                    )
                                }
                            }
                        }

                        if (route.passengers.isNotEmpty()) {
                            RouteUnitGroup(label = "Пассажиром") {
                                route.passengers.forEach { passenger ->
                                    val dep = passenger.stationDeparture?.takeIf { it.isNotBlank() }
                                    val arr = passenger.stationArrival?.takeIf { it.isNotBlank() }
                                    val path = when {
                                        dep != null && arr != null -> "$dep — $arr"
                                        else -> dep ?: arr
                                    }
                                    RouteUnitRow(
                                        iconRes = R.drawable.ic_card_passenger_ref,
                                        title = passenger.trainNumber?.takeIf { it.isNotBlank() }
                                            ?.let { "№$it" } ?: "Поездка",
                                        subtitle = path,
                                    )
                                }
                            }
                        }

                        if (route.otherWorks.isNotEmpty()) {
                            RouteUnitGroup(label = "Прочая работа") {
                                route.otherWorks.forEach { otherWork ->
                                    RouteUnitRow(
                                        iconRes = R.drawable.ic_card_other_work_ref,
                                        title = otherWork.workType?.takeIf { it.isNotBlank() }
                                            ?: "Прочая работа",
                                        subtitle = otherWork.station?.takeIf { it.isNotBlank() },
                                    )
                                }
                            }
                        }

                        if (!route.basicData.notes.isNullOrBlank()) {
                            Text(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 6.dp),
                                text = route.basicData.notes!!,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }

                        shiftPaymentText?.let { payment ->
                            ShiftPaymentBlock(payment = payment)
                        }
                      }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        number?.let {
                            Text(
                                text = "#$it",
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }

                        Row(
                                modifier = Modifier
                                    .wrapContentWidth()
                                    .pointerInput(Unit) {
                                        detectTapGestures(
                                            onLongPress = { showLegend = true }
                                        )
                                    },
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                            ) {
                                if (isHolidayTimeInRoute) {
                                    Image(
                                        modifier = Modifier.size(20.dp),
                                        painter = painterResource(id = R.drawable.icon_holiday),
                                        contentDescription = null
                                    )
                                }
                                if (breakDuration > 0L) {
                                    Icon(
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp),
                                        painter = painterResource(id = R.drawable.pause_24px),
                                        contentDescription = null
                                    )
                                }
                                if (isLongCompositionTrain) {
                                    Icon(
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp),
                                        painter = painterResource(id = R.drawable.straighten_24px),
                                        contentDescription = null
                                    )
                                }
                                if (isHeavyTrains) {
                                    Icon(
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp),
                                        painter = painterResource(id = R.drawable.weight_24px),
                                        contentDescription = null
                                    )
                                }
                                if (isExtendedServicePhaseTrains) {
                                    Icon(
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp),
                                        painter = painterResource(id = R.drawable.long_distance_24px),
                                        contentDescription = null
                                    )
                                }
                                if (route.basicData.isOnePersonOperation) {
                                    Icon(
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp),
                                        painter = painterResource(id = R.drawable.person_24px),
                                        contentDescription = null
                                    )
                                }
                                passengerTime?.let { time ->
                                    if (time > 0L) {
                                        Icon(
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp),
                                            painter = painterResource(id = R.drawable.passenger_24px),
                                            contentDescription = null
                                        )
                                    }
                                }
                                workTime?.let { time ->
                                    val oneHourInMillis = 3600000
                                    val normaHours = 12
                                    if (time > normaHours * oneHourInMillis) {
                                        Image(
                                            modifier = Modifier.size(20.dp),
                                            painter = painterResource(id = R.drawable.orden),
                                            contentDescription = null,
                                        )
                                    }
                                }
                                if (route.trains.any { it.pusher != null }) {
                                    Icon(
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp),
                                        painter = painterResource(id = R.drawable.ic_pusher_24px),
                                        contentDescription = null
                                    )
                                }
                                if (route.trains.any { it.doubleTraction != null }) {
                                    Icon(
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp),
                                        painter = painterResource(id = R.drawable.ic_double_traction_24px),
                                        contentDescription = null
                                    )
                                }
                                if (route.trains.any { it.doubledTrain != null }) {
                                    Icon(
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp),
                                        painter = painterResource(id = R.drawable.ic_doubled_train_24px),
                                        contentDescription = null
                                    )
                                }
                                if (route.basicData.isFavorite) {
                                    Icon(
                                        tint = Color(0xFFf1642e),
                                        modifier = Modifier.size(20.dp),
                                        painter = painterResource(R.drawable.favorite_fill_24px),
                                        contentDescription = null,
                                    )
                                }

                                if (route.basicData.isSynchronized) {
                                    Image(
                                        modifier = Modifier.size(20.dp),
                                        painter = painterResource(id = R.drawable.sync_on_icon),
                                        contentDescription = null,
                                    )
                                } else {
                                    Icon(
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp),
                                        painter = painterResource(id = R.drawable.sync_disabled_24px),
                                        contentDescription = null,
                                    )
                                }
                            }
                    }
                }
            }
        }
}

// ─────────────────────────────────────────────────────────────
// Развёрнутая карточка: группа единиц (Локомотивы/Поезда/Пассажиром)
// ─────────────────────────────────────────────────────────────
@Composable
private fun RouteUnitGroup(
    label: String,
    content: @Composable () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            modifier = Modifier.padding(top = 4.dp, bottom = 2.dp),
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = com.z_company.core.ui.theme.MonoFont,
                letterSpacing = 1.2.sp,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        )
        content()
    }
}

// Строка единицы: иконка-аватар в мягком круге + название (+ моно-номер) + подпись.
@Composable
private fun RouteUnitRow(
    iconRes: Int,
    title: String,
    titleMono: String? = null,
    subtitle: String? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.size(18.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!titleMono.isNullOrBlank()) {
                    Text(
                        text = " · $titleMono",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = com.z_company.core.ui.theme.MonoFont,
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

// Итог оплаты за смену — нейтральный блок в подложке.
@Composable
private fun ShiftPaymentBlock(payment: String) {
    val label: @Composable () -> Unit = {
        Text(
            text = "Расчёт за смену",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    val value: @Composable () -> Unit = {
        Text(
            text = payment,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontFamily = com.z_company.core.ui.theme.MonoFont,
                fontWeight = FontWeight.Bold,
            ),
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            softWrap = false,
        )
    }
    val boxModifier = Modifier
        .fillMaxWidth()
        .padding(top = 8.dp)
        .clip(RoundedCornerShape(12.dp))
        .background(MaterialTheme.colorScheme.surfaceBright)
        .padding(horizontal = 14.dp, vertical = 11.dp)
    // Крупный шрифт: подпись и сумма друг под другом (в ряд сумма не помещается).
    if (LocalDensity.current.fontScale > 1.15f) {
        Column(
            modifier = boxModifier,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            label()
            value()
        }
    } else {
        Row(
            modifier = boxModifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            label()
            value()
        }
    }
}
