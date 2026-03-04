package com.z_company.shared.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.z_company.core.ResultState
import com.z_company.domain.util.str2decimalSign
import com.z_company.shared.viewmodel.SalaryCalculationSharedViewModel
import com.z_company.shared.viewmodel.SalaryCalculationUIState
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalaryCalculationScreen(
    onBackClick: () -> Unit,
    onShowSettingSalary: () -> Unit,
) {
    val viewModel: SalaryCalculationSharedViewModel = koinInject()
    val uiState by viewModel.uiState.collectAsState()

    val colorPrimary = MaterialTheme.colorScheme.primary
    val styleHint = MaterialTheme.typography.bodyMedium

    var infoBlockVisible by rememberSaveable { mutableStateOf(true) }
    var infoSetTariffRate by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(uiState.tariffRate) {
        infoSetTariffRate = uiState.tariffRate == "0,00 ₽" || uiState.tariffRate == null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "${uiState.toBeCredited?.str2decimalSign() ?: "0,00"} ₽",
                        overflow = TextOverflow.Visible,
                        maxLines = 2,
                        style = MaterialTheme.typography.titleSmall,
                        color = colorPrimary,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                actions = {
                    IconButton(
                        modifier = Modifier.padding(end = 16.dp),
                        onClick = onShowSettingSalary,
                    ) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Настройки",
                            tint = colorPrimary,
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        if (uiState.screenState is ResultState.Loading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text((uiState.screenState as ResultState.Loading).message)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
            ) {
                // Dismissible info block
                item {
                    AnimatedVisibility(visible = infoBlockVisible) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = MaterialTheme.shapes.medium,
                                )
                                .border(
                                    0.5.dp,
                                    MaterialTheme.colorScheme.tertiary,
                                    MaterialTheme.shapes.medium,
                                )
                                .padding(16.dp),
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                style = styleHint,
                                color = colorPrimary,
                                text = "Данный расчет носит информационный характер, некоторые виды выплат могут отличаться в зависимости от внутренних нормативных документов вашего депо.",
                            )
                            Button(
                                shape = MaterialTheme.shapes.medium,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                ),
                                onClick = { infoBlockVisible = false },
                            ) {
                                Text(
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.secondary,
                                    text = "Понятно",
                                )
                            }
                        }
                    }
                }

                // Tariff rate warning
                item {
                    AnimatedVisibility(visible = infoSetTariffRate) {
                        Column(
                            modifier = Modifier
                                .padding(top = 8.dp)
                                .fillMaxWidth()
                                .background(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = MaterialTheme.shapes.medium,
                                )
                                .border(0.5.dp, MaterialTheme.colorScheme.tertiary, MaterialTheme.shapes.medium)
                                .padding(16.dp),
                            horizontalAlignment = Alignment.End,
                        ) {
                            Text(
                                style = styleHint,
                                color = colorPrimary,
                                text = "Не установлена тарифная ставка. Перейдите в настройки для её указания.",
                            )
                        }
                    }
                }

                // Earnings header
                item {
                    Text(
                        text = "Начисления",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(top = 16.dp),
                    )
                }

                // Earnings table
                item {
                    EarningsTable(uiState = uiState, convertTimeToStringFormat = viewModel::convertTimeToStringFormat)
                }

                // Retentions header
                item {
                    Text(
                        text = "Удержания",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(top = 32.dp, bottom = 16.dp),
                    )
                }

                // Retentions table
                item {
                    RetentionsTable(uiState = uiState)
                }

                // Total row
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "К выдаче",
                            style = MaterialTheme.typography.titleSmall,
                            color = colorPrimary,
                        )
                        Text(
                            text = "${uiState.toBeCredited?.str2decimalSign() ?: "0,00"} ₽",
                            style = MaterialTheme.typography.titleSmall,
                            color = colorPrimary,
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(32.dp)) }
            }
        }
    }
}

// ===========================================
// Earnings Table (with horizontal scroll)
// ===========================================
@Composable
private fun EarningsTable(uiState: SalaryCalculationUIState, convertTimeToStringFormat: (Long?) -> String) {
    val scrollState = rememberScrollState()

    val rows = buildEarningsRows(uiState).filter { row ->
        row.isTotal || (row.money != null && row.money > 0)
    }

    Column {
        // Header
        Row(
            modifier = Modifier
                .horizontalScroll(scrollState)
                .padding(vertical = 8.dp),
        ) {
            TableCell(text = "Вид выплаты", width = 180.dp, isHeader = true, contentAlignment = Alignment.CenterStart)
            TableCell(text = "Часы", width = 80.dp, isHeader = true, contentAlignment = Alignment.Center)
            TableCell(text = "%", width = 60.dp, isHeader = true, contentAlignment = Alignment.Center)
            TableCell(text = "Сумма", width = 100.dp, isHeader = true, contentAlignment = Alignment.CenterEnd)
        }
        HorizontalDivider()

        // Rows
        rows.forEach { row ->
            Row(
                modifier = Modifier
                    .horizontalScroll(scrollState)
                    .background(if (row.isTotal) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent),
            ) {
                TableCell(text = row.title, width = 180.dp, maxLines = 2, contentAlignment = Alignment.CenterStart)
                TableCell(
                    text = row.hours?.let { convertTimeToStringFormat(it) } ?: "",
                    width = 80.dp,
                    contentAlignment = Alignment.Center,
                )
                TableCell(
                    text = row.percent?.let { formatPercent(it) } ?: "",
                    width = 60.dp,
                    contentAlignment = Alignment.Center,
                )
                TableCell(
                    text = row.money?.str2decimalSign() ?: "",
                    width = 100.dp,
                    contentAlignment = Alignment.CenterEnd,
                )
            }
            HorizontalDivider()
        }
    }
}

// ===========================================
// Retentions Table
// ===========================================
@Composable
private fun RetentionsTable(uiState: SalaryCalculationUIState) {
    val scrollState = rememberScrollState()

    val rows = listOfNotNull(
        uiState.retentionNdfl?.takeIf { it > 0 }?.let { RetentionRow("НДФЛ (13%)", it) },
        uiState.unionistsRetention?.takeIf { it > 0 }?.let { RetentionRow("Профсоюз", it) },
        uiState.otherRetention?.takeIf { it > 0 }?.let { RetentionRow("Прочие удержания", it) },
        uiState.totalRetention?.takeIf { it > 0 }?.let { RetentionRow("Всего удержано", it, isTotal = true) },
    )

    Column {
        Row(
            modifier = Modifier.horizontalScroll(scrollState),
        ) {
            TableCell(text = "Вид удержания", width = 240.dp, isHeader = true, contentAlignment = Alignment.CenterStart)
            TableCell(text = "Сумма", width = 120.dp, isHeader = true, contentAlignment = Alignment.CenterEnd)
        }
        HorizontalDivider()

        rows.forEach { row ->
            Row(
                modifier = Modifier
                    .horizontalScroll(scrollState)
                    .background(if (row.isTotal) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent),
            ) {
                TableCell(text = row.title, width = 240.dp, maxLines = 2, contentAlignment = Alignment.CenterStart)
                TableCell(text = row.amount.str2decimalSign(), width = 120.dp, contentAlignment = Alignment.CenterEnd)
            }
            HorizontalDivider()
        }
    }
}

// ===========================================
// Helper data classes
// ===========================================
private data class EarningsRow(
    val title: String,
    val hours: Long?,
    val percent: Double?,
    val money: Double?,
    val isTotal: Boolean = false,
)

private data class RetentionRow(
    val title: String,
    val amount: Double,
    val isTotal: Boolean = false,
)

@Composable
private fun TableCell(
    text: String,
    width: Dp,
    maxLines: Int = 1,
    isHeader: Boolean = false,
    contentAlignment: Alignment = Alignment.Center,
) {
    Box(
        modifier = Modifier
            .width(width)
            .padding(horizontal = 4.dp, vertical = 4.dp),
        contentAlignment = contentAlignment,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = if (isHeader) MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                else MaterialTheme.colorScheme.primary,
            ),
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun formatPercent(value: Double): String {
    val intPart = value.toLong()
    val fracPart = ((value - intPart) * 10).toLong()
    return if (fracPart == 0L) "$intPart" else "$intPart,$fracPart"
}

private fun buildEarningsRows(uiState: SalaryCalculationUIState): List<EarningsRow> {
    return listOfNotNull(
        EarningsRow("Оплата по тарифу", uiState.paymentAtTariffHours, null, uiState.paymentAtTariffMoney),
        EarningsRow("Ночные часы", uiState.paymentNightTimeHours, uiState.paymentNightTimePercent, uiState.paymentNightTimeMoney),
        EarningsRow("Пассажиром", uiState.paymentAtPassengerHours, null, uiState.paymentAtPassengerMoney),
        EarningsRow("Резервом", uiState.paymentAtSingleLocomotiveHours, null, uiState.paymentAtSingleLocomotiveMoney),
        EarningsRow("Праздничные", uiState.paymentHolidayHours, null, uiState.paymentHolidayMoney),
        EarningsRow("Оплата по среднему", uiState.averagePaymentHours, null, uiState.averagePaymentMoney),
        EarningsRow("По уходу за ребенком-инвалидом", uiState.caringForDisableChildrenHours, null, uiState.caringForDisableChildrenMoney),
        uiState.zonalSurchargePercent?.let {
            EarningsRow("Зональная надбавка", null, it, uiState.zonalSurchargeMoney)
        },
        uiState.surchargeQualificationClassPercent?.let {
            EarningsRow("Надбавка за класс квалификации", null, it, uiState.surchargeQualificationClassMoney)
        },
        uiState.onePersonOperationPercent?.let {
            EarningsRow("В одно лицо (грузовые)", null, it, uiState.onePersonOperationMoney)
        },
        uiState.onePersonOperationPassengerTrainPercent?.let {
            EarningsRow("В одно лицо (пассажирские)", null, it, uiState.onePersonOperationPassengerTrainMoney)
        },
        uiState.harmfulnessSurchargePercent?.let {
            EarningsRow("Вредность", null, it, uiState.harmfulnessSurchargeMoney)
        },
        uiState.surchargeLongDistanceTrainsPercent?.let {
            EarningsRow("Длинные поезда", uiState.surchargeLongDistanceTrainsHours, it, uiState.surchargeLongDistanceTrainsMoney)
        },
        uiState.districtSurchargeCoefficient?.let {
            EarningsRow("Районный коэффициент", null, it, uiState.districtSurchargeMoney)
        },
        uiState.nordicSurchargePercent?.let {
            EarningsRow("Северная надбавка", null, it, uiState.nordicSurchargeMoney)
        },
        uiState.otherSurchargePercent?.let {
            EarningsRow("Прочие надбавки", null, it, uiState.otherSurchargeMoney)
        },
        // Extended service phase surcharges
        *(0 until minOf(
            uiState.surchargeExtendedServicePhaseHour.size,
            uiState.surchargeExtendedServicePhasePercent.size,
            uiState.surchargeExtendedServicePhaseMoney.size,
        )).mapNotNull { i ->
            val money = uiState.surchargeExtendedServicePhaseMoney.getOrNull(i) ?: 0.0
            if (money > 0) {
                EarningsRow(
                    "Удлиненное плечо (${uiState.surchargeExtendedServicePhasePercent[i] ?: ""}%)",
                    uiState.surchargeExtendedServicePhaseHour.getOrNull(i),
                    uiState.surchargeExtendedServicePhasePercent.getOrNull(i)?.toDoubleOrNull(),
                    money,
                )
            } else null
        }.toTypedArray(),
        // Heavy trains surcharges
        *(0 until minOf(
            uiState.surchargeHeavyTransHour.size,
            uiState.surchargeHeavyTransPercent.size,
            uiState.surchargeHeavyTransMoney.size,
        )).mapNotNull { i ->
            val money = uiState.surchargeHeavyTransMoney.getOrNull(i) ?: 0.0
            if (money > 0) {
                EarningsRow(
                    "Тяжелые поезда (${uiState.surchargeHeavyTransPercent[i] ?: ""}%)",
                    uiState.surchargeHeavyTransHour.getOrNull(i),
                    uiState.surchargeHeavyTransPercent.getOrNull(i)?.toDoubleOrNull(),
                    money,
                )
            } else null
        }.toTypedArray(),
        // Overtime
        EarningsRow("Сверхурочные часы", uiState.paymentAtOvertimeHours, null, uiState.paymentAtOvertimeMoney),
        uiState.surchargeAtOvertime05Money?.takeIf { it > 0 }?.let {
            EarningsRow("Доплата за сверхурочные (50%)", uiState.surchargeAtOvertime05Hours, 50.0, it)
        },
        uiState.surchargeAtOvertimeMoney?.takeIf { it > 0 }?.let {
            EarningsRow("Доплата за сверхурочные (100%)", uiState.surchargeAtOvertimeHours, 100.0, it)
        },
        // Total
        EarningsRow("Всего начислено", null, null, uiState.totalChargedMoney, isTotal = true),
    )
}
