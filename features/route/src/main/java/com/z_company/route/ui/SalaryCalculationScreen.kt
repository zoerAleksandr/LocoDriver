package com.z_company.route.ui

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.z_company.core.ResultState
import com.z_company.core.ui.theme.Shapes
import com.z_company.domain.util.str2decimalSign
import com.z_company.route.viewmodel.SalaryCalculationUIState
import com.z_company.core.ui.component.CustomDivider
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.z_company.route.component.PdfActionSheet
import com.z_company.route.component.PdfContentDialog
import com.z_company.route.viewmodel.PdfViewModel
import com.z_company.route.viewmodel.SalaryCalculationViewModel
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalaryCalculationScreen(
    viewModel: SalaryCalculationViewModel,
    uiState: SalaryCalculationUIState,
    onSettingsSalaryClick: () -> Unit,
) {
    val styleHint = MaterialTheme.typography.bodyMedium
    val colorPrimary = MaterialTheme.colorScheme.primary

    val context = LocalContext.current
    val pdfViewModel: PdfViewModel = koinInject()
    var showPdfDialog by remember { mutableStateOf(false) }
    var pdfUri by remember { mutableStateOf<android.net.Uri?>(null) }
    val isPdfGenerating by pdfViewModel.isGenerating.collectAsState()
    val pdfError by pdfViewModel.errorMessage.collectAsState()

    // Update pdfViewModel with latest salary state
    LaunchedEffect(uiState) {
        pdfViewModel.updateSalaryState(uiState)
    }

    // PDF ready event
    LaunchedEffect(Unit) {
        pdfViewModel.pdfReady.collect { uri ->
            pdfUri = uri
        }
    }

    // Show PDF error
    LaunchedEffect(pdfError) {
        pdfError?.let {
            android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_LONG).show()
        }
    }

    // Состояния для информационных блоков
    var infoBlockVisible by rememberSaveable { mutableStateOf(true) }
    var infoSetTariffRate by remember { mutableStateOf(false) }

    // Определяем, нужно ли показывать предупреждение о тарифной ставке
    LaunchedEffect(uiState.tariffRate) {
        infoSetTariffRate = uiState.tariffRate == "0,00 ₽" || uiState.tariffRate == null
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
            TopAppBar(
                title = {
                    Text(
                        text = "Зарплата",
                        style = MaterialTheme.typography.titleMedium,
                        color = colorPrimary
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                actions = {
                    IconButton(
                        onClick = { if (!isPdfGenerating) showPdfDialog = true },
                        enabled = !isPdfGenerating
                    ) {
                        if (isPdfGenerating) {
                            CircularProgressIndicator(
                                modifier = Modifier.padding(8.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                painter = painterResource(com.z_company.route.R.drawable.picture_as_pdf_24px),
                                contentDescription = "PDF",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    IconButton(
                        modifier = Modifier.padding(end = 16.dp),
                        onClick = onSettingsSalaryClick
                    ) {
                        Icon(
                            painter = painterResource(com.z_company.route.R.drawable.settings_24px),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
            // Hero — К выдаче
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                Text(
                    text = "К ВЫДАЧЕ · ${uiState.month.uppercase()}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "${uiState.toBeCredited?.str2decimalSign() ?: "0,00"} ₽",
                    style = MaterialTheme.typography.displayMedium,
                    color = colorPrimary,
                )
            }
            }
        },
    ) { paddingValues ->
        if (uiState.screenState is ResultState.Loading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(uiState.screenState.message)
                }
            }
        } else {
            // Основной контейнер - LazyColumn для вертикальной прокрутки
            LazyColumn(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .testTag("salary_lazy_column"),
            ) {
                // Информационный блок с предупреждением
                item {
                    AnimatedVisibility(visible = infoBlockVisible) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = MaterialTheme.colorScheme.surfaceDim,
                                    shape = Shapes.medium
                                )
                                .border(
                                    0.5.dp,
                                    MaterialTheme.colorScheme.tertiary.copy(),
                                    Shapes.medium
                                )
                                .padding(16.dp),
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                style = styleHint,
                                color = colorPrimary,
                                text = "Данный расчет носит информационный характер, некоторые виды выплат могут отличаться в зависимости от внутренних нормативных документов вашего депо."
                            )
                            Button(
                                shape = Shapes.medium,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                                onClick = { infoBlockVisible = false }
                            ) {
                                Text(
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.secondary,
                                    text = "Понятно"
                                )
                            }
                        }
                    }
                }

                // Предупреждение о неустановленной тарифной ставке
                item {
                    AnimatedVisibility(visible = infoSetTariffRate) {
                        Column(
                            modifier = Modifier
                                .padding(top = 8.dp)
                                .fillMaxWidth()
                                .background(
                                    color = MaterialTheme.colorScheme.surfaceDim,
                                    shape = Shapes.medium
                                )
                                .border(0.5.dp, MaterialTheme.colorScheme.tertiary, Shapes.medium)
                                .padding(16.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                style = styleHint,
                                color = colorPrimary,
                                text = "Не установлена тарифная ставка. Перейдите в настройки для её указания."
                            )
                        }
                    }
                }

                // Заголовок для таблицы начислений
                item {
                    Text(
                        text = "НАЧИСЛЕНИЯ",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                    )
                }

                // Таблица начислений
                item {
                    EarningsTable(uiState = uiState, convertTimeToStringFormat = viewModel::convertTimeToStringFormat)
                }

                // Заголовок для таблицы удержаний
                item {
                    Text(
                        text = "УДЕРЖАНИЯ",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
                    )
                }

                // Таблица удержаний
                item {
                    RetentionsTable(uiState = uiState)
                }

                // Итоговая строка "К выдаче" — акцентный блок
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                            .background(
                                color = MaterialTheme.colorScheme.secondary,
                                shape = Shapes.medium
                            )
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "К ВЫДАЧЕ",
                            style = MaterialTheme.typography.labelSmall,
                            color = colorPrimary,
                        )
                        Text(
                            text = uiState.toBeCredited?.str2decimalSign() ?: "0,00 ₽",
                            style = MaterialTheme.typography.displayMedium.copy(
                                fontSize = 24.sp
                            ),
                            color = colorPrimary,
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(32.dp)) } // Нижний отступ для удобства
            }
        }
    }

    // PDF dialog
    if (showPdfDialog) {
        PdfContentDialog(
            onDismiss = { showPdfDialog = false },
            onGenerate = { sections ->
                showPdfDialog = false
                pdfViewModel.generateAndShare(
                    sections = sections,
                    routes = emptyList(),
                    monthLabel = uiState.month,
                    calendarDays = emptyList()
                )
            }
        )
    }

    pdfUri?.let { uri ->
        PdfActionSheet(
            uri = uri,
            onDismiss = { pdfUri = null }
        )
    }
}

// ===========================================
// Таблица начислений (с горизонтальной прокруткой)
// ===========================================
@Composable
private fun EarningsTable(uiState: SalaryCalculationUIState, convertTimeToStringFormat: (Long?) -> String) {
    val scrollState = rememberScrollState() // Состояние для горизонтальной прокрутки
    val hintStyle = MaterialTheme.typography.bodyMedium

    // Список строк таблицы (вид выплаты, часы, процент, сумма)
    val rows = listOfNotNull(
        // Основные выплаты
        EarningsRow(
            "Оплата по тарифу",
            uiState.paymentAtTariffHours,
            null,
            uiState.paymentAtTariffMoney
        ),
        EarningsRow(
            "Ночные часы",
            uiState.paymentNightTimeHours,
            uiState.paymentNightTimePercent,
            uiState.paymentNightTimeMoney
        ),
        EarningsRow(
            "Пассажиром",
            uiState.paymentAtPassengerHours,
            null,
            uiState.paymentAtPassengerMoney
        ),
        EarningsRow(
            "Резервом",
            uiState.paymentAtSingleLocomotiveHours,
            null,
            uiState.paymentAtSingleLocomotiveMoney
        ),
        EarningsRow("Праздничные", uiState.paymentHolidayHours, null, uiState.paymentHolidayMoney),
        EarningsRow(
            "Оплата по среднему",
            uiState.averagePaymentHours,
            null,
            uiState.averagePaymentMoney
        ),
        EarningsRow(
            "По уходу за ребенком-инвалидом",
            uiState.caringForDisableChildrenHours,
            null,
            uiState.caringForDisableChildrenMoney
        ),


        // Надбавки
        uiState.zonalSurchargePercent?.let {
            EarningsRow(
                "Зональная надбавка",
                null,
                it,
                uiState.zonalSurchargeMoney
            )
        },
        uiState.surchargeQualificationClassPercent?.let {
            EarningsRow(
                "Надбавка за класс квалификации",
                null,
                it,
                uiState.surchargeQualificationClassMoney
            )
        },
        uiState.onePersonOperationPercent?.let {
            EarningsRow(
                "В одно лицо (грузовые)",
                uiState.onePersonOperationHours,
                it,
                uiState.onePersonOperationMoney
            )
        },
        uiState.onePersonOperationPassengerTrainPercent?.let {
            EarningsRow(
                "В одно лицо (пассажирские)",
                uiState.onePersonOperationPassengerTrainHours,
                it,
                uiState.onePersonOperationPassengerTrainMoney
            )
        },
        uiState.harmfulnessSurchargePercent?.let {
            EarningsRow(
                "Вредность",
                null,
                it,
                uiState.harmfulnessSurchargeMoney
            )
        },
        uiState.districtSurchargeCoefficient?.let {
            EarningsRow(
                "Районный коэффициент",
                null,
                it,
                uiState.districtSurchargeMoney
            )
        },
        uiState.nordicSurchargePercent?.let {
            EarningsRow(
                "Северная надбавка",
                null,
                it,
                uiState.nordicSurchargeMoney
            )
        },
        uiState.otherSurchargePercent?.let {
            EarningsRow(
                "Прочие надбавки",
                null,
                it,
                uiState.otherSurchargeMoney
            )
        },
        uiState.restInExcessOfTheNormMoney?.takeIf { it > 0 }?.let {
            EarningsRow(
                "Переотдых",
                uiState.restInExcessOfTheNormTime,
                null,
                it
            )
        },

        // Добавляем списки надбавок (если не пустые)
        *(0 until minOf(
            uiState.surchargeExtendedServicePhaseHour.size,
            uiState.surchargeExtendedServicePhasePercent.size,
            uiState.surchargeExtendedServicePhaseMoney.size
        )).mapNotNull { i ->
            val money = uiState.surchargeExtendedServicePhaseMoney.getOrNull(i) ?: 0.0
            if (money > 0) {
                EarningsRow(
                    "Удлиненное плечо (${uiState.surchargeExtendedServicePhasePercent[i] ?: ""}%)",
                    uiState.surchargeExtendedServicePhaseHour.getOrNull(i),
                    uiState.surchargeExtendedServicePhasePercent.getOrNull(i)?.toDoubleOrNull(),
                    money
                )
            } else null
        }.toTypedArray(),

        *(0 until minOf(
            uiState.surchargeHeavyTransHour.size,
            uiState.surchargeHeavyTransPercent.size,
            uiState.surchargeHeavyTransMoney.size
        )).mapNotNull { i ->
            val money = uiState.surchargeHeavyTransMoney.getOrNull(i) ?: 0.0
            if (money > 0) {
                EarningsRow(
                    "Тяжелые поезда (${uiState.surchargeHeavyTransPercent[i] ?: ""}%)",
                    uiState.surchargeHeavyTransHour.getOrNull(i),
                    uiState.surchargeHeavyTransPercent.getOrNull(i)?.toDoubleOrNull(),
                    money
                )
            } else null
        }.toTypedArray(),

        *(0 until minOf(
            uiState.surchargeLongTrainHour.size,
            uiState.surchargeLongTrainPercent.size,
            uiState.surchargeLongTrainMoney.size
        )).mapNotNull { i ->
            val money = uiState.surchargeLongTrainMoney.getOrNull(i) ?: 0.0
            if (money > 0) {
                EarningsRow(
                    "Длинносост. (${uiState.surchargeLongTrainPercent[i] ?: ""}%)",
                    uiState.surchargeLongTrainHour.getOrNull(i),
                    uiState.surchargeLongTrainPercent.getOrNull(i)?.toDoubleOrNull(),
                    money
                )
            } else null
        }.toTypedArray(),

        uiState.surchargeDoubledTrainFirstMoney?.takeIf { it > 0 }?.let {
            EarningsRow(
                "Сдвоенные поезда (30%)",
                uiState.surchargeDoubledTrainFirstHours,
                30.0,
                it
            )
        },
        uiState.surchargeDoubledTrainSecondMoney?.takeIf { it > 0 }?.let {
            EarningsRow(
                "Сдвоенные поезда (15%)",
                uiState.surchargeDoubledTrainSecondHours,
                15.0,
                it
            )
        },

        // Сверхурочные
        EarningsRow(
            "Сверхурочные часы",
            uiState.paymentAtOvertimeHours,
            null,
            uiState.paymentAtOvertimeMoney
        ),
        uiState.surchargeAtOvertime05Money?.takeIf { it > 0 }?.let {
            EarningsRow(
                "Доплата за сверхурочные (50%)",
                uiState.surchargeAtOvertime05Hours,
                50.0,
                uiState.surchargeAtOvertime05Money
            )
        },
        uiState.surchargeAtOvertimeMoney?.takeIf { it > 0 }?.let {
            EarningsRow(
                "Доплата за сверхурочные (100%)",
                uiState.surchargeAtOvertimeHours,
                100.0,
                uiState.surchargeAtOvertimeMoney
            )
        },

        // Итог начислений
        EarningsRow("Всего начислено", null, null, uiState.totalChargedMoney, isTotal = true)
    ).filter { row ->
        // Фильтр: показываем только строки с ненулевыми суммами (кроме итога)
        row.isTotal || (row.money != null && row.money > 0)
    }

    val density = LocalDensity.current
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp - 32.dp

    // Изменено: Убрал TextMeasurer и расчет maxWidth с measure. Вместо этого добавил скрытый Box для измерения ширины с IntrinsicSize.Max и onGloballyPositioned.
    // Для чего: Чтобы измерить ширину текстов динамически на рендере с помощью IntrinsicSize.Max (как предлагал пользователь), без ручного measure. Это учитывает display size лучше, так как измерение происходит в реальном контексте Compose. Собираем max ширину для каждого столбца в state.
    var maxTitleWidth by remember { mutableStateOf(0.dp) }
    var maxHoursWidth by remember { mutableStateOf(0.dp) }
    var maxPercentWidth by remember { mutableStateOf(0.dp) }
    var maxMoneyWidth by remember { mutableStateOf(0.dp) }
    Box {
        // Скрытый composable для измерения
        Box(modifier = Modifier.alpha(0f)) {
            Row(
                modifier = Modifier
                    .horizontalScroll(scrollState)
            ) {
                Column(modifier = Modifier.width(IntrinsicSize.Max)) {
                    Text("Вид выплаты", style = hintStyle, maxLines = 2)
                    rows.forEach { row ->
                        Text(
                            row.title,
                            style = hintStyle,
                            maxLines = 2,
                            modifier = Modifier
                                .padding(horizontal = 4.dp, vertical = 4.dp)
                                .onGloballyPositioned { coords ->
                                    val widthDp = with(density) { coords.size.width.toDp() } + 16.dp
                                    if (widthDp > maxTitleWidth) maxTitleWidth = widthDp
                                })
                    }
                }
                Column(modifier = Modifier.width(IntrinsicSize.Max)) {
                    Text("Часы", style = hintStyle)
                    rows.forEach { row ->
                        Text(row.hours?.let { convertTimeToStringFormat(it) } ?: "",
                            style = hintStyle,
                            modifier = Modifier
                                .padding(horizontal = 4.dp, vertical = 4.dp)
                                .onGloballyPositioned { coords ->
                                    val widthDp = with(density) { coords.size.width.toDp() } + 16.dp
                                    if (widthDp > maxHoursWidth) {
                                        maxHoursWidth = widthDp
                                        Log.d("zzz", "maxHoursWidth измеряемое $maxHoursWidth")
                                    }
                                })
                    }
                }
                Column(modifier = Modifier.width(IntrinsicSize.Max)) {
                    Text("%", style = hintStyle)
                    rows.forEach { row ->
                        Text(row.percent?.let { "%.1f".format(it) } ?: "",
                            style = hintStyle,
                            modifier = Modifier
                                .padding(horizontal = 4.dp, vertical = 4.dp)
                                .onGloballyPositioned { coords ->
                                    val widthDp = with(density) { coords.size.width.toDp() } + 16.dp
                                    if (widthDp > maxPercentWidth) maxPercentWidth = widthDp
                                })
                    }
                }
                Column(modifier = Modifier.width(IntrinsicSize.Max)) {
                    Text("Сумма", style = hintStyle)
                    rows.forEach { row ->
                        Text(
                            row.money?.str2decimalSign() ?: "",
                            style = hintStyle,
                            modifier = Modifier
                                .padding(horizontal = 4.dp, vertical = 4.dp)
                                .onGloballyPositioned { coords ->
                                    val widthDp = with(density) { coords.size.width.toDp() } + 16.dp
                                    if (widthDp > maxMoneyWidth) maxMoneyWidth = widthDp
                                })
                    }
                }
            }
        }

        // Изменено: Добавил minOf для maxTitleWidth с screenWidth / 2, как в исходном.
        // Для чего: Чтобы сохранить ограничение ширины первого столбца, как было.
        maxTitleWidth = minOf(maxTitleWidth, screenWidth / 2)

        // Вычисляем общую рассчитанную ширину
        val totalCalculatedWidth = maxTitleWidth + maxHoursWidth + maxPercentWidth + maxMoneyWidth

        // Если общая ширина меньше ширины экрана, добавляем разницу к последнему столбцу
        if (totalCalculatedWidth != 0.dp && totalCalculatedWidth < screenWidth) {
            val extraWidth = screenWidth - totalCalculatedWidth
            maxMoneyWidth += extraWidth
        }

        Column {
            // Заголовок таблицы с горизонтальной прокруткой
            Row(
                modifier = Modifier
                    .horizontalScroll(scrollState)
                    .padding(vertical = 8.dp)
            ) {
                TableCell(
                    text = "Вид выплаты",
                    width = maxTitleWidth,
                    maxLines = 2,
                    isHeader = true,
                    contentAlignment = Alignment.CenterStart
                )
                TableCell(
                    text = "Часы",
                    width = maxHoursWidth,
                    isHeader = true,
                    contentAlignment = Alignment.Center
                )
                TableCell(
                    text = "%",
                    width = maxPercentWidth,
                    isHeader = true,
                    contentAlignment = Alignment.Center
                )
                TableCell(
                    text = "Сумма",
                    width = maxMoneyWidth,
                    isHeader = true,
                    contentAlignment = Alignment.CenterEnd
                )
            }

            CustomDivider(orientation = Orientation.Horizontal)

            // Строки таблицы
            rows.forEach { row ->
                Row(
                    modifier = Modifier
                        .horizontalScroll(scrollState)
                        .background(if (row.isTotal) MaterialTheme.colorScheme.surfaceDim else Color.Transparent)
                ) {
                    TableCell(
                        text = row.title,
                        width = maxTitleWidth,
                        maxLines = 2,
                        contentAlignment = Alignment.CenterStart
                    )
                    TableCell(
                        text = row.hours?.let { convertTimeToStringFormat(it) }
                            ?: "",
                        width = maxHoursWidth,
                        contentAlignment = Alignment.Center
                    )
                    TableCell(
                        text = row.percent?.let { "%.1f".format(it) } ?: "",
                        width = maxPercentWidth,
                        contentAlignment = Alignment.Center
                    )
                    TableCell(
                        text = row.money?.str2decimalSign() ?: "",
                        width = maxMoneyWidth,
                        contentAlignment = Alignment.CenterEnd
                    )
                }
                CustomDivider(orientation = Orientation.Horizontal)
            }
        }
    }
}

// ===========================================
// Таблица удержаний (с горизонтальной прокруткой)
// ===========================================
@Composable
private fun RetentionsTable(uiState: SalaryCalculationUIState) {
    val scrollState = rememberScrollState() // Состояние для горизонтальной прокрутки
    val hintStyle = MaterialTheme.typography.bodyMedium

    val retentionNames = listOf("НДФЛ (13%)", "Профсоюз", "Прочие удержания", "Всего удержано")

    val rows = listOfNotNull(
        uiState.retentionNdfl?.takeIf { it > 0 }?.let { RetentionRow(retentionNames[0], it) },
        uiState.unionistsRetention?.takeIf { it > 0 }?.let { RetentionRow(retentionNames[1], it) },
        uiState.otherRetention?.takeIf { it > 0 }?.let { RetentionRow(retentionNames[2], it) },
        uiState.totalRetention?.takeIf { it > 0 }
            ?.let { RetentionRow(retentionNames[3], it, isTotal = true) }
    )

    val density = LocalDensity.current
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp - 32.dp

    var maxTitleWidth by remember { mutableStateOf(0.dp) }
    var maxAmountWidth by remember { mutableStateOf(0.dp) }

    Box {
        // Скрытый composable для измерения
        Box(modifier = Modifier.alpha(0f)) {
            Row {
                Column(modifier = Modifier.width(IntrinsicSize.Max)) {
                    Text("Вид удержания", style = hintStyle, maxLines = 2)
                    rows.forEach { row ->
                        Text(
                            row.title,
                            style = hintStyle,
                            maxLines = 2,
                            modifier = Modifier
                                .padding(horizontal = 4.dp, vertical = 4.dp)
                                .onGloballyPositioned { coords ->
                                    val widthDp = with(density) { coords.size.width.toDp() } + 16.dp
                                    if (widthDp > maxTitleWidth) maxTitleWidth = widthDp
                                })
                    }
                }
                Column(modifier = Modifier.width(IntrinsicSize.Max)) {
                    Text("Сумма", style = hintStyle)
                    rows.forEach { row ->
                        Text(
                            row.amount.str2decimalSign(),
                            style = hintStyle,
                            modifier = Modifier
                                .padding(horizontal = 4.dp, vertical = 4.dp)
                                .onGloballyPositioned { coords ->
                                    val widthDp = with(density) { coords.size.width.toDp() } + 16.dp
                                    if (widthDp > maxAmountWidth) maxAmountWidth = widthDp
                                })
                    }
                }
            }
        }

        // Изменено: Добавил minOf для maxTitleWidth с screenWidth / 2.
        // Для чего: Чтобы сохранить ограничение, как в исходном коде.
        maxTitleWidth = minOf(maxTitleWidth, screenWidth / 2)

        // Вычисляем общую рассчитанную ширину
        val totalCalculatedWidth = maxTitleWidth + maxAmountWidth

        // Если общая ширина меньше ширины экрана, добавляем разницу к первому столбцу (как в исходном)
        if (totalCalculatedWidth < screenWidth) {
            val extraWidth = screenWidth - totalCalculatedWidth
            maxTitleWidth += extraWidth
        }

        Column {
            Row(
                modifier = Modifier
                    .horizontalScroll(scrollState)
//                .padding(vertical = 8.dp)
            ) {
                TableCell(
                    text = "Вид удержания",
                    width = maxTitleWidth,
                    maxLines = 2,
                    isHeader = true,
                    contentAlignment = Alignment.CenterStart
                )
                TableCell(
                    text = "Сумма",
                    width = maxAmountWidth,
                    isHeader = true,
                    contentAlignment = Alignment.CenterEnd
                )
            }

            CustomDivider(orientation = Orientation.Horizontal)

            rows.forEach { row ->
                Row(
                    modifier = Modifier
                        .horizontalScroll(scrollState)
                        .background(if (row.isTotal) MaterialTheme.colorScheme.surfaceDim else Color.Transparent)
                ) {
                    TableCell(
                        text = row.title,
                        width = maxTitleWidth,
                        maxLines = 2,
                        contentAlignment = Alignment.CenterStart
                    )
                    TableCell(
                        text = row.amount.str2decimalSign(),
                        width = maxAmountWidth,
                        contentAlignment = Alignment.CenterEnd
                    )
                }
                CustomDivider(orientation = Orientation.Horizontal)
            }
        }
    }
}

// ===========================================
// Вспомогательные data-классы и компонент ячейки таблицы
// ===========================================
private data class EarningsRow(
    val title: String,
    val hours: Long?,
    val percent: Double?,
    val money: Double?,
    val isTotal: Boolean = false
)

private data class RetentionRow(
    val title: String,
    val amount: Double,
    val isTotal: Boolean = false
)

@Composable
private fun TableCell(
    modifier: Modifier = Modifier,
    text: String,
    width: Dp,  // Динамическая ширина, вычисленная на основе контента
    maxLines: Int = 1,
    isHeader: Boolean = false,
    contentAlignment: Alignment = Alignment.Center
) {
    Box(
        modifier = modifier
            .width(width)  // Используем вычисленную максимальную ширину
            .padding(horizontal = 4.dp, vertical = 4.dp),
        contentAlignment = contentAlignment
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = if (isHeader) MaterialTheme.colorScheme.primary.copy(alpha = 0.7f) else MaterialTheme.colorScheme.primary
            ),
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
        )
    }
}