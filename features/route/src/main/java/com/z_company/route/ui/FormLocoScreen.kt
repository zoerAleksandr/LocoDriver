package com.z_company.route.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTimePickerState
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
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.z_company.core.ResultState
import com.z_company.core.ui.component.AsyncData
import com.z_company.core.ui.theme.Shapes
import com.z_company.core.ui.theme.custom.AppTypography
import com.z_company.core.util.DateAndTimeConverter
import com.z_company.core.util.LocoTypeHelper.converterLocoTypeToString
import com.z_company.domain.entities.route.LocoType
import com.z_company.domain.entities.route.Locomotive
import com.z_company.domain.util.CalculationEnergy
import com.z_company.domain.util.str
import com.z_company.core.R as CoreR
import com.z_company.route.component.BottomShadow
import com.z_company.route.component.CustomDatePickerDialog
import com.z_company.route.component.DieselSectionItem
import com.z_company.core.ui.component.TimePickerDialog
import com.z_company.core.ui.component.CustomSnackBar
import com.z_company.route.extention.isScrollInInitialState
import com.z_company.route.viewmodel.LocoFormUiState
import java.util.Calendar
import com.z_company.domain.util.*
import com.z_company.route.component.ConfirmExitDialog
import com.z_company.route.component.ElectricSectionItem
import com.z_company.route.component.rememberDatePickerStateInLocale
import com.z_company.route.viewmodel.DieselSectionFormState
import com.z_company.route.viewmodel.DieselSectionType
import com.z_company.route.viewmodel.ElectricSectionFormState
import com.z_company.route.viewmodel.ElectricSectionType
import kotlinx.coroutines.launch
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormLocoScreen(
    currentLoco: Locomotive?,
    dieselSectionListState: SnapshotStateList<DieselSectionFormState>?,
    electricSectionListState: SnapshotStateList<ElectricSectionFormState>?,
    onBackPressed: () -> Unit,
    onSaveClick: () -> Unit,
    onLocoSaved: () -> Unit,
    formUiState: LocoFormUiState,
    resetSaveState: () -> Unit,
    onNumberChanged: (String) -> Unit,
    onSeriesChanged: (String) -> Unit,
    onChangedTypeLoco: (Int) -> Unit,
    onStartAcceptedTimeChanged: (Long?) -> Unit,
    onEndAcceptedTimeChanged: (Long?) -> Unit,
    onStartDeliveryTimeChanged: (Long?) -> Unit,
    onEndDeliveryTimeChanged: (Long?) -> Unit,
    onFuelAcceptedChanged: (Int, String?) -> Unit,
    onFuelDeliveredChanged: (Int, String?) -> Unit,
    onDeleteSectionDiesel: (DieselSectionFormState) -> Unit,
    addingSectionDiesel: () -> Unit,
    focusChangedDieselSection: (Int, DieselSectionType) -> Unit,
    onEnergyAcceptedChanged: (Int, String?) -> Unit,
    onEnergyDeliveryChanged: (Int, String?) -> Unit,
    onRecoveryAcceptedChanged: (Int, String?) -> Unit,
    onRecoveryDeliveryChanged: (Int, String?) -> Unit,
    onDeleteSectionElectric: (ElectricSectionFormState) -> Unit,
    addingSectionElectric: () -> Unit,
    focusChangedElectricSection: (Int, ElectricSectionType) -> Unit,
    onExpandStateElectricSection: (Int, Boolean) -> Unit,
    onRefuelValueChanged: (Int, String?) -> Unit,
    onCoefficientValueChanged: (Int, String?) -> Unit,
    exitScreen: () -> Unit,
    changeShowConfirmExitDialog: (Boolean) -> Unit,
    exitWithoutSave: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val hintStyle = AppTypography.getType().titleLarge
        .copy(
            fontSize = 18.sp,
            fontWeight = FontWeight.Light
        )
    val titleStyle = AppTypography.getType().headlineMedium.copy(fontWeight = FontWeight.Light)
    Scaffold(
        modifier = Modifier
            .fillMaxWidth(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Локомотив",
                        style = titleStyle
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { onBackPressed() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Default.ArrowBack,
                            contentDescription = "Назад"
                        )
                    }
                },
                actions = {
                    AsyncData(
                        resultState = formUiState.saveLocoState,
                        loadingContent = {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        },
                        errorContent = {}
                    ) {
                        TextButton(
                            modifier = Modifier
                                .padding(end = 16.dp),
                            enabled = formUiState.changesHaveState,
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
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState) { snackBarData ->
                CustomSnackBar(snackBarData = snackBarData)
            }
        }
    ) { paddingValues ->
        if (formUiState.saveLocoState is ResultState.Error) {
            LaunchedEffect(Unit) {
                scope.launch {
                    snackbarHostState.showSnackbar("Ошибка: ${formUiState.saveLocoState.entity.message}")
                }
                resetSaveState()
            }
        }
        if (formUiState.exitFromScreen) {
            LaunchedEffect(Unit) {
                exitScreen()
            }
        }
        Box(modifier = Modifier.padding(paddingValues)) {
            AsyncData(resultState = formUiState.locoDetailState) {
                currentLoco?.let { locomotive ->
                    if (formUiState.saveLocoState is ResultState.Success) {
                        LaunchedEffect(formUiState.saveLocoState) {
                            onLocoSaved()
                        }
                    } else {
                        LocoFormScreenContent(
                            locomotive = locomotive,
                            dieselSectionListState = dieselSectionListState,
                            electricSectionListState = electricSectionListState,
                            onNumberChanged = onNumberChanged,
                            onSeriesChanged = onSeriesChanged,
                            onTypeLocoChanged = onChangedTypeLoco,
                            onStartAcceptedTimeChanged = onStartAcceptedTimeChanged,
                            onEndAcceptedTimeChanged = onEndAcceptedTimeChanged,
                            onStartDeliveryTimeChanged = onStartDeliveryTimeChanged,
                            onEndDeliveryTimeChanged = onEndDeliveryTimeChanged,
                            onFuelAcceptedChanged = onFuelAcceptedChanged,
                            onFuelDeliveredChanged = onFuelDeliveredChanged,
                            onDeleteSectionDiesel = onDeleteSectionDiesel,
                            addingSectionDiesel = addingSectionDiesel,
                            focusChangedDieselSection = focusChangedDieselSection,
                            onEnergyAcceptedChanged = onEnergyAcceptedChanged,
                            onEnergyDeliveryChanged = onEnergyDeliveryChanged,
                            onRecoveryAcceptedChanged = onRecoveryAcceptedChanged,
                            onRecoveryDeliveryChanged = onRecoveryDeliveryChanged,
                            onDeleteSectionElectric = onDeleteSectionElectric,
                            addingSectionElectric = addingSectionElectric,
                            focusChangedElectricSection = focusChangedElectricSection,
                            onExpandStateElectricSection = onExpandStateElectricSection,
                            onRefuelValueChanged = onRefuelValueChanged,
                            onCoefficientValueChanged = onCoefficientValueChanged,
                            showConfirmExitDialog = formUiState.confirmExitDialogShow,
                            changeShowConfirmExitDialog = changeShowConfirmExitDialog,
                            exitWithoutSave = exitWithoutSave,
                            onSaveClick = onSaveClick
                        )
                    }

                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LocoFormScreenContent(
    locomotive: Locomotive,
    dieselSectionListState: SnapshotStateList<DieselSectionFormState>?,
    electricSectionListState: SnapshotStateList<ElectricSectionFormState>?,
    onNumberChanged: (String) -> Unit,
    onSeriesChanged: (String) -> Unit,
    onTypeLocoChanged: (Int) -> Unit,
    onStartAcceptedTimeChanged: (Long?) -> Unit,
    onEndAcceptedTimeChanged: (Long?) -> Unit,
    onStartDeliveryTimeChanged: (Long?) -> Unit,
    onEndDeliveryTimeChanged: (Long?) -> Unit,
    onFuelAcceptedChanged: (Int, String?) -> Unit,
    onFuelDeliveredChanged: (Int, String?) -> Unit,
    onDeleteSectionDiesel: (DieselSectionFormState) -> Unit,
    addingSectionDiesel: () -> Unit,
    focusChangedDieselSection: (Int, DieselSectionType) -> Unit,
    onEnergyAcceptedChanged: (Int, String?) -> Unit,
    onEnergyDeliveryChanged: (Int, String?) -> Unit,
    onRecoveryAcceptedChanged: (Int, String?) -> Unit,
    onRecoveryDeliveryChanged: (Int, String?) -> Unit,
    onDeleteSectionElectric: (ElectricSectionFormState) -> Unit,
    addingSectionElectric: () -> Unit,
    focusChangedElectricSection: (Int, ElectricSectionType) -> Unit,
    onExpandStateElectricSection: (Int, Boolean) -> Unit,
    onRefuelValueChanged: (Int, String?) -> Unit,
    onCoefficientValueChanged: (Int, String?) -> Unit,
    showConfirmExitDialog: Boolean,
    changeShowConfirmExitDialog: (Boolean) -> Unit,
    exitWithoutSave: () -> Unit,
    onSaveClick: () -> Unit,
) {
    val scrollState = rememberLazyListState()
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    val dataTextStyle = AppTypography.getType().titleLarge.copy(fontWeight = FontWeight.Light)
    val subTitleTextStyle = AppTypography.getType().titleLarge
        .copy(
            fontSize = 18.sp,
            fontWeight = FontWeight.Normal
        )
    val hintStyle = AppTypography.getType().titleLarge
        .copy(
            fontSize = 18.sp,
            fontWeight = FontWeight.Light
        )

    if (showConfirmExitDialog) {
        ConfirmExitDialog(
            showExitConfirmDialog = changeShowConfirmExitDialog,
            onSaveClick = onSaveClick,
            exitWithoutSave = exitWithoutSave
        )
    }

    AnimatedVisibility(
        modifier = Modifier
            .zIndex(1f),
        visible = !scrollState.isScrollInInitialState(),
        enter = fadeIn(animationSpec = tween(durationMillis = 300)),
        exit = fadeOut(animationSpec = tween(durationMillis = 300))
    ) {
        BottomShadow()
    }
    LazyColumn(
        state = scrollState,
        horizontalAlignment = Alignment.End,
        contentPadding = PaddingValues(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                OutlinedTextField(
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .weight(1f),
                    value = locomotive.series ?: "",
                    textStyle = dataTextStyle,
                    placeholder = {
                        Text(text = "Серия", style = dataTextStyle)
                    },
                    onValueChange = {
                        onSeriesChanged(it)
                    },
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = {
                            focusManager.moveFocus(FocusDirection.Right)
                        }
                    ),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent
                    ),
                    shape = Shapes.medium,
                )
                OutlinedTextField(
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .weight(1f),
                    value = locomotive.number ?: "",
                    textStyle = dataTextStyle,
                    placeholder = {
                        Text(
                            text = "Номер",
                            style = dataTextStyle
                        )
                    },
                    onValueChange = { onNumberChanged(it) },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                        }
                    ),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent
                    ),
                    shape = Shapes.medium,
                )
            }
        }
        item {
            val types = LocoType.values().map {
                converterLocoTypeToString(it)
            }

            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
                    .background(MaterialTheme.colorScheme.surface, shape = Shapes.medium)
            ) {
                types.forEachIndexed { index, type ->
                    SegmentedButton(
                        modifier = Modifier.padding(4.dp),
                        selected = index == locomotive.type.ordinal,
                        onClick = { onTypeLocoChanged(index) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = types.size,
                            baseShape = Shapes.medium
                        ),
                        border = BorderStroke(color = Color.Transparent, width = 0.dp)
                    ) {
                        Text(
                            text = type,
                            style = dataTextStyle.copy(
                                color = if (index == locomotive.type.ordinal) {
                                    Color.Unspecified
                                } else {
                                    Color.Unspecified.copy(alpha = 0.9f)
                                }
                            ),
                        )
                    }
                }
            }
        }
        item {
            val startAcceptedTime = locomotive.timeStartOfAcceptance
            val endAcceptedTime = locomotive.timeEndOfAcceptance

            val startAcceptedCalendar by remember {
                mutableStateOf(Calendar.getInstance().also { calendar ->
                    startAcceptedTime?.let { millis ->
                        calendar.timeInMillis = millis
                    }
                })
            }

            val startAcceptedTimePickerState = rememberTimePickerState(
                initialHour = startAcceptedCalendar.get(Calendar.HOUR_OF_DAY),
                initialMinute = startAcceptedCalendar.get(Calendar.MINUTE),
                is24Hour = true
            )

            val startAcceptedDatePickerState =
                rememberDatePickerStateInLocale(
                    initialSelectedDateMillis = startAcceptedTime
                        ?: startAcceptedCalendar.timeInMillis
                )

            var showStartAcceptedTimePicker by remember {
                mutableStateOf(false)
            }

            var showStartAcceptedDatePicker by remember {
                mutableStateOf(false)
            }

            if (showStartAcceptedDatePicker) {
                CustomDatePickerDialog(
                    datePickerState = startAcceptedDatePickerState,
                    onDismissRequest = {
                        showStartAcceptedDatePicker = false
                    },
                    onConfirmRequest = {
                        showStartAcceptedDatePicker = false
                        showStartAcceptedTimePicker = true
                        startAcceptedCalendar.timeInMillis =
                            startAcceptedDatePickerState.selectedDateMillis!!
                    }
                )
            }

            if (showStartAcceptedTimePicker) {
                TimePickerDialog(
                    timePickerState = startAcceptedTimePickerState,
                    onDismissRequest = { showStartAcceptedTimePicker = false },
                    onConfirmRequest = {
                        showStartAcceptedTimePicker = false
                        startAcceptedCalendar.set(
                            Calendar.HOUR_OF_DAY,
                            startAcceptedTimePickerState.hour
                        )
                        startAcceptedCalendar.set(
                            Calendar.MINUTE,
                            startAcceptedTimePickerState.minute
                        )
                        onStartAcceptedTimeChanged(startAcceptedCalendar.timeInMillis)
                    }
                )
            }

            val endAcceptedCalendar by remember {
                mutableStateOf(Calendar.getInstance().also { calendar ->
                    endAcceptedTime?.let { millis ->
                        calendar.timeInMillis = millis
                    }
                })
            }

            val endAcceptedTimePickerState = rememberTimePickerState(
                initialHour = endAcceptedCalendar.get(Calendar.HOUR_OF_DAY),
                initialMinute = endAcceptedCalendar.get(Calendar.MINUTE),
                is24Hour = true
            )

            val endAcceptedDatePickerState =
                rememberDatePickerStateInLocale(
                    initialSelectedDateMillis = endAcceptedTime ?: endAcceptedCalendar.timeInMillis
                )

            var showEndAcceptedTimePicker by remember {
                mutableStateOf(false)
            }

            var showEndAcceptedDatePicker by remember {
                mutableStateOf(false)
            }

            if (showEndAcceptedDatePicker) {
                CustomDatePickerDialog(
                    datePickerState = endAcceptedDatePickerState,
                    onDismissRequest = {
                        showEndAcceptedDatePicker = false
                    },
                    onConfirmRequest = {
                        showEndAcceptedDatePicker = false
                        showEndAcceptedTimePicker = true
                        endAcceptedCalendar.timeInMillis =
                            endAcceptedDatePickerState.selectedDateMillis!!
                    }
                )
            }

            if (showEndAcceptedTimePicker) {
                TimePickerDialog(
                    timePickerState = endAcceptedTimePickerState,
                    onDismissRequest = { showEndAcceptedTimePicker = false },
                    onConfirmRequest = {
                        showEndAcceptedTimePicker = false
                        endAcceptedCalendar.set(
                            Calendar.HOUR_OF_DAY,
                            endAcceptedTimePickerState.hour
                        )
                        endAcceptedCalendar.set(
                            Calendar.MINUTE,
                            endAcceptedTimePickerState.minute
                        )
                        onEndAcceptedTimeChanged(endAcceptedCalendar.timeInMillis)
                    }
                )
            }

            Column(
                modifier = Modifier
                    .padding(top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Приемка",
                    style = subTitleTextStyle
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                color = MaterialTheme.colorScheme.surface,
                                shape = Shapes.medium
                            )
                            .clickable {
                                showStartAcceptedDatePicker = true
                            }
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val dateStartText = startAcceptedTime?.let {
                            DateAndTimeConverter.getDateFromDateLong(startAcceptedTime)
                        } ?: "Начало"
                        val timeStartText = startAcceptedTime?.let {
                            DateAndTimeConverter.getTimeFromDateLong(startAcceptedTime)
                        } ?: ""
                        Text(
                            text = dateStartText,
                            style = dataTextStyle,
                        )

                        Text(
                            text = " $timeStartText",
                            style = dataTextStyle,
                        )
                    }

                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                color = MaterialTheme.colorScheme.surface,
                                shape = Shapes.medium
                            )
                            .clickable {
                                showEndAcceptedDatePicker = true
                            }
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val dateEndText = endAcceptedTime?.let {
                            DateAndTimeConverter.getDateFromDateLong(endAcceptedTime)
                        } ?: "Окончание"
                        val timeEndText = endAcceptedTime?.let {
                            DateAndTimeConverter.getTimeFromDateLong(endAcceptedTime)
                        } ?: ""
                        Text(
                            text = dateEndText,
                            style = dataTextStyle,
                        )

                        Text(
                            text = " $timeEndText",
                            style = dataTextStyle,
                        )
                    }
                }
            }
        }
        item {
            val startDeliveryTime = locomotive.timeStartOfDelivery
            val endDeliveryTime = locomotive.timeEndOfDelivery
            val startDeliveryCalendar by remember {
                mutableStateOf(Calendar.getInstance().also { calendar ->
                    startDeliveryTime?.let { millis ->
                        calendar.timeInMillis = millis
                    }
                })
            }

            val startDeliveryTimePickerState = rememberTimePickerState(
                initialHour = startDeliveryCalendar.get(Calendar.HOUR_OF_DAY),
                initialMinute = startDeliveryCalendar.get(Calendar.MINUTE),
                is24Hour = true
            )

            val startDeliveryDatePickerState =
                rememberDatePickerStateInLocale(
                    initialSelectedDateMillis = startDeliveryTime
                        ?: startDeliveryCalendar.timeInMillis
                )

            var showStartDeliveryTimePicker by remember {
                mutableStateOf(false)
            }

            var showStartDeliveryDatePicker by remember {
                mutableStateOf(false)
            }

            if (showStartDeliveryDatePicker) {
                CustomDatePickerDialog(
                    datePickerState = startDeliveryDatePickerState,
                    onDismissRequest = {
                        showStartDeliveryDatePicker = false
                    },
                    onConfirmRequest = {
                        showStartDeliveryDatePicker = false
                        showStartDeliveryTimePicker = true
                        startDeliveryCalendar.timeInMillis =
                            startDeliveryDatePickerState.selectedDateMillis!!
                    }
                )
            }

            if (showStartDeliveryTimePicker) {
                TimePickerDialog(
                    timePickerState = startDeliveryTimePickerState,
                    onDismissRequest = { showStartDeliveryTimePicker = false },
                    onConfirmRequest = {
                        showStartDeliveryTimePicker = false
                        startDeliveryCalendar.set(
                            Calendar.HOUR_OF_DAY,
                            startDeliveryTimePickerState.hour
                        )
                        startDeliveryCalendar.set(
                            Calendar.MINUTE,
                            startDeliveryTimePickerState.minute
                        )
                        onStartDeliveryTimeChanged(startDeliveryCalendar.timeInMillis)
                    }
                )
            }

            val endDeliveryCalendar by remember {
                mutableStateOf(Calendar.getInstance().also { calendar ->
                    endDeliveryTime?.let { millis ->
                        calendar.timeInMillis = millis
                    }
                })
            }

            val endDeliveryTimePickerState = rememberTimePickerState(
                initialHour = endDeliveryCalendar.get(Calendar.HOUR_OF_DAY),
                initialMinute = endDeliveryCalendar.get(Calendar.MINUTE),
                is24Hour = true
            )

            val endDeliveryDatePickerState =
                rememberDatePickerStateInLocale(
                    initialSelectedDateMillis = endDeliveryTime ?: endDeliveryCalendar.timeInMillis
                )

            var showEndDeliveryTimePicker by remember {
                mutableStateOf(false)
            }

            var showEndDeliveryDatePicker by remember {
                mutableStateOf(false)
            }

            if (showEndDeliveryDatePicker) {
                CustomDatePickerDialog(
                    datePickerState = endDeliveryDatePickerState,
                    onDismissRequest = {
                        showEndDeliveryDatePicker = false
                    },
                    onConfirmRequest = {
                        showEndDeliveryDatePicker = false
                        showEndDeliveryTimePicker = true
                        endDeliveryCalendar.timeInMillis =
                            endDeliveryDatePickerState.selectedDateMillis!!
                    }
                )
            }

            if (showEndDeliveryTimePicker) {
                TimePickerDialog(
                    timePickerState = endDeliveryTimePickerState,
                    onDismissRequest = { showEndDeliveryTimePicker = false },
                    onConfirmRequest = {
                        showEndDeliveryTimePicker = false
                        endDeliveryCalendar.set(
                            Calendar.HOUR_OF_DAY,
                            endDeliveryTimePickerState.hour
                        )
                        endDeliveryCalendar.set(
                            Calendar.MINUTE,
                            endDeliveryTimePickerState.minute
                        )
                        onEndDeliveryTimeChanged(endDeliveryCalendar.timeInMillis)
                    }
                )
            }

            Column(
                modifier = Modifier
                    .padding(top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Сдача",
                    style = subTitleTextStyle
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                color = MaterialTheme.colorScheme.surface,
                                shape = Shapes.medium
                            )
                            .clickable {
                                showStartDeliveryDatePicker = true
                            }
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val dateStartText = startDeliveryTime?.let {
                            DateAndTimeConverter.getDateFromDateLong(startDeliveryTime)
                        } ?: "Начало"
                        val timeStartText = startDeliveryTime?.let {
                            DateAndTimeConverter.getTimeFromDateLong(startDeliveryTime)
                        } ?: ""

                        Text(
                            text = dateStartText,
                            style = dataTextStyle,
                        )

                        Text(
                            text = " $timeStartText",
                            style = dataTextStyle,
                        )
                    }

                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                color = MaterialTheme.colorScheme.surface,
                                shape = Shapes.medium
                            )
                            .clickable {
                                showEndDeliveryDatePicker = true
                            }
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val dateEndText = endDeliveryTime?.let {
                            DateAndTimeConverter.getDateFromDateLong(endDeliveryTime)
                        } ?: "Окончание"
                        val timeEndText = endDeliveryTime?.let {
                            DateAndTimeConverter.getTimeFromDateLong(endDeliveryTime)
                        } ?: ""
                        Text(
                            text = dateEndText,
                            style = dataTextStyle,
                        )

                        Text(
                            text = " $timeEndText",
                            style = dataTextStyle,
                        )
                    }
                }
            }
        }
        when (locomotive.type.name) {
            LocoType.DIESEL.name -> {
                dieselSectionListState?.let {
                    itemsIndexed(
                        items = dieselSectionListState,
                        key = { _, item -> item.sectionId }
                    ) { index, item ->
                        Column(horizontalAlignment = Alignment.End) {
                            Spacer(modifier = Modifier.height(dimensionResource(id = CoreR.dimen.secondary_spacing)))
                            DieselSectionItem(
                                item = item,
                                index = index,
                                onFuelAcceptedChanged = onFuelAcceptedChanged,
                                onFuelDeliveredChanged = onFuelDeliveredChanged,
                                onDeleteItem = onDeleteSectionDiesel,
                                focusChangedDieselSection = focusChangedDieselSection,
                                onRefuelValueChanged = onRefuelValueChanged,
                                onCoefficientValueChanged = onCoefficientValueChanged
                            )

                            if (index == dieselSectionListState.lastIndex && index > 0) {
                                var overResult: Double? = null
                                dieselSectionListState.forEach {
                                    val accepted = it.accepted.data?.toDoubleOrNull()
                                    val delivery = it.delivery.data?.toDoubleOrNull()
                                    val refuel = it.refuel.data?.toDoubleOrNull()
                                    val result = CalculationEnergy.getTotalFuelConsumption(
                                        accepted, delivery, refuel
                                    )
                                    overResult += result
                                }
                                overResult?.let {
                                    Text(
                                        modifier = Modifier.padding(top = 8.dp),
                                        text = "Всего расход = ${maskInLiter(it.str())}",
                                        style = hintStyle
                                    )
                                }
                            }
                        }
                    }
                }
            }

            LocoType.ELECTRIC.name -> {
                electricSectionListState?.let {
                    itemsIndexed(
                        items = electricSectionListState,
                        key = { _, item -> item.sectionId }
                    ) { index, item ->
                        Column(horizontalAlignment = Alignment.End) {
                            Spacer(modifier = Modifier.height(dimensionResource(id = CoreR.dimen.secondary_spacing)))
                            ElectricSectionItem(
                                index = index,
                                item = item,
                                onDeleteItem = onDeleteSectionElectric,
                                onEnergyAcceptedChanged = onEnergyAcceptedChanged,
                                onEnergyDeliveryChanged = onEnergyDeliveryChanged,
                                onRecoveryAcceptedChanged = onRecoveryAcceptedChanged,
                                onRecoveryDeliveryChanged = onRecoveryDeliveryChanged,
                                focusChangedElectricSection = focusChangedElectricSection,
                                onExpandStateChanged = onExpandStateElectricSection
                            )

                            if (index == electricSectionListState.lastIndex && index > 0) {
                                var overResult: BigDecimal? = null
                                var overRecovery: BigDecimal? = null

                                electricSectionListState.forEach {
                                    val accepted = it.accepted.data?.toBigDecimalOrNull()
                                    val delivery = it.delivery.data?.toBigDecimalOrNull()
                                    val acceptedRecovery =
                                        it.recoveryAccepted.data?.toBigDecimalOrNull()
                                    val deliveryRecovery =
                                        it.recoveryDelivery.data?.toBigDecimalOrNull()

                                    val result = delivery - accepted
                                    val resultRecovery = deliveryRecovery - acceptedRecovery
                                    overResult += result
                                    overRecovery += resultRecovery
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Column(
                                    horizontalAlignment = Alignment.End,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    overResult?.let {
                                        Text(
                                            text = "Всего расход = ${it.toPlainString()}",
                                            style = hintStyle,
                                        )
                                    }
                                    overRecovery?.let {
                                        Text(
                                            text = "Всего рекуперация = ${it.toPlainString()}",
                                            style = hintStyle,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
                shape = Shapes.medium,
                onClick = {
                    when (locomotive.type.name) {
                        LocoType.DIESEL.name -> addingSectionDiesel()
                        LocoType.ELECTRIC.name -> addingSectionElectric()
                    }
                    scope.launch {
                        val countItems = scrollState.layoutInfo.totalItemsCount
                        scrollState.animateScrollToItem(countItems)
                    }
                }
            ) {
                Text(
                    text = "Добавить секцию",
                    style = subTitleTextStyle
                )

            }
        }
        item { Spacer(modifier = Modifier.height(20.dp)) }
    }
}