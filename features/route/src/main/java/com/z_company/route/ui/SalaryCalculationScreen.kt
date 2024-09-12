package com.z_company.route.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.z_company.core.ui.theme.Shapes
import com.z_company.core.ui.theme.custom.AppTypography
import com.z_company.core.util.ConverterLongToTime
import com.z_company.domain.util.str
import com.z_company.route.viewmodel.SalaryCalculationUIState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalaryCalculationScreen(
    onBack: () -> Unit,
    uiState: SalaryCalculationUIState,
    onSettingsSalaryClick: () -> Unit
) {
    val styleDataLight = AppTypography.getType().titleLarge.copy(fontWeight = FontWeight.Light)
    val titleStyle = AppTypography.getType().headlineMedium.copy(fontWeight = FontWeight.Light)
    val styleDataMedium = AppTypography.getType().titleMedium.copy(fontWeight = FontWeight.Normal)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Заработная плата",
                        overflow = TextOverflow.Visible,
                        maxLines = 2,
                        style = titleStyle
                    )
                }, navigationIcon = {
                    IconButton(onClick = {
                        onBack()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                actions = {
                    IconButton(
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .background(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = Shapes.medium
                            ),
                        onClick = { onSettingsSalaryClick() }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        },
    ) {
        LazyColumn(
            modifier = Modifier
                .padding(it)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        modifier = Modifier.weight(0.5f),
                        overflow = TextOverflow.Visible,
                        textAlign = TextAlign.Center,
                        style = styleDataMedium,
                        text = "Вид оплаты"
                    )
                    Text(
                        modifier = Modifier.weight(0.25f),
                        overflow = TextOverflow.Visible,
                        textAlign = TextAlign.Center,
                        style = styleDataMedium,
                        text = "Часы"
                    )
                    Text(
                        modifier = Modifier.weight(0.25f),
                        overflow = TextOverflow.Visible,
                        textAlign = TextAlign.Center,
                        style = styleDataMedium,
                        text = "Сумма"
                    )
                }
            }
            item {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        modifier = Modifier.weight(0.5f),
                        overflow = TextOverflow.Visible,
                        style = styleDataLight,
                        text = "ПоврОплатаПоТарифСтавкам"
                    )
                    Text(
                        modifier = Modifier.weight(0.25f),
                        overflow = TextOverflow.Visible,
                        style = styleDataLight,
                        textAlign = TextAlign.Center,
                        text = ConverterLongToTime.getTimeInHourDecimal(uiState.paymentAtTariffHours)
                    )
                    Text(
                        modifier = Modifier.weight(0.25f),
                        overflow = TextOverflow.Visible,
                        style = styleDataLight,
                        textAlign = TextAlign.Center,
                        text = uiState.paymentAtTariffMoney.str()
                    )
                }
            }
            item {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        modifier = Modifier.weight(0.5f),
                        overflow = TextOverflow.Visible,
                        style = styleDataLight,
                        text = "ОплЗаРабНаОдиночСледЛоком"
                    )
                    Text(
                        modifier = Modifier.weight(0.25f),
                        overflow = TextOverflow.Visible,
                        style = styleDataLight,
                        textAlign = TextAlign.Center,
                        text = ConverterLongToTime.getTimeInHourDecimal(uiState.paymentAtSingleLocomotiveHours)
                    )
                    Text(
                        modifier = Modifier.weight(0.25f),
                        overflow = TextOverflow.Visible,
                        style = styleDataLight,
                        textAlign = TextAlign.Center,
                        text = uiState.paymentAtSingleLocomotiveMoney.str()
                    )
                }
            }
            item {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        modifier = Modifier.weight(0.5f),
                        overflow = TextOverflow.Visible,
                        style = styleDataLight,
                        text = "ОплЗаСледПасс"
                    )
                    Text(
                        modifier = Modifier.weight(0.25f),
                        overflow = TextOverflow.Visible,
                        style = styleDataLight,
                        textAlign = TextAlign.Center,
                        text = ConverterLongToTime.getTimeInHourDecimal(uiState.paymentAtPassengerHours)
                    )
                    Text(
                        modifier = Modifier.weight(0.25f),
                        overflow = TextOverflow.Visible,
                        style = styleDataLight,
                        textAlign = TextAlign.Center,
                        text = uiState.paymentAtPassengerMoney.str()
                    )
                }
            }
        }
    }
}