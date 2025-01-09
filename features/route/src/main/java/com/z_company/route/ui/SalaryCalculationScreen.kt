package com.z_company.route.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.z_company.core.ui.theme.Shapes
import com.z_company.core.ui.theme.custom.AppTypography
import com.z_company.core.util.ConverterLongToTime
import com.z_company.domain.util.str
import com.z_company.domain.util.str2decimalSign
import com.z_company.route.viewmodel.SalaryCalculationUIState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalaryCalculationScreen(
    onBack: () -> Unit,
    uiState: SalaryCalculationUIState,
    onSettingsSalaryClick: () -> Unit,
    updateData: () -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(key1 = lifecycleOwner, effect = {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) {
                updateData()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose { }
    })

    val styleDataLight =
        AppTypography.getType().titleMedium.copy(fontWeight = FontWeight.Light, fontSize = 18.sp)
    val titleStyle = AppTypography.getType().headlineMedium.copy(fontWeight = FontWeight.Light)
    val styleHint = AppTypography.getType().titleMedium.copy(fontWeight = FontWeight.Normal)

    val widthColumn1 = 0.4f
    val widthColumn2 = 0.2f
    val widthColumn3 = 0.2f
    val widthColumn4 = 0.2f

    val verticalPaddingSmall = 6.dp
    val verticalPaddingLarge = 18.dp

    var infoBlockVisible by remember {
        mutableStateOf(true)
    }
    var infoSetTariffRate by remember {
        mutableStateOf(false)
    }

    LaunchedEffect(uiState.tariffRate) {
        infoSetTariffRate = uiState.tariffRate == 0.0 || uiState.tariffRate == null
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Расчетный листок",
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
                            .padding(end = 16.dp),
                        onClick = onSettingsSalaryClick
                    ) {
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
                .padding(horizontal = 16.dp),
        ) {
            item {
                AnimatedVisibility(visible = infoBlockVisible) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = MaterialTheme.colorScheme.surface,
                                shape = Shapes.medium
                            )
                            .padding(vertical = 8.dp, horizontal = 16.dp),
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            modifier = Modifier,
                            style = styleHint.copy(fontWeight = FontWeight.Light),
                            text = "Данный расчет носит информационный характер, некоторые виды выплат могут отличаться в зависимости от внутренних нормативных документов вашего депо."
                        )
                        Button(
                            shape = Shapes.medium,
                            onClick = { infoBlockVisible = false }
                        ) {
                            Text(
                                modifier = Modifier,
                                style = styleHint,
                                text = "Понятно"
                            )
                        }
                    }
                }
            }
            item {
                AnimatedVisibility(visible = infoSetTariffRate) {
                    Column(
                        modifier = Modifier
                            .padding(top = verticalPaddingSmall)
                            .fillMaxWidth()
                            .background(
                                color = MaterialTheme.colorScheme.surface,
                                shape = Shapes.medium
                            )
                            .padding(vertical = 8.dp, horizontal = 16.dp),
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            modifier = Modifier.fillMaxWidth(),
                            overflow = TextOverflow.Visible,
                            style = styleHint.copy(fontWeight = FontWeight.Light),
                            text = "Установите значение часовой ставки."
                        )
                        Button(
                            shape = Shapes.medium,
                            onClick = onSettingsSalaryClick
                        ) {
                            Text(
                                modifier = Modifier,
                                overflow = TextOverflow.Visible,
                                style = styleHint,
                                text = "Настройки"
                            )
                        }
                    }
                }
            }
            item {
                Row(
                    modifier = Modifier
                        .padding(top = verticalPaddingLarge)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        overflow = TextOverflow.Visible,
                        style = styleHint,
                        text = "Месяц"
                    )
                    Text(
                        overflow = TextOverflow.Visible,
                        style = styleHint,
                        text = uiState.month
                    )
                }
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = verticalPaddingSmall),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        overflow = TextOverflow.Visible,
                        style = styleHint,
                        text = "Всего отработано часов за месяц"
                    )
                    Text(
                        overflow = TextOverflow.Visible,
                        style = styleDataLight.copy(fontWeight = FontWeight.Medium),
                        textAlign = TextAlign.Center,
                        text = ConverterLongToTime.getTimeInHourDecimal(uiState.totalWorkTime)
                    )
                }
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = verticalPaddingSmall),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        overflow = TextOverflow.Visible,
                        style = styleHint,
                        text = "Норма часов(по закону)"
                    )
                    Text(
                        overflow = TextOverflow.Visible,
                        style = styleDataLight.copy(fontWeight = FontWeight.Medium),
                        textAlign = TextAlign.Center,
                        text = uiState.normaHours.toString()
                    )
                }
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = verticalPaddingSmall),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        overflow = TextOverflow.Visible,
                        style = styleHint,
                        text = "Тариф"
                    )
                    Text(
                        overflow = TextOverflow.Visible,
                        style = styleDataLight.copy(fontWeight = FontWeight.Medium),
                        textAlign = TextAlign.Center,
                        text = uiState.tariffRate.str()
                    )
                }
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = verticalPaddingLarge)
                ) {
                    Text(
                        modifier = Modifier.weight(widthColumn1),
                        overflow = TextOverflow.Visible,
                        textAlign = TextAlign.Start,
                        style = styleHint,
                        text = "Вид оплаты"
                    )
                    Text(
                        modifier = Modifier.weight(widthColumn2),
                        overflow = TextOverflow.Visible,
                        textAlign = TextAlign.Center,
                        style = styleHint,
                        text = "Часы"
                    )
                    Text(
                        modifier = Modifier.weight(widthColumn3),
                        overflow = TextOverflow.Visible,
                        textAlign = TextAlign.Center,
                        style = styleHint,
                        text = "Процент"
                    )
                    Text(
                        modifier = Modifier.weight(widthColumn4),
                        overflow = TextOverflow.Visible,
                        textAlign = TextAlign.End,
                        style = styleHint,
                        text = "Сумма"
                    )
                }
            }
            item {
                uiState.paymentAtTariffMoney?.let { value ->
                    if (value != 0.0) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = verticalPaddingSmall)
                        ) {
                            Text(
                                modifier = Modifier.weight(widthColumn1),
                                overflow = TextOverflow.Visible,
                                style = styleDataLight,
                                text = "ПоврОплатаПоТарифСтавкам"
                            )
                            Text(
                                modifier = Modifier.weight(widthColumn2),
                                overflow = TextOverflow.Visible,
                                style = styleDataLight,
                                textAlign = TextAlign.Center,
                                text = ConverterLongToTime.getTimeInHourDecimal(uiState.paymentAtTariffHours)
                            )
                            Box(
                                modifier = Modifier.weight(widthColumn3),
                            )
                            Text(
                                modifier = Modifier.weight(widthColumn4),
                                overflow = TextOverflow.Visible,
                                style = styleDataLight,
                                textAlign = TextAlign.End,
                                text = uiState.paymentAtTariffMoney.str2decimalSign()
                            )
                        }
                    }
                }
            }
            item {
                uiState.paymentAtSingleLocomotiveMoney?.let { value ->
                    if (value != 0.0) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = verticalPaddingSmall)
                        ) {
                            Text(
                                modifier = Modifier.weight(widthColumn1),
                                overflow = TextOverflow.Visible,
                                style = styleDataLight,
                                text = "ОплЗаРабНаОдиночСледЛоком"
                            )
                            Text(
                                modifier = Modifier.weight(widthColumn2),
                                overflow = TextOverflow.Visible,
                                style = styleDataLight,
                                textAlign = TextAlign.Center,
                                text = ConverterLongToTime.getTimeInHourDecimal(uiState.paymentAtSingleLocomotiveHours)
                            )
                            Box(
                                modifier = Modifier.weight(widthColumn3),
                            )
                            Text(
                                modifier = Modifier.weight(widthColumn4),
                                overflow = TextOverflow.Visible,
                                style = styleDataLight,
                                textAlign = TextAlign.End,
                                text = uiState.paymentAtSingleLocomotiveMoney.str2decimalSign()
                            )
                        }
                    }
                }
            }
            item {
                uiState.paymentAtPassengerMoney?.let { value ->
                    if (value != 0.0) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = verticalPaddingSmall)
                        ) {
                            Text(
                                modifier = Modifier.weight(widthColumn1),
                                overflow = TextOverflow.Visible,
                                style = styleDataLight,
                                text = "ОплЗаСледПасс"
                            )
                            Text(
                                modifier = Modifier.weight(widthColumn2),
                                overflow = TextOverflow.Visible,
                                style = styleDataLight,
                                textAlign = TextAlign.Center,
                                text = ConverterLongToTime.getTimeInHourDecimal(uiState.paymentAtPassengerHours)
                            )
                            Box(
                                modifier = Modifier.weight(widthColumn3),
                            )
                            Text(
                                modifier = Modifier.weight(widthColumn4),
                                overflow = TextOverflow.Visible,
                                style = styleDataLight,
                                textAlign = TextAlign.End,
                                text = uiState.paymentAtPassengerMoney.str2decimalSign()
                            )
                        }
                    }
                }
            }
            item {
                uiState.paymentAtOvertimeMoney?.let { value ->
                    if (value != 0.0) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = verticalPaddingSmall)
                        ) {
                            Text(
                                modifier = Modifier.weight(widthColumn1),
                                overflow = TextOverflow.Visible,
                                style = styleDataLight,
                                text = "ОплРабСверУрочВр"
                            )
                            Text(
                                modifier = Modifier.weight(widthColumn2),
                                overflow = TextOverflow.Visible,
                                style = styleDataLight,
                                textAlign = TextAlign.Center,
                                text = ConverterLongToTime.getTimeInHourDecimal(uiState.paymentAtOvertimeHours)
                            )
                            Box(
                                modifier = Modifier.weight(widthColumn3),
                            )
                            Text(
                                modifier = Modifier.weight(widthColumn4),
                                overflow = TextOverflow.Visible,
                                style = styleDataLight,
                                textAlign = TextAlign.End,
                                text = uiState.paymentAtOvertimeMoney.str2decimalSign()
                            )
                        }
                    }
                }
            }
            item {
                uiState.surchargeAtOvertime05Money?.let { value ->
                    if (value != 0.0) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = verticalPaddingSmall)
                        ) {
                            Text(
                                modifier = Modifier.weight(widthColumn1),
                                overflow = TextOverflow.Visible,
                                style = styleDataLight,
                                text = "ДоплСверхУрочнВр0,5размер"
                            )
                            Text(
                                modifier = Modifier.weight(widthColumn2),
                                overflow = TextOverflow.Visible,
                                style = styleDataLight,
                                textAlign = TextAlign.Center,
                                text = ConverterLongToTime.getTimeInHourDecimal(uiState.surchargeAtOvertime05Hours)
                            )
                            Box(
                                modifier = Modifier.weight(widthColumn3),
                            )
                            Text(
                                modifier = Modifier.weight(widthColumn4),
                                overflow = TextOverflow.Visible,
                                style = styleDataLight,
                                textAlign = TextAlign.End,
                                text = uiState.surchargeAtOvertime05Money.str2decimalSign()
                            )
                        }
                    }
                }
            }
            item {
                uiState.surchargeAtOvertimeMoney?.let { value ->
                    if (value != 0.0) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = verticalPaddingSmall)
                        ) {
                            Text(
                                modifier = Modifier.weight(widthColumn1),
                                overflow = TextOverflow.Visible,
                                style = styleDataLight,
                                text = "ДоплВыхПраздСверхНормВрем"
                            )
                            Text(
                                modifier = Modifier.weight(widthColumn2),
                                overflow = TextOverflow.Visible,
                                style = styleDataLight,
                                textAlign = TextAlign.Center,
                                text = ConverterLongToTime.getTimeInHourDecimal(uiState.surchargeAtOvertimeHours)
                            )
                            Box(
                                modifier = Modifier.weight(widthColumn3),
                            )
                            Text(
                                modifier = Modifier.weight(widthColumn4),
                                overflow = TextOverflow.Visible,
                                style = styleDataLight,
                                textAlign = TextAlign.End,
                                text = uiState.surchargeAtOvertimeMoney.str2decimalSign()
                            )
                        }
                    }
                }
            }
            item {
                uiState.paymentHolidayMoney?.let { value ->
                    if (value != 0.0) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = verticalPaddingSmall)
                        ) {
                            Text(
                                modifier = Modifier.weight(widthColumn1),
                                overflow = TextOverflow.Visible,
                                style = styleDataLight,
                                text = "ОплВыходДнСверхНорВрВед"
                            )
                            Text(
                                modifier = Modifier.weight(widthColumn2),
                                overflow = TextOverflow.Visible,
                                style = styleDataLight,
                                textAlign = TextAlign.Center,
                                text = ConverterLongToTime.getTimeInHourDecimal(uiState.paymentHolidayHours)
                            )
                            Box(
                                modifier = Modifier.weight(widthColumn3),
                            )
                            Text(
                                modifier = Modifier.weight(widthColumn4),
                                overflow = TextOverflow.Visible,
                                style = styleDataLight,
                                textAlign = TextAlign.End,
                                text = uiState.paymentHolidayMoney.str2decimalSign()
                            )
                        }
                    }
                }
            }
            item {
                uiState.surchargeHolidayMoney?.let { value ->
                    if (value != 0.0) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = verticalPaddingSmall)
                        ) {
                            Text(
                                modifier = Modifier.weight(widthColumn1),
                                overflow = TextOverflow.Visible,
                                style = styleDataLight,
                                text = "ДопЗаРабВыходПразДниНорВр"
                            )
                            Text(
                                modifier = Modifier.weight(widthColumn2),
                                overflow = TextOverflow.Visible,
                                style = styleDataLight,
                                textAlign = TextAlign.Center,
                                text = ConverterLongToTime.getTimeInHourDecimal(uiState.surchargeHolidayHours)
                            )
                            Box(
                                modifier = Modifier.weight(widthColumn3),
                            )
                            Text(
                                modifier = Modifier.weight(widthColumn4),
                                overflow = TextOverflow.Visible,
                                style = styleDataLight,
                                textAlign = TextAlign.End,
                                text = uiState.surchargeHolidayMoney.str2decimalSign()
                            )
                        }
                    }
                }
            }
            item {
                uiState.zonalSurchargeMoney?.let { value ->
                    if (value != 0.0) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = verticalPaddingSmall)
                        ) {
                            Text(
                                modifier = Modifier.weight(widthColumn1),
                                overflow = TextOverflow.Visible,
                                style = styleDataLight,
                                text = "ЗоналНадб%ОтОтрабВремФакт"
                            )
                            Box(
                                modifier = Modifier.weight(widthColumn2),
                            )
                            Text(
                                modifier = Modifier.weight(widthColumn3),
                                overflow = TextOverflow.Visible,
                                style = styleDataLight,
                                textAlign = TextAlign.Center,
                                text = uiState.zonalSurchargePercent.str()
                            )
                            Text(
                                modifier = Modifier.weight(widthColumn4),
                                overflow = TextOverflow.Visible,
                                style = styleDataLight,
                                textAlign = TextAlign.End,
                                text = uiState.zonalSurchargeMoney.str2decimalSign()
                            )
                        }
                    }
                }
            }
            item {
                uiState.paymentNightTimeMoney?.let { value ->
                    if (value != 0.0) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = verticalPaddingSmall)
                        ) {
                            Text(
                                modifier = Modifier.weight(widthColumn1),
                                overflow = TextOverflow.Visible,
                                style = styleDataLight,
                                text = "ДоплатЗаРаботуНочноеВремя"
                            )
                            Text(
                                modifier = Modifier.weight(widthColumn2),
                                overflow = TextOverflow.Visible,
                                style = styleDataLight,
                                textAlign = TextAlign.Center,
                                text = ConverterLongToTime.getTimeInHourDecimal(uiState.paymentNightTimeHours)
                            )
                            Text(
                                modifier = Modifier.weight(widthColumn3),
                                overflow = TextOverflow.Visible,
                                style = styleDataLight,
                                textAlign = TextAlign.Center,
                                text = uiState.paymentNightTimePercent.str()
                            )
                            Text(
                                modifier = Modifier.weight(widthColumn4),
                                overflow = TextOverflow.Visible,
                                style = styleDataLight,
                                textAlign = TextAlign.End,
                                text = uiState.paymentNightTimeMoney.str2decimalSign()
                            )
                        }
                    }
                }
            }
            item {
                uiState.surchargeQualificationClassMoney?.let { value ->
                    if (value != 0.0) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = verticalPaddingSmall)
                        ) {
                            Text(
                                modifier = Modifier.weight(widthColumn1),
                                overflow = TextOverflow.Visible,
                                style = styleDataLight,
                                text = "НадЗаКласКвал"
                            )
                            Box(
                                modifier = Modifier.weight(widthColumn2),
                            )
                            Text(
                                modifier = Modifier.weight(widthColumn3),
                                overflow = TextOverflow.Visible,
                                style = styleDataLight,
                                textAlign = TextAlign.Center,
                                text = uiState.surchargeQualificationClassPercent.str()
                            )
                            Text(
                                modifier = Modifier.weight(widthColumn4),
                                overflow = TextOverflow.Visible,
                                style = styleDataLight,
                                textAlign = TextAlign.End,
                                text = uiState.surchargeQualificationClassMoney.str2decimalSign()
                            )
                        }
                    }
                }
            }

            item {
                uiState.onePersonOperationMoney?.let { value ->
                    if (value != 0.0) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = verticalPaddingSmall)
                        ) {
                            Text(
                                modifier = Modifier.weight(widthColumn1),
                                overflow = TextOverflow.Visible,
                                style = styleDataLight,
                                text = "НадРабОдноЛицо"
                            )
                            Box(
                                modifier = Modifier.weight(widthColumn2),
                            )
                            Text(
                                modifier = Modifier.weight(widthColumn3),
                                overflow = TextOverflow.Visible,
                                style = styleDataLight,
                                textAlign = TextAlign.Center,
                                text = uiState.onePersonOperationPercent.str()
                            )
                            Text(
                                modifier = Modifier.weight(widthColumn4),
                                overflow = TextOverflow.Visible,
                                style = styleDataLight,
                                textAlign = TextAlign.End,
                                text = uiState.onePersonOperationMoney.str2decimalSign()
                            )
                        }
                    }
                }
            }

            item {
                uiState.harmfulnessSurchargeMoney?.let { value ->
                    if (value != 0.0) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = verticalPaddingSmall)
                        ) {
                            Text(
                                modifier = Modifier.weight(widthColumn1),
                                overflow = TextOverflow.Visible,
                                style = styleDataLight,
                                text = "НадВреднНаПроизв"
                            )
                            Box(
                                modifier = Modifier.weight(widthColumn2),
                            )
                            Text(
                                modifier = Modifier.weight(widthColumn3),
                                overflow = TextOverflow.Visible,
                                style = styleDataLight,
                                textAlign = TextAlign.Center,
                                text = uiState.harmfulnessSurchargePercent.str()
                            )
                            Text(
                                modifier = Modifier.weight(widthColumn4),
                                overflow = TextOverflow.Visible,
                                style = styleDataLight,
                                textAlign = TextAlign.End,
                                text = uiState.harmfulnessSurchargeMoney.str2decimalSign()
                            )
                        }
                    }
                }
            }

            item {
                uiState.surchargeLongDistanceTrainsMoney?.let { value ->
                    if (value != 0.0) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = verticalPaddingSmall)
                        ) {
                            Text(
                                modifier = Modifier.weight(widthColumn1),
                                overflow = TextOverflow.Visible,
                                style = styleDataLight,
                                text = "ДоплРабЛокомБрДлинПоез"
                            )
                            Text(
                                modifier = Modifier.weight(widthColumn2),
                                overflow = TextOverflow.Visible,
                                style = styleDataLight,
                                textAlign = TextAlign.Center,
                                text = ConverterLongToTime.getTimeInHourDecimal(uiState.surchargeLongDistanceTrainsHours)
                            )
                            Text(
                                modifier = Modifier.weight(widthColumn3),
                                overflow = TextOverflow.Visible,
                                style = styleDataLight,
                                textAlign = TextAlign.Center,
                                text = uiState.surchargeLongDistanceTrainsPercent.str()
                            )
                            Text(
                                modifier = Modifier.weight(widthColumn4),
                                overflow = TextOverflow.Visible,
                                style = styleDataLight,
                                textAlign = TextAlign.End,
                                text = uiState.surchargeLongDistanceTrainsMoney.str2decimalSign()
                            )
                        }
                    }
                }
            }

            itemsIndexed(
                items = uiState.surchargeHeavyTransMoney,
            ) {index, item ->
                item?.let { value ->
                    if (value != 0.0) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = verticalPaddingSmall)
                        ) {
                            Text(
                                modifier = Modifier.weight(widthColumn1),
                                overflow = TextOverflow.Visible,
                                style = styleDataLight,
                                text = "ДоплРабЛокомБрТяжПоез"
                            )
                            Text(
                                modifier = Modifier.weight(widthColumn2),
                                overflow = TextOverflow.Visible,
                                style = styleDataLight,
                                textAlign = TextAlign.Center,
                                text = ConverterLongToTime.getTimeInHourDecimal(uiState.surchargeHeavyTransHour[index])
                            )
                            Text(
                                modifier = Modifier.weight(widthColumn3),
                                overflow = TextOverflow.Visible,
                                style = styleDataLight,
                                textAlign = TextAlign.Center,
                                text = uiState.surchargeHeavyTransPercent[index] ?: ""
                            )
                            Text(
                                modifier = Modifier.weight(widthColumn4),
                                overflow = TextOverflow.Visible,
                                style = styleDataLight,
                                textAlign = TextAlign.End,
                                text = uiState.surchargeHeavyTransMoney[index].str2decimalSign()
                            )
                        }
                    }
                }
            }

            itemsIndexed(
                items = uiState.surchargeExtendedServicePhaseMoney,
            ) { index, item ->
                item?.let { value ->
                    if (value != 0.0) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = verticalPaddingSmall)
                        ) {
                            Text(
                                modifier = Modifier.weight(widthColumn1),
                                overflow = TextOverflow.Visible,
                                style = styleDataLight,
                                text = "ДоплРабЛокомБрУдлинУчОбсл"
                            )
                            Text(
                                modifier = Modifier.weight(widthColumn2),
                                overflow = TextOverflow.Visible,
                                style = styleDataLight,
                                textAlign = TextAlign.Center,
                                text = ConverterLongToTime.getTimeInHourDecimal(uiState.surchargeExtendedServicePhaseHour[index])
                            )
                            Text(
                                modifier = Modifier.weight(widthColumn3),
                                overflow = TextOverflow.Visible,
                                style = styleDataLight,
                                textAlign = TextAlign.Center,
                                text = uiState.surchargeExtendedServicePhasePercent[index] ?: ""
                            )
                            Text(
                                modifier = Modifier.weight(widthColumn4),
                                overflow = TextOverflow.Visible,
                                style = styleDataLight,
                                textAlign = TextAlign.End,
                                text = uiState.surchargeExtendedServicePhaseMoney[index].str2decimalSign()
                            )
                        }
                    }
                }
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = verticalPaddingSmall),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        overflow = TextOverflow.Visible,
                        style = styleHint,
                        text = "Всего начислено"
                    )
                    Text(
                        overflow = TextOverflow.Visible,
                        style = styleDataLight,
                        text = uiState.totalChargedMoney.str2decimalSign()
                    )
                }
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = verticalPaddingLarge),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        overflow = TextOverflow.Visible,
                        style = styleHint,
                        text = "Вид удержания"
                    )
                    Text(
                        overflow = TextOverflow.Visible,
                        style = styleHint,
                        text = "Сумма"
                    )
                }
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = verticalPaddingSmall),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        overflow = TextOverflow.Visible,
                        style = styleDataLight,
                        text = "НалогНаДохФизЛицаУдер%"
                    )
                    Text(
                        overflow = TextOverflow.Visible,
                        style = styleDataLight,
                        text = uiState.retentionNdfl.str2decimalSign()
                    )
                }
            }
            item {
                uiState.unionistsRetention?.let { value ->
                    if (value != 0.0) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = verticalPaddingSmall),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                overflow = TextOverflow.Visible,
                                style = styleDataLight,
                                text = "ПрофсоюзПервыйКредитор"
                            )
                            Text(
                                overflow = TextOverflow.Visible,
                                style = styleDataLight,
                                text = uiState.unionistsRetention.str2decimalSign()
                            )
                        }
                    }
                }
            }

            item {
                uiState.otherRetention?.let { value ->
                    if (value != 0.0) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = verticalPaddingSmall),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                overflow = TextOverflow.Visible,
                                style = styleDataLight,
                                text = "Прочие удержания"
                            )
                            Text(
                                overflow = TextOverflow.Visible,
                                style = styleDataLight,
                                text = uiState.otherRetention.str2decimalSign()
                            )
                        }
                    }
                }
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = verticalPaddingSmall),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        overflow = TextOverflow.Visible,
                        style = styleHint,
                        text = "Всего удержано"
                    )
                    Text(
                        overflow = TextOverflow.Visible,
                        style = styleDataLight,
                        textAlign = TextAlign.Center,
                        text = uiState.totalRetention.str2decimalSign()
                    )
                }
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = verticalPaddingLarge),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        overflow = TextOverflow.Visible,
                        style = styleHint,
                        text = "К выдаче"
                    )
                    Text(
                        overflow = TextOverflow.Visible,
                        style = styleDataLight.copy(fontWeight = FontWeight.Medium),
                        textAlign = TextAlign.Center,
                        text = uiState.toBeCredited.str2decimalSign()
                    )
                }
            }
        }
    }
}