package com.z_company.route.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.z_company.core.ResultState
import com.z_company.core.ui.theme.MonoFont
import com.z_company.core.ui.theme.Shapes
import com.z_company.core.util.MonthFullText.getMonthFullText
import com.z_company.domain.entities.salary.PayrollPaymentCatalog
import com.z_company.domain.entities.salary.PayrollCodeReference
import com.z_company.domain.entities.salary.PayrollCodeReferenceCatalog
import com.z_company.domain.entities.salary.PayrollPaymentType
import com.z_company.domain.entities.salary.SalaryPaymentId
import com.z_company.domain.util.str2decimalSign
import com.z_company.route.component.AppAlertDialog
import com.z_company.route.component.ChipApp
import com.z_company.route.viewmodel.SalaryCalculationUIState
import com.z_company.route.viewmodel.SalaryCalculationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalaryCalculationScreen(
    viewModel: SalaryCalculationViewModel,
    uiState: SalaryCalculationUIState,
    onSettingsSalaryClick: () -> Unit,
) {
    var isCodeSearchActive by rememberSaveable { mutableStateOf(false) }
    var codeSearchQuery by rememberSaveable { mutableStateOf("") }
    val salaryListState = rememberLazyListState()

    BackHandler(enabled = isCodeSearchActive) {
        isCodeSearchActive = false
    }

    if (isCodeSearchActive) {
        PayrollCodeSearchScreen(
            query = codeSearchQuery,
            onQueryChange = { codeSearchQuery = it },
            onBack = { isCodeSearchActive = false },
        )
        return
    }

    // Предупреждение о неустановленной тарифной ставке (без привязки к валюте:
    // разбираем ведущее число тарифа — 0 или отсутствие = не установлена).
    var infoSetTariffRate by remember { mutableStateOf(false) }
    LaunchedEffect(uiState.tariffRate) {
        val rate = uiState.tariffRate?.substringBefore(' ')?.replace(',', '.')?.toDoubleOrNull()
        infoSetTariffRate = uiState.tariffRate == null || rate == 0.0
    }

    // Список доступных месяцев + состояние шторки выбора месяца (как на главном).
    val monthYearList by viewModel.monthYearList.collectAsState()
    var showMonthSheet by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Surface(
                        onClick = { isCodeSearchActive = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                painter = painterResource(com.z_company.route.R.drawable.search_24px),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Расшифровать код",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                actions = {
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
            LazyColumn(
                state = salaryListState,
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .testTag("salary_lazy_column"),
            ) {
                // Переключатель месяца расчёта (месяц + год + стрелки) — как на главном.
                item {
                    SalaryMonthSelector(
                        monthIndex = uiState.monthIndex,
                        year = uiState.year,
                        monthYearList = monthYearList,
                        onSelect = viewModel::selectYearAndMonth,
                        onMonthClick = { showMonthSheet = true },
                    )
                }

                // Hero — крупная сумма «К выдаче»
                item {
                    SalaryHero(
                        amount = uiState.toBeCredited,
                        currency = uiState.currency,
                        totalWorkTime = viewModel.convertTimeToStringFormat(uiState.totalWorkTime),
                    )
                }

                // Предупреждение о неустановленной тарифной ставке
                item {
                    AnimatedVisibility(visible = infoSetTariffRate) {
                        TariffWarningCard()
                    }
                }

                // Начисления
                item { GroupHeader("Начисления") }
                item {
                    AccrualsCard(
                        uiState = uiState,
                        convertTimeToStringFormat = viewModel::convertTimeToStringFormat
                    )
                }

                // Удержания
                item { GroupHeader("Удержания") }
                item { DeductionsCard(uiState = uiState) }

                // Итог «К выдаче» — акцентный тёмный блок
                item { TotalPayoutBlock(amount = uiState.toBeCredited) }

                // Пояснение
                item { SalaryDisclaimer() }

                item { Spacer(modifier = Modifier.height(32.dp)) }
            }
        }
    }

    // Шторка выбора месяца/года (как на главном): чипы месяцев и лет + «Применить».
    if (showMonthSheet && uiState.monthIndex != null && uiState.year != null) {
        SalaryMonthSheet(
            currentMonthIndex = uiState.monthIndex!!,
            currentYear = uiState.year!!,
            monthYearList = monthYearList,
            onApply = { year, month ->
                viewModel.selectYearAndMonth(year to month)
                showMonthSheet = false
            },
            onDismiss = { showMonthSheet = false },
        )
    }

    // Инфо-окно: отработано меньше нормы, но средний час не задан — предлагаем
    // указать его. «Понятно» закрывает навсегда (флаг в настройках), «В настройки»
    // ведёт в настройки ЗП (в этой сессии окно тоже скрываем).
    var underworkInfoSessionDismissed by rememberSaveable { mutableStateOf(false) }
    if (uiState.showSetAverageHourInfo && !underworkInfoSessionDismissed) {
        AppAlertDialog(
            onDismissRequest = { underworkInfoSessionDismissed = true },
            title = "Оплата недоработки",
            text = "За выбранный период отработано меньше нормы. Укажите средний час в настройках зарплаты — и приложение рассчитает оплату недоработки за недостающие часы.",
            confirmText = "В настройки",
            onConfirm = {
                underworkInfoSessionDismissed = true
                onSettingsSalaryClick()
            },
            dismissText = "Понятно",
            onDismiss = {
                viewModel.dismissUnderworkInfoForever()
                underworkInfoSessionDismissed = true
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PayrollCodeSearchScreen(
    query: String,
    onQueryChange: (String) -> Unit,
    onBack: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val results = remember(query) { PayrollCodeReferenceCatalog.search(query) }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(com.z_company.core.R.drawable.ic_arrow_back),
                            contentDescription = "Назад к расчётному листу",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                },
                title = {
                    OutlinedTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                            .testTag("payroll_code_search_field"),
                        placeholder = { Text("Расшифровать код") },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(com.z_company.route.R.drawable.search_24px),
                                contentDescription = null,
                            )
                        },
                        trailingIcon = if (query.isNotEmpty()) {
                            {
                                IconButton(onClick = { onQueryChange("") }) {
                                    Icon(
                                        painter = painterResource(com.z_company.route.R.drawable.ic_close_24px),
                                        contentDescription = "Очистить поиск",
                                    )
                                }
                            }
                        } else null,
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .testTag("payroll_code_search_results"),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 12.dp,
                bottom = 32.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    ),
                ) {
                    Text(
                        text = "Введите код, служебное название или слова из расшифровки. Можно искать по типу — «начисление» или «удержание». Пока строка пуста, показан весь справочник.",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }

            item {
                Text(
                    text = "Найдено: ${results.size}",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (results.isEmpty()) {
                item {
                    Text(
                        text = "Совпадений нет. Проверьте код или попробуйте часть названия.",
                        modifier = Modifier.padding(vertical = 24.dp),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            } else {
                itemsIndexed(
                    items = results,
                    key = { index, item -> "${item.source}-${item.code}-$index" },
                ) { _, item ->
                    PayrollCodeReferenceCard(item)
                }
            }
        }
    }
}

@Composable
private fun PayrollCodeReferenceCard(item: PayrollCodeReference) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = item.code,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = if (item.type == PayrollPaymentType.ACCRUAL) "Начисление" else "Удержание",
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = item.shortName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = item.description, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

// ===========================================
// Переключатель месяца расчёта: «Июль 2026» + стрелки ‹ ›
// (тот же паттерн, что на главном экране).
// ===========================================
@Composable
private fun SalaryMonthSelector(
    monthIndex: Int?,
    year: Int?,
    monthYearList: List<Pair<Int, Int>>,
    onSelect: (Pair<Int, Int>) -> Unit,
    onMonthClick: () -> Unit,
) {
    val currentIndex = if (monthIndex != null && year != null) {
        monthYearList.indexOfFirst { it.first == year && it.second == monthIndex }
    } else -1
    val hasPrev = currentIndex > 0
    val hasNext = currentIndex in 0 until monthYearList.lastIndex

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .clickable { onMonthClick() }
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                text = monthIndex?.let { getMonthFullText(it) } ?: "",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            year?.let {
                Text(
                    modifier = Modifier.padding(start = 6.dp),
                    text = it.toString(),
                    style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        IconButton(
            onClick = { monthYearList.getOrNull(currentIndex - 1)?.let(onSelect) },
            enabled = hasPrev,
        ) {
            Icon(
                painter = painterResource(com.z_company.route.R.drawable.keyboard_arrow_left_24px),
                contentDescription = "Предыдущий месяц",
                tint = if (hasPrev) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
            )
        }
        IconButton(
            onClick = { monthYearList.getOrNull(currentIndex + 1)?.let(onSelect) },
            enabled = hasNext,
        ) {
            Icon(
                painter = painterResource(com.z_company.route.R.drawable.keyboard_arrow_right_24px),
                contentDescription = "Следующий месяц",
                tint = if (hasNext) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
            )
        }
    }
}

// ===========================================
// Шторка выбора месяца и года (тот же паттерн, что на главном экране):
// чипы месяцев + чипы лет + кнопка «Применить».
// ===========================================
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun SalaryMonthSheet(
    currentMonthIndex: Int,
    currentYear: Int,
    monthYearList: List<Pair<Int, Int>>,
    onApply: (year: Int, month: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val monthList = monthYearList.map { it.second }.distinct().sorted()
    val yearList = monthYearList.map { it.first }.distinct().sorted()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        // tonalElevation=0, иначе Material накладывает surfaceTint и белая шторка мятнеет.
        tonalElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Text(
                text = "Выберите месяц и год",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 24.dp),
            )

            var selectedMonth by remember { mutableIntStateOf(currentMonthIndex) }
            var selectedYear by remember { mutableIntStateOf(currentYear) }

            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                monthList.forEach { m ->
                    ChipApp(
                        selected = selectedMonth == m,
                        onClick = { selectedMonth = m },
                        label = getMonthFullText(m),
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                yearList.forEach { y ->
                    ChipApp(
                        selected = selectedYear == y,
                        onClick = { selectedYear = y },
                        label = "$y",
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { onApply(selectedYear, selectedMonth) },
                modifier = Modifier.fillMaxWidth(),
                shape = Shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                ),
            ) {
                Text(
                    text = "Применить",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// ===========================================
// Hero — «К ВЫДАЧЕ · МЕСЯЦ» + крупная mono-сумма
// ===========================================
@Composable
private fun SalaryHero(amount: Double?, currency: String, totalWorkTime: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 4.dp, top = 6.dp, bottom = 16.dp)
    ) {
        // Месяц вынесен в отдельный переключатель над Hero, поэтому здесь — только «К ВЫДАЧЕ».
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "К ВЫДАЧЕ",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = formatMoney(amount),
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = 34.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-1.5).sp,
                        ),
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                    )
                    Text(
                        text = " $currency",
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "ВСЕГО ОТРАБОТАНО",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.End,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = totalWorkTime,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = MonoFont,
                        fontWeight = FontWeight.Bold,
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.End,
                )
            }
        }
    }
}

// ===========================================
// Заголовок группы карточек — mono-caption
// ===========================================
@Composable
private fun GroupHeader(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp, top = 20.dp, bottom = 8.dp)
    )
}

// ===========================================
// Карточка (белая поверхность + тонкая рамка, 16r)
// ===========================================
@Composable
private fun PayCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = Shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondary),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column { content() }
    }
}

// ===========================================
// Прокручиваемая таблица начислений/удержаний.
//
// При нормальном размере шрифта столбцы заполняют ширину карточки и имена
// переносятся в 2 строки — как в референсе. При крупном системном шрифте или
// узком экране содержимое не обрезается, а прокручивается по горизонтали
// (иначе суммы и часы схлопываются). Ширины столбцов измеряются по контенту,
// «сумма» дотягивается до правого края.
// ===========================================
private enum class ColType { NAME, VALUE, MONEY }

private data class CellVal(val text: String, val faint: Boolean = false)

private data class PayColumn(val header: String, val type: ColType)

@Composable
private fun PayScrollTable(columns: List<PayColumn>, rows: List<List<CellVal>>) {
    val scroll = rememberScrollState()
    val density = LocalDensity.current
    // Ширина карточки ≈ ширина экрана минус горизонтальные отступы LazyColumn (16+16).
    val cardWidth = LocalConfiguration.current.screenWidthDp.dp - 32.dp
    val leadInset = 16.dp
    val trailInset = 16.dp

    val nameStyle = MaterialTheme.typography.bodyMedium
    val valueStyle = MaterialTheme.typography.bodyMedium.copy(
        fontFamily = MonoFont, fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
    )
    val moneyStyle = MaterialTheme.typography.bodyMedium.copy(
        fontFamily = MonoFont, fontWeight = FontWeight.SemiBold, fontSize = 15.sp,
    )
    fun styleFor(type: ColType): TextStyle = when (type) {
        ColType.NAME -> nameStyle
        ColType.VALUE -> valueStyle
        ColType.MONEY -> moneyStyle
    }

    // Измеренные «естественные» ширины столбцов (по контенту, без ограничений).
    // Хранятся отдельно от итоговых ширин, чтобы измерение не зациклилось.
    val natural = remember(columns, rows) {
        List(columns.size) { mutableStateOf(0.dp) }
    }
    // Ширина самого длинного отдельного слова в столбце имени — используется как
    // нижняя граница ширины столбца, чтобы ни одно слово не обрезалось при крупном
    // шрифте (если слово шире доступного места — таблица прокрутится по горизонтали).
    val nameWordFloor = remember(columns, rows) { mutableStateOf(0.dp) }
    val measureScroll = rememberScrollState()

    // Итоговые ширины столбцов на основе измеренных.
    // Столбец «Вид …» получает место, оставшееся после числовых столбцов
    // (длинные имена переносятся в 2 строки, как в референсе). Если числовые
    // столбцы сами шире карточки (крупный шрифт) — имя ужимается до минимума,
    // и таблица прокручивается по горизонтали, ничего не обрезая.
    val target = cardWidth - leadInset - trailInset - 6.dp
    val nameIdx = columns.indexOfFirst { it.type == ColType.NAME }
    val moneyIdx = columns.indexOfLast { it.type == ColType.MONEY }
    val display = natural.map { it.value }.toMutableList()
    if (nameIdx >= 0) {
        val numericTotal = display.filterIndexed { i, _ -> i != nameIdx }.fold(0.dp) { a, d -> a + d }
        val availForName = target - numericTotal
        val base = if (availForName >= 120.dp) {
            // Числа помещаются — имя занимает остаток и переносится (нормальный шрифт).
            minOf(natural[nameIdx].value, availForName)
        } else {
            // Числа сами шире карточки (крупный шрифт): даём имени до половины карточки,
            // таблица прокручивается по горизонтали, суммы не обрезаются.
            maxOf(minOf(natural[nameIdx].value, cardWidth / 2), 88.dp)
        }
        // Ширина столбца имени не меньше самого длинного слова — иначе слово
        // обрежется многоточием при крупном шрифте. Если получилось шире доступного
        // места, таблица прокрутится по горизонтали (перенос по словам сохраняется).
        display[nameIdx] = maxOf(base, nameWordFloor.value)
    }
    // Если суммарно уже ширины карточки — дотягиваем «сумму» до правого края.
    val colsTotal = display.fold(0.dp) { a, d -> a + d }
    if (moneyIdx >= 0 && colsTotal > 0.dp && colsTotal < target) {
        display[moneyIdx] = display[moneyIdx] + (target - colsTotal)
    }

    // Скрытый проход измерения и реальный контент лежат в одном Box (перекрываются),
    // чтобы измерение не занимало собственную высоту.
    Box {
        // Измерение (невидимое, без ограничения ширины — через horizontalScroll).
        // Раскладываем детей (чтобы сработал onGloballyPositioned), но сообщаем
        // родителю нулевой размер: иначе высокий столбец измерения растягивает Box
        // и под таблицей появляется пустое место (особенно заметно при крупном шрифте).
        Box(
            modifier = Modifier
                .alpha(0f)
                .layout { measurable, constraints ->
                    val placeable = measurable.measure(constraints)
                    layout(0, 0) { placeable.place(0, 0) }
                },
        ) {
            Row(modifier = Modifier.horizontalScroll(measureScroll)) {
                columns.forEachIndexed { c, col ->
                    Column {
                        val texts = listOf(col.header) + rows.map { it[c].text }
                        val measureStyle = styleFor(col.type)
                        texts.forEach { text ->
                            Text(
                                text = text,
                                style = measureStyle,
                                maxLines = 1,
                                modifier = Modifier.onGloballyPositioned { coords ->
                                    val w = with(density) { coords.size.width.toDp() } + 16.dp
                                    if (w > natural[c].value) natural[c].value = w
                                },
                            )
                            if (col.type == ColType.NAME) {
                                // Отдельно меряем каждое слово: столбец имени не должен
                                // быть уже самого длинного слова, иначе оно обрежется.
                                text.split(' ').forEach { word ->
                                    Text(
                                        text = word,
                                        style = measureStyle,
                                        maxLines = 1,
                                        modifier = Modifier.onGloballyPositioned { coords ->
                                            val w = with(density) { coords.size.width.toDp() } + 12.dp
                                            if (w > nameWordFloor.value) nameWordFloor.value = w
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Column {
            // Заголовок столбцов
            Row(
                modifier = Modifier
                    .horizontalScroll(scroll)
                    .padding(top = 11.dp, bottom = 9.dp),
            ) {
                Spacer(modifier = Modifier.width(leadInset))
                columns.forEachIndexed { c, col ->
                    HeaderCell(text = col.header, width = display[c], type = col.type)
                }
                Spacer(modifier = Modifier.width(trailInset))
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Строки данных
            rows.forEachIndexed { r, row ->
                Row(modifier = Modifier.horizontalScroll(scroll)) {
                    Spacer(modifier = Modifier.width(leadInset))
                    columns.forEachIndexed { c, col ->
                        DataCell(cell = row[c], width = display[c], type = col.type)
                    }
                    Spacer(modifier = Modifier.width(trailInset))
                }
                if (r < rows.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 20.dp),
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun HeaderCell(text: String, width: Dp, type: ColType) {
    Box(
        modifier = Modifier.width(width).padding(horizontal = 4.dp),
        contentAlignment = if (type == ColType.NAME) Alignment.CenterStart else Alignment.CenterEnd,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            textAlign = if (type == ColType.NAME) TextAlign.Start else TextAlign.End,
        )
    }
}

@Composable
private fun DataCell(cell: CellVal, width: Dp, type: ColType) {
    val style = when (type) {
        ColType.NAME -> MaterialTheme.typography.bodyMedium
        ColType.VALUE -> MaterialTheme.typography.bodyMedium.copy(
            fontFamily = MonoFont, fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
        )
        ColType.MONEY -> MaterialTheme.typography.bodyMedium.copy(
            fontFamily = MonoFont, fontWeight = FontWeight.SemiBold, fontSize = 15.sp,
        )
    }
    val color = when (type) {
        ColType.NAME, ColType.MONEY -> MaterialTheme.colorScheme.primary
        ColType.VALUE -> if (cell.faint) MaterialTheme.colorScheme.primary.copy(alpha = 0.38f)
        else MaterialTheme.colorScheme.onSurfaceVariant
    }
    Box(
        modifier = Modifier.width(width).padding(horizontal = 4.dp, vertical = 13.dp),
        contentAlignment = if (type == ColType.NAME) Alignment.CenterStart else Alignment.CenterEnd,
    ) {
        Text(
            text = cell.text,
            style = style,
            color = color,
            // Имя выплаты может занять до 3 строк — при крупном шрифте длинные
            // названия («Надбавка за класс квалификации») не обрезаются многоточием.
            maxLines = if (type == ColType.NAME) 3 else 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = if (type == ColType.NAME) TextAlign.Start else TextAlign.End,
        )
    }
}

// ===========================================
// Итоговая строка внутри карточки — accent-tint фон
// ===========================================
@Composable
private fun PayTotalRow(name: String, amount: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = amount,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = MonoFont,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
            ),
            color = MaterialTheme.colorScheme.tertiary,
            maxLines = 1,
        )
    }
}

// ===========================================
// Таблица начислений
// ===========================================
@Composable
private fun AccrualsCard(
    uiState: SalaryCalculationUIState,
    convertTimeToStringFormat: (Long?) -> String,
) {
    val columns = listOf(
        PayColumn("КОД", ColType.VALUE),
        PayColumn("ВИД ВЫПЛАТЫ", ColType.NAME),
        PayColumn("ЧАСЫ", ColType.VALUE),
        PayColumn("%", ColType.VALUE),
        PayColumn("СУММА", ColType.MONEY),
    )
    val rows = buildAccrualRows(uiState).map { row ->
        listOf(
            CellVal(PayrollPaymentCatalog[row.paymentId].codeLabel, faint = PayrollPaymentCatalog[row.paymentId].codes.isEmpty()),
            CellVal(row.title),
            CellVal(row.hours?.let { convertTimeToStringFormat(it) } ?: "—", faint = row.hours == null),
            CellVal(row.percent?.let { formatPercent(it) } ?: "—", faint = row.percent == null),
            CellVal(formatMoney(row.money)),
        )
    }

    PayCard {
        PayScrollTable(columns = columns, rows = rows)
        PayTotalRow(
            name = "Всего начислено",
            amount = formatMoney(uiState.totalChargedMoney),
        )
    }
}

// ===========================================
// Таблица удержаний
// ===========================================
@Composable
private fun DeductionsCard(uiState: SalaryCalculationUIState) {
    val columns = listOf(
        PayColumn("КОД", ColType.VALUE),
        PayColumn("ВИД УДЕРЖАНИЯ", ColType.NAME),
        PayColumn("СУММА", ColType.MONEY),
    )
    val rows = buildDeductionRows(uiState).map { row ->
        val payment = PayrollPaymentCatalog[row.paymentId]
        listOf(
            CellVal(payment.codeLabel, faint = payment.codes.isEmpty()),
            CellVal(row.title),
            CellVal(formatMoney(row.money)),
        )
    }

    PayCard {
        PayScrollTable(columns = columns, rows = rows)
        PayTotalRow(
            name = "Всего удержано",
            amount = formatMoney(uiState.totalRetention),
        )
    }
}

// ===========================================
// Итог «К выдаче» — тёмный акцентный блок
// ===========================================
@Composable
private fun TotalPayoutBlock(amount: Double?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 22.dp)
            .background(MaterialTheme.colorScheme.primary, Shapes.medium)
            .padding(horizontal = 20.dp, vertical = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "К ВЫДАЧЕ",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f),
        )
        Text(
            text = formatMoney(amount),
            style = MaterialTheme.typography.displayMedium.copy(
                fontFamily = MonoFont,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 26.sp,
                letterSpacing = (-1).sp,
            ),
            color = MaterialTheme.colorScheme.onPrimary,
            maxLines = 1,
        )
    }
}

// ===========================================
// Пояснение (info-иконка + текст)
// ===========================================
@Composable
private fun SalaryDisclaimer() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 6.dp, end = 6.dp, top = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            painter = painterResource(com.z_company.route.R.drawable.info_24px),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(top = 1.dp)
                .height(16.dp)
                .width(16.dp),
        )
        Text(
            text = "Расчёт носит информационный характер: некоторые виды выплат могут отличаться в зависимости от нормативных документов вашего депо.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ===========================================
// Предупреждение о неустановленной тарифной ставке
// ===========================================
@Composable
private fun TariffWarningCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp),
        shape = Shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondary),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                painter = painterResource(com.z_company.route.R.drawable.info_24px),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier
                    .padding(top = 1.dp)
                    .height(18.dp)
                    .width(18.dp),
            )
            Text(
                text = "Не установлена тарифная ставка. Перейдите в настройки для её указания.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

// ===========================================
// Формирование строк начислений (только ненулевые суммы)
// ===========================================
internal data class AccrualRow(
    val paymentId: SalaryPaymentId,
    val title: String,
    val hours: Long?,
    val percent: Double?,
    val money: Double?,
)

internal data class DeductionRow(
    val paymentId: SalaryPaymentId,
    val title: String,
    val percent: Double?,
    val money: Double,
)

internal fun buildDeductionRows(uiState: SalaryCalculationUIState): List<DeductionRow> = listOfNotNull(
    uiState.retentionNdfl?.takeIf { it > 0 }?.let { DeductionRow(SalaryPaymentId.NDFL, "НДФЛ (13 %)", 13.0, it) },
    uiState.unionistsRetention?.takeIf { it > 0 }?.let { DeductionRow(SalaryPaymentId.UNION, "Профсоюз", null, it) },
    uiState.otherRetention?.takeIf { it > 0 }?.let { DeductionRow(SalaryPaymentId.OTHER_DEDUCTION, "Прочие удержания", null, it) },
    uiState.welfareRetention?.takeIf { it > 0 }?.let { DeductionRow(SalaryPaymentId.WELFARE, "Благосостояние", null, it) },
    uiState.alimonyRetention?.takeIf { it > 0 }?.let { DeductionRow(SalaryPaymentId.ALIMONY, "Алименты", null, it) },
)

private fun formatPercent(value: Double): String = "%.1f".format(value).replace('.', ',')

// Денежный формат — общий для приложения (str2decimalSign → «69 928,32»).
// null трактуем как ноль, чтобы в шапке/итогах всегда было «0,00».
private fun formatMoney(value: Double?): String = (value ?: 0.0).str2decimalSign()

internal fun buildAccrualRows(uiState: SalaryCalculationUIState): List<AccrualRow> = listOfNotNull(
    // Основные выплаты
    AccrualRow(SalaryPaymentId.TARIFF, "Оплата по тарифу", uiState.paymentAtTariffHours, null, uiState.paymentAtTariffMoney),
    AccrualRow(
        SalaryPaymentId.NIGHT, "Ночные часы",
        uiState.paymentNightTimeHours,
        uiState.paymentNightTimePercent,
        uiState.paymentNightTimeMoney
    ),
    AccrualRow(SalaryPaymentId.PASSENGER, "Пассажиром", uiState.paymentAtPassengerHours, null, uiState.paymentAtPassengerMoney),
    AccrualRow(
        SalaryPaymentId.RESERVE, "Резервом",
        uiState.paymentAtSingleLocomotiveHours,
        null,
        uiState.paymentAtSingleLocomotiveMoney
    ),
    AccrualRow(SalaryPaymentId.HOLIDAY, "Праздничные", uiState.paymentHolidayHours, null, uiState.paymentHolidayMoney),
    AccrualRow(SalaryPaymentId.AVERAGE, "Оплата по среднему", uiState.averagePaymentHours, null, uiState.averagePaymentMoney),
    uiState.underworkMoney?.takeIf { it > 0 }?.let {
        AccrualRow(SalaryPaymentId.UNDERWORK, "Оплата недоработки", uiState.underworkHours, null, it)
    },
    AccrualRow(
        SalaryPaymentId.DISABLED_CHILD_CARE, "По уходу за ребенком-инвалидом",
        uiState.caringForDisableChildrenHours,
        null,
        uiState.caringForDisableChildrenMoney
    ),
    uiState.businessTripHours?.takeIf { it > 0 }?.let {
        AccrualRow(SalaryPaymentId.BUSINESS_TRIP, "Командировка (по среднему)", it, null, uiState.businessTripMoney)
    },
    uiState.technicalStudyHours?.takeIf { it > 0 }?.let {
        AccrualRow(SalaryPaymentId.TECHNICAL_STUDY, "Технические занятия", it, null, uiState.technicalStudyMoney)
    },

    // Надбавки
    uiState.zonalSurchargePercent?.let {
        AccrualRow(SalaryPaymentId.ZONAL, "Зональная надбавка", null, it, uiState.zonalSurchargeMoney)
    },
    uiState.surchargeQualificationClassPercent?.let {
        AccrualRow(SalaryPaymentId.QUALIFICATION_CLASS, "Надбавка за класс квалификации", null, it, uiState.surchargeQualificationClassMoney)
    },
    *uiState.linearMileageAccruals.map { accrual ->
        AccrualRow(
            SalaryPaymentId.LINEAR_MILEAGE, "Доплата за пробег: ${accrual.phaseName} (${formatMoney(accrual.rate)} ₽/км)",
            null,
            null,
            accrual.money,
        )
    }.toTypedArray(),
    uiState.onePersonOperationPercent?.let {
        AccrualRow(SalaryPaymentId.ONE_PERSON_FREIGHT, "В одно лицо (грузовые)", uiState.onePersonOperationHours, it, uiState.onePersonOperationMoney)
    },
    uiState.onePersonOperationPassengerTrainPercent?.let {
        AccrualRow(
            SalaryPaymentId.ONE_PERSON_PASSENGER, "В одно лицо (пассажирские)",
            uiState.onePersonOperationPassengerTrainHours,
            it,
            uiState.onePersonOperationPassengerTrainMoney
        )
    },
    uiState.harmfulnessSurchargePercent?.let {
        AccrualRow(SalaryPaymentId.HARMFULNESS, "Вредность", null, it, uiState.harmfulnessSurchargeMoney)
    },
    uiState.districtSurchargeCoefficient?.let {
        AccrualRow(SalaryPaymentId.DISTRICT, "Районный коэффициент", null, it, uiState.districtSurchargeMoney)
    },
    uiState.nordicSurchargePercent?.let {
        AccrualRow(SalaryPaymentId.NORDIC, "Северная надбавка", null, it, uiState.nordicSurchargeMoney)
    },
    uiState.otherSurchargePercent?.let {
        AccrualRow(SalaryPaymentId.OTHER_SURCHARGE, "Прочие надбавки", null, it, uiState.otherSurchargeMoney)
    },
    uiState.restInExcessOfTheNormMoney?.takeIf { it > 0 }?.let {
        AccrualRow(SalaryPaymentId.EXCESS_REST, "Переотдых", uiState.restInExcessOfTheNormTime, null, it)
    },

    // Списки надбавок
    *(0 until minOf(
        uiState.surchargeExtendedServicePhaseHour.size,
        uiState.surchargeExtendedServicePhasePercent.size,
        uiState.surchargeExtendedServicePhaseMoney.size
    )).mapNotNull { i ->
        val money = uiState.surchargeExtendedServicePhaseMoney.getOrNull(i) ?: 0.0
        if (money > 0) {
            AccrualRow(
                SalaryPaymentId.EXTENDED_SERVICE, "Удлиненное плечо (${uiState.surchargeExtendedServicePhasePercent[i] ?: ""}%)",
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
            AccrualRow(
                SalaryPaymentId.HEAVY_TRAIN, "Тяжелые поезда (${uiState.surchargeHeavyTransPercent[i] ?: ""}%)",
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
            AccrualRow(
                SalaryPaymentId.LONG_TRAIN, "Длинносост. (${uiState.surchargeLongTrainPercent[i] ?: ""}%)",
                uiState.surchargeLongTrainHour.getOrNull(i),
                uiState.surchargeLongTrainPercent.getOrNull(i)?.toDoubleOrNull(),
                money
            )
        } else null
    }.toTypedArray(),

    uiState.surchargeHeavyLongDistanceTrainsMoney?.takeIf { it > 0 }?.let {
        AccrualRow(
            SalaryPaymentId.HEAVY_LONG_DISTANCE, "Доплата за ПДМ (>6000 т. и >350 осей)",
            uiState.surchargeHeavyLongDistanceTrainsHours,
            uiState.surchargeHeavyLongDistanceTrainsPercent,
            it
        )
    },

    uiState.surchargeDoubledTrainFirstMoney?.takeIf { it > 0 }?.let {
        AccrualRow(SalaryPaymentId.DOUBLED_TRAIN, "Сдвоенные поезда (30%)", uiState.surchargeDoubledTrainFirstHours, 30.0, it)
    },
    uiState.surchargeDoubledTrainSecondMoney?.takeIf { it > 0 }?.let {
        AccrualRow(SalaryPaymentId.DOUBLED_TRAIN, "Сдвоенные поезда (15%)", uiState.surchargeDoubledTrainSecondHours, 15.0, it)
    },

    // Сверхурочные
    AccrualRow(SalaryPaymentId.OVERTIME_BASE, "Сверхурочные часы", uiState.paymentAtOvertimeHours, null, uiState.paymentAtOvertimeMoney),
    uiState.surchargeAtOvertime05Money?.takeIf { it > 0 }?.let {
        AccrualRow(SalaryPaymentId.OVERTIME_HALF, "Доплата за сверхурочные (50%)", uiState.surchargeAtOvertime05Hours, 50.0, it)
    },
    uiState.surchargeAtOvertimeMoney?.takeIf { it > 0 }?.let {
        AccrualRow(SalaryPaymentId.OVERTIME_FULL, "Доплата за сверхурочные (100%)", uiState.surchargeAtOvertimeHours, 100.0, it)
    },
).filter { it.money != null && it.money > 0 }
