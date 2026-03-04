package com.z_company.shared.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.z_company.core.ResultState
import com.z_company.shared.viewmodel.SalaryCalculationSharedViewModel
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalaryCalculationScreen(
    onBackClick: () -> Unit,
    onShowSettingSalary: () -> Unit,
) {
    val viewModel: SalaryCalculationSharedViewModel = koinInject()
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Расчёт зарплаты") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
            )
        }
    ) { padding ->
        when (uiState.screenState) {
            is ResultState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        CircularProgressIndicator()
                        Text(
                            text = "Пересчет...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            is ResultState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Ошибка загрузки данных",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }

            is ResultState.Success -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Spacer(modifier = Modifier.height(4.dp))

                    // Header
                    Text(
                        text = uiState.month,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )

                    // Summary card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                        ),
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = "Сводка",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                            uiState.tariffRate?.let { rate ->
                                SalaryRow("Тарифная ставка", rate)
                            }
                            uiState.normaHours?.let { norma ->
                                SalaryRow("Норма часов", "$norma ч")
                            }
                            SalaryRow("Рабочее время", viewModel.convertTimeToStringFormat(uiState.totalWorkTime))
                        }
                    }

                    // Payment details
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        ),
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                text = "Начисления",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                            )
                            HorizontalDivider()

                            SalaryTimeMoneyRow(
                                "Работа по тарифу",
                                viewModel.convertTimeToStringFormat(uiState.paymentAtTariffHours),
                                uiState.paymentAtTariffMoney,
                            )
                            SalaryTimeMoneyRow(
                                "Ночное время",
                                viewModel.convertTimeToStringFormat(uiState.paymentNightTimeHours),
                                uiState.paymentNightTimeMoney,
                            )
                            SalaryTimeMoneyRow(
                                "Резервом",
                                viewModel.convertTimeToStringFormat(uiState.paymentAtSingleLocomotiveHours),
                                uiState.paymentAtSingleLocomotiveMoney,
                            )
                            SalaryTimeMoneyRow(
                                "Пассажиром",
                                viewModel.convertTimeToStringFormat(uiState.paymentAtPassengerHours),
                                uiState.paymentAtPassengerMoney,
                            )
                            SalaryTimeMoneyRow(
                                "Праздничные",
                                viewModel.convertTimeToStringFormat(uiState.paymentHolidayHours),
                                uiState.paymentHolidayMoney,
                            )
                            SalaryTimeMoneyRow(
                                "Сверхурочные",
                                viewModel.convertTimeToStringFormat(uiState.paymentAtOvertimeHours),
                                uiState.paymentAtOvertimeMoney,
                            )
                        }
                    }

                    // Surcharges
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        ),
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                text = "Доплаты и надбавки",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                            )
                            HorizontalDivider()

                            uiState.surchargeQualificationClassMoney?.let {
                                SalaryMoneyRow("Квалификационный класс", it)
                            }
                            uiState.zonalSurchargeMoney?.let {
                                SalaryMoneyRow("Зональная надбавка", it)
                            }
                            uiState.harmfulnessSurchargeMoney?.let {
                                SalaryMoneyRow("Вредность", it)
                            }
                            uiState.onePersonOperationMoney?.let {
                                SalaryMoneyRow("Работа в одно лицо", it)
                            }
                            uiState.surchargeLongDistanceTrainsMoney?.let {
                                SalaryMoneyRow("Дальнее следование", it)
                            }
                            uiState.nordicSurchargeMoney?.let {
                                SalaryMoneyRow("Северная надбавка", it)
                            }
                            uiState.districtSurchargeMoney?.let {
                                SalaryMoneyRow("Районный коэффициент", it)
                            }
                            uiState.averagePaymentMoney?.let {
                                SalaryMoneyRow("Средний заработок", it)
                            }
                            uiState.otherSurchargeMoney?.let {
                                SalaryMoneyRow("Другие надбавки", it)
                            }
                        }
                    }

                    // Totals
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        ),
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = "Итого",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                            HorizontalDivider()
                            uiState.totalChargedMoney?.let {
                                SalaryMoneyRowBold("Начислено", it)
                            }
                            uiState.totalRetention?.let {
                                SalaryMoneyRow("Удержано", it)
                            }
                            uiState.toBeCredited?.let {
                                SalaryMoneyRowBold("К выплате", it)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onShowSettingSalary,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Настройка зарплаты")
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun SalaryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun SalaryTimeMoneyRow(label: String, time: String, money: Double?) {
    if (money == null || money == 0.0) return
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = time,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = 8.dp),
        )
        Text(
            text = formatMoney(money),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun SalaryMoneyRow(label: String, money: Double) {
    if (money == 0.0) return
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = formatMoney(money),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun SalaryMoneyRowBold(label: String, money: Double) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = formatMoney(money),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
        )
    }
}

private fun formatMoney(value: Double): String {
    val intPart = value.toLong()
    val fracPart = ((value - intPart) * 100).toLong()
    val fracStr = if (fracPart < 10) "0$fracPart" else fracPart.toString()
    return "$intPart,$fracStr \u20BD"
}
