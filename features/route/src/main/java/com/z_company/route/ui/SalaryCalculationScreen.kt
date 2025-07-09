package com.z_company.route.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.z_company.core.ResultState
import com.z_company.core.ui.component.AutoSizeText
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
    val maxTextSize = 18.sp

    var infoBlockVisible by remember {
        mutableStateOf(true)
    }
    var infoSetTariffRate by remember {
        mutableStateOf(false)
    }

    LaunchedEffect(uiState.tariffRate) {
        infoSetTariffRate = uiState.tariffRate == "0 ₽" || uiState.tariffRate == null
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
        if (uiState.screenState is ResultState.Loading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    val text = uiState.screenState.message
                    Text(text)
                }
            }
        } else {
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
                        AutoSizeText(
                            modifier = Modifier,
                            maxTextSize = maxTextSize,
                            style = styleHint.copy(fontWeight = FontWeight.Light),
                            text = "Данный расчет носит информационный характер, некоторые виды выплат могут отличаться в зависимости от внутренних нормативных документов вашего депо."
                        )
                        Button(
                            shape = Shapes.medium,
                            onClick = { infoBlockVisible = false }
                        ) {
                            AutoSizeText(
                                modifier = Modifier,
                                style = styleHint,
                                maxTextSize = maxTextSize,
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
                        AutoSizeText(
                            modifier = Modifier.fillMaxWidth(),
                            overflow = TextOverflow.Visible,
                            maxTextSize = maxTextSize,
                            style = styleHint.copy(fontWeight = FontWeight.Light),
                            text = "Установите значение часовой ставки."
                        )
                        Button(
                            shape = Shapes.medium,
                            onClick = onSettingsSalaryClick
                        ) {
                            AutoSizeText(
                                modifier = Modifier,
                                overflow = TextOverflow.Visible,
                                style = styleHint,
                                maxTextSize = maxTextSize,
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
                    AutoSizeText(
                        overflow = TextOverflow.Visible,
                        style = styleHint,
                        maxTextSize = maxTextSize,
                        text = "Месяц"
                    )
                    AutoSizeText(
                        maxTextSize = maxTextSize,
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
                    AutoSizeText(
                        maxTextSize = maxTextSize,
                        overflow = TextOverflow.Visible,
                        style = styleHint,
                        text = "Всего отработано часов за месяц"
                    )
                    AutoSizeText(
                        maxTextSize = maxTextSize,
                        overflow = TextOverflow.Visible,
                        style = styleDataLight.copy(fontWeight = FontWeight.Medium),
                        alignment = Alignment.Center,
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
                    AutoSizeText(
                        maxTextSize = maxTextSize,
                        overflow = TextOverflow.Visible,
                        style = styleHint,
                        text = "Норма часов(по закону)"
                    )
                    AutoSizeText(
                        maxTextSize = maxTextSize,
                        overflow = TextOverflow.Visible,
                        style = styleDataLight.copy(fontWeight = FontWeight.Medium),
                        alignment = Alignment.Center,
                        text = uiState.normaHours.str()
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
                    AutoSizeText(
                        maxTextSize = maxTextSize,
                        overflow = TextOverflow.Visible,
                        style = styleHint,
                        text = "Тариф"
                    )
                    AutoSizeText(
                        maxTextSize = maxTextSize,
                        overflow = TextOverflow.Visible,
                        style = styleDataLight.copy(fontWeight = FontWeight.Medium),
                        alignment = Alignment.Center,
                        text = uiState.tariffRate ?: ""
                    )
                }
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = verticalPaddingLarge)
                ) {
                    AutoSizeText(
                        maxTextSize = maxTextSize,
                        modifier = Modifier.weight(widthColumn1),
                        overflow = TextOverflow.Visible,
                        alignment = Alignment.CenterStart,
                        style = styleHint,
                        text = "Вид оплаты"
                    )
                    AutoSizeText(
                        maxTextSize = maxTextSize,
                        modifier = Modifier.weight(widthColumn2),
                        overflow = TextOverflow.Visible,
                        alignment = Alignment.Center,
                        style = styleHint,
                        text = "Часы"
                    )
                    AutoSizeText(
                        maxTextSize = maxTextSize,
                        modifier = Modifier.weight(widthColumn3),
                        overflow = TextOverflow.Visible,
                        alignment = Alignment.Center,
                        style = styleHint,
                        text = "%"
                    )
                    AutoSizeText(
                        maxTextSize = maxTextSize,
                        modifier = Modifier.weight(widthColumn4),
                        overflow = TextOverflow.Visible,
                        alignment = Alignment.CenterEnd,
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
                            AutoSizeText(
                                modifier = Modifier.weight(widthColumn1),
                                overflow = TextOverflow.Visible,
                                style = styleDataLight,
                                maxTextSize = maxTextSize,
                                text = "ПоврОплатаПоТарифСтавкам"
                            )
                            AutoSizeText(
                                modifier = Modifier.weight(widthColumn2),
                                overflow = TextOverflow.Visible,
                                style = styleDataLight,
                                maxTextSize = maxTextSize,
                                alignment = Alignment.Center,
                                text = ConverterLongToTime.getTimeInHourDecimal(uiState.paymentAtTariffHours)
                            )
                            Box(
                                modifier = Modifier.weight(widthColumn3),
                            )
                            AutoSizeText(
                                maxTextSize = maxTextSize,
                                modifier = Modifier.weight(widthColumn4),
                                overflow = TextOverflow.Visible,
                                style = styleDataLight,
                                alignment = Alignment.CenterEnd,
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
                            AutoSizeText(
                                maxTextSize = maxTextSize,
                                modifier = Modifier.weight(widthColumn1),
                                overflow = TextOverflow.Visible,
                                style = styleDataLight,
                                text = "ОплЗаРабНаОдиночСледЛоком"
                            )
                            AutoSizeText(
                                maxTextSize = maxTextSize,
                                modifier = Modifier.weight(widthColumn2),
                                overflow = TextOverflow.Visible,
                                style = styleDataLight,
                                alignment = Alignment.Center,
                                text = ConverterLongToTime.getTimeInHourDecimal(uiState.paymentAtSingleLocomotiveHours)
                            )
                            Box(
                                modifier = Modifier.weight(widthColumn3),
                            )
                            AutoSizeText(
                                maxTextSize = maxTextSize,
                                modifier = Modifier.weight(widthColumn4),
                                overflow = TextOverflow.Visible,
                                style = styleDataLight,
                                alignment = Alignment.CenterEnd,
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
                            AutoSizeText(
                                maxTextSize = maxTextSize,
                                modifier = Modifier.weight(widthColumn1),
                                overflow = TextOverflow.Visible,
                                style = styleDataLight,
                                text = "ОплЗаСледПасс"
                            )
                            AutoSizeText(
                                maxTextSize = maxTextSize,
                                modifier = Modifier.weight(widthColumn2),
                                overflow = TextOverflow.Visible,
                                style = styleDataLight,
                                alignment = Alignment.Center,
                                text = ConverterLongToTime.getTimeInHourDecimal(uiState.paymentAtPassengerHours)
                            )
                            Box(
                                modifier = Modifier.weight(widthColumn3),
                            )
                            AutoSizeText(
                                maxTextSize = maxTextSize,
                                modifier = Modifier.weight(widthColumn4),
                                overflow = TextOverflow.Visible,
                                style = styleDataLight,
                                alignment = Alignment.CenterEnd,
                                text = uiState.paymentAtPassengerMoney.str2decimalSign()
                            )
                        }
                    }
                }
            }
            item {
                uiState.paymentAtOvertimeMoney?.let { value ->
                    if (value != 0.0 && !value.isNaN()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = verticalPaddingSmall)
                        ) {
                            AutoSizeText(
                                maxTextSize = maxTextSize,
                                modifier = Modifier.weight(widthColumn1),
                                overflow = TextOverflow.Visible,
                                style = styleDataLight,
                                text = "ОплРабСверУрочВр"
                            )
                            AutoSizeText(
                                maxTextSize = maxTextSize,
                                modifier = Modifier.weight(widthColumn2),
                                overflow = TextOverflow.Visible,
                                style = styleDataLight,
                                alignment = Alignment.Center,
                                text = ConverterLongToTime.getTimeInHourDecimal(uiState.paymentAtOvertimeHours)
                            )
                            Box(
                                modifier = Modifier.weight(widthColumn3),
                            )
                            AutoSizeText(
                                maxTextSize = maxTextSize,
                                modifier = Modifier.weight(widthColumn4),
                                overflow = TextOverflow.Visible,
                                style = styleDataLight,
                                alignment = Alignment.CenterEnd,
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
                            AutoSizeText(
                                maxTextSize = maxTextSize,
                                modifier = Modifier.weight(widthColumn1),
                                overflow = TextOverflow.Visible,
                                style = styleDataLight,
                                text = "ДоплСверхУрочнВр0,5размер"
                            )
                            AutoSizeText(
                                maxTextSize = maxTextSize,
                                modifier = Modifier.weight(widthColumn2),
                                overflow = TextOverflow.Visible,
                                style = styleDataLight,
                                alignment = Alignment.Center,
                                text = ConverterLongToTime.getTimeInHourDecimal(uiState.surchargeAtOvertime05Hours)
                            )
                            Box(
                                modifier = Modifier.weight(widthColumn3),
                            )
                            AutoSizeText(
                                maxTextSize = maxTextSize,
                                modifier = Modifier.weight(widthColumn4),
                                overflow = TextOverflow.Visible,
                                style = styleDataLight,
                                alignment = Alignment.CenterEnd,
                                text = uiState.surchargeAtOvertime05Money.str2decimalSign()
                            )
                        }
                    }
                }
            }
            item {
                uiState.surchargeAtOvertimeMoney?.let { value ->
                    if (value != 0.0 && !value.isNaN()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = verticalPaddingSmall)
                        ) {
                            AutoSizeText(
                                maxTextSize = maxTextSize,
                                modifier = Modifier.weight(widthColumn1),
                                overflow = TextOverflow.Visible,
                                style = styleDataLight,
                                text = "ДоплВыхПраздСверхНормВрем"
                            )
                            AutoSizeText(
                                maxTextSize = maxTextSize,
                                modifier = Modifier.weight(widthColumn2),
                                overflow = TextOverflow.Visible,
                                style = styleDataLight,
                                alignment = Alignment.Center,
                                text = ConverterLongToTime.getTimeInHourDecimal(uiState.surchargeAtOvertimeHours)
                            )
                            Box(
                                modifier = Modifier.weight(widthColumn3),
                            )
                            AutoSizeText(
                                maxTextSize = maxTextSize,
                                modifier = Modifier.weight(widthColumn4),
                                overflow = TextOverflow.Visible,
                                style = styleDataLight,
                                alignment = Alignment.CenterEnd,
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
                            AutoSizeText(
                                maxTextSize = maxTextSize,
                                modifier = Modifier.weight(widthColumn1),
                                overflow = TextOverflow.Visible,
                                style = styleDataLight,
                                text = "ОплВыходДнСверхНорВрВед"
                            )
                            AutoSizeText(
                                maxTextSize = maxTextSize,
                                modifier = Modifier.weight(widthColumn2),
                                overflow = TextOverflow.Visible,
                                style = styleDataLight,
                                alignment = Alignment.Center,
                                text = ConverterLongToTime.getTimeInHourDecimal(uiState.paymentHolidayHours)
                            )
                            Box(
                                modifier = Modifier.weight(widthColumn3),
                            )
                            AutoSizeText(
                                maxTextSize = maxTextSize,
                                modifier = Modifier.weight(widthColumn4),
                                overflow = TextOverflow.Visible,
                                style = styleDataLight,
                                alignment = Alignment.CenterEnd,
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
                            AutoSizeText(
                                maxTextSize = maxTextSize,
                                modifier = Modifier.weight(widthColumn1),
                                overflow = TextOverflow.Visible,
                                style = styleDataLight,
                                text = "ДопЗаРабВыходПразДниНорВр"
                            )
                            AutoSizeText(
                                maxTextSize = maxTextSize,
                                modifier = Modifier.weight(widthColumn2),
                                overflow = TextOverflow.Visible,
                                style = styleDataLight,
                                alignment = Alignment.Center,
                                text = ConverterLongToTime.getTimeInHourDecimal(uiState.surchargeHolidayHours)
                            )
                            Box(
                                modifier = Modifier.weight(widthColumn3),
                            )
                            AutoSizeText(
                                maxTextSize = maxTextSize,
                                modifier = Modifier.weight(widthColumn4),
                                overflow = TextOverflow.Visible,
                                style = styleDataLight,
                                alignment = Alignment.CenterEnd,
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
                            AutoSizeText(
                                maxTextSize = maxTextSize,
                                modifier = Modifier.weight(widthColumn1),
                                overflow = TextOverflow.Visible,
                                style = styleDataLight,
                                text = "ЗоналНадб%ОтОтрабВремФакт"
                            )
                            Box(
                                modifier = Modifier.weight(widthColumn2),
                            )
                            AutoSizeText(
                                maxTextSize = maxTextSize,
                                modifier = Modifier.weight(widthColumn3),
                                overflow = TextOverflow.Visible,
                                style = styleDataLight,
                                alignment = Alignment.Center,
                                text = uiState.zonalSurchargePercent.str()
                            )
                            AutoSizeText(
                                maxTextSize = maxTextSize,
                                modifier = Modifier.weight(widthColumn4),
                                overflow = TextOverflow.Visible,
                                style = styleDataLight,
                                alignment = Alignment.CenterEnd,
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
                            AutoSizeText(
                                maxTextSize = maxTextSize,
                                modifier = Modifier.weight(widthColumn1),
                                overflow = TextOverflow.Visible,
                                style = styleDataLight,
                                text = "ДоплатЗаРаботуНочноеВремя"
                            )
                            AutoSizeText(
                                maxTextSize = maxTextSize,
                                modifier = Modifier.weight(widthColumn2),
                                overflow = TextOverflow.Visible,
                                style = styleDataLight,
                                alignment = Alignment.Center,
                                text = ConverterLongToTime.getTimeInHourDecimal(uiState.paymentNightTimeHours)
                            )
                            AutoSizeText(
                                maxTextSize = maxTextSize,
                                modifier = Modifier.weight(widthColumn3),
                                overflow = TextOverflow.Visible,
                                style = styleDataLight,
                                alignment = Alignment.Center,
                                text = uiState.paymentNightTimePercent.str()
                            )
                            AutoSizeText(
                                maxTextSize = maxTextSize,
                                modifier = Modifier.weight(widthColumn4),
                                overflow = TextOverflow.Visible,
                                style = styleDataLight,
                                alignment = Alignment.CenterEnd,
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
                            AutoSizeText(
                                maxTextSize = maxTextSize,
                                modifier = Modifier.weight(widthColumn1),
                                overflow = TextOverflow.Visible,
                                style = styleDataLight,
                                text = "НадЗаКласКвал"
                            )
                            Box(
                                modifier = Modifier.weight(widthColumn2),
                            )
                            AutoSizeText(
                                maxTextSize = maxTextSize,
                                modifier = Modifier.weight(widthColumn3),
                                overflow = TextOverflow.Visible,
                                style = styleDataLight,
                                alignment = Alignment.Center,
                                text = uiState.surchargeQualificationClassPercent.str()
                            )
                            AutoSizeText(
                                maxTextSize = maxTextSize,
                                modifier = Modifier.weight(widthColumn4),
                                overflow = TextOverflow.Visible,
                                style = styleDataLight,
                                alignment = Alignment.CenterEnd,
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
                            AutoSizeText(
                                maxTextSize = maxTextSize,
                                modifier = Modifier.weight(widthColumn1),
                                overflow = TextOverflow.Visible,
                                style = styleDataLight,
                                text = "НадРабОдноЛицо"
                            )
                            Box(
                                modifier = Modifier.weight(widthColumn2),
                            )
                            AutoSizeText(
                                maxTextSize = maxTextSize,
                                modifier = Modifier.weight(widthColumn3),
                                overflow = TextOverflow.Visible,
                                style = styleDataLight,
                                alignment = Alignment.Center,
                                text = uiState.onePersonOperationPercent.str()
                            )
                            AutoSizeText(
                                maxTextSize = maxTextSize,
                                modifier = Modifier.weight(widthColumn4),
                                overflow = TextOverflow.Visible,
                                style = styleDataLight,
                                alignment = Alignment.CenterEnd,
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
                            AutoSizeText(
                                maxTextSize = maxTextSize,
                                modifier = Modifier.weight(widthColumn1),
                                overflow = TextOverflow.Visible,
                                style = styleDataLight,
                                text = "НадВреднНаПроизв"
                            )
                            Box(
                                modifier = Modifier.weight(widthColumn2),
                            )
                            AutoSizeText(
                                maxTextSize = maxTextSize,
                                modifier = Modifier.weight(widthColumn3),
                                overflow = TextOverflow.Visible,
                                style = styleDataLight,
                                alignment = Alignment.Center,
                                text = uiState.harmfulnessSurchargePercent.str()
                            )
                            AutoSizeText(
                                maxTextSize = maxTextSize,
                                modifier = Modifier.weight(widthColumn4),
                                overflow = TextOverflow.Visible,
                                style = styleDataLight,
                                alignment = Alignment.CenterEnd,
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
                            AutoSizeText(
                                maxTextSize = maxTextSize,
                                modifier = Modifier.weight(widthColumn1),
                                overflow = TextOverflow.Visible,
                                style = styleDataLight,
                                text = "ДоплРабЛокомБрДлинПоез"
                            )
                            AutoSizeText(
                                maxTextSize = maxTextSize,
                                modifier = Modifier.weight(widthColumn2),
                                overflow = TextOverflow.Visible,
                                style = styleDataLight,
                                alignment = Alignment.Center,
                                text = ConverterLongToTime.getTimeInHourDecimal(uiState.surchargeLongDistanceTrainsHours)
                            )
                            AutoSizeText(
                                maxTextSize = maxTextSize,
                                modifier = Modifier.weight(widthColumn3),
                                overflow = TextOverflow.Visible,
                                style = styleDataLight,
                                alignment = Alignment.Center,
                                text = uiState.surchargeLongDistanceTrainsPercent.str()
                            )
                            AutoSizeText(
                                maxTextSize = maxTextSize,
                                modifier = Modifier.weight(widthColumn4),
                                overflow = TextOverflow.Visible,
                                style = styleDataLight,
                                alignment = Alignment.CenterEnd,
                                text = uiState.surchargeLongDistanceTrainsMoney.str2decimalSign()
                            )
                        }
                    }
                }
            }
            itemsIndexed(
                items = uiState.surchargeHeavyTransMoney,
            ) { index, item ->
                item?.let { value ->
                    if (value != 0.0) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = verticalPaddingSmall)
                        ) {
                            AutoSizeText(
                                maxTextSize = maxTextSize,
                                modifier = Modifier.weight(widthColumn1),
                                overflow = TextOverflow.Visible,
                                style = styleDataLight,
                                text = "ДоплРабЛокомБрТяжПоез"
                            )
                            AutoSizeText(
                                maxTextSize = maxTextSize,
                                modifier = Modifier.weight(widthColumn2),
                                overflow = TextOverflow.Visible,
                                style = styleDataLight,
                                alignment = Alignment.Center,
                                text = ConverterLongToTime.getTimeInHourDecimal(uiState.surchargeHeavyTransHour[index])
                            )
                            AutoSizeText(
                                maxTextSize = maxTextSize,
                                modifier = Modifier.weight(widthColumn3),
                                overflow = TextOverflow.Visible,
                                style = styleDataLight,
                                alignment = Alignment.Center,
                                text = uiState.surchargeHeavyTransPercent[index] ?: ""
                            )
                            AutoSizeText(
                                maxTextSize = maxTextSize,
                                modifier = Modifier.weight(widthColumn4),
                                overflow = TextOverflow.Visible,
                                style = styleDataLight,
                                alignment = Alignment.CenterEnd,
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
                            AutoSizeText(
                                maxTextSize = maxTextSize,
                                modifier = Modifier.weight(widthColumn1),
                                overflow = TextOverflow.Visible,
                                style = styleDataLight,
                                text = "ДоплРабЛокомБрУдлинУчОбсл"
                            )
                            AutoSizeText(
                                maxTextSize = maxTextSize,
                                modifier = Modifier.weight(widthColumn2),
                                overflow = TextOverflow.Visible,
                                style = styleDataLight,
                                alignment = Alignment.Center,
                                text = ConverterLongToTime.getTimeInHourDecimal(uiState.surchargeExtendedServicePhaseHour[index])
                            )
                            AutoSizeText(
                                maxTextSize = maxTextSize,
                                modifier = Modifier.weight(widthColumn3),
                                overflow = TextOverflow.Visible,
                                style = styleDataLight,
                                alignment = Alignment.Center,
                                text = uiState.surchargeExtendedServicePhasePercent[index] ?: ""
                            )
                            AutoSizeText(
                                maxTextSize = maxTextSize,
                                modifier = Modifier.weight(widthColumn4),
                                overflow = TextOverflow.Visible,
                                style = styleDataLight,
                                alignment = Alignment.CenterEnd,
                                text = uiState.surchargeExtendedServicePhaseMoney[index].str2decimalSign()
                            )
                        }
                    }
                }
            }
            item {
                uiState.districtSurchargeMoney?.let { value ->
                    if (value != 0.0 && !value.isNaN()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = verticalPaddingSmall)
                        ) {
                            AutoSizeText(
                                maxTextSize = maxTextSize,
                                modifier = Modifier.weight(widthColumn1),
                                overflow = TextOverflow.Visible,
                                style = styleDataLight,
                                text = "РайонНадб"
                            )
                            Box(
                                modifier = Modifier.weight(widthColumn2),
                            )
                            AutoSizeText(
                                maxTextSize = maxTextSize,
                                modifier = Modifier.weight(widthColumn3),
                                overflow = TextOverflow.Visible,
                                style = styleDataLight,
                                alignment = Alignment.Center,
                                text = uiState.districtSurchargeCoefficient.str()
                            )
                            AutoSizeText(
                                maxTextSize = maxTextSize,
                                modifier = Modifier.weight(widthColumn4),
                                overflow = TextOverflow.Visible,
                                style = styleDataLight,
                                alignment = Alignment.CenterEnd,
                                text = uiState.districtSurchargeMoney.str2decimalSign()
                            )
                        }
                    }
                }
            }
            item {
                uiState.nordicSurchargeMoney?.let { value ->
                    if (value != 0.0 && !value.isNaN()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = verticalPaddingSmall)
                        ) {
                            AutoSizeText(
                                maxTextSize = maxTextSize,
                                modifier = Modifier.weight(widthColumn1),
                                overflow = TextOverflow.Visible,
                                style = styleDataLight,
                                text = "СевернНадб"
                            )
                            Box(
                                modifier = Modifier.weight(widthColumn2),
                            )
                            AutoSizeText(
                                maxTextSize = maxTextSize,
                                modifier = Modifier.weight(widthColumn3),
                                overflow = TextOverflow.Visible,
                                style = styleDataLight,
                                alignment = Alignment.Center,
                                text = uiState.nordicSurchargePercent.str()
                            )
                            AutoSizeText(
                                maxTextSize = maxTextSize,
                                modifier = Modifier.weight(widthColumn4),
                                overflow = TextOverflow.Visible,
                                style = styleDataLight,
                                alignment = Alignment.CenterEnd,
                                text = uiState.nordicSurchargeMoney.str2decimalSign()
                            )
                        }
                    }
                }
            }
            item {
                uiState.averagePaymentMoney?.let { value ->
                    if (value != 0.0) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = verticalPaddingSmall)
                        ) {
                            AutoSizeText(
                                maxTextSize = maxTextSize,
                                modifier = Modifier.weight(widthColumn1),
                                overflow = TextOverflow.Visible,
                                style = styleDataLight,
                                text = "ОплПоСреднему"
                            )
                            AutoSizeText(
                                maxTextSize = maxTextSize,
                                modifier = Modifier.weight(widthColumn2),
                                overflow = TextOverflow.Visible,
                                style = styleDataLight,
                                alignment = Alignment.Center,
                                text = String.format(
                                    "%.2f",
                                    uiState.averagePaymentHours?.toDouble() ?: 0.0
                                )
                            )
                            Box(
                                modifier = Modifier.weight(widthColumn3),
                            )
                            AutoSizeText(
                                maxTextSize = maxTextSize,
                                modifier = Modifier.weight(widthColumn4),
                                overflow = TextOverflow.Visible,
                                style = styleDataLight,
                                alignment = Alignment.CenterEnd,
                                text = String.format("%.2f", uiState.averagePaymentMoney)
                            )
                        }
                    }
                }
            }
            item {
                uiState.otherSurchargeMoney?.let { value ->
                    if (value != 0.0) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = verticalPaddingSmall)
                        ) {
                            AutoSizeText(
                                maxTextSize = maxTextSize,
                                modifier = Modifier.weight(widthColumn1),
                                overflow = TextOverflow.Visible,
                                style = styleDataLight,
                                text = "ПрочиеНачисления"
                            )
                            Box(
                                modifier = Modifier.weight(widthColumn2),
                            )
                            AutoSizeText(
                                maxTextSize = maxTextSize,
                                modifier = Modifier.weight(widthColumn3),
                                overflow = TextOverflow.Visible,
                                style = styleDataLight,
                                alignment = Alignment.Center,
                                text = uiState.otherSurchargePercent.str()
                            )
                            AutoSizeText(
                                maxTextSize = maxTextSize,
                                modifier = Modifier.weight(widthColumn4),
                                overflow = TextOverflow.Visible,
                                style = styleDataLight,
                                alignment = Alignment.CenterEnd,
                                text = uiState.otherSurchargeMoney.str2decimalSign()
                            )
                        }
                    }
                }
            }
            item {
                uiState.totalChargedMoney?.let { value ->
                    if (value != 0.0 && !value.isNaN()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = verticalPaddingSmall),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            AutoSizeText(
                                maxTextSize = maxTextSize,
                                overflow = TextOverflow.Visible,
                                style = styleHint,
                                text = "Всего начислено"
                            )
                            AutoSizeText(
                                maxTextSize = maxTextSize,
                                overflow = TextOverflow.Visible,
                                style = styleDataLight,
                                text = uiState.totalChargedMoney.str2decimalSign()
                            )
                        }
                    }
                }
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = verticalPaddingLarge),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    AutoSizeText(
                        maxTextSize = maxTextSize,
                        overflow = TextOverflow.Visible,
                        style = styleHint,
                        text = "Вид удержания"
                    )
                    AutoSizeText(
                        maxTextSize = maxTextSize,
                        overflow = TextOverflow.Visible,
                        style = styleHint,
                        text = "Сумма"
                    )
                }
            }
            item {
                uiState.retentionNdfl?.let { value ->
                    if (value != 0.0 && !value.isNaN()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = verticalPaddingSmall),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            AutoSizeText(
                                maxTextSize = maxTextSize,
                                overflow = TextOverflow.Visible,
                                style = styleDataLight,
                                text = "НалогНаДохФизЛицаУдер%"
                            )
                            AutoSizeText(
                                maxTextSize = maxTextSize,
                                overflow = TextOverflow.Visible,
                                style = styleDataLight,
                                text = uiState.retentionNdfl.str2decimalSign()
                            )
                        }
                    }
                }
            }
            item {
                uiState.unionistsRetention?.let { value ->
                    if (value != 0.0 && !value.isNaN()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = verticalPaddingSmall),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            AutoSizeText(
                                maxTextSize = maxTextSize,
                                overflow = TextOverflow.Visible,
                                style = styleDataLight,
                                text = "ПрофсоюзПервыйКредитор"
                            )
                            AutoSizeText(
                                maxTextSize = maxTextSize,
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
                    if (value != 0.0 && !value.isNaN()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = verticalPaddingSmall),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            AutoSizeText(
                                maxTextSize = maxTextSize,
                                overflow = TextOverflow.Visible,
                                style = styleDataLight,
                                text = "Прочие удержания"
                            )
                            AutoSizeText(
                                maxTextSize = maxTextSize,
                                overflow = TextOverflow.Visible,
                                style = styleDataLight,
                                text = uiState.otherRetention.str2decimalSign()
                            )
                        }
                    }
                }
            }
            item {
                uiState.totalRetention?.let { value ->
                    if (value != 0.0 && !value.isNaN()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = verticalPaddingSmall),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            AutoSizeText(
                                maxTextSize = maxTextSize,
                                overflow = TextOverflow.Visible,
                                style = styleHint,
                                text = "Всего удержано"
                            )
                            AutoSizeText(
                                maxTextSize = maxTextSize,
                                overflow = TextOverflow.Visible,
                                style = styleDataLight,
                                alignment = Alignment.Center,
                                text = uiState.totalRetention.str2decimalSign()
                            )
                        }
                    }
                }
            }
            item {
                uiState.toBeCredited?.let { value ->
                    if (value != 0.0 && !value.isNaN()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = verticalPaddingLarge),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            AutoSizeText(
                                maxTextSize = maxTextSize,
                                overflow = TextOverflow.Visible,
                                style = styleHint,
                                text = "К выдаче"
                            )
                            AutoSizeText(
                                maxTextSize = maxTextSize,
                                overflow = TextOverflow.Visible,
                                style = styleDataLight.copy(fontWeight = FontWeight.Medium),
                                alignment = Alignment.Center,
                                text = uiState.toBeCredited.str2decimalSign()
                            )
                        }
                    }
                }
            }
        }
    }
    }
}