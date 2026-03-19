package com.z_company.route.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.z_company.core.ResultState
import com.z_company.core.ui.component.AsyncDataValue
import com.z_company.core.ui.component.CustomSnackBar
import com.z_company.core.ui.component.DateRangePickerBottomSheet
import com.z_company.core.ui.component.customDatePicker.noRippleEffect
import com.z_company.core.ui.theme.Shapes
import com.z_company.core.util.MonthFullText.getMonthFullText
import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.entities.setting.SurchargeExtendedServicePhase
import com.z_company.domain.entities.setting.SurchargeHeavyTrains
import com.z_company.domain.entities.setting.SurchargeLongTrains
import com.z_company.route.component.AnimationDialog
import com.z_company.route.component.OutlinedTextFieldApp
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
    surchargeHeavyTrainsState: SnapshotStateList<SurchargeHeavyTrains>,
    addSurchargeHeavyTran: () -> Unit,
    setSurchargeHeavyTrainPercent: (Int, String) -> Unit,
    setSurchargeHeavyTrainWeight: (Int, String) -> Unit,
    onSurchargeHeavyTrainDismissed: (Int) -> Unit,
    surchargeLongTrainsState: SnapshotStateList<SurchargeLongTrains>,
    addSurchargeLongTrain: () -> Unit,
    setSurchargeLongTrainPercent: (Int, String) -> Unit,
    setSurchargeLongTrainLength: (Int, String) -> Unit,
    onSurchargeLongTrainDismissed: (Int) -> Unit,
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
    setDateNewTariffRate: (Calendar) -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val hintStyle = MaterialTheme.typography.bodyMedium
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val paddingLarge = 6.dp
    val paddingSmall = 6.dp

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

        // изменить !!!!!
        DateRangePickerBottomSheet(
            onDateRangeSelected = { list ->
                if (list.isNotEmpty()) {
                    setDateNewTariffRate(list.first())
                }
                isShowSetDateTariffRateDialog = false
            },
            onDismiss = {
                isShowSetDateTariffRateDialog = false
            },
            title = "Дата начала действия нового тарифа",
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
        )
        {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(color = MaterialTheme.colorScheme.secondary, shape = Shapes.medium)
                    .padding(horizontal = 16.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Изменилась тарифная ставка",
                    overflow = TextOverflow.Visible,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = "Для какого месяца сохранить тариф?",
                    overflow = TextOverflow.Visible,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium,
                )

                Text(
                    modifier = Modifier.noRippleEffect {
                        isShowSetDateTariffRateDialog = true
                    },
                    text = "Новый тариф начнет действовать с $currentDateSetTariffRate ${
                        getMonthFullText(
                            currentMonthOfYear?.month
                        )
                    } ${currentMonthOfYear?.year.toString()}",
                    overflow = TextOverflow.Visible,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        shape = Shapes.medium,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        ),
                        border = BorderStroke(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.primary
                        ),
                        elevation = ButtonDefaults.elevatedButtonElevation(defaultElevation = 2.dp),
                        onClick = saveOnlyMonthTariffRate
                    ) {
                        Text(
                            text = "Только для этого",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        shape = Shapes.medium,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        ),
                        border = BorderStroke(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.primary
                        ),
                        elevation = ButtonDefaults.elevatedButtonElevation(defaultElevation = 2.dp),
                        onClick = saveTariffRateCurrentAndNextMonth
                    ) {
                        Text(
                            text = "Для этого и следующих",
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2,
                            overflow = TextOverflow.Visible,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Button(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 24.dp),
                        shape = Shapes.medium,
                        elevation = ButtonDefaults.elevatedButtonElevation(defaultElevation = 2.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        ),
                        border = BorderStroke(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.error
                        ),
                        onClick = onHideDialogChangeTariffRate,
                    ) {
                        Text(
                            style = MaterialTheme.typography.bodySmall,
                            text = "Отмена",
                            color = MaterialTheme.colorScheme.primary
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
                        text = "Зарплата",
                        style = MaterialTheme.typography.titleMedium,
                        color = primaryColor
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(com.z_company.core.R.drawable.ic_arrow_back),
                            contentDescription = "Назад",
                            tint = primaryColor
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
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
                    style = MaterialTheme.typography.titleSmall,
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
                    Text(
                        text = "Тарифная ставка, руб.",
                        overflow = TextOverflow.Visible,
                        style = hintStyle,
                        color = primaryColor,
                        maxLines = 2,
                    )
                    Row(
                        modifier = Modifier.clickable(
                            onClick = {
                                isShowSetDateTariffRateDialog = true
                            }
                        ),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "на $dateSetTariffRate",
                            overflow = TextOverflow.Visible,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )

                        Text(
                            text = getMonthFullText(currentMonthOfYear?.month),
                            overflow = TextOverflow.Visible,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )

                        Text(
                            text = currentMonthOfYear?.year.toString(),
                            overflow = TextOverflow.Visible,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }

                    AsyncDataValue(resultState = tariffRateValueState) { tariffRateValue ->
                        tariffRateValue?.let {
                            OutlinedTextFieldApp(
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
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Decimal
                                )
                            )
                        }
                    }

                    if (currentMonthOfYear?.dateSetTariffRate != null) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth(),
                        ) {
                            Text(
                                text = "Тарифная ставка, руб. ",
                                overflow = TextOverflow.Visible,
                                style = hintStyle,
                                maxLines = 2,
                                color = primaryColor
                            )
                            Row(
                                modifier = Modifier.clickable(
                                    onClick = {
                                        isShowSetDateTariffRateDialog = true
                                    }
                                ),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "до $dateSetTariffRate",
                                    overflow = TextOverflow.Visible,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.tertiary
                                )

                                Text(
                                    text = getMonthFullText(currentMonthOfYear.month),
                                    overflow = TextOverflow.Visible,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.tertiary
                                )

                                Text(
                                    text = currentMonthOfYear.year.toString(),
                                    overflow = TextOverflow.Visible,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                            }
                        }
                        AsyncDataValue(resultState = oldTariffRateValueState) { oldTariffRateValue ->
                            oldTariffRateValue?.let {
                                OutlinedTextFieldApp(
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
                    Text(
                        text = "Средний час, руб.",
                        overflow = TextOverflow.Visible,
                        style = hintStyle,
                        maxLines = 2,
                        color = primaryColor
                    )
                    AsyncDataValue(resultState = uiState.averagePaymentHour) { averagePaymentHourValue ->
                        averagePaymentHourValue?.let {
                            OutlinedTextFieldApp(
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
                    Text(
                        text = "Зональная надбавка, %",
                        overflow = TextOverflow.Visible,
                        maxLines = 2,
                        style = hintStyle,
                        color = primaryColor
                    )
                    AsyncDataValue(resultState = zonalSurchargeValueState) { zonalSurchargeValue ->
                        zonalSurchargeValue?.let {
                            OutlinedTextFieldApp(
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
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Decimal
                                ),
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
                    Text(
                        text = "Доплаты за класс и права, %",
                        overflow = TextOverflow.Visible,
                        maxLines = 2,
                        style = hintStyle,
                        color = primaryColor
                    )
                    AsyncDataValue(resultState = surchargeQualificationClassValueState) { surchargeQualificationClassValue ->
                        surchargeQualificationClassValue?.let {
                            OutlinedTextFieldApp(
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
                    Text(
                        text = "Работа в одно лицо (грузовой), %",
                        overflow = TextOverflow.Visible,
                        maxLines = 2,
                        style = hintStyle,
                        color = primaryColor
                    )
                    AsyncDataValue(resultState = onePersonOperationPercent) { onePersonPercent ->
                        onePersonPercent?.let {
                            OutlinedTextFieldApp(
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
                    Text(
                        text = "Работа в одно лицо (пассажирский), %",
                        overflow = TextOverflow.Visible,
                        maxLines = 2,
                        style = hintStyle,
                        color = primaryColor
                    )
                    AsyncDataValue(resultState = onePersonOperationPassengerTrainPercent) { onePersonOperationPassengerTrainPercent ->
                        onePersonOperationPassengerTrainPercent?.let {
                            OutlinedTextFieldApp(
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
                    Text(
                        text = "Доплата за вредность, %",
                        overflow = TextOverflow.Visible,
                        maxLines = 2,
                        style = hintStyle,
                        color = primaryColor
                    )
                    AsyncDataValue(resultState = harmfulnessPercentState) { harmfulnessPercent ->
                        harmfulnessPercent?.let {
                            OutlinedTextFieldApp(
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
                    Text(
                        text = "Северная надбавка, %",
                        overflow = TextOverflow.Visible,
                        maxLines = 2,
                        style = hintStyle,
                        color = primaryColor
                    )
                    AsyncDataValue(resultState = uiState.nordicCoefficient) { nordicCoefficient ->
                        nordicCoefficient?.let {
                            OutlinedTextFieldApp(
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
                    Text(
                        text = "Районный коэффициент, %",
                        overflow = TextOverflow.Visible,
                        maxLines = 2,
                        style = hintStyle,
                        color = primaryColor
                    )

                    AsyncDataValue(resultState = uiState.districtCoefficient) { districtCoefficient ->
                        districtCoefficient?.let {
                            OutlinedTextFieldApp(
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
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Decimal
                                )
                            )
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        modifier = Modifier.weight(1f),
                        text = "Доплата за тяж. поезда",
                        overflow = TextOverflow.Visible,
                        style = hintStyle,
                        color = primaryColor,
                        maxLines = 2
                    )

                    Text(
                        modifier = Modifier.noRippleEffect {
                            addSurchargeHeavyTran()
                        },
                        text = "Добавить",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )

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
                                .padding(top = 6.dp)
                                .fillMaxSize()
                                .background(color = color, shape = Shapes.medium),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            Icon(
                                modifier = Modifier.padding(end = 16.dp),
                                painter = painterResource(com.z_company.route.R.drawable.delete_24px),
                                tint = MaterialTheme.colorScheme.onError,
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
                        OutlinedTextFieldApp(
                            modifier = Modifier.weight(1f),
                            value = item.weight,
                            onValueChange = { value ->
                                setSurchargeHeavyTrainWeight(index, value)
                            },
                            singleLine = true,
                            suffix = {
                                Text(
                                    text = "т.",
                                    style = hintStyle
                                )
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal
                            )
                        )
                        OutlinedTextFieldApp(
                            modifier = Modifier.weight(1f),
                            value = item.percentSurcharge,
                            onValueChange = { value ->
                                setSurchargeHeavyTrainPercent(index, value)
                            },
                            singleLine = true,
                            suffix = {
                                Text(
                                    text = "%",
                                    style = hintStyle
                                )
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal
                            )
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        modifier = Modifier.weight(1f),
                        text = "Доплата за длинносост. поезда",
                        overflow = TextOverflow.Visible,
                        style = hintStyle,
                        color = primaryColor,
                        maxLines = 2
                    )

                    Text(
                        modifier = Modifier.noRippleEffect {
                            addSurchargeLongTrain()
                        },
                        text = "Добавить",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )

                }
            }

            itemsIndexed(
                items = surchargeLongTrainsState,
                key = { _, item -> item.id }
            ) { index, item ->
                val dismissState = rememberSwipeToDismissBoxState()
                if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
                    onSurchargeLongTrainDismissed(index)
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
                                .padding(top = 6.dp)
                                .fillMaxSize()
                                .background(color = color, shape = Shapes.medium),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            Icon(
                                modifier = Modifier.padding(end = 16.dp),
                                painter = painterResource(com.z_company.route.R.drawable.delete_24px),
                                tint = MaterialTheme.colorScheme.onError,
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
                        OutlinedTextFieldApp(
                            modifier = Modifier.weight(1f),
                            value = item.conditionalLength,
                            onValueChange = { value ->
                                setSurchargeLongTrainLength(index, value)
                            },
                            singleLine = true,
                            suffix = {
                                Text(
                                    text = "ваг.",
                                    style = hintStyle
                                )
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal
                            )
                        )
                        OutlinedTextFieldApp(
                            modifier = Modifier.weight(1f),
                            value = item.percentSurcharge,
                            onValueChange = { value ->
                                setSurchargeLongTrainPercent(index, value)
                            },
                            singleLine = true,
                            suffix = {
                                Text(
                                    text = "%",
                                    style = hintStyle
                                )
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal
                            )
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        modifier = Modifier
                            .weight(1f),
                        text = "Доплата за удлиненное плечо",
                        overflow = TextOverflow.Ellipsis,
                        style = hintStyle,
                        maxLines = 2,
                        color = primaryColor
                    )
                    Text(
                        modifier = Modifier.noRippleEffect {
                            addServicePhase()
                        },
                        text = "Добавить",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
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
                                painter = painterResource(com.z_company.route.R.drawable.delete_24px),
                                tint = MaterialTheme.colorScheme.onError,
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
                        OutlinedTextFieldApp(
                            modifier = Modifier.weight(1f),
                            value = item.distance,
                            onValueChange = { value ->
                                setSurchargeExtendedServicePhaseDistance(index, value)
                            },
                            singleLine = true,
                            suffix = {
                                Text(
                                    text = "км",
                                    style = hintStyle
                                )
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal
                            )
                        )

                        OutlinedTextFieldApp(
                            modifier = Modifier.weight(1f),
                            value = item.percentSurcharge,
                            onValueChange = { value ->
                                setSurchargeExtendedServicePhasePercent(index, value)
                            },
                            singleLine = true,
                            suffix = {
                                Text(
                                    text = "%",
                                    style = hintStyle
                                )
                            },
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
                        .padding(top = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(paddingSmall)
                ) {
                    Text(
                        text = "Другие надбавки, %",
                        overflow = TextOverflow.Visible,
                        style = hintStyle,
                        color = primaryColor,
                        maxLines = 2
                    )
                    AsyncDataValue(resultState = uiState.otherSurchargeState) { otherSurcharge ->
                        otherSurcharge?.let {
                            OutlinedTextFieldApp(
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
                    style = MaterialTheme.typography.titleSmall,
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
                    Text(
                        text = "Подоходный налог, %",
                        overflow = TextOverflow.Visible,
                        style = hintStyle,
                        maxLines = 2,
                        color = primaryColor
                    )
                    AsyncDataValue(resultState = ndflValueState) { ndflValue ->
                        ndflValue?.let {
                            OutlinedTextFieldApp(
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
                    Text(
                        text = "Профсоюз, %",
                        overflow = TextOverflow.Visible,
                        style = hintStyle,
                        maxLines = 2,
                        color = primaryColor
                    )
                    AsyncDataValue(resultState = unionistsRetentionState) { unionistsRetention ->
                        unionistsRetention?.let {
                            OutlinedTextFieldApp(
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
                    Text(
                        text = "Прочие удержания, %",
                        overflow = TextOverflow.Visible,
                        style = hintStyle,
                        maxLines = 2,
                        color = primaryColor
                    )
                    AsyncDataValue(resultState = otherRetentionValueState) { otherRetentionValue ->
                        otherRetentionValue?.let {
                            OutlinedTextFieldApp(
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