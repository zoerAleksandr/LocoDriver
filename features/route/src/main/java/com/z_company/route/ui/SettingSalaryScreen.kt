package com.z_company.route.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.z_company.core.ResultState
import com.z_company.core.ui.component.AsyncDataValue
import com.z_company.core.ui.component.AutoSizeText
import com.z_company.core.ui.component.CustomSnackBar
import com.z_company.core.ui.component.rememberDatePickerStateInLocale
import com.z_company.core.ui.theme.Shapes
import com.z_company.core.ui.theme.custom.AppTypography
import com.z_company.core.util.DateAndTimeConverter.getMonthFullText
import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.entities.SurchargeExtendedServicePhase
import com.z_company.domain.entities.SurchargeHeavyTrains
import com.z_company.route.component.AnimationDialog
import com.z_company.route.component.CustomDatePickerDialog
import com.z_company.route.viewmodel.SettingSalaryUIState
import kotlinx.coroutines.launch
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingSalaryScreen(
    onBack: () -> Unit,
    onSaveClick: () -> Unit,
    isEnableSaveButton: Boolean,
    uiState: SettingSalaryUIState,
    saveSettingState: ResultState<Unit>?,
    resetSaveState: () -> Unit,
    tariffRateValueState: ResultState<String>,
    setTariffRate: (String) -> Unit,
    oldTariffRateValueState: ResultState<String>,
    setOldTariffRate: (String) -> Unit,
    isErrorInputTariffRate: Boolean,
    setAveragePaymentHour: (String) -> Unit,
    setNordicCoefficient: (String) -> Unit,
    setDistrictCoefficient: (String) -> Unit,
    zonalSurchargeValueState: ResultState<String>,
    setZonalSurcharge: (String) -> Unit,
    isErrorInputZonalSurcharge: Boolean,
    surchargeQualificationClassValueState: ResultState<String>,
    setSurchargeQualificationClass: (String) -> Unit,
    isErrorInputSurchargeQualificationClass: Boolean,
    surchargeExtendedServicePhaseValueState: SnapshotStateList<SurchargeExtendedServicePhase>,
    addServicePhase: () -> Unit,
    setSurchargeExtendedServicePhaseDistance: (Int, String) -> Unit,
    setSurchargeExtendedServicePhasePercent: (Int, String) -> Unit,
    onePersonOperationPercent: ResultState<String>,
    setOnePersonOperationPercent: (String) -> Unit,
    isErrorInputOnePersonOperation: Boolean,
    onePersonOperationPassengerTrainPercent: ResultState<String>,
    setOnePersonOperationPassengerTrainPercent: (String) -> Unit,
    isErrorInputOnePersonOperationPassengerTrain: Boolean,
    harmfulnessPercentState: ResultState<String>,
    setHarmfulnessPercent: (String) -> Unit,
    isErrorInputHarmfulness: Boolean,
    surchargeLongDistanceTrainState: ResultState<String>,
    setSurchargeLongTrain: (String) -> Unit,
    isErrorInputSurchargeLongDistance: Boolean,
    lengthLongDistanceTrainState: ResultState<String>,
    setLengthLongDistanceTrain: (String) -> Unit,
    isErrorInputLengthLongDistance: Boolean,
    surchargeHeavyTrainsState: SnapshotStateList<SurchargeHeavyTrains>,
    addSurchargeHeavyTran: () -> Unit,
    setSurchargeHeavyTrainPercent: (Int, String) -> Unit,
    setSurchargeHeavyTrainWeight: (Int, String) -> Unit,
    onSurchargeHeavyTrainDismissed: (Int) -> Unit,
    ndflValueState: ResultState<String>,
    setNDFL: (String) -> Unit,
    isErrorInputNdfl: Boolean,
    unionistsRetentionState: ResultState<String>,
    setUnionistsRetention: (String) -> Unit,
    isErrorInputUnionistsRetention: Boolean,
    otherRetentionValueState: ResultState<String>,
    setOtherRetention: (String) -> Unit,
    isErrorInputOtherRetention: Boolean,
    onServicePhaseDismissed: (Int) -> Unit,
    isShowDialogChangeTariffRate: Boolean,
    onHideDialogChangeTariffRate: () -> Unit,
    saveOnlyMonthTariffRate: () -> Unit,
    saveTariffRateCurrentAndNextMonth: () -> Unit,
    setOtherSurcharge: (String) -> Unit,
    currentMonthOfYear: MonthOfYear?,
    setDateNewTariffRate: (Int) -> Unit
) {
    val styleDataLight = AppTypography.getType().titleLarge.copy(fontWeight = FontWeight.Light)
    val titleStyle = AppTypography.getType().titleLarge.copy(fontWeight = FontWeight.Medium)
    val styleDataMedium = AppTypography.getType().titleMedium.copy(fontWeight = FontWeight.Normal)
    val hintStyle = AppTypography.getType().titleLarge
        .copy(
            fontSize = 18.sp,
            fontWeight = FontWeight.Light
        )
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val paddingLarge = 12.dp
    val paddingSmall = 6.dp

    val maxTextSize = 18.sp

    if (saveSettingState is ResultState.Success) {
        LaunchedEffect(Unit) {
            onBack()
        }
    }
    if (saveSettingState is ResultState.Error) {
        LaunchedEffect(Unit) {
            scope.launch {
                snackbarHostState.showSnackbar("Ошибка: ${saveSettingState.entity.message}")
            }
            resetSaveState()
        }
    }

    var isShowSetDateTariffRateDialog by remember { mutableStateOf(false) }

    AnimationDialog(
        showDialog = isShowSetDateTariffRateDialog,
        onDismissRequest = { isShowSetDateTariffRateDialog = false }
    ) {

        val currentCalendar = Calendar.getInstance()
        currentMonthOfYear?.let {
            currentCalendar.apply {
                set(Calendar.YEAR, it.year)
                set(Calendar.MONTH, it.month)
                set(Calendar.DAY_OF_MONTH, 1)
            }
            it.dateSetTariffRate?.let { date ->
                currentCalendar.set(Calendar.DAY_OF_MONTH, date.dateNewRate)
            }
        }

        val datePickerState = rememberDatePickerStateInLocale(currentCalendar.timeInMillis)

        CustomDatePickerDialog(
            datePickerState = datePickerState,
            title = {
                Text(
                    modifier = Modifier.padding(16.dp),
                    text = "Дата начала действия нового тарифа",
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                    style = hintStyle
                )
            },
            onDismissRequest = {
                isShowSetDateTariffRateDialog = false
            },
            onConfirmRequest = {
                val date = Calendar.getInstance().also {
                    it.timeInMillis = datePickerState.selectedDateMillis!!
                }.get(Calendar.DAY_OF_MONTH)

                setDateNewTariffRate(date)
                isShowSetDateTariffRateDialog = false
            }
        )
    }

    AnimationDialog(
        showDialog = isShowDialogChangeTariffRate,
        onDismissRequest = onHideDialogChangeTariffRate
    ) {
        val currentDateSetTariffRate = currentMonthOfYear?.dateSetTariffRate?.dateNewRate ?: 1

        Box(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(color = MaterialTheme.colorScheme.surface, shape = Shapes.medium)
                    .padding(horizontal = 16.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                AutoSizeText(
                    maxTextSize = maxTextSize,
                    modifier = Modifier,
                    text = "Изменилась тарифная ставка",
                    overflow = TextOverflow.Visible,
                    style = styleDataMedium.copy(color = MaterialTheme.colorScheme.primary),
                    alignment = Alignment.CenterEnd
                )
                AutoSizeText(
                    maxTextSize = maxTextSize,
                    text = "Для какого месяца сохранить тариф?",
                    overflow = TextOverflow.Visible,
                    style = styleDataLight.copy(color = MaterialTheme.colorScheme.primary)
                )
                TextButton(
                    onClick = { isShowSetDateTariffRateDialog = true }
                ) {
                    AutoSizeText(
                        maxTextSize = maxTextSize,
                        text = "Новый тариф начнет действовать с $currentDateSetTariffRate ${getMonthFullText(currentMonthOfYear?.month)} ${currentMonthOfYear?.year.toString()}",
                        overflow = TextOverflow.Visible,
                        style = styleDataLight.copy(color = MaterialTheme.colorScheme.tertiary)
                    )
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        shape = Shapes.medium,
                        onClick = saveOnlyMonthTariffRate
                    ) {
                        AutoSizeText(
                            maxTextSize = maxTextSize,
                            modifier = Modifier,
                            style = styleDataMedium,
                            text = "Только для этого"
                        )
                    }
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        shape = Shapes.medium,
                        onClick = saveTariffRateCurrentAndNextMonth
                    ) {
                        AutoSizeText(
                            maxTextSize = maxTextSize,
                            modifier = Modifier,
                            style = styleDataMedium,
                            text = "Для этого и следующих"
                        )
                    }
                    Button(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 24.dp),
                        shape = Shapes.medium,
                        onClick = onHideDialogChangeTariffRate
                    ) {
                        AutoSizeText(
                            maxTextSize = maxTextSize,
                            modifier = Modifier,
                            style = styleDataMedium,
                            text = "Отмена"
                        )
                    }
                }
            }
        }
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = "Тарифная ставка и коэффициенты",
                        overflow = TextOverflow.Visible,
                        maxLines = 2,
                        style = titleStyle
                    )
                },
                navigationIcon = {
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
                    AsyncDataValue(resultState = saveSettingState) {
                        TextButton(
                            modifier = Modifier
                                .padding(end = 16.dp),
                            enabled = isEnableSaveButton,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                contentColor = MaterialTheme.colorScheme.tertiary
                            ),
                            onClick = { onSaveClick() }
                        ) {
                            Text(text = "Сохранить", style = hintStyle)
                        }
                    }
                }
            )
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState) { snackBarData ->
                CustomSnackBar(snackBarData = snackBarData)
            }
        }
    ) { paddingValue ->
        LazyColumn(
            modifier = Modifier
                .padding(paddingValue)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            item {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = paddingLarge),
                    text = "Начисления",
                    overflow = TextOverflow.Visible,
                    style = styleDataLight,
                    textAlign = TextAlign.End
                )
            }
            item {
                var dateSetTariffRate = 1
                currentMonthOfYear?.dateSetTariffRate?.let {
                    dateSetTariffRate = it.dateNewRate
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = paddingLarge),
                    verticalArrangement = Arrangement.spacedBy(paddingSmall)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        AutoSizeText(
                            maxTextSize = maxTextSize,
                            text = "Тарифная ставка, руб. ",
                            overflow = TextOverflow.Visible,
                            style = styleDataMedium
                        )
                        Row(
                            modifier = Modifier.clickable(
                                onClick = {
                                    isShowSetDateTariffRateDialog = true
                                }
                            ),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            AutoSizeText(
                                maxTextSize = maxTextSize,
                                text = "на $dateSetTariffRate",
                                overflow = TextOverflow.Visible,
                                style = styleDataMedium,
                                color = MaterialTheme.colorScheme.tertiary
                            )

                            AutoSizeText(
                                maxTextSize = maxTextSize,
                                text = getMonthFullText(currentMonthOfYear?.month),
                                overflow = TextOverflow.Visible,
                                style = styleDataMedium,
                                color = MaterialTheme.colorScheme.tertiary
                            )

                            AutoSizeText(
                                maxTextSize = maxTextSize,
                                text = currentMonthOfYear?.year.toString(),
                                overflow = TextOverflow.Visible,
                                style = styleDataMedium,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                    AsyncDataValue(resultState = tariffRateValueState) { tariffRateValue ->
                        tariffRateValue?.let {
                            OutlinedTextField(
                                modifier = Modifier
                                    .fillMaxWidth(),
                                value = tariffRateValue,
                                onValueChange = { value ->
                                    setTariffRate(value)
                                },
                                isError = isErrorInputTariffRate,
                                supportingText = {
                                    if (isErrorInputTariffRate) {
                                        Text(text = "Некорректные данные")
                                    }
                                },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                    focusedBorderColor = Color.Transparent,
                                    unfocusedBorderColor = Color.Transparent
                                ),
                                shape = Shapes.medium,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Decimal
                                )
                            )
                        }
                    }

                    if (currentMonthOfYear?.dateSetTariffRate != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            AutoSizeText(
                                maxTextSize = maxTextSize,
                                text = "Тарифная ставка, руб. ",
                                overflow = TextOverflow.Visible,
                                style = styleDataMedium
                            )
                            Row(
                                modifier = Modifier.clickable(
                                    onClick = {
                                        isShowSetDateTariffRateDialog = true
                                    }
                                ),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                AutoSizeText(
                                    maxTextSize = maxTextSize,
                                    text = "до $dateSetTariffRate",
                                    overflow = TextOverflow.Visible,
                                    style = styleDataMedium,
                                    color = MaterialTheme.colorScheme.tertiary
                                )

                                AutoSizeText(
                                    maxTextSize = maxTextSize,
                                    text = getMonthFullText(currentMonthOfYear.month),
                                    overflow = TextOverflow.Visible,
                                    style = styleDataMedium,
                                    color = MaterialTheme.colorScheme.tertiary
                                )

                                AutoSizeText(
                                    maxTextSize = maxTextSize,
                                    text = currentMonthOfYear.year.toString(),
                                    overflow = TextOverflow.Visible,
                                    style = styleDataMedium,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                            }
                        }
                        AsyncDataValue(resultState = oldTariffRateValueState) { oldTariffRateValue ->
                            oldTariffRateValue?.let {
                                OutlinedTextField(
                                    modifier = Modifier
                                        .fillMaxWidth(),
                                    value = oldTariffRateValue,
                                    onValueChange = { value ->
                                        setOldTariffRate(value)
                                    },
                                    isError = isErrorInputTariffRate,
                                    supportingText = {
                                        if (isErrorInputTariffRate) {
                                            Text(text = "Некорректные данные")
                                        }
                                    },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                                        focusedBorderColor = Color.Transparent,
                                        unfocusedBorderColor = Color.Transparent
                                    ),
                                    shape = Shapes.medium,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Decimal
                                    )
                                )
                            }
                        }
                    }
                }
            }
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = paddingLarge),
                    verticalArrangement = Arrangement.spacedBy(paddingSmall)
                ) {
                    AutoSizeText(
                        maxTextSize = maxTextSize,
                        text = "Средний час, руб.",
                        overflow = TextOverflow.Visible,
                        style = styleDataMedium
                    )
                    AsyncDataValue(resultState = uiState.averagePaymentHour) { averagePaymentHourValue ->
                        averagePaymentHourValue?.let {
                            OutlinedTextField(
                                modifier = Modifier
                                    .fillMaxWidth(),
                                value = averagePaymentHourValue,
                                onValueChange = { value ->
                                    setAveragePaymentHour(value)
                                },
                                isError = uiState.isErrorInputAveragePayment,
                                supportingText = {
                                    if (uiState.isErrorInputAveragePayment) {
                                        Text(text = "Некорректные данные")
                                    }
                                },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                    focusedBorderColor = Color.Transparent,
                                    unfocusedBorderColor = Color.Transparent
                                ),
                                shape = Shapes.medium,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Decimal
                                )
                            )
                        }
                    }
                }
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = paddingLarge),
                    verticalArrangement = Arrangement.spacedBy(paddingSmall)
                ) {
                    AutoSizeText(
                        maxTextSize = maxTextSize,
                        text = "Зональная надбавка, %",
                        overflow = TextOverflow.Visible,
                        style = styleDataMedium
                    )
                    AsyncDataValue(resultState = zonalSurchargeValueState) { zonalSurchargeValue ->
                        zonalSurchargeValue?.let {
                            OutlinedTextField(
                                modifier = Modifier
                                    .fillMaxWidth(),
                                value = zonalSurchargeValue,
                                onValueChange = { value ->
                                    setZonalSurcharge(value)
                                },
                                isError = isErrorInputZonalSurcharge,
                                supportingText = {
                                    if (isErrorInputZonalSurcharge) {
                                        Text(text = "Некорректные данные")
                                    }
                                },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                    focusedBorderColor = Color.Transparent,
                                    unfocusedBorderColor = Color.Transparent
                                ),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Decimal
                                ),
                                shape = Shapes.medium,
                            )
                        }
                    }
                }
            }
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = paddingLarge),
                    verticalArrangement = Arrangement.spacedBy(paddingSmall)
                ) {
                    AutoSizeText(
                        maxTextSize = maxTextSize,
                        text = "Доплаты за класс и права, %",
                        overflow = TextOverflow.Visible,
                        style = styleDataMedium
                    )
                    AsyncDataValue(resultState = surchargeQualificationClassValueState) { surchargeQualificationClassValue ->
                        surchargeQualificationClassValue?.let {
                            OutlinedTextField(
                                modifier = Modifier
                                    .fillMaxWidth(),
                                value = surchargeQualificationClassValue,
                                onValueChange = { value ->
                                    setSurchargeQualificationClass(value)
                                },
                                isError = isErrorInputSurchargeQualificationClass,
                                supportingText = {
                                    if (isErrorInputSurchargeQualificationClass) {
                                        Text(text = "Некорректные данные")
                                    }
                                },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                    focusedBorderColor = Color.Transparent,
                                    unfocusedBorderColor = Color.Transparent
                                ),
                                shape = Shapes.medium,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Decimal
                                )
                            )
                        }
                    }
                }
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = paddingLarge),
                    verticalArrangement = Arrangement.spacedBy(paddingSmall)
                ) {
                    AutoSizeText(
                        maxTextSize = maxTextSize,
                        text = "Работа в одно лицо, %",
                        overflow = TextOverflow.Visible,
                        style = styleDataMedium
                    )
                    AsyncDataValue(resultState = onePersonOperationPercent) { onePersonPercent ->
                        onePersonPercent?.let {
                            OutlinedTextField(
                                modifier = Modifier
                                    .fillMaxWidth(),
                                value = onePersonPercent,
                                onValueChange = { value ->
                                    setOnePersonOperationPercent(value)
                                },
                                isError = isErrorInputOnePersonOperation,
                                supportingText = {
                                    if (isErrorInputOnePersonOperation) {
                                        Text(text = "Некорректные данные")
                                    }
                                },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                    focusedBorderColor = Color.Transparent,
                                    unfocusedBorderColor = Color.Transparent
                                ),
                                shape = Shapes.medium,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Decimal
                                )
                            )
                        }
                    }
                }
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = paddingLarge),
                    verticalArrangement = Arrangement.spacedBy(paddingSmall)
                ) {
                    AutoSizeText(
                        maxTextSize = maxTextSize,
                        text = "Работа в одно лицо пассажирский, %",
                        overflow = TextOverflow.Visible,
                        style = styleDataMedium
                    )
                    AsyncDataValue(resultState = onePersonOperationPassengerTrainPercent) { onePersonOperationPassengerTrainPercent ->
                        onePersonOperationPassengerTrainPercent?.let {
                            OutlinedTextField(
                                modifier = Modifier
                                    .fillMaxWidth(),
                                value = onePersonOperationPassengerTrainPercent,
                                onValueChange = { value ->
                                    setOnePersonOperationPassengerTrainPercent(value)
                                },
                                isError = isErrorInputOnePersonOperationPassengerTrain,
                                supportingText = {
                                    if (isErrorInputOnePersonOperationPassengerTrain) {
                                        Text(text = "Некорректные данные")
                                    }
                                },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                    focusedBorderColor = Color.Transparent,
                                    unfocusedBorderColor = Color.Transparent
                                ),
                                shape = Shapes.medium,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Decimal
                                )
                            )
                        }
                    }
                }
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = paddingLarge),
                    verticalArrangement = Arrangement.spacedBy(paddingSmall)
                ) {
                    AutoSizeText(
                        maxTextSize = maxTextSize,
                        text = "Доплата за вредность, %",
                        overflow = TextOverflow.Visible,
                        style = styleDataMedium
                    )
                    AsyncDataValue(resultState = harmfulnessPercentState) { harmfulnessPercent ->
                        harmfulnessPercent?.let {
                            OutlinedTextField(
                                modifier = Modifier
                                    .fillMaxWidth(),
                                value = harmfulnessPercent,
                                onValueChange = { value ->
                                    setHarmfulnessPercent(value)
                                },
                                isError = isErrorInputHarmfulness,
                                supportingText = {
                                    if (isErrorInputHarmfulness) {
                                        Text(text = "Некорректные данные")
                                    }
                                },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                    focusedBorderColor = Color.Transparent,
                                    unfocusedBorderColor = Color.Transparent
                                ),
                                shape = Shapes.medium,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Decimal
                                )
                            )
                        }
                    }
                }
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = paddingLarge),
                    verticalArrangement = Arrangement.spacedBy(paddingSmall)
                ) {
                    AutoSizeText(
                        maxTextSize = maxTextSize,
                        text = "Северная надбавка, %",
                        overflow = TextOverflow.Visible,
                        style = styleDataMedium
                    )
                    AsyncDataValue(resultState = uiState.nordicCoefficient) { nordicCoefficient ->
                        nordicCoefficient?.let {
                            OutlinedTextField(
                                modifier = Modifier
                                    .fillMaxWidth(),
                                value = nordicCoefficient,
                                onValueChange = { value ->
                                    setNordicCoefficient(value)
                                },
                                isError = uiState.isErrorInputNordicCoefficient,
                                supportingText = {
                                    if (uiState.isErrorInputNordicCoefficient) {
                                        Text(text = "Некорректные данные")
                                    }
                                },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                    focusedBorderColor = Color.Transparent,
                                    unfocusedBorderColor = Color.Transparent
                                ),
                                shape = Shapes.medium,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Decimal
                                )
                            )
                        }
                    }
                }
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = paddingLarge),
                    verticalArrangement = Arrangement.spacedBy(paddingSmall)
                ) {
                    AutoSizeText(
                        maxTextSize = maxTextSize,
                        text = "Районный коэффициент",
                        overflow = TextOverflow.Visible,
                        style = styleDataMedium
                    )
                    AsyncDataValue(resultState = uiState.districtCoefficient) { districtCoefficient ->
                        districtCoefficient?.let {
                            OutlinedTextField(
                                modifier = Modifier
                                    .fillMaxWidth(),
                                value = districtCoefficient,
                                onValueChange = { value ->
                                    setDistrictCoefficient(value)
                                },
                                isError = uiState.isErrorInputDistrictCoefficient,
                                supportingText = {
                                    if (uiState.isErrorInputDistrictCoefficient) {
                                        Text(text = "Некорректные данные")
                                    }
                                },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                    focusedBorderColor = Color.Transparent,
                                    unfocusedBorderColor = Color.Transparent
                                ),
                                shape = Shapes.medium,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Decimal
                                )
                            )
                        }
                    }
                }
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = paddingLarge),
                    verticalArrangement = Arrangement.spacedBy(paddingSmall)
                ) {
                    AutoSizeText(
                        maxTextSize = maxTextSize,
                        text = "Доплата за длинносоставные поезда",
                        overflow = TextOverflow.Visible,
                        style = styleDataMedium
                    )

                    Row(
                        modifier = Modifier
                            .padding(top = 6.dp)
                            .background(
                                color = MaterialTheme.colorScheme.background,
                                shape = Shapes.medium
                            )
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AsyncDataValue(resultState = lengthLongDistanceTrainState) { lengthLongDistanceTrain ->
                            lengthLongDistanceTrain?.let { lengthInAxle ->
                                OutlinedTextField(
                                    modifier = Modifier.weight(1f),
                                    value = lengthInAxle,
                                    onValueChange = { value ->
                                        setLengthLongDistanceTrain(value)
                                    },
                                    singleLine = true,
                                    suffix = {
                                        Text(
                                            text = "у.д.",
                                            style = styleDataMedium
                                        )
                                    },
                                    isError = isErrorInputLengthLongDistance,
                                    supportingText = {
                                        if (isErrorInputLengthLongDistance) {
                                            Text(text = "Некорректные данные")
                                        }
                                    },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                                        focusedBorderColor = Color.Transparent,
                                        unfocusedBorderColor = Color.Transparent
                                    ),
                                    shape = Shapes.medium,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Decimal
                                    )
                                )
                            }
                        }
                        AsyncDataValue(resultState = surchargeLongDistanceTrainState) { surchargeLongDistanceTrain ->
                            surchargeLongDistanceTrain?.let { surcharge ->
                                OutlinedTextField(
                                    modifier = Modifier.weight(1f),
                                    value = surcharge,
                                    onValueChange = { value ->
                                        setSurchargeLongTrain(value)
                                    },
                                    singleLine = true,
                                    suffix = {
                                        Text(
                                            text = "%",
                                            style = styleDataMedium
                                        )
                                    },
                                    isError = isErrorInputSurchargeLongDistance,
                                    supportingText = {
                                        if (isErrorInputSurchargeLongDistance) {
                                            Text(text = "Некорректные данные")
                                        }
                                    },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                                        focusedBorderColor = Color.Transparent,
                                        unfocusedBorderColor = Color.Transparent
                                    ),
                                    shape = Shapes.medium,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Decimal
                                    )
                                )
                            }
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    AutoSizeText(
                        maxTextSize = maxTextSize,
                        text = "Доплата за тяж. поезда",
                        overflow = TextOverflow.Visible,
                        style = styleDataMedium
                    )
                    TextButton(
                        onClick = addSurchargeHeavyTran
                    ) {
                        AutoSizeText(
                            maxTextSize = maxTextSize,
                            text = "Добавить",
                            style = styleDataMedium.copy(color = MaterialTheme.colorScheme.tertiary)
                        )
                    }
                }
            }

            itemsIndexed(
                items = surchargeHeavyTrainsState,
                key = { _, item -> item.id }
            ) { index, item ->
                val dismissState = rememberSwipeToDismissBoxState()
                if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
                    onSurchargeHeavyTrainDismissed(index)
                }
                SwipeToDismissBox(
                    state = dismissState,
                    enableDismissFromStartToEnd = false,
                    backgroundContent = {
                        val color by animateColorAsState(
                            when (dismissState.targetValue) {
                                SwipeToDismissBoxValue.Settled -> Color.Transparent
                                SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.error
                                else -> Color.Transparent
                            }, label = ""
                        )
                        Box(
                            Modifier
                                .fillMaxSize()
                                .padding(top = 6.dp)
                                .background(color = color, shape = Shapes.medium),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            Icon(
                                modifier = Modifier.padding(end = 16.dp),
                                imageVector = Icons.Outlined.Delete,
                                tint = MaterialTheme.colorScheme.background,
                                contentDescription = null
                            )
                        }
                    }
                ) {
                    Row(
                        modifier = Modifier
                            .padding(top = 6.dp)
                            .background(
                                color = MaterialTheme.colorScheme.background,
                                shape = Shapes.medium
                            )
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            modifier = Modifier.weight(1f),
                            value = item.weight,
                            onValueChange = { value ->
                                setSurchargeHeavyTrainWeight(index, value)
                            },
                            singleLine = true,
                            suffix = {
                                Text(
                                    text = "т.",
                                    style = styleDataMedium
                                )
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent
                            ),
                            shape = Shapes.medium,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal
                            )
                        )
                        OutlinedTextField(
                            modifier = Modifier.weight(1f),
                            value = item.percentSurcharge,
                            onValueChange = { value ->
                                setSurchargeHeavyTrainPercent(index, value)
                            },
                            singleLine = true,
                            suffix = {
                                Text(
                                    text = "%",
                                    style = styleDataMedium
                                )
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent
                            ),
                            shape = Shapes.medium,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal
                            )
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    AutoSizeText(
                        modifier = Modifier.weight(1f).padding(top = 6.dp),
                        maxTextSize = maxTextSize,
                        text = "Доплата за удлиненное плечо",
                        overflow = TextOverflow.Ellipsis,
                        style = styleDataMedium
                    )
                    TextButton(
                        onClick = addServicePhase
                    ) {
                        AutoSizeText(
                            maxTextSize = maxTextSize,
                            text = "Добавить",
                            style = styleDataMedium.copy(color = MaterialTheme.colorScheme.tertiary)
                        )
                    }
                }
            }

            itemsIndexed(
                items = surchargeExtendedServicePhaseValueState,
                key = { _, item -> item.id }
            ) { index, item ->
                val dismissState = rememberSwipeToDismissBoxState()

                if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
                    onServicePhaseDismissed(index)
                }

                SwipeToDismissBox(
                    state = dismissState,
                    enableDismissFromStartToEnd = false,
                    backgroundContent = {
                        val color by animateColorAsState(
                            when (dismissState.targetValue) {
                                SwipeToDismissBoxValue.Settled -> Color.Transparent
                                SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.error
                                else -> Color.Transparent
                            }, label = ""
                        )
                        Box(
                            Modifier
                                .fillMaxSize()
                                .padding(top = 6.dp)
                                .background(color = color, shape = Shapes.medium),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            Icon(
                                modifier = Modifier.padding(end = 16.dp),
                                imageVector = Icons.Outlined.Delete,
                                tint = MaterialTheme.colorScheme.background,
                                contentDescription = null
                            )
                        }
                    }
                ) {
                    Row(
                        modifier = Modifier
                            .padding(top = 6.dp)
                            .background(
                                color = MaterialTheme.colorScheme.background,
                                shape = Shapes.medium
                            )
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            modifier = Modifier.weight(1f),
                            value = item.distance,
                            onValueChange = { value ->
                                setSurchargeExtendedServicePhaseDistance(index, value)
                            },
                            singleLine = true,
                            suffix = {
                                Text(
                                    text = "км",
                                    style = styleDataMedium
                                )
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent
                            ),
                            shape = Shapes.medium,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal
                            )
                        )
                        OutlinedTextField(
                            modifier = Modifier.weight(1f),
                            value = item.percentSurcharge,
                            onValueChange = { value ->
                                setSurchargeExtendedServicePhasePercent(index, value)
                            },
                            singleLine = true,
                            suffix = {
                                Text(
                                    text = "%",
                                    style = styleDataMedium
                                )
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent
                            ),
                            shape = Shapes.medium,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal
                            )
                        )
                    }
                }
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = paddingLarge),
                    verticalArrangement = Arrangement.spacedBy(paddingSmall)
                ) {
                    AutoSizeText(
                        maxTextSize = maxTextSize,
                        text = "Другие надбавки, %",
                        overflow = TextOverflow.Visible,
                        style = styleDataMedium
                    )
                    AsyncDataValue(resultState = uiState.otherSurchargeState) { otherSurcharge ->
                        otherSurcharge?.let {
                            OutlinedTextField(
                                modifier = Modifier
                                    .fillMaxWidth(),
                                value = otherSurcharge,
                                onValueChange = { value ->
                                    setOtherSurcharge(value)
                                },
                                isError = uiState.isErrorInputOtherSurcharge,
                                supportingText = {
                                    if (uiState.isErrorInputOtherSurcharge) {
                                        Text(text = "Некорректные данные")
                                    }
                                },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                    focusedBorderColor = Color.Transparent,
                                    unfocusedBorderColor = Color.Transparent
                                ),
                                shape = Shapes.medium,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Decimal
                                )
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    "Удержания",
                    overflow = TextOverflow.Visible,
                    style = styleDataLight,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = paddingLarge),
                    textAlign = TextAlign.End
                )
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = paddingLarge),
                    verticalArrangement = Arrangement.spacedBy(paddingSmall)
                ) {
                    AutoSizeText(
                        maxTextSize = maxTextSize,
                        text = "Подоходный налог, %",
                        overflow = TextOverflow.Visible,
                        style = styleDataMedium
                    )
                    AsyncDataValue(resultState = ndflValueState) { ndflValue ->
                        ndflValue?.let {
                            OutlinedTextField(
                                modifier = Modifier
                                    .fillMaxWidth(),
                                value = ndflValue,
                                onValueChange = { value ->
                                    setNDFL(value)
                                },
                                isError = isErrorInputNdfl,
                                supportingText = {
                                    if (isErrorInputNdfl) {
                                        Text(text = "Некорректные данные")
                                    }
                                },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                    focusedBorderColor = Color.Transparent,
                                    unfocusedBorderColor = Color.Transparent
                                ),
                                shape = Shapes.medium,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Decimal
                                )
                            )
                        }
                    }
                }
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = paddingLarge),
                    verticalArrangement = Arrangement.spacedBy(paddingSmall)
                ) {
                    AutoSizeText(
                        maxTextSize = maxTextSize,
                        text = "Профсоюз, %",
                        overflow = TextOverflow.Visible,
                        style = styleDataMedium
                    )
                    AsyncDataValue(resultState = unionistsRetentionState) { unionistsRetention ->
                        unionistsRetention?.let {
                            OutlinedTextField(
                                modifier = Modifier
                                    .fillMaxWidth(),
                                value = unionistsRetention,
                                onValueChange = { value ->
                                    setUnionistsRetention(value)
                                },
                                isError = isErrorInputUnionistsRetention,
                                supportingText = {
                                    if (isErrorInputUnionistsRetention) {
                                        Text(text = "Некорректные данные")
                                    }
                                },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                    focusedBorderColor = Color.Transparent,
                                    unfocusedBorderColor = Color.Transparent
                                ),
                                shape = Shapes.medium,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Decimal
                                )
                            )
                        }
                    }
                }
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = paddingLarge),
                    verticalArrangement = Arrangement.spacedBy(paddingSmall)
                ) {
                    AutoSizeText(
                        maxTextSize = maxTextSize,
                        text = "Прочие удержания, %",
                        overflow = TextOverflow.Visible,
                        style = styleDataMedium
                    )
                    AsyncDataValue(resultState = otherRetentionValueState) { otherRetentionValue ->
                        otherRetentionValue?.let {
                            OutlinedTextField(
                                modifier = Modifier
                                    .fillMaxWidth(),
                                value = otherRetentionValue,
                                onValueChange = { value ->
                                    setOtherRetention(value)
                                },
                                isError = isErrorInputOtherRetention,
                                supportingText = {
                                    if (isErrorInputOtherRetention) {
                                        Text(text = "Некорректные данные")
                                    }
                                },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                    focusedBorderColor = Color.Transparent,
                                    unfocusedBorderColor = Color.Transparent
                                ),
                                shape = Shapes.medium,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Decimal
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}